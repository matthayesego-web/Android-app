/**
 * TornFCA global premium entitlement backend.
 * Deploy as a SEPARATE Apps Script web app from the per-faction backend.
 *
 * Security:
 * - The owner Torn key lives only in Script Properties as OWNER_API_KEY.
 * - The app never submits a payment claim; it only reads server-verified entitlement state.
 * - Incoming payments are deduped by Torn log id.
 *
 * API load:
 * - scanPremiumPayments() is intended to run once per minute: one Torn /user log request/minute.
 */
const TORNFCA_PREMIUM_VERSION='0.9.12';
const TORNFCA_XANAX_ITEM_ID=206;
const TORNFCA_ITEM_RECEIVE_LOG=4103;
const P_SHEETS=Object.freeze({SETTINGS:'PremiumSettings',ENTITLEMENTS:'PremiumEntitlements',PAYMENTS:'PremiumPayments'});

function setupTornFcaPremiumBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet();
  PropertiesService.getScriptProperties().setProperty('PREMIUM_SHEET_ID',ss.getId());
  const settings=ensurePremiumSheet_(ss,P_SHEETS.SETTINGS,['key','value']);
  setPremiumSetting_(settings,'days_per_xanax','15');
  setPremiumSettingIfMissing_(settings,'required_message','TORNFCA');
  setPremiumSettingIfMissing_(settings,'stacking','true');
  ensurePremiumSheet_(ss,P_SHEETS.ENTITLEMENTS,['player_id','tier','expires_at','updated_at','source','total_xanax']);
  ensurePremiumSheet_(ss,P_SHEETS.PAYMENTS,['log_id','timestamp','sender_id','xanax_qty','days_added','message','processed_at']);
  return {ok:true,next:'Set Script Property OWNER_API_KEY to a custom Torn key with user log access for Item receive (4103), then run installPremiumScanTrigger().'};
}

function installPremiumScanTrigger(){
  ScriptApp.getProjectTriggers().forEach(t=>{if(t.getHandlerFunction()==='scanPremiumPayments')ScriptApp.deleteTrigger(t);});
  ScriptApp.newTrigger('scanPremiumPayments').timeBased().everyMinutes(1).create();
  return 'Installed one-minute TornFCA premium scan trigger.';
}

function setPremiumAdminPassword(password){
  if(!password||String(password).length<10)throw new Error('Use a developer password of at least 10 characters.');
  PropertiesService.getScriptProperties().setProperty('PREMIUM_ADMIN_SHA256',sha256_(String(password)));
  return 'Premium admin password updated.';
}

function doGet(){return premiumJson_({ok:true,app:'TornFCA Premium Entitlements',version:TORNFCA_PREMIUM_VERSION});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}');
    const action=String(body.action||'status');
    if(action==='status')return premiumJson_({ok:true,entitlement:readEntitlement_(Number(body.player_id||0))});
    if(action==='admin_config'){
      requirePremiumAdmin_(String(body.admin_password||''));
      const days=Math.max(1,Math.min(365,Number(body.days_per_xanax||15)));
      const required=String(body.required_message==null?'TORNFCA':body.required_message).trim().slice(0,80);
      const settings=premiumDb_().getSheetByName(P_SHEETS.SETTINGS);
      setPremiumSetting_(settings,'days_per_xanax',String(days));
      setPremiumSetting_(settings,'required_message',required);
      return premiumJson_({ok:true,config:readPremiumConfig_()});
    }
    if(action==='admin_grant'){
      requirePremiumAdmin_(String(body.admin_password||''));
      const playerId=Number(body.player_id||0),days=Math.max(1,Math.min(3650,Number(body.days||0)));
      if(!playerId||!days)throw new Error('Valid player_id and days required.');
      return premiumJson_({ok:true,entitlement:extendEntitlement_(playerId,days,'DEVELOPER_GRANT',0)});
    }
    throw new Error('Unknown action.');
  }catch(err){return premiumJson_({ok:false,error:String(err&&err.message||err)});}
}

function scanPremiumPayments(){
  const props=PropertiesService.getScriptProperties();
  const key=String(props.getProperty('OWNER_API_KEY')||'').trim();
  if(!key)throw new Error('OWNER_API_KEY Script Property is not configured.');
  const config=readPremiumConfig_();
  const url='https://api.torn.com/v2/user?selections=log&log='+TORNFCA_ITEM_RECEIVE_LOG+'&limit=100&sort=DESC&key='+encodeURIComponent(key);
  const response=UrlFetchApp.fetch(url,{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Premium/'+TORNFCA_PREMIUM_VERSION}});
  let root;try{root=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));
  if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());

  const logs=normalizeLogs_(root&&root.log);let processed=0,ignored=0;
  logs.sort((a,b)=>Number(a.timestamp||0)-Number(b.timestamp||0));
  logs.forEach(entry=>{
    const logId=String(entry.id||entry._id||entry.log_id||'').trim();
    if(!logId||paymentSeen_(logId)){ignored++;return;}
    const data=entry.data||{};const sender=Number(data.sender||data.user||0);const qty=xanaxQty_(data.items);
    const message=String(data.message||'').trim();
    if(!sender||qty<=0){recordPayment_(logId,Number(entry.timestamp||0),sender,0,0,message);ignored++;return;}
    if(config.required_message&&message.toUpperCase().indexOf(config.required_message.toUpperCase())<0){recordPayment_(logId,Number(entry.timestamp||0),sender,qty,0,message);ignored++;return;}
    const days=Math.round(qty*config.days_per_xanax);
    extendEntitlement_(sender,days,'XANAX_LOG_4103',qty);
    recordPayment_(logId,Number(entry.timestamp||0),sender,qty,days,message);processed++;
  });
  return {ok:true,processed:processed,ignored:ignored,checked:logs.length};
}

