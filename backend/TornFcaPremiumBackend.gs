/**
 * TornFCA global premium entitlement backend v1.3.0.
 * Deploy as a SEPARATE Apps Script web app from faction/community/developer backends.
 *
 * Security:
 * - Status reads require a Torn API key and can only read the verified caller's own entitlement.
 * - Admin mutations require BOTH verified TornFCA owner identity and the premium admin password.
 * - Client API keys are never persisted; only SHA-256 fingerprints may be used for short-lived identity cache keys.
 * - The owner Torn key used by the payment scanner lives only in Script Properties as OWNER_API_KEY.
 * - Incoming payments are deduped by Torn log id under ScriptLock.
 * - Payment grants are receipt-idempotent so a failure between grant and audit-row write cannot double-credit Premium.
 * - Automatic paid entitlement processing fails closed until MONETIZATION_APPROVED=true is explicitly set after the operator has handled Torn/payment-distribution approval requirements.
 *
 * API load:
 * - scanPremiumPayments() is intended to run once per minute only after monetization approval: one Torn /user log request/minute.
 * - entitlement identity verification is cached briefly by API-key fingerprint.
 */
const TORNFCA_PREMIUM_VERSION='1.3.0';
const TORNFCA_PREMIUM_DEVELOPER_ID=3987363;
const TORNFCA_XANAX_ITEM_ID=206;
const TORNFCA_ITEM_RECEIVE_LOG=4103;
const TORNFCA_DEFAULT_DAYS_PER_XANAX=7;
const P_SHEETS=Object.freeze({SETTINGS:'PremiumSettings',ENTITLEMENTS:'PremiumEntitlements',PAYMENTS:'PremiumPayments'});

function setupTornFcaPremiumBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet(),props=PropertiesService.getScriptProperties();
  props.setProperty('PREMIUM_SHEET_ID',ss.getId());
  if(!props.getProperty('MONETIZATION_APPROVED'))props.setProperty('MONETIZATION_APPROVED','false');
  const settings=ensurePremiumSheet_(ss,P_SHEETS.SETTINGS,['key','value']);
  setPremiumSettingIfMissing_(settings,'days_per_xanax',String(TORNFCA_DEFAULT_DAYS_PER_XANAX));
  setPremiumSettingIfMissing_(settings,'required_message','TORNFCA');
  setPremiumSettingIfMissing_(settings,'stacking','true');
  ensurePremiumSheet_(ss,P_SHEETS.ENTITLEMENTS,['player_id','tier','expires_at','updated_at','source','total_xanax']);
  ensurePremiumSheet_(ss,P_SHEETS.PAYMENTS,['log_id','timestamp','sender_id','xanax_qty','days_added','message','processed_at']);
  return {ok:true,version:TORNFCA_PREMIUM_VERSION,monetization_approved:premiumMonetizationApproved_(),days_per_xanax:readPremiumConfig_().days_per_xanax,next:'Set the one-time PREMIUM_ADMIN_PASSWORD_SETUP property and bootstrap the admin password. Existing deployments should confirm days_per_xanax is 7 in Premium Admin before enabling paid scanning. Automatic payment scanning remains disabled until MONETIZATION_APPROVED=true is deliberately set after required Torn/distribution approval.'};
}

/** One-time helper for an existing Premium sheet created before the 7-day launch price was finalized. */
function applyPremiumSevenDayLaunchPricing(){
  const settings=premiumDb_().getSheetByName(P_SHEETS.SETTINGS);
  setPremiumSetting_(settings,'days_per_xanax',String(TORNFCA_DEFAULT_DAYS_PER_XANAX));
  return {ok:true,config:readPremiumConfig_()};
}

function installPremiumScanTrigger(){
  if(!premiumMonetizationApproved_())throw new Error('Automatic Premium payment scanning is disabled. Obtain/handle the required Torn and distribution payment approval first, then explicitly set MONETIZATION_APPROVED=true in Script Properties.');
  ScriptApp.getProjectTriggers().forEach(t=>{if(t.getHandlerFunction()==='scanPremiumPayments')ScriptApp.deleteTrigger(t);});
  ScriptApp.newTrigger('scanPremiumPayments').timeBased().everyMinutes(1).create();
  return 'Installed one-minute TornFCA premium scan trigger.';
}

function setPremiumAdminPassword(password){
  if(!password||String(password).length<10)throw new Error('Use a developer password of at least 10 characters.');
  PropertiesService.getScriptProperties().setProperty('PREMIUM_ADMIN_SHA256',sha256_(String(password)));
  return 'Premium admin password updated.';
}

/**
 * Safe Apps Script UI bootstrap: temporarily set PREMIUM_ADMIN_PASSWORD_SETUP in Script Properties,
 * run this once, and the plaintext property is deleted immediately after hashing.
 */
