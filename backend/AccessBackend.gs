/** Duck Force Companion shared backend v0.5.0. API keys verify requests and are never stored. */
const DF_TOOLKIT_VERSION = '0.5.0';
const DF_FACTION_NAME = 'Duck Force';
const DF_SHEETS = Object.freeze({
  SETTINGS: 'Settings',
  RANKS: 'RankAccess',
  USERS: 'UserOverrides',
  POSITIONS: 'FactionPositions',
  NOTICES: 'Notices',
  BANKING: 'BankingRequests'
});
const DF_TOOLS = Object.freeze([
  ['ARMORY', 'Faction Armory Auditor'],
  ['TRAIN', 'Company Train Calculator']
]);
const DF_BANKING_STATUSES = Object.freeze(['PENDING', 'LIKELY_HANDLED', 'PAID', 'HANDLED', 'CANCELLED']);
const DF_LIKELY_HANDLED_THRESHOLD = 1000000;

function setupDuckForceBackend() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const props = PropertiesService.getScriptProperties();
  props.setProperty('SHEET_ID', ss.getId());
  if (!props.getProperty('LISTENER_TOKEN')) props.setProperty('LISTENER_TOKEN', Utilities.getUuid().replace(/-/g, ''));

  const settings = ensureSheet_(ss, DF_SHEETS.SETTINGS, ['key', 'value']);
  setSetting_(settings, 'faction_name', DF_FACTION_NAME);
  if (!getSetting_(settings, 'faction_id')) setSetting_(settings, 'faction_id', '0');
  if (!getSetting_(settings, 'restrict_faction')) setSetting_(settings, 'restrict_faction', 'true');
  setSetting_(settings, 'schema_version', '3');

  ensureSheet_(ss, DF_SHEETS.RANKS, ['rank_name', 'tool_id', 'allowed', 'updated_at', 'updated_by_id', 'updated_by_name']);
  ensureSheet_(ss, DF_SHEETS.USERS, ['user_id', 'tool_id', 'allowed', 'updated_at', 'updated_by_id', 'updated_by_name']);
  ensureSheet_(ss, DF_SHEETS.POSITIONS, ['faction_id', 'position_name', 'abilities_json', 'updated_at', 'updated_by_id', 'updated_by_name']);
  ensureSheet_(ss, DF_SHEETS.NOTICES, ['id', 'faction_id', 'title', 'message', 'created_at', 'expires_at', 'author_id', 'author_name', 'active']);
  ensureSheet_(ss, DF_SHEETS.BANKING, [
    'id', 'faction_id', 'requester_id', 'requester_name', 'requested_amount', 'request_mode', 'note', 'request_text',
    'source', 'fingerprint', 'message_id', 'requested_at', 'detected_at', 'status', 'likely_handled',
    'handled_at', 'handled_by_id', 'handled_by_name', 'updated_at'
  ]);

  return {
    ok: true,
    sheet_id: ss.getId(),
    schema_version: 3,
    next: 'Set Settings!faction_id to the numeric faction ID, deploy as a web app, then run getDuckForceListenerToken() for the listener token.'
  };
}

function getDuckForceListenerToken() {
  const props = PropertiesService.getScriptProperties();
  let token = props.getProperty('LISTENER_TOKEN');
  if (!token) {
    token = Utilities.getUuid().replace(/-/g, '');
    props.setProperty('LISTENER_TOKEN', token);
  }
  return token;
}

function rotateDuckForceListenerToken() {
  const token = Utilities.getUuid().replace(/-/g, '');
  PropertiesService.getScriptProperties().setProperty('LISTENER_TOKEN', token);
  return token;
}

function doGet() {
  return json_({ ok: true, app: 'Duck Force Companion Backend', version: DF_TOOLKIT_VERSION, authenticated_actions: 'POST only' });
}