function normalizeLogs_(raw){
  if(Array.isArray(raw))return raw.map((v,i)=>Object.assign({id:String(v&&v.id||i)},v||{}));
  if(raw&&typeof raw==='object')return Object.keys(raw).map(id=>Object.assign({id:id},raw[id]||{}));
  return [];
}

function xanaxQty_(items){
  if(!items||typeof items!=='object')return 0;
  const raw=items[String(TORNFCA_XANAX_ITEM_ID)];
  if(raw==null)return 0;
  if(Array.isArray(raw))return Math.max(0,Number(raw[0]||0));
  if(typeof raw==='object')return Math.max(0,Number(raw.quantity||raw.qty||0));
  return Math.max(0,Number(raw||0));
}

function extendEntitlement_(playerId,days,source,xanaxQty){
  const sheet=premiumDb_().getSheetByName(P_SHEETS.ENTITLEMENTS),values=sheet.getDataRange().getValues();
  const now=Math.floor(Date.now()/1000),add=Math.round(days*86400),updated=now;let row=0,current=0,total=0;
  for(let i=1;i<values.length;i++)if(Number(values[i][0])===Number(playerId)){row=i+1;current=Number(values[i][2]||0);total=Number(values[i][5]||0);break;}
  const base=Math.max(now,current),expires=base+add;const valuesOut=[playerId,'PREMIUM',expires,updated,String(source||'backend'),total+Number(xanaxQty||0)];
  if(row)sheet.getRange(row,1,1,valuesOut.length).setValues([valuesOut]);else sheet.appendRow(valuesOut);
  return {player_id:playerId,tier:'PREMIUM',expires_at:expires,verified_at:updated,source:String(source||'backend')};
}

function readEntitlement_(playerId){
  const now=Math.floor(Date.now()/1000);if(!playerId)return {player_id:0,tier:'FREE',expires_at:0,verified_at:now,source:'backend'};
  const sheet=premiumDb_().getSheetByName(P_SHEETS.ENTITLEMENTS),values=sheet.getDataRange().getValues();
  for(let i=1;i<values.length;i++)if(Number(values[i][0])===Number(playerId)){
    const expires=Number(values[i][2]||0),active=expires>now;
    return {player_id:playerId,tier:active?'PREMIUM':'FREE',expires_at:expires,verified_at:now,source:String(values[i][4]||'backend')};
  }
  return {player_id:playerId,tier:'FREE',expires_at:0,verified_at:now,source:'backend'};
}

function paymentSeen_(logId){
  const values=premiumDb_().getSheetByName(P_SHEETS.PAYMENTS).getDataRange().getValues();
  for(let i=values.length-1;i>=1;i--)if(String(values[i][0]||'')===String(logId))return true;return false;
}
function recordPayment_(logId,timestamp,sender,qty,days,message){premiumDb_().getSheetByName(P_SHEETS.PAYMENTS).appendRow([safePremiumText_(logId),timestamp,sender,qty,days,safePremiumText_(message),Math.floor(Date.now()/1000)]);}

function readPremiumConfig_(){const s=premiumSettings_();return {days_per_xanax:Math.max(1,Number(s.days_per_xanax||15)),required_message:String(s.required_message==null?'TORNFCA':s.required_message),stacking:String(s.stacking||'true').toLowerCase()!=='false'};}
function premiumSettings_(){const values=premiumDb_().getSheetByName(P_SHEETS.SETTINGS).getDataRange().getValues(),out={};for(let i=1;i<values.length;i++){const k=String(values[i][0]||'');if(k)out[k]=values[i][1];}return out;}
function setPremiumSetting_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key){sheet.getRange(i+1,2).setValue(value);return;}sheet.appendRow([key,value]);}
function setPremiumSettingIfMissing_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return;sheet.appendRow([key,value]);}
function ensurePremiumSheet_(ss,name,headers){let s=ss.getSheetByName(name);if(!s)s=ss.insertSheet(name);if(s.getLastRow()===0)s.appendRow(headers);return s;}
function premiumDb_(){const id=PropertiesService.getScriptProperties().getProperty('PREMIUM_SHEET_ID');if(!id)throw new Error('Run setupTornFcaPremiumBackend() first.');return SpreadsheetApp.openById(id);}
function requirePremiumAdmin_(password){const expected=String(PropertiesService.getScriptProperties().getProperty('PREMIUM_ADMIN_SHA256')||'');if(!expected||sha256_(password)!==expected)throw new Error('Developer authorization failed.');}
function sha256_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b).toString(16))).slice(-2)).join('').toUpperCase();}
function safePremiumText_(v){const t=String(v==null?'':v);return /^[=+\-@]/.test(t)?"'"+t:t;}
function premiumJson_(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}
