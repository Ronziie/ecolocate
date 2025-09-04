require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const Joi = require('joi');
const db = require('./db');

const app = express();
const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'change-me';

app.use(express.json({ limit: '256kb' }));
app.use(cors());
app.use(helmet());
app.use(morgan('tiny'));

const limiter = rateLimit({ windowMs: 60 * 1000, max: 120 });
app.use(limiter);

// Auth helpers
function authenticateDevice(req, res, next) {
  const headerDeviceId = req.get('x-device-id');
  const apiKey = req.get('x-api-key');
  if (!headerDeviceId || !apiKey) return res.status(401).json({ error: 'missing headers' });
  let row = db.prepare('SELECT * FROM devices WHERE deviceId = ?').get(headerDeviceId);
  if (!row) {
    // Auto-register device on first contact
    db.prepare('INSERT INTO devices (deviceId, apiKey) VALUES (?, ?)').run(headerDeviceId, apiKey);
    row = { deviceId: headerDeviceId, apiKey };
  }
  if (row.apiKey !== apiKey) return res.status(401).json({ error: 'unauthorized' });
  req.device = { deviceId: headerDeviceId };
  return next();
}

function authenticateAdmin(req, res, next) {
  const auth = req.get('authorization');
  if (!auth || !auth.startsWith('Bearer ')) return res.status(401).json({ error: 'missing token' });
  try {
    const token = auth.slice('Bearer '.length);
    const payload = jwt.verify(token, JWT_SECRET);
    req.user = payload;
    next();
  } catch (e) {
    return res.status(401).json({ error: 'invalid token' });
  }
}

// Validation schema
const telemetrySchema = Joi.object({
  deviceId: Joi.string().required(),
  timestamp: Joi.string().isoDate().required(),
  location: Joi.object({
    latitude: Joi.number().allow(null),
    longitude: Joi.number().allow(null),
    accuracy: Joi.number().allow(null),
    altitude: Joi.number().allow(null),
    speed: Joi.number().allow(null),
    bearing: Joi.number().allow(null),
    provider: Joi.string().allow('', null)
  }).allow(null),
  battery: Joi.object({
    level: Joi.number().integer().min(0).max(100).allow(null),
    charging: Joi.boolean().allow(null),
    powerSource: Joi.string().allow('', null),
    temperature: Joi.number().allow(null)
  }).allow(null),
  network: Joi.object({
    type: Joi.string().allow('', null),
    signalStrength: Joi.number().allow(null),
    ssid: Joi.string().allow('', null),
    carrier: Joi.string().allow('', null)
  }).allow(null),
  deviceInfo: Joi.object({
    model: Joi.string().allow(''),
    osVersion: Joi.string().allow(''),
    appVersion: Joi.string().allow('')
  }).required(),
  system: Joi.object({
    uptimeSeconds: Joi.number().integer().min(0).required(),
    serviceRestartCount: Joi.number().integer().min(0).required()
  }).required()
});

