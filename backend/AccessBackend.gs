/**
 * Duck Force Toolkit shared access backend (Google Apps Script).
 *
 * PURPOSE
 * - Keep app-by-rank access rules shared across every phone.
 * - Let only Torn Leader / Co-leader accounts change those rules.
 * - Never store a Torn API key in the sheet; keys are used only to verify requests.
 *
 * SETUP
 * 1. Create a NEW Google Sheet specifically for Duck Force Toolkit access.
 * 2. Open Extensions > Apps Script and paste this file.
 * 3. Run setupDuckForceBackend() once from the editor and approve permissions.
 * 4. In the Settings sheet, set faction_id to Duck Force's numeric Torn faction ID.
 * 5. Deploy as Web App: Execute as Me, access Anyone.
 * 6. Put the deployment URL into the Android app's backend configuration.
 */

const DF_TOOLKIT_VERSION = '0.2.0';
const DF_FACTION_NAME = 'Duck Force';

const DF_SHEETS = Object.freeze({
  SETTINGS: 'Settings',
  RANKS: 'RankAccess',
  USERS: 'UserOverrides'
});

const DF_TOOLS = Object.freeze([
  ['TRAIN', 'Train Payment Calculator'],
  ['ARMORY', 'Xanax Armory Log'],
  ['AUDITOR', 'Faction Xanax Auditor']
]);

function setupDuckForceBackend() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  PropertiesService.getScriptProperties().setProperty('SHEET_ID', ss.getId());

  const settings = ensureSheet_(ss, DF_SHEETS.SETTINGS, ['key', 'value']);
  setSetting_(settings, 'faction_name', DF_FACTION_NAME);
  if (!getSetting_(settings, 'faction_id')) setSetting_(settings, 'faction_id', '0');
  setSetting_(settings, 'schema_version', '1');

  ensureSheet_(ss, DF_SHEETS.RANKS,
    ['rank_name', 'tool_id', 'allowed', 'updated_at', 'updated_by_id', 'updated_by_name']);
  ensureSheet_(ss, DF_SHEETS.USERS,
    ['user_id', 'tool_id', 'allowed', 'updated_at', 'updated_by_id', 'updated_by_name']);

  return {
    ok: true,
    sheet_id: ss.getId(),
    next: 'Set Settings!faction_id to Duck Force numeric faction ID, then deploy as a web app.'
  };
}

function doGet(e) {
  try {
    const action = String((e && e.parameter && e.parameter.action) || 'health');
    if (action === 'health') return json_({ok: true, app: 'Duck Force Toolkit Access', version: DF_TOOLKIT_VERSION});

    const apiKey = String((e.parameter && e.parameter.apiKey) || '').trim();
    if (!apiKey) throw new Error('API key required.');

    const user = verifyDuckForceUser_(apiKey);

    if (action === 'config') {
      return json_({
        ok: true,
        user: publicUser_(user),
        can_manage: isLeaderOrCoLeader_(user.position),
        rank_rules: readRankRules_(),
        user_overrides: readUserOverrides_(user.id)
      });
    }

    if (action === 'positions') {
      if (!isLeaderOrCoLeader_(user.position)) throw new Error('Leader or Co-leader required.');
      return json_({
        ok: true,
        user: publicUser_(user),
        positions: tornGet_('/faction/positions', apiKey).positions || []
      });
    }

    throw new Error('Unknown action.');
  } catch (err) {
    return json_({ok: false, error: String(err && err.message || err)});
  }
}

function doPost(e) {
  try {
    const body = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const action = String(body.action || '');
    const apiKey = String(body.apiKey || '').trim();
    if (!apiKey) throw new Error('API key required.');

    const user = verifyDuckForceUser_(apiKey);
    if (!isLeaderOrCoLeader_(user.position)) throw new Error('Leader or Co-leader required.');

    if (action === 'save_rank_rules') {
      saveRankRules_(String(body.rank_name || ''), body.tools || {}, user);
      return json_({ok: true, rank_rules: readRankRules_()});
    }

    if (action === 'save_user_overrides') {
      const userId = Number(body.user_id || 0);
      if (!userId) throw new Error('Valid user_id required.');
      saveUserOverrides_(userId, body.tools || {}, user);
      return json_({ok: true, user_id: userId, user_overrides: readUserOverrides_(userId)});
    }

    throw new Error('Unknown action.');
  } catch (err) {
    return json_({ok: false, error: String(err && err.message || err)});
  }
}

function verifyDuckForceUser_(apiKey) {
  const factionData = tornGet_('/user/faction', apiKey);
  const faction = factionData && factionData.faction;
  if (!faction) throw new Error('Torn account is not currently in a faction.');

  const basicData = tornGet_('/user/basic', apiKey);
  const profile = basicData && basicData.profile || {};

  const settings = settings_();
  const expectedId = Number(settings.faction_id || 0);
  const expectedName = String(settings.faction_name || DF_FACTION_NAME);

  if (expectedId && Number(faction.id) !== expectedId) throw new Error('This backend is restricted to Duck Force.');
  if (String(faction.name || '').toLowerCase() !== expectedName.toLowerCase()) {
    throw new Error('This backend is restricted to Duck Force.');
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
  const normalized = String(position || '').toLowerCase().replace(/[-_\s]/g, '');
  return normalized === 'leader' || normalized === 'coleader';
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
      if (String(rows[i][0]) === rankName && String(rows[i][1]) === toolId) {
        rowIndex = i + 1;
        break;
      }
    }

    const values = [rankName, toolId, allowed, now, actor.id, actor.name];
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
      if (Number(rows[i][0]) === userId && String(rows[i][1]) === toolId) {
        rowIndex = i + 1;
        break;
      }
    }

    const values = [userId, toolId, allowed, now, actor.id, actor.name];
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
  for (let i = 1; i < values.length; i++) {
    if (String(values[i][0]) === key) return values[i][1];
  }
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
  const url = 'https://api.torn.com/v2' + path + '?key=' + encodeURIComponent(apiKey);
  const response = UrlFetchApp.fetch(url, {
    method: 'get',
    muteHttpExceptions: true,
    headers: {'User-Agent': 'DuckForceToolkit-AccessBackend/' + DF_TOOLKIT_VERSION}
  });

  let data;
  try {
    data = JSON.parse(response.getContentText());
  } catch (e) {
    throw new Error('Unreadable Torn API response.');
  }

  if (data && data.error) throw new Error(data.error.error || ('Torn API error ' + data.error.code));
  if (response.getResponseCode() < 200 || response.getResponseCode() >= 300) {
    throw new Error('Torn API HTTP ' + response.getResponseCode());
  }
  return data;
}

function publicUser_(user) {
  return {
    id: user.id,
    name: user.name,
    faction_id: user.faction_id,
    faction_name: user.faction_name,
    position: user.position
  };
}

function toBoolean_(value) {
  return value === true || String(value).toLowerCase() === 'true' || Number(value) === 1;
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
