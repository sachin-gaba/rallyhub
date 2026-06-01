import * as SQLite from 'expo-sqlite';

const db = SQLite.openDatabase('rallyhub.db');

/**
 * Initialises all offline cache tables.
 * Called once on app start.
 */
export function initOfflineDb(): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        // Upcoming sessions (next 4 weeks)
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS sessions (
            id TEXT PRIMARY KEY,
            club_id TEXT NOT NULL,
            schedule_name TEXT,
            date TEXT NOT NULL,
            status TEXT NOT NULL,
            booked INTEGER DEFAULT 0,
            attendee_count INTEGER DEFAULT 0,
            capacity INTEGER DEFAULT 20,
            price_credits INTEGER DEFAULT 1,
            venue TEXT,
            data TEXT,           -- full JSON blob
            synced_at TEXT
          )
        `);
        // Credit balance snapshot
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS credit_balance (
            club_id TEXT PRIMARY KEY,
            balance INTEGER NOT NULL,
            synced_at TEXT
          )
        `);
        // Announcements (last 30 days)
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS announcements (
            id TEXT PRIMARY KEY,
            club_id TEXT NOT NULL,
            body TEXT NOT NULL,
            pinned INTEGER DEFAULT 0,
            created_at TEXT,
            synced_at TEXT
          )
        `);
        // Tournament draw + matches
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS tournament_data (
            id TEXT PRIMARY KEY,
            club_id TEXT NOT NULL,
            data TEXT NOT NULL,   -- full JSON blob
            synced_at TEXT
          )
        `);
        // Sync metadata
        tx.executeSql(`
          CREATE TABLE IF NOT EXISTS sync_state (
            key TEXT PRIMARY KEY,
            last_synced_at TEXT,
            etag TEXT
          )
        `);
      },
      reject,
      () => resolve()
    );
  });
}

// ── Sessions ──────────────────────────────────────────────────────

export function cacheSessions(sessions: any[]): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        sessions.forEach((s) => {
          tx.executeSql(
            `INSERT OR REPLACE INTO sessions
               (id, club_id, schedule_name, date, status, booked, attendee_count, capacity, price_credits, venue, data, synced_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
            [s.id, s.clubId, s.scheduleName, s.date, s.status,
             s.booked ? 1 : 0, s.attendeeCount, s.capacity,
             s.priceCredits, s.venue, JSON.stringify(s), new Date().toISOString()]
          );
        });
      },
      reject,
      () => resolve()
    );
  });
}

export function getCachedSessions(clubId: string): Promise<any[]> {
  return new Promise((resolve, reject) => {
    db.transaction((tx) => {
      tx.executeSql(
        `SELECT data FROM sessions WHERE club_id = ? AND date >= date('now') ORDER BY date ASC LIMIT 28`,
        [clubId],
        (_, result) => resolve(result.rows._array.map((r) => JSON.parse(r.data))),
        (_, err) => { reject(err); return false; }
      );
    });
  });
}

// ── Credit balance ────────────────────────────────────────────────

export function cacheBalance(clubId: string, balance: number): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        tx.executeSql(
          `INSERT OR REPLACE INTO credit_balance (club_id, balance, synced_at) VALUES (?, ?, ?)`,
          [clubId, balance, new Date().toISOString()]
        );
      },
      reject,
      () => resolve()
    );
  });
}

export function getCachedBalance(clubId: string): Promise<number | null> {
  return new Promise((resolve, reject) => {
    db.transaction((tx) => {
      tx.executeSql(
        `SELECT balance FROM credit_balance WHERE club_id = ?`,
        [clubId],
        (_, result) => resolve(result.rows.length > 0 ? result.rows.item(0).balance : null),
        (_, err) => { reject(err); return false; }
      );
    });
  });
}

// ── Announcements ─────────────────────────────────────────────────

export function cacheAnnouncements(announcements: any[]): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        announcements.forEach((a) => {
          tx.executeSql(
            `INSERT OR REPLACE INTO announcements (id, club_id, body, pinned, created_at, synced_at)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [a.id, a.clubId, a.body, a.pinned ? 1 : 0, a.createdAt, new Date().toISOString()]
          );
        });
      },
      reject,
      () => resolve()
    );
  });
}

export function getCachedAnnouncements(clubId: string): Promise<any[]> {
  return new Promise((resolve, reject) => {
    db.transaction((tx) => {
      tx.executeSql(
        `SELECT * FROM announcements
         WHERE club_id = ? AND created_at >= datetime('now', '-30 days')
         ORDER BY pinned DESC, created_at DESC`,
        [clubId],
        (_, result) => resolve(result.rows._array),
        (_, err) => { reject(err); return false; }
      );
    });
  });
}

// ── Sync state ────────────────────────────────────────────────────

export function updateSyncState(key: string): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        tx.executeSql(
          `INSERT OR REPLACE INTO sync_state (key, last_synced_at) VALUES (?, ?)`,
          [key, new Date().toISOString()]
        );
      },
      reject,
      () => resolve()
    );
  });
}

export function clearOfflineCache(): Promise<void> {
  return new Promise((resolve, reject) => {
    db.transaction(
      (tx) => {
        ['sessions', 'credit_balance', 'announcements', 'tournament_data'].forEach((t) => {
          tx.executeSql(`DELETE FROM ${t}`);
        });
      },
      reject,
      () => resolve()
    );
  });
}