function bootstrapPremiumAdminPassword(){
  const props=PropertiesService.getScriptProperties(),plain=String(props.getProperty('PREMIUM_ADMIN_PASSWORD_SETUP')||'');
  if(plain.length<10)throw new Error('Set PREMIUM_ADMIN_PASSWORD_SETUP in Script Properties to a password of at least 10 characters first.');
  try{return setPremiumAdminPassword(plain);}finally{props.deleteProperty('PREMIUM_ADMIN_PASSWORD_SETUP');}
}

function doGet(){return premiumJson_({ok:true,app:'TornFCA Premium Entitlements',version:TORNFCA_PREMIUM_VERSION,monetization_approved:premiumMonetizationApproved_(),authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}');
    const action=String(body.action||'status');
    const apiKey=String(body.apiKey||'').trim();
    if(!apiKey)throw new Error('API key required.');
    const user=verifyPremiumUser_(apiKey);

    if(action==='status'){
      const requested=Number(body.player_id||user.id);
      if(requested!==user.id)throw new Error('Premium entitlement reads are limited to the verified signed-in player.');
      return premiumJson_({ok:true,entitlement:readEntitlement_(user.id)});
    }
    if(action==='admin_config'){
      requirePremiumDeveloper_(user);
      requirePremiumAdmin_(String(body.admin_password||''));
      const current=readPremiumConfig_();
      const days=Math.max(1,Math.min(365,Number(body.days_per_xanax||current.days_per_xanax||TORNFCA_DEFAULT_DAYS_PER_XANAX)));
      const required=String(body.required_message==null?current.required_message:body.required_message).trim().slice(0,80);
      const stacking=body.stacking==null?current.stacking:premiumBool_(body.stacking);
      const settings=premiumDb_().getSheetByName(P_SHEETS.SETTINGS);
      setPremiumSetting_(settings,'days_per_xanax',String(days));
      setPremiumSetting_(settings,'required_message',required);
      setPremiumSetting_(settings,'stacking',stacking?'true':'false');
      return premiumJson_({ok:true,config:readPremiumConfig_(),monetization_approved:premiumMonetizationApproved_()});
    }
    if(action==='admin_grant'){
      requirePremiumDeveloper_(user);
      requirePremiumAdmin_(String(body.admin_password||''));
      const playerId=Number(body.player_id||0),days=Math.max(1,Math.min(3650,Number(body.days||0)));
      if(!playerId||!days)throw new Error('Valid player_id and days required.');
      const grantType=String(body.grant_type||'developer').trim().toLowerCase();
      const source=grantType==='complimentary'?'COMPLIMENTARY_GRANT':'DEVELOPER_GRANT';
      return premiumJson_({ok:true,grant_type:grantType,entitlement:extendEntitlement_(playerId,days,source,0,true)});
    }
    throw new Error('Unknown action.');
  }catch(err){return premiumJson_({ok:false,error:String(err&&err.message||err)});}
}

function verifyPremiumUser_(apiKey){
  const fingerprint=sha256_(apiKey).toLowerCase(),cache=CacheService.getScriptCache(),cacheKey='premium_identity:'+fingerprint;
  const cached=cache.get(cacheKey);
  if(cached){try{return JSON.parse(cached);}catch(_){} }
  const root=premiumTornGet_('/user/basic',apiKey),profile=root&&root.profile||{};
  const user={id:Number(profile.id||0),name:String(profile.name||'Unknown')};
  if(!user.id)throw new Error('Unable to verify Torn player identity.');
  cache.put(cacheKey,JSON.stringify(user),120);
  return user;
}

function requirePremiumDeveloper_(user){if(Number(user&&user.id||0)!==TORNFCA_PREMIUM_DEVELOPER_ID)throw new Error('Verified TornFCA developer account required.');}

function premiumTornGet_(path,key){
  const joiner=String(path).indexOf('?')>=0?'&':'?';
  const response=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(key),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Premium/'+TORNFCA_PREMIUM_VERSION}});
  let root;try{root=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));
  if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());
  return root;
}