function doPost(e) {
  try {
    const body = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const action = String(body.action || '');

    if (action === 'listener_event') return json_(handleListenerEvent_(body));

    const apiKey = String(body.apiKey || '').trim();
    if (!apiKey) throw new Error('API key required.');
    const user = verifyFactionUser_(apiKey);

    if (action === 'config') {
      if (isLeaderOrCoLeader_(user.position)) {
        try { syncFactionPositions_(apiKey, user); } catch (_) {}
      }
      return json_({
        ok: true,
        user: publicUser_(user),
        can_manage: isLeaderOrCoLeader_(user.position),
        can_manage_banking: canManageBankingQueue_(user),
        permissions: readPositionPermissions_(user.faction_id, user.position),
        rank_rules: readRankRules_(),
        user_overrides: readUserOverrides_(user.id),
        capabilities: { notices: true, banking: true, listener: true }
      });
    }

    if (action === 'notices') return json_({ ok: true, user: publicUser_(user), notices: readActiveNotices_(user.faction_id) });

    if (action === 'positions' || action === 'sync_positions') {
      if (!isLeaderOrCoLeader_(user.position)) throw new Error('Leader or Co-leader required.');
      const positions = syncFactionPositions_(apiKey, user);
      return json_({ ok: true, user: publicUser_(user), positions: positions });
    }

    if (action === 'post_notice') {
      if (!canPublishNotice_(user)) throw new Error('Announcement Changes permission (or Leader/Co-leader) required.');
      return json_({ ok: true, notice: createNotice_(user, body) });
    }

    if (action === 'banking_submit') {
      return json_({ ok: true, request: createAppBankingRequest_(user, body) });
    }

    if (action === 'banking_list') {
      const canManage = canManageBankingQueue_(user);
      let reconciliation = null;
      let reconcileError = '';
      if (canManage && toBoolean_(body.reconcile)) {
        try { reconciliation = reconcileBanking_(apiKey, user); }
        catch (err) { reconcileError = String(err && err.message || err); }
      }
      return json_({
        ok: true,
        user: publicUser_(user),
        can_manage: canManage,
        reconciliation: reconciliation,
        reconcile_error: reconcileError,
        requests: readBankingRequests_(user.faction_id, canManage ? 0 : user.id)
      });
    }

    if (action === 'banking_update') {
      if (!canManageBankingQueue_(user)) throw new Error('Money Giving / Balance Adjustment permission (or Leader/Co-leader) required.');
      return json_({ ok: true, request: updateBankingRequest_(user, body) });
    }

    if (action === 'banking_reconcile') {
      if (!canManageBankingQueue_(user)) throw new Error('Money Giving / Balance Adjustment permission (or Leader/Co-leader) required.');
      const reconciliation = reconcileBanking_(apiKey, user);
      return json_({ ok: true, reconciliation: reconciliation, requests: readBankingRequests_(user.faction_id, 0) });
    }

    if (!isLeaderOrCoLeader_(user.position)) throw new Error('Leader or Co-leader required.');

    if (action === 'save_rank_rules') {
      saveRankRules_(String(body.rank_name || ''), body.tools || {}, user);
      return json_({ ok: true, rank_rules: readRankRules_() });
    }

    if (action === 'save_user_overrides') {
      const userId = Number(body.user_id || 0);
      if (!userId) throw new Error('Valid user_id required.');
      saveUserOverrides_(userId, body.tools || {}, user);
      return json_({ ok: true, user_id: userId, user_overrides: readUserOverrides_(userId) });
    }

    throw new Error('Unknown action.');
  } catch (err) {
    return json_({ ok: false, error: String(err && err.message || err) });
  }
}

function handleListenerEvent_(body) {
  const expected = String(PropertiesService.getScriptProperties().getProperty('LISTENER_TOKEN') || '');
  const supplied = String(body.listener_token || '');
  if (!expected || !supplied || supplied !== expected) throw new Error('Invalid listener token.');

  const settings = settings_();
  const factionId = Number(settings.faction_id || 0);
  if (!factionId) throw new Error('Backend faction_id is not configured.');

  const result = ingestListenerBankingRequest_(factionId, body);
  return { ok: true, request: result.request, duplicate: result.duplicate };
}

