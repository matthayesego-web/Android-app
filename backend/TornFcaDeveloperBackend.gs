/**
 * TornFCA Developer Control Plane v1.0.0.
 * Deploy as its OWN Google Apps Script web app.
 *
 * Security boundaries:
 * - Every API action requires a Torn API key and verifies the caller is the configured TornFCA owner.
 * - Mutating actions additionally require the developer password hash stored in Script Properties.
 * - Torn API keys are never persisted. Only a SHA-256 key fingerprint may be used in short-lived CacheService entries.
 * - All successful mutations are written to an append-only audit sheet.
 */
const TD_VERSION='1.0.0';
const TD_DEVELOPER_PLAYER_ID=3987363;
const TD_CONFIG='DeveloperConfig';
const TD_AUDIT='DeveloperAudit';
const TD_ALLOWED_CONFIG=Object.freeze([
  'maintenance_mode',
  'minimum_version_code',
  'beta_message',
  'disable_activity',
  'disable_war',
  'disable_chain',
  'disable_oc',
  'disable_pulse',
  'disable_lookup',
  'disable_premium'
]);

function setupTornFcaDeveloperBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet();
  const props=PropertiesService.getScriptProperties();
  props.setProperty('DEVELOPER_SHEET_ID',ss.getId());
  const config=tdEnsureSheet_(ss,TD_CONFIG,['key','value','updated_at','updated_by_id','updated_by_name']);
  tdSetIfMissing_(config,'maintenance_mode','false',0,'setup');
  tdSetIfMissing_(config,'minimum_version_code','0',0,'setup');
  tdSetIfMissing_(config,'beta_message','',0,'setup');
  ['activity','war','chain','oc','pulse','lookup','premium'].forEach(v=>tdSetIfMissing_(config,'disable_'+v,'false',0,'setup'));
  tdEnsureSheet_(ss,TD_AUDIT,['id','timestamp','actor_id','actor_name','action','details_json']);
  return {ok:true,version:TD_VERSION,sheet_id:ss.getId(),next:'Run setTornFcaDeveloperAdminPassword(), then deploy this project as a web app.'};
}

function setTornFcaDeveloperAdminPassword(password){
  const value=String(password||'');
  if(value.length<10)throw new Error('Use a developer password of at least 10 characters.');
  PropertiesService.getScriptProperties().setProperty('DEVELOPER_ADMIN_SHA256',tdSha256_(value).toUpperCase());
  return 'TornFCA developer admin password updated.';
}

function doGet(){
  return tdJson_({ok:true,app:'TornFCA Developer Control Plane',version:TD_VERSION,authenticated_actions:'POST only'});
}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}');
    const action=String(body.action||'').trim();
    const apiKey=String(body.apiKey||'').trim();
    if(!apiKey)throw new Error('API key required.');
    const user=tdVerifyDeveloper_(apiKey);

    if(action==='status'||action==='config_read'){
      return tdJson_({ok:true,user:tdPublicUser_(user),version:TD_VERSION,config:tdReadConfig_()});
    }

    if(action==='audit_list'){
      tdRequireAdmin_(String(body.admin_password||''));
      return tdJson_({ok:true,audit:tdReadAudit_()});
    }

    if(action==='config_write'){
      tdRequireAdmin_(String(body.admin_password||''));
      const updates=body.config&&typeof body.config==='object'?body.config:{};
      const applied=tdWriteConfig_(updates,user);
      tdAudit_(user,'config_write',{keys:Object.keys(applied)});
      return tdJson_({ok:true,config:tdReadConfig_(),applied:applied});
    }

    throw new Error('Unknown action.');
  }catch(err){
    return tdJson_({ok:false,error:String(err&&err.message||err)});
  }
}

function tdVerifyDeveloper_(apiKey){
  const fp=tdSha256_(apiKey),cache=CacheService.getScriptCache(),cacheKey='developer_identity:'+fp;
  const cached=cache.get(cacheKey);
  if(cached){
    try{const u=JSON.parse(cached);if(Number(u.id)===TD_DEVELOPER_PLAYER_ID)return u;}catch(_){}
  }
  const root=tdTornGet_('/user/basic',apiKey),profile=root&&root.profile||{};
  const user={id:Number(profile.id||0),name:String(profile.name||'Unknown')};
  if(user.id!==TD_DEVELOPER_PLAYER_ID)throw new Error('Verified TornFCA developer account required.');
  cache.put(cacheKey,JSON.stringify(user),90);
  return user;
}