function scanPremiumPayments(){
  if(!premiumMonetizationApproved_())throw new Error('Automatic Premium payment processing is disabled until MONETIZATION_APPROVED=true is explicitly configured after required approval.');
  const props=PropertiesService.getScriptProperties();
  const key=String(props.getProperty('OWNER_API_KEY')||'').trim();
  if(!key)throw new Error('OWNER_API_KEY Script Property is not configured.');
  const config=readPremiumConfig_();
  const url='https://api.torn.com/v2/user?selections=log&log='+TORNFCA_ITEM_RECEIVE_LOG+'&limit=100&sort=DESC&key='+encodeURIComponent(key);
  const response=UrlFetchApp.fetch(url,{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Premium/'+TORNFCA_PREMIUM_VERSION}});
  let root;try{root=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));
  if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());

  const logs=normalizeLogs_(root&&root.log);logs.sort((a,b)=>Number(a.timestamp||0)-Number(b.timestamp||0));
  const lock=LockService.getScriptLock();lock.waitLock(30000);
  try{
    const seen=paymentIds_();let processed=0,ignored=0;
    logs.forEach(entry=>{
      const logId=String(entry.id||entry._id||entry.log_id||'').trim();
      if(!logId||seen.has(logId)){ignored++;return;}
      const data=entry.data||{},sender=Number(data.sender||data.user||0),qty=xanaxQty_(data.items),message=String(data.message||'').trim(),timestamp=Number(entry.timestamp||0);
      if(!sender||qty<=0){recordPayment_(logId,timestamp,sender,0,0,message);seen.add(logId);ignored++;return;}
      if(config.required_message&&message.toUpperCase().indexOf(config.required_message.toUpperCase())<0){recordPayment_(logId,timestamp,sender,qty,0,message);seen.add(logId);ignored++;return;}
      const days=Math.round(qty*config.days_per_xanax),receiptSource='XANAX_LOG_4103:'+logId;
      extendEntitlement_(sender,days,receiptSource,qty,config.stacking);
      recordPayment_(logId,timestamp,sender,qty,days,message);
      seen.add(logId);processed++;
    });
    return {ok:true,processed:processed,ignored:ignored,checked:logs.length,stacking:config.stacking};
  }finally{lock.releaseLock();}
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

function extendEntitlement_(playerId,days,source,xanaxQty,stacking){
  const sheet=premiumDb_().getSheetByName(P_SHEETS.ENTITLEMENTS),values=sheet.getDataRange().getValues();
  const now=Math.floor(Date.now()/1000),add=Math.round(days*86400),updated=now;let row=0,current=0,total=0,currentSource='';
  for(let i=1;i<values.length;i++)if(Number(values[i][0])===Number(playerId)){row=i+1;current=Number(values[i][2]||0);currentSource=String(values[i][4]||'');total=Number(values[i][5]||0);break;}
  // If a prior scanner run granted this exact receipt and failed before writing PremiumPayments,
  // return the existing entitlement instead of extending it again.
  if(source&&String(source).indexOf('XANAX_LOG_4103:')===0&&currentSource===String(source))return {player_id:playerId,tier:current>now?'PREMIUM':'FREE',expires_at:current,verified_at:updated,source:currentSource,idempotent:true};
  const base=stacking===false?now:Math.max(now,current),expires=base+add;
  const valuesOut=[playerId,'PREMIUM',expires,updated,String(source||'backend'),total+Number(xanaxQty||0)];
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

function paymentIds_(){
  const values=premiumDb_().getSheetByName(P_SHEETS.PAYMENTS).getDataRange().getValues(),seen=new Set();
  for(let i=1;i<values.length;i++){const id=String(values[i][0]||'');if(id)seen.add(id);}return seen;
}
function paymentSeen_(logId){return paymentIds_().has(String(logId));}
function recordPayment_(logId,timestamp,sender,qty,days,message){premiumDb_().getSheetByName(P_SHEETS.PAYMENTS).appendRow([safePremiumText_(logId),timestamp,sender,qty,days,safePremiumText_(message),Math.floor(Date.now()/1000)]);}

function readPremiumConfig_(){const s=premiumSettings_();return {days_per_xanax:Math.max(1,Number(s.days_per_xanax||TORNFCA_DEFAULT_DAYS_PER_XANAX)),required_message:String(s.required_message==null?'TORNFCA':s.required_message),stacking:premiumBool_(s.stacking==null?'true':s.stacking)};}
function premiumSettings_(){const values=premiumDb_().getSheetByName(P_SHEETS.SETTINGS).getDataRange().getValues(),out={};for(let i=1;i<values.length;i++){const k=String(values[i][0]||'');if(k)out[k]=values[i][1];}return out;}
function setPremiumSetting_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key){sheet.getRange(i+1,2).setValue(value);return;}sheet.appendRow([key,value]);}
function setPremiumSettingIfMissing_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return;sheet.appendRow([key,value]);}
function ensurePremiumSheet_(ss,name,headers){let s=ss.getSheetByName(name);if(!s)s=ss.insertSheet(name);if(s.getLastRow()===0)s.appendRow(headers);return s;}
function premiumDb_(){const id=PropertiesService.getScriptProperties().getProperty('PREMIUM_SHEET_ID');if(!id)throw new Error('Run setupTornFcaPremiumBackend() first.');return SpreadsheetApp.openById(id);}
function requirePremiumAdmin_(password){const expected=String(PropertiesService.getScriptProperties().getProperty('PREMIUM_ADMIN_SHA256')||'');if(!expected||sha256_(password)!==expected)throw new Error('Developer authorization failed.');}
function premiumMonetizationApproved_(){return premiumBool_(PropertiesService.getScriptProperties().getProperty('MONETIZATION_APPROVED')||'false');}
function premiumBool_(v){if(typeof v==='boolean')return v;const n=String(v==null?'':v).trim().toLowerCase();return n==='true'||n==='1'||n==='yes'||n==='on';}
function sha256_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b).toString(16))).slice(-2)).join('').toUpperCase();}
function safePremiumText_(v){const t=String(v==null?'':v);return /^[=+\-@]/.test(t)?"'"+t:t;}
function premiumJson_(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}