function verifyFactionUser_(apiKey) {
  const factionData = tornGet_('/user/faction', apiKey);
  const faction = factionData && factionData.faction;
  if (!faction) throw new Error('Torn account is not currently in a faction.');

  const basicData = tornGet_('/user/basic', apiKey);
  const profile = basicData && basicData.profile || {};
  const settings = settings_();
  const restrict = String(settings.restrict_faction || 'true').toLowerCase() !== 'false';
  const expectedId = Number(settings.faction_id || 0);
  const expectedName = String(settings.faction_name || DF_FACTION_NAME);

  if (restrict) {
    if (expectedId && Number(faction.id) !== expectedId) throw new Error('This backend is restricted to the configured faction.');
    if (expectedName && String(faction.name || '').toLowerCase() !== expectedName.toLowerCase()) throw new Error('This backend is restricted to the configured faction.');
  }

  return {
    id: Number(profile.id || 0),
    name: String(profile.name || 'Unknown'),
    faction_id: Number(faction.id || 0),
    faction_name: String(faction.name || ''),
    position: String(faction.position || '')
  };
}

function isLeaderOrCoLeader_(position) {
  const n = String(position || '').toLowerCase().replace(/[-_\s]/g, '');
  return n === 'leader' || n === 'coleader';
}

function syncFactionPositions_(apiKey, actor) {
  const result = tornGet_('/faction/positions', apiKey);
  const positions = Array.isArray(result.positions) ? result.positions : [];
  const sheet = db_().getSheetByName(DF_SHEETS.POSITIONS);
  const values = sheet.getDataRange().getValues();

  for (let i = values.length - 1; i >= 1; i--) {
    if (Number(values[i][0]) === Number(actor.faction_id)) sheet.deleteRow(i + 1);
  }

  if (positions.length) {
    const now = new Date();
    const rows = positions.map(p => [
      actor.faction_id,
      safeCellText_(String(p.name || '')),
      JSON.stringify(Array.isArray(p.abilities) ? p.abilities : []),
      now,
      actor.id,
      safeCellText_(actor.name)
    ]);
    sheet.getRange(sheet.getLastRow() + 1, 1, rows.length, rows[0].length).setValues(rows);
  }
  return positions;
}

function readPositionPermissions_(factionId, positionName) {
  const sheet = db_().getSheetByName(DF_SHEETS.POSITIONS);
  const values = sheet.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (Number(values[i][0]) !== Number(factionId)) continue;
    if (String(values[i][1]).toLowerCase() !== String(positionName || '').toLowerCase()) continue;
    try {
      const p = JSON.parse(String(values[i][2] || '[]'));
      return Array.isArray(p) ? p : [];
    } catch (_) { return []; }
  }
  return [];
}

function hasCachedPermission_(user, permission) {
  if (isLeaderOrCoLeader_(user.position)) return true;
  return readPositionPermissions_(user.faction_id, user.position)
    .some(v => String(v).toLowerCase() === String(permission).toLowerCase());
}

function canPublishNotice_(user) {
  return isLeaderOrCoLeader_(user.position) || hasCachedPermission_(user, 'Announcement Changes');
}

function canManageBankingQueue_(user) {
  return isLeaderOrCoLeader_(user.position)
    || hasCachedPermission_(user, 'Money Giving')
    || hasCachedPermission_(user, 'Balance Adjustment');
}