// POST telemetry
app.post('/api/devices/:id/location', authenticateDevice, (req, res) => {
  const pathId = req.params.id;
  const headerDeviceId = req.device.deviceId;
  if (pathId !== headerDeviceId) return res.status(401).json({ error: 'device id mismatch' });

  const { error, value } = telemetrySchema.validate(req.body, { abortEarly: false });
  if (error) return res.status(400).json({ error: 'invalid payload', details: error.details });

  const payload = value;

  const tx = db.transaction(() => {
    // Upsert device row heartbeat
    db.prepare(
      `INSERT INTO devices (deviceId, apiKey, updatedAt) VALUES (@deviceId, @apiKey, datetime('now'))
       ON CONFLICT(deviceId) DO UPDATE SET updatedAt=datetime('now')`
    ).run({ deviceId: headerDeviceId, apiKey: req.get('x-api-key') });

    if (payload.location) {
      db.prepare(`INSERT INTO locations (deviceId, timestamp, latitude, longitude, accuracy, altitude, speed, bearing, provider)
        VALUES (@deviceId, @timestamp, @latitude, @longitude, @accuracy, @altitude, @speed, @bearing, @provider)`)
        .run({ deviceId: headerDeviceId, timestamp: payload.timestamp, ...payload.location });
    }

    const b = payload.battery || {};
    const n = payload.network || {};
    const d = payload.deviceInfo || {};
    const s = payload.system || {};
    db.prepare(`INSERT INTO telemetry (
      deviceId, timestamp, batteryLevel, charging, powerSource, temperature,
      networkType, signalStrength, ssid, carrier,
      model, osVersion, appVersion, uptimeSeconds, serviceRestartCount
    ) VALUES (@deviceId, @timestamp, @batteryLevel, @charging, @powerSource, @temperature,
      @networkType, @signalStrength, @ssid, @carrier,
      @model, @osVersion, @appVersion, @uptimeSeconds, @serviceRestartCount)`)
      .run({
        deviceId: headerDeviceId,
        timestamp: payload.timestamp,
        batteryLevel: b.level ?? null,
        charging: b.charging === undefined ? null : (b.charging ? 1 : 0),
        powerSource: b.powerSource ?? null,
        temperature: b.temperature ?? null,
        networkType: n.type ?? null,
        signalStrength: n.signalStrength ?? null,
        ssid: n.ssid ?? null,
        carrier: n.carrier ?? null,
        model: d.model ?? null,
        osVersion: d.osVersion ?? null,
        appVersion: d.appVersion ?? null,
        uptimeSeconds: s.uptimeSeconds ?? 0,
        serviceRestartCount: s.serviceRestartCount ?? 0
      });
  });

  tx();
  return res.status(200).json({ ok: true });
});

// List devices summary
app.get('/api/devices', authenticateAdmin, (req, res) => {
  const rows = db.prepare(`
    SELECT d.deviceId,
      d.updatedAt AS lastSeen,
      (
        SELECT t.batteryLevel FROM telemetry t
        WHERE t.deviceId = d.deviceId
        ORDER BY t.timestamp DESC LIMIT 1
      ) AS lastBattery
    FROM devices d
    ORDER BY d.updatedAt DESC
  `).all();

  const now = Date.now();
  const withStatus = rows.map(r => {
    const lastSeen = r.lastSeen ? new Date(r.lastSeen + 'Z').getTime() : 0;
    const online = lastSeen && (now - lastSeen) < (15 * 60 * 1000);
    return { deviceId: r.deviceId, lastSeen: r.lastSeen, lastBattery: r.lastBattery, status: online ? 'online' : 'offline' };
  });
  res.json(withStatus);
});

// Latest telemetry for device
app.get('/api/devices/:id/location/latest', authenticateAdmin, (req, res) => {
  const id = req.params.id;
  const location = db.prepare(`
    SELECT * FROM locations WHERE deviceId = ? ORDER BY timestamp DESC LIMIT 1
  `).get(id);
  const telem = db.prepare(`
    SELECT * FROM telemetry WHERE deviceId = ? ORDER BY timestamp DESC LIMIT 1
  `).get(id);
  if (!location && !telem) return res.status(404).json({ error: 'not found' });
  res.json({ location, telemetry: telem });
});

// Admin login -> JWT
app.post('/auth/login', (req, res) => {
  const { username, password } = req.body || {};
  const expectedUser = process.env.ADMIN_USERNAME || 'admin';
  const expectedPass = process.env.ADMIN_PASSWORD || 'admin';
  if (username === expectedUser && password === expectedPass) {
    const token = jwt.sign({ sub: username, role: 'admin' }, JWT_SECRET, { expiresIn: '7d' });
    return res.json({ token });
  }
  return res.status(401).json({ error: 'invalid credentials' });
});

app.get('/health', (req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`Ecolocate backend listening on :${PORT}`);
});