function tdRequireAdmin_(password){
  const expected=String(PropertiesService.getScriptProperties().getProperty('DEVELOPER_ADMIN_SHA256')||'').toUpperCase();
  if(!expected||tdSha256_(String(password||'')).toUpperCase()!==expected)throw new Error('Developer authorization failed.');
}

function tdWriteConfig_(updates,user){
  const sheet=tdDb_().getSheetByName(TD_CONFIG),applied={};
  TD_ALLOWED_CONFIG.forEach(key=>{
    if(!(key in updates))return;
    let value=String(updates[key]==null?'':updates[key]).trim();
    if(key==='maintenance_mode'||key.indexOf('disable_')===0)value=tdBool_(updates[key])?'true':'false';
    else if(key==='minimum_version_code')value=String(Math.max(0,Math.floor(Number(updates[key])||0)));
    else if(key==='beta_message')value=value.slice(0,1000);
    tdSet_(sheet,key,value,user.id,user.name);
    applied[key]=value;
  });
  if(!Object.keys(applied).length)throw new Error('No supported developer configuration keys were supplied.');
  return applied;
}

function tdReadConfig_(){
  const sheet=tdDb_().getSheetByName(TD_CONFIG),values=sheet.getDataRange().getValues(),out={};
  for(let i=1;i<values.length;i++){
    const key=String(values[i][0]||'');
    if(TD_ALLOWED_CONFIG.indexOf(key)<0)continue;
    const raw=String(values[i][1]==null?'':values[i][1]);
    if(key==='maintenance_mode'||key.indexOf('disable_')===0)out[key]=tdBool_(raw);
    else if(key==='minimum_version_code')out[key]=Math.max(0,Number(raw)||0);
    else out[key]=raw;
  }
  return out;
}

function tdAudit_(user,action,details){
  tdDb_().getSheetByName(TD_AUDIT).appendRow([
    Utilities.getUuid(),Math.floor(Date.now()/1000),user.id,tdSafe_(user.name),tdSafe_(action),tdSafe_(JSON.stringify(details||{}))
  ]);
}

function tdReadAudit_(){
  const values=tdDb_().getSheetByName(TD_AUDIT).getDataRange().getValues(),out=[];
  for(let i=Math.max(1,values.length-200);i<values.length;i++)out.push({
    id:String(values[i][0]||''),timestamp:Number(values[i][1]||0),actor_id:Number(values[i][2]||0),actor_name:String(values[i][3]||''),action:String(values[i][4]||''),details_json:String(values[i][5]||'{}')
  });
  out.sort((a,b)=>b.timestamp-a.timestamp);
  return out;
}

function tdSetIfMissing_(sheet,key,value,id,name){
  const values=sheet.getDataRange().getValues();
  for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return;
  sheet.appendRow([key,value,Math.floor(Date.now()/1000),id,tdSafe_(name)]);
}
function tdSet_(sheet,key,value,id,name){
  const values=sheet.getDataRange().getValues(),now=Math.floor(Date.now()/1000),row=[key,tdSafe_(value),now,id,tdSafe_(name)];
  for(let i=1;i<values.length;i++)if(String(values[i][0])===key){sheet.getRange(i+1,1,1,5).setValues([row]);return;}
  sheet.appendRow(row);
}
function tdDb_(){const id=PropertiesService.getScriptProperties().getProperty('DEVELOPER_SHEET_ID');if(!id)throw new Error('Developer backend is not configured. Run setupTornFcaDeveloperBackend() first.');return SpreadsheetApp.openById(id);}
function tdEnsureSheet_(ss,name,headers){let s=ss.getSheetByName(name);if(!s)s=ss.insertSheet(name);if(s.getLastRow()===0)s.appendRow(headers);return s;}
function tdTornGet_(path,key){const joiner=String(path).indexOf('?')>=0?'&':'?';const r=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(key),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Developer/'+TD_VERSION}});let root;try{root=JSON.parse(r.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));if(r.getResponseCode()<200||r.getResponseCode()>=300)throw new Error('Torn API HTTP '+r.getResponseCode());return root;}
function tdPublicUser_(u){return{id:u.id,name:u.name};}
function tdBool_(v){return v===true||String(v).toLowerCase()==='true'||Number(v)===1;}
function tdSafe_(v){const t=String(v==null?'':v);return/^[=+\-@]/.test(t)?"'"+t:t;}
function tdSha256_(v){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(v||''),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function tdJson_(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}