function createNotice_(user, body) {
  const title = String(body.title || '').trim();
  const message = String(body.message || '').trim();
  if (!title || !message) throw new Error('Notice title and message are required.');
  if (title.length > 120) throw new Error('Notice title is too long.');
  if (message.length > 2000) throw new Error('Notice message is too long.');

  const now = Math.floor(Date.now() / 1000);
  let expiresAt = Number(body.expires_at || 0);
  if (!expiresAt || expiresAt <= now) expiresAt = now + 72 * 3600;

  const notice = {
    id: Utilities.getUuid(),
    faction_id: user.faction_id,
    title: title,
    message: message,
    created_at: now,
    expires_at: expiresAt,
    author_id: user.id,
    author_name: user.name,
    active: true
  };

  db_().getSheetByName(DF_SHEETS.NOTICES).appendRow([
    notice.id, notice.faction_id, safeCellText_(notice.title), safeCellText_(notice.message),
    notice.created_at, notice.expires_at, notice.author_id, safeCellText_(notice.author_name), true
  ]);
  return notice;
}

function readActiveNotices_(factionId) {
  const sheet = db_().getSheetByName(DF_SHEETS.NOTICES);
  const values = sheet.getDataRange().getValues();
  const now = Math.floor(Date.now() / 1000);
  const out = [];

  for (let i = 1; i < values.length; i++) {
    const active = toBoolean_(values[i][8]);
    const expires = Number(values[i][5] || 0);
    if (Number(values[i][1]) !== Number(factionId) || !active || (expires && expires <= now)) continue;
    out.push({
      id: String(values[i][0] || ''),
      faction_id: Number(values[i][1] || 0),
      title: String(values[i][2] || ''),
      message: String(values[i][3] || ''),
      created_at: Number(values[i][4] || 0),
      expires_at: expires,
      author_id: Number(values[i][6] || 0),
      author_name: String(values[i][7] || 'Leadership')
    });
  }
  out.sort((a, b) => b.created_at - a.created_at);
  return out.slice(0, 25);
}

function createAppBankingRequest_(user, body) {
  const rawAmount = String(body.requested_amount == null ? '' : body.requested_amount).trim();
  let amount = null;
  if (rawAmount) {
    amount = Number(rawAmount.replace(/,/g, ''));
    if (!Number.isFinite(amount) || amount <= 0) throw new Error('Requested amount must be a positive number.');
    amount = Math.round(amount);
  }

  const note = String(body.note || '').trim();
  if (note.length > 500) throw new Error('Banking note is too long.');
  const now = Math.floor(Date.now() / 1000);
  const request = {
    id: Utilities.getUuid(),
    faction_id: user.faction_id,
    requester_id: user.id,
    requester_name: user.name,
    requested_amount: amount,
    request_mode: amount ? 'AMOUNT' : 'FULL_BALANCE',
    note: note,
    request_text: '',
    source: 'ANDROID_APP',
    fingerprint: 'app:' + Utilities.getUuid(),
    message_id: '',
    requested_at: now,
    detected_at: now,
    status: 'PENDING',
    likely_handled: false,
    handled_at: 0,
    handled_by_id: 0,
    handled_by_name: '',
    updated_at: now
  };
  appendBankingRequest_(request);
  return request;
}

function ingestListenerBankingRequest_(factionId, body) {
  const requesterId = Number(body.requester_id || 0);
  const requesterName = String(body.requester_name || '').trim();
  const fingerprint = String(body.fingerprint || '').trim();
  if (!requesterId || !fingerprint) throw new Error('Listener event is missing requester_id or fingerprint.');

  const rawAmount = body.requested_amount;
  let amount = null;
  if (rawAmount !== null && rawAmount !== undefined && String(rawAmount).trim() !== '') {
    amount = Number(rawAmount);
    if (!Number.isFinite(amount) || amount <= 0) amount = null;
    else amount = Math.round(amount);
  }

  const now = Math.floor(Date.now() / 1000);
  const requestedAt = Number(body.message_timestamp || 0) || Number(body.detected_timestamp || 0) || now;
  const detectedAt = Number(body.detected_timestamp || 0) || now;

  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const existing = findBankingRequestByFingerprint_(factionId, fingerprint);
    if (existing) return { request: existing, duplicate: true };

    const request = {
      id: Utilities.getUuid(),
      faction_id: factionId,
      requester_id: requesterId,
      requester_name: requesterName || ('ID ' + requesterId),
      requested_amount: amount,
      request_mode: amount ? 'AMOUNT' : 'FULL_BALANCE',
      note: '',
      request_text: String(body.request_text || '').trim().slice(0, 900),
      source: String(body.source || 'FACTION_CHAT').trim().slice(0, 80),
      fingerprint: fingerprint.slice(0, 180),
      message_id: String(body.message_id || '').trim().slice(0, 120),
      requested_at: requestedAt,
      detected_at: detectedAt,
      status: 'PENDING',
      likely_handled: false,
      handled_at: 0,
      handled_by_id: 0,
      handled_by_name: '',
      updated_at: now
    };
    appendBankingRequest_(request);
    return { request: request, duplicate: false };
  } finally {
    lock.releaseLock();
  }
}

