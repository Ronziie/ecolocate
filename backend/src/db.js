// SQLite connection and schema migration using better-sqlite3
const Database = require('better-sqlite3');
const path = require('path');
const fs = require('fs');

const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '..', 'data');
const DB_PATH = path.join(DATA_DIR, 'ecolocate.sqlite');

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

const db = new Database(DB_PATH);
db.pragma('journal_mode = WAL');

// Run simple migrations (idempotent)
db.exec(`
CREATE TABLE IF NOT EXISTS devices (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  deviceId TEXT UNIQUE NOT NULL,
  apiKey TEXT NOT NULL,
  createdAt TEXT NOT NULL DEFAULT (datetime('now')),
  updatedAt TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS telemetry (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  deviceId TEXT NOT NULL,
  timestamp TEXT NOT NULL,
  batteryLevel INTEGER,
  charging INTEGER,
  powerSource TEXT,
  temperature REAL,
  networkType TEXT,
  signalStrength INTEGER,
  ssid TEXT,
  carrier TEXT,
  model TEXT,
  osVersion TEXT,
  appVersion TEXT,
  uptimeSeconds INTEGER,
  serviceRestartCount INTEGER,
  createdAt TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY(deviceId) REFERENCES devices(deviceId)
);

CREATE TABLE IF NOT EXISTS locations (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  deviceId TEXT NOT NULL,
  timestamp TEXT NOT NULL,
  latitude REAL,
  longitude REAL,
  accuracy REAL,
  altitude REAL,
  speed REAL,
  bearing REAL,
  provider TEXT,
  createdAt TEXT NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY(deviceId) REFERENCES devices(deviceId)
);

CREATE INDEX IF NOT EXISTS idx_locations_device_time ON locations(deviceId, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_device_time ON telemetry(deviceId, timestamp DESC);
`);

// Seed admin user and sample device if env provided
function seed() {
  const sampleDeviceId = process.env.SEED_DEVICE_ID;
  const sampleApiKey = process.env.SEED_DEVICE_API_KEY;
  if (sampleDeviceId && sampleApiKey) {
    const upsert = db.prepare(
      `INSERT INTO devices (deviceId, apiKey) VALUES (@deviceId, @apiKey)
       ON CONFLICT(deviceId) DO UPDATE SET apiKey=excluded.apiKey, updatedAt=datetime('now')`
    );
    upsert.run({ deviceId: sampleDeviceId, apiKey: sampleApiKey });
  }
}

seed();

module.exports = db;


