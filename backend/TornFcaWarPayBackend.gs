/**
 * TornFCA WarPay persistence backend v1.1.0.
 * Deploy as its OWN Google Apps Script web app.
 *
 * Purpose: cross-device/faction-wide persistence for calculated WarPay receipts.
 * Security: every request is verified against Torn; receipts are isolated by verified faction_id.
 * Current access intentionally matches the Android WarPay screen: Leader/Co-leader only.
 * Torn API keys are never persisted. Basic identity cache keys contain only a SHA-256 key fingerprint.
 * Faction membership/position is re-read from Torn on every request so a stale leadership role cannot retain access.
 */
const TW_VERSION='1.1.0';
const TW_SHEET='WarPayoutReceipts';

function setupTornFcaWarPayBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet();
  PropertiesService.getScriptProperties().setProperty('WARPAY_SHEET_ID',ss.getId());
  twEnsureSheet_(ss,TW_SHEET,['faction_id','war_id','receipt_json','created_at','created_by_id','created_by_name','updated_at']);
  return{ok:true,version:TW_VERSION,sheet_id:ss.getId(),next:'Deploy this Apps Script project as a web app and set TORNFCA_WARPAY_BACKEND_URL for the Android build.'};
}

function doGet(){return twJson_({ok:true,app:'TornFCA WarPay Backend',version:TW_VERSION,authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}'),action=String(body.action||''),apiKey=String(body.apiKey||'').trim();
    if(!apiKey)throw new Error('API key required.');
    const user=twVerifyUser_(apiKey);
    if(!twLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required for WarPay receipts.');

    if(action==='list')return twJson_({ok:true,user:twPublicUser_(user),receipts:twList_(user.faction_id)});
    if(action==='get'){
      const warId=Number(body.war_id||0);if(!warId)throw new Error('Valid war_id required.');
      return twJson_({ok:true,receipt:twGet_(user.faction_id,warId)});
    }
    if(action==='save')return twJson_({ok:true,receipt:twSave_(user,body.receipt)});
    throw new Error('Unknown action.');
  }catch(err){return twJson_({ok:false,error:String(err&&err.message||err)});}
}

function twVerifyUser_(apiKey){
  // Faction/position is authorization-sensitive and deliberately fresh on every request.
  const factionData=twTornGet_('/user/faction',apiKey),faction=factionData&&factionData.faction;
  if(!faction||!Number(faction.id||0))throw new Error('Torn account is not currently in a faction.');

  // Player identity is stable and may be briefly cached by a non-secret key fingerprint.
  const fp=twHash_(apiKey),cache=CacheService.getScriptCache(),cacheKey='warpay_basic:'+fp,cached=cache.get(cacheKey);let profile=null;
  if(cached){try{profile=JSON.parse(cached);}catch(_){} }
  if(!profile||!Number(profile.id||0)){
    const basic=twTornGet_('/user/basic',apiKey);profile=basic&&basic.profile||{};
    if(!Number(profile.id||0))throw new Error('Unable to verify Torn player identity.');
    cache.put(cacheKey,JSON.stringify({id:Number(profile.id),name:String(profile.name||'Unknown')}),120);
  }
  return{id:Number(profile.id||0),name:String(profile.name||'Unknown'),faction_id:Number(faction.id||0),faction_name:String(faction.name||''),position:String(faction.position||'')};
}

function twTornGet_(path,key){
  const joiner=String(path).indexOf('?')>=0?'&':'?',r=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(key),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-WarPay/'+TW_VERSION}});
  let root;try{root=JSON.parse(r.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));
  if(r.getResponseCode()<200||r.getResponseCode()>=300)throw new Error('Torn API HTTP '+r.getResponseCode());
  return root;
}

function twSave_(user,raw){
  if(!raw||typeof raw!=='object'||Array.isArray(raw))throw new Error('WarPay receipt object required.');
  const warId=Math.floor(Number(raw.war_id||0));if(warId<=0)throw new Error('Receipt requires a valid war_id.');
  const rows=Array.isArray(raw.rows)?raw.rows:[];if(rows.length>200)throw new Error('WarPay receipt has too many member rows.');
  const normalized={
    war_id:warId,
    created_at:Math.max(0,Number(raw.created_at||Date.now())),
    pool:twMoney_(raw.pool),
    total_paid:twMoney_(raw.total_paid),
    total_penalty:twMoney_(raw.total_penalty),
    member_count:rows.length,
    rows:rows.map(twMemberRow_)
  };
  const json=JSON.stringify(normalized);if(json.length>100000)throw new Error('WarPay receipt is too large to store.');
  const sheet=twDb_().getSheetByName(TW_SHEET),now=Math.floor(Date.now()/1000),record=[user.faction_id,warId,json,normalized.created_at,user.id,twSafe_(user.name),now];
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    // Read only after acquiring the lock. Two simultaneous saves for a new war can therefore
    // never both observe an absent row and append duplicate faction/war records.
    const values=sheet.getDataRange().getValues();
    for(let i=1;i<values.length;i++){
      if(Number(values[i][0])!==user.faction_id||Number(values[i][1])!==warId)continue;
      sheet.getRange(i+1,1,1,7).setValues([record]);return normalized;
    }
    sheet.appendRow(record);return normalized;
  }finally{lock.releaseLock();}
}

