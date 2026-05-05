/**
 * Active Spark Gen 7 — Firebase Cloud Functions
 *
 * Functions:
 *   onMatchFinalized        — triggered when a match's xpAwarded is set > 0;
 *                             updates both players' leaderboard entries using
 *                             atomic transactions (no client-side race conditions).
 *
 *   resetDailyLeaderboard   — runs at 00:00 UTC every day;
 *                             archives & clears the "daily" period.
 *
 *   resetWeeklyLeaderboard  — runs at 00:00 UTC every Monday;
 *                             archives & clears the "weekly" period.
 *
 *   resetMonthlyLeaderboard — runs at 00:00 UTC on the 1st of each month;
 *                             archives & clears the "monthly" period.
 */

import * as admin from "firebase-admin";
import { onValueWritten } from "firebase-functions/v2/database";
import { onSchedule } from "firebase-functions/v2/scheduler";

admin.initializeApp();

const db = admin.database();

// ─── Types ────────────────────────────────────────────────────────────────────

interface Match {
  matchId: string;
  player1Uid: string;
  player2Uid: string;
  player1Username: string;
  player2Username: string;
  player1AvatarUrl: string;
  player2AvatarUrl: string;
  challengeId: string;
  winner: string;         // UID or "" for draw
  player1Reps: number;
  player2Reps: number;
  player1Score: number;
  player2Score: number;
  xpAwarded: number;
  status: string;
}

interface UserRecord {
  uid: string;
  username: string;
  displayName: string;
  avatarUrl: string;
  rank: string;
  xp: number;
}

interface LeaderboardEntry {
  playerUid: string;
  username: string;
  displayName: string;
  avatarUrl: string;
  playerRank: string;
  totalXp: number;
  totalWins: number;
  totalScore: number;
  period: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

const LEADERBOARD_PERIODS = ["daily", "weekly", "monthly", "all_time"] as const;
type Period = typeof LEADERBOARD_PERIODS[number];

/**
 * Atomically increments a player's leaderboard entry for a single period.
 * Uses a Firebase transaction so concurrent calls from both players never
 * clobber each other.
 */
async function upsertLeaderboardEntryAtomic(
  uid: string,
  period: Period,
  xpEarned: number,
  score: number,
  isWinner: boolean,
  userRecord: UserRecord
): Promise<void> {
  const ref = db.ref(`leaderboard/${period}/${uid}`);
  await ref.transaction((current: LeaderboardEntry | null) => {
    if (current === null) {
      // First entry for this player in this period
      return {
        playerUid: uid,
        username: userRecord.username,
        displayName: userRecord.displayName,
        avatarUrl: userRecord.avatarUrl,
        playerRank: userRecord.rank,
        totalXp: xpEarned,
        totalWins: isWinner ? 1 : 0,
        totalScore: score,
        period: period,
      } satisfies LeaderboardEntry;
    }
    // Increment existing entry
    return {
      ...current,
      // Keep display info current
      username: userRecord.username,
      displayName: userRecord.displayName,
      avatarUrl: userRecord.avatarUrl,
      playerRank: userRecord.rank,
      // Accumulate stats
      totalXp: (current.totalXp ?? 0) + xpEarned,
      totalWins: (current.totalWins ?? 0) + (isWinner ? 1 : 0),
      totalScore: (current.totalScore ?? 0) + score,
    };
  });
}

/** Fetches a user record from /users/{uid}. Returns null if not found. */
async function getUser(uid: string): Promise<UserRecord | null> {
  const snap = await db.ref(`users/${uid}`).get();
  return snap.exists() ? (snap.val() as UserRecord) : null;
}

// ─── onMatchFinalized ─────────────────────────────────────────────────────────

/**
 * Triggers whenever any field on a match document changes.
 * Runs the leaderboard upsert only when xpAwarded transitions from 0 → positive.
 * This is idempotent: if the function re-runs with the same data, nothing changes.
 */
export const onMatchFinalized = onValueWritten(
  {
    ref: "/matches/{matchId}",
    region: "asia-southeast1",   // match the RTDB region
  },
  async (event) => {
    const before = event.data.before.val() as Match | null;
    const after  = event.data.after.val()  as Match | null;

    // Only proceed when xpAwarded just became > 0 (first finalization)
    if (!after) return;
    if ((before?.xpAwarded ?? 0) > 0) return;   // already processed
    if ((after.xpAwarded ?? 0) <= 0) return;     // not yet finalized

    const {
      player1Uid, player2Uid,
      player1Score, player2Score,
      winner, xpAwarded,
    } = after;

    if (!player1Uid || !player2Uid) return;

    // Fetch both user records in parallel
    const [user1, user2] = await Promise.all([
      getUser(player1Uid),
      getUser(player2Uid),
    ]);
    if (!user1 || !user2) return;

    const p1IsWinner = winner === player1Uid;
    const p2IsWinner = winner === player2Uid;

    // Update all 4 leaderboard periods for both players in parallel
    const jobs: Promise<void>[] = [];
    for (const period of LEADERBOARD_PERIODS) {
      jobs.push(
        upsertLeaderboardEntryAtomic(
          player1Uid, period, xpAwarded, player1Score, p1IsWinner, user1
        ),
        upsertLeaderboardEntryAtomic(
          player2Uid, period, xpAwarded, player2Score, p2IsWinner, user2
        )
      );
    }
    await Promise.all(jobs);

    console.log(
      `[onMatchFinalized] match=${event.params.matchId} ` +
      `p1=${player1Uid} p2=${player2Uid} xp=${xpAwarded} winner=${winner || "draw"}`
    );
  }
);

// ─── Leaderboard period resets ────────────────────────────────────────────────

/**
 * Archives the current leaderboard period under /leaderboard_archive/{period}/{timestamp}
 * then deletes all entries from the live /leaderboard/{period} node.
 */
async function resetPeriod(period: Period): Promise<void> {
  const liveRef    = db.ref(`leaderboard/${period}`);
  const archiveRef = db.ref(`leaderboard_archive/${period}/${Date.now()}`);

  const snapshot = await liveRef.get();
  if (snapshot.exists()) {
    // Write to archive first, then delete live data
    await archiveRef.set(snapshot.val());
    await liveRef.remove();
    console.log(`[resetPeriod] Archived and reset leaderboard/${period}`);
  } else {
    console.log(`[resetPeriod] leaderboard/${period} was already empty — nothing to reset`);
  }
}

/** Runs daily at 00:00 UTC — resets the daily leaderboard. */
export const resetDailyLeaderboard = onSchedule(
  { schedule: "0 0 * * *", timeZone: "UTC", region: "asia-southeast1" },
  async () => { await resetPeriod("daily"); }
);

/** Runs every Monday at 00:00 UTC — resets the weekly leaderboard. */
export const resetWeeklyLeaderboard = onSchedule(
  { schedule: "0 0 * * 1", timeZone: "UTC", region: "asia-southeast1" },
  async () => { await resetPeriod("weekly"); }
);

/** Runs on the 1st of each month at 00:00 UTC — resets the monthly leaderboard. */
export const resetMonthlyLeaderboard = onSchedule(
  { schedule: "0 0 1 * *", timeZone: "UTC", region: "asia-southeast1" },
  async () => { await resetPeriod("monthly"); }
);