function appendBankingRequest_(request) {
  db_().getSheetByName(DF_SHEETS.BANKING).appendRow([
    request.id,
    request.faction_id,
    request.requester_id,
    safeCellText_(request.requester_name),
    request.requested_amount == null ? '' : request.requested_amount,
    request.request_mode,
    safeCellText_(request.note),
    safeCellText_(request.request_text),
    safeCellText_(request.source),
    safeCellText_(request.fingerprint),
    safeCellText_(request.message_id),
    request.requested_at,
    request.detected_at,
    request.status,
    request.likely_handled,
    request.handled_at || '',
    request.handled_by_id || '',
    safeCellText_(request.handled_by_name || ''),
    request.updated_at
  ]);
}

function findBankingRequestByFingerprint_(factionId, fingerprint) {
  const sheet = db_().getSheetByName(DF_SHEETS.BANKING);
  const values = sheet.getDataRange().getValues();
  for (let i = values.length - 1; i >= 1; i--) {
    if (Number(values[i][1]) !== Number(factionId)) continue;
    if (String(values[i][9] || '') !== String(fingerprint || '')) continue;
    return bankingRowToObject_(values[i]);
  }
  return null;
}

function readBankingRequests_(factionId, requesterId) {
  const sheet = db_().getSheetByName(DF_SHEETS.BANKING);
  const values = sheet.getDataRange().getValues();
  const out = [];
  for (let i = 1; i < values.length; i++) {
    if (Number(values[i][1]) !== Number(factionId)) continue;
    if (requesterId && Number(values[i][2]) !== Number(requesterId)) continue;
    out.push(bankingRowToObject_(values[i]));
  }
  out.sort((a, b) => (b.requested_at || b.detected_at) - (a.requested_at || a.detected_at));
  return out.slice(0, requesterId ? 50 : 150);
}

function bankingRowToObject_(row) {
  const rawAmount = row[4];
  return {
    id: String(row[0] || ''),
    faction_id: Number(row[1] || 0),
    requester_id: Number(row[2] || 0),
    requester_name: String(row[3] || ''),
    requested_amount: rawAmount === '' ? null : Number(rawAmount || 0),
    request_mode: String(row[5] || 'FULL_BALANCE'),
    note: String(row[6] || ''),
    request_text: String(row[7] || ''),
    source: String(row[8] || ''),
    fingerprint: String(row[9] || ''),
    message_id: String(row[10] || ''),
    requested_at: Number(row[11] || 0),
    detected_at: Number(row[12] || 0),
    status: String(row[13] || 'PENDING'),
    likely_handled: toBoolean_(row[14]),
    handled_at: Number(row[15] || 0),
    handled_by_id: Number(row[16] || 0),
    handled_by_name: String(row[17] || ''),
    updated_at: Number(row[18] || 0)
  };
}