function twMemberRow_(row){
  row=row&&typeof row==='object'?row:{};
  return{
    player_id:Math.max(0,Math.floor(Number(row.player_id||0))),
    name:String(row.name||'Member').trim().slice(0,80),
    gross:twMoney_(row.gross),
    penalty:twMoney_(row.penalty),
    net:twMoney_(row.net),
    reason:String(row.reason||'').trim().slice(0,500),
    war_hits:Math.max(0,Math.floor(Number(row.war_hits||0))),
    outside_hits:Math.max(0,Math.floor(Number(row.outside_hits||0))),
    respect:Math.max(0,Number(row.respect||0))
  };
}

function twMoney_(value){const n=Number(value||0);if(!Number.isFinite(n)||n<0)return 0;return Math.floor(n);}

function twGet_(factionId,warId){
  const values=twDb_().getSheetByName(TW_SHEET).getDataRange().getValues();
  for(let i=values.length-1;i>=1;i--){
    if(Number(values[i][0])!==Number(factionId)||Number(values[i][1])!==Number(warId))continue;
    try{return JSON.parse(String(values[i][2]||'{}'));}catch(_){return null;}
  }
  return null;
}

function twList_(factionId){
  const values=twDb_().getSheetByName(TW_SHEET).getDataRange().getValues(),out=[];
  for(let i=1;i<values.length;i++){
    if(Number(values[i][0])!==Number(factionId))continue;
    try{const receipt=JSON.parse(String(values[i][2]||'{}'));if(receipt&&Number(receipt.war_id||0)>0)out.push({receipt:receipt,updated_at:Number(values[i][6]||0)});}catch(_){}
  }
  out.sort((a,b)=>b.updated_at-a.updated_at);
  return out.slice(0,30).map(v=>v.receipt);
}

function twLeader_(position){const n=String(position||'').toLowerCase().replace(/[-_\s]/g,'');return n==='leader'||n==='coleader';}
function twPublicUser_(u){return{id:u.id,name:u.name,faction_id:u.faction_id,faction_name:u.faction_name,position:u.position};}
function twDb_(){const id=PropertiesService.getScriptProperties().getProperty('WARPAY_SHEET_ID');if(!id)throw new Error('WarPay backend is not configured. Run setupTornFcaWarPayBackend() first.');return SpreadsheetApp.openById(id);}
function twEnsureSheet_(ss,name,headers){let sheet=ss.getSheetByName(name);if(!sheet)sheet=ss.insertSheet(name);if(sheet.getLastRow()===0)sheet.appendRow(headers);return sheet;}
function twHash_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value||''),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function twSafe_(value){const text=String(value==null?'':value);return/^[=+\-@]/.test(text)?"'"+text:text;}
function twJson_(obj){return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);}