function updateBankingRequest_(actor, body) {
  const requestId = String(body.request_id || '').trim();
  const status = String(body.status || '').trim().toUpperCase();
  if (!requestId) throw new Error('request_id required.');
  if (DF_BANKING_STATUSES.indexOf(status) < 0) throw new Error('Invalid banking status.');

  const sheet = db_().getSheetByName(DF_SHEETS.BANKING);
  const values = sheet.getDataRange().getValues();
  const now = Math.floor(Date.now() / 1000);

  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0] || '') !== requestId) continue;
    if (Number(values[i][1]) !== Number(actor.faction_id)) throw new Error('Banking request belongs to another faction.');

    const likelyHandled = status === 'LIKELY_HANDLED';
    const finalStatus = status === 'PAID' || status === 'HANDLED' || status === 'CANCELLED';
    const handledAt = finalStatus ? now : '';
    const handledById = finalStatus ? actor.id : '';
    const handledByName = finalStatus ? safeCellText_(actor.name) : '';

    sheet.getRange(i + 1, 14, 1, 6).setValues([[
      status,
      likelyHandled,
      handledAt,
      handledById,
      handledByName,
      now
    ]]);
    return bankingRowToObject_(sheet.getRange(i + 1, 1, 1, 19).getValues()[0]);
  }
  throw new Error('Banking request not found.');
}

function reconcileBanking_(apiKey, actor) {
  const data = tornGet_('/faction/balance?cat=current', apiKey);
  const balance = data && data.balance || {};
  const members = Array.isArray(balance.members) ? balance.members : [];
  const moneyById = {};
  members.forEach(member => { moneyById[Number(member.id || 0)] = Number(member.money || 0); });

  const sheet = db_().getSheetByName(DF_SHEETS.BANKING);
  const values = sheet.getDataRange().getValues();
  const now = Math.floor(Date.now() / 1000);
  let checked = 0;
  let flagged = 0;

  for (let i = 1; i < values.length; i++) {
    if (Number(values[i][1]) !== Number(actor.faction_id)) continue;
    if (String(values[i][13] || 'PENDING') !== 'PENDING') continue;
    const source = String(values[i][8] || '');
    if (!/^FACTION_CHAT/i.test(source)) continue;

    const requesterId = Number(values[i][2] || 0);
    if (!(requesterId in moneyById)) continue;
    checked++;

    if (moneyById[requesterId] < DF_LIKELY_HANDLED_THRESHOLD) {
      sheet.getRange(i + 1, 14, 1, 6).setValues([[
        'LIKELY_HANDLED', true, '', '', '', now
      ]]);
      flagged++;
    }
  }

  return {
    checked: checked,
    flagged: flagged,
    threshold: DF_LIKELY_HANDLED_THRESHOLD,
    note: 'Only retroactive faction-chat requests are auto-flagged; app-submitted requests remain pending until explicitly handled.'
  };
}

function saveRankRules_(rankName, tools, actor) {
  rankName = rankName.trim();
  if (!rankName) throw new Error('rank_name required.');
  const sheet = db_().getSheetByName(DF_SHEETS.RANKS);
  const rows = sheet.getDataRange().getValues();
  const now = new Date();

  DF_TOOLS.forEach(([toolId]) => {
    if (!(toolId in tools)) return;
    const allowed = Boolean(tools[toolId]);
    let rowIndex = 0;
    for (let i = 1; i < rows.length; i++) {
      if (String(rows[i][0]) === rankName && String(rows[i][1]) === toolId) { rowIndex = i + 1; break; }
    }
    const values = [safeCellText_(rankName), toolId, allowed, now, actor.id, safeCellText_(actor.name)];
    if (rowIndex) sheet.getRange(rowIndex, 1, 1, values.length).setValues([values]);
    else sheet.appendRow(values);
  });
}

function saveUserOverrides_(userId, tools, actor) {
  const sheet = db_().getSheetByName(DF_SHEETS.USERS);
  const rows = sheet.getDataRange().getValues();
  const now = new Date();

  DF_TOOLS.forEach(([toolId]) => {
    if (!(toolId in tools)) return;
    const allowed = tools[toolId] === null ? '' : Boolean(tools[toolId]);
    let rowIndex = 0;
    for (let i = 1; i < rows.length; i++) {
      if (Number(rows[i][0]) === userId && String(rows[i][1]) === toolId) { rowIndex = i + 1; break; }
    }
    const values = [userId, toolId, allowed, now, actor.id, safeCellText_(actor.name)];
    if (rowIndex) sheet.getRange(rowIndex, 1, 1, values.length).setValues([values]);
    else sheet.appendRow(values);
  });
}

function readRankRules_() {
  const sheet = db_().getSheetByName(DF_SHEETS.RANKS);
  const values = sheet.getDataRange().getValues();
  const out = {};
  for (let i = 1; i < values.length; i++) {
    const rank = String(values[i][0] || '');
    const tool = String(values[i][1] || '');
    if (!rank || !tool) continue;
    if (!out[rank]) out[rank] = {};
    out[rank][tool] = toBoolean_(values[i][2]);
  }
  return out;
}

function readUserOverrides_(userId) {
  const sheet = db_().getSheetByName(DF_SHEETS.USERS);
  const values = sheet.getDataRange().getValues();
  const out = {};
  for (let i = 1; i < values.length; i++) {
    if (Number(values[i][0]) !== Number(userId)) continue;
    const tool = String(values[i][1] || '');
    if (!tool) continue;
    const raw = values[i][2];
    out[tool] = raw === '' ? null : toBoolean_(raw);
  }
  return out;
}

function settings_() {
  const sheet = db_().getSheetByName(DF_SHEETS.SETTINGS);
  const values = sheet.getDataRange().getValues();
  const out = {};
  for (let i = 1; i < values.length; i++) {
    const key = String(values[i][0] || '');
    if (key) out[key] = values[i][1];
  }
  return out;
}

function setSetting_(sheet, key, value) {
  const values = sheet.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) {
      sheet.getRange(i + 1, 2).setValue(value);
      return;
    }
  }
  sheet.appendRow([key, value]);
}

function getSetting_(sheet, key) {
  const values = sheet.getDataRange().getValues();
  for (let i = 1; i < values.length; i++) if (String(values[i][0]) === key) return values[i][1];
  return '';
}

function ensureSheet_(ss, name, headers) {
  let sheet = ss.getSheetByName(name);
  if (!sheet) sheet = ss.insertSheet(name);
  if (sheet.getLastRow() === 0) sheet.appendRow(headers);
  return sheet;
}

function db_() {
  const id = PropertiesService.getScriptProperties().getProperty('SHEET_ID');
  if (!id) throw new Error('Backend is not configured. Run setupDuckForceBackend() first.');
  return SpreadsheetApp.openById(id);
}

function tornGet_(path, apiKey) {
  const joiner = String(path).indexOf('?') >= 0 ? '&' : '?';
  const url = 'https://api.torn.com/v2' + path + joiner + 'key=' + encodeURIComponent(apiKey);
  const response = UrlFetchApp.fetch(url, {
    method: 'get',
    muteHttpExceptions: true,
    headers: { 'User-Agent': 'DuckForceCompanion-Backend/' + DF_TOOLKIT_VERSION }
  });

  let data;
  try { data = JSON.parse(response.getContentText()); }
  catch (_) { throw new Error('Unreadable Torn API response.'); }
  if (data && data.error) throw new Error(data.error.error || ('Torn API error ' + data.error.code));
  if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) throw new Error('Torn API HTTP ' + response.getResponseCode());
  return data;
}

function publicUser_(user) {
  return { id: user.id, name: user.name, faction_id: user.faction_id, faction_name: user.faction_name, position: user.position };
}

function safeCellText_(value) {
  const text = String(value == null ? '' : value);
  return /^[=+\-@]/.test(text) ? "'" + text : text;
}

function toBoolean_(value) {
  return value === true || String(value).toLowerCase() === 'true' || Number(value) === 1;
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
