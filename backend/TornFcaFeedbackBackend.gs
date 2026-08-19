/**
 * TornFCA Feedback Backend v1.0.0
 * Authenticated Beta feedback queue. Torn API keys are used only to verify the current request and are never stored.
 */
const TF_VERSION='1.0.0';
const TF_SHEET='Feedback';
const TF_CATEGORIES=Object.freeze(['Bug','Feature Request','UI/UX','Performance','Access/Permissions','Other']);

function setupTornFcaFeedbackBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet(),props=PropertiesService.getScriptProperties();
  props.setProperty('FEEDBACK_SHEET_ID',ss.getId());
  tfEnsureSheet_(ss,TF_SHEET,['id','created_at','category','title','message','app_version','version_code','screen','player_id','player_name','faction_id','faction_name','status','priority','developer_notes','fixed_in_version','updated_at','platform']);
  return{ok:true,version:TF_VERSION,sheet_id:ss.getId(),schema_version:1,categories:TF_CATEGORIES,next:'Deploy this Apps Script project as a web app. Android submissions must send action=submit with the current Torn API key.'};
}

function doGet(){return tfJson_({ok:true,app:'TornFCA Feedback Backend',version:TF_VERSION,authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}'),action=String(body.action||'').trim(),apiKey=String(body.apiKey||'').trim();
    if(!apiKey)throw new Error('API key required.');
    const user=tfVerifyUser_(apiKey);
    if(action==='submit')return tfJson_({ok:true,feedback:tfSubmit_(user,body)});
    if(action==='mine')return tfJson_({ok:true,feedback:tfReadMine_(user.id)});
    throw new Error('Unknown action.');
  }catch(err){return tfJson_({ok:false,error:String(err&&err.message||err)});}
}

function tfVerifyUser_(apiKey){
  const factionRoot=tfTornGet_('/user/faction',apiKey),faction=factionRoot&&factionRoot.faction||{},fingerprint=tfHash_(apiKey),cache=CacheService.getScriptCache(),cacheKey='feedback_basic:'+fingerprint,cached=cache.get(cacheKey);let profile=null;
  if(cached){try{profile=JSON.parse(cached);}catch(_){} }
  if(!profile||!Number(profile.id||0)){
    const basicRoot=tfTornGet_('/user/basic',apiKey);profile=basicRoot&&basicRoot.profile||{};
    if(!Number(profile.id||0))throw new Error('Unable to verify Torn player identity.');
    profile={id:Number(profile.id),name:String(profile.name||'Unknown')};cache.put(cacheKey,JSON.stringify(profile),120);
  }
  return{id:Number(profile.id),name:String(profile.name||'Unknown'),faction_id:Number(faction.id||0),faction_name:String(faction.name||'')};
}

function tfSubmit_(user,body){
  tfRateLimit_(user.id);
  const category=tfCategory_(body.category),title=tfText_(body.title,140),message=tfText_(body.message,5000);
  if(!title)throw new Error('Feedback title required.');if(!message)throw new Error('Feedback message required.');
  const appVersion=tfText_(body.app_version||body.version_name,80),versionCode=Math.max(0,Math.floor(Number(body.version_code||0)||0)),screen=tfText_(body.screen||body.feature,120),platform=tfText_(body.platform||'Android',40)||'Android',now=Math.floor(Date.now()/1000),id=Utilities.getUuid();
  const row=[id,now,category,tfSafe_(title),tfSafe_(message),tfSafe_(appVersion),versionCode,tfSafe_(screen),user.id,tfSafe_(user.name),user.faction_id,tfSafe_(user.faction_name),'NEW','UNSET','','',now,tfSafe_(platform)];
  const sheet=tfDb_().getSheetByName(TF_SHEET),lock=LockService.getScriptLock();lock.waitLock(10000);try{sheet.appendRow(row);}finally{lock.releaseLock();}
  return{id:id,created_at:now,category:category,title:title,status:'NEW'};
}

function tfReadMine_(playerId){
  const values=tfDb_().getSheetByName(TF_SHEET).getDataRange().getValues(),out=[];
  for(let i=values.length-1;i>=1;i--){
    if(Number(values[i][8]||0)!==Number(playerId))continue;
    out.push({id:String(values[i][0]||''),created_at:Number(values[i][1]||0),category:String(values[i][2]||'Other'),title:String(values[i][3]||''),message:String(values[i][4]||''),app_version:String(values[i][5]||''),version_code:Number(values[i][6]||0),screen:String(values[i][7]||''),status:String(values[i][12]||'NEW'),priority:String(values[i][13]||'UNSET'),fixed_in_version:String(values[i][15]||''),updated_at:Number(values[i][16]||0)});
    if(out.length>=50)break;
  }
  return out;
}

function tfCategory_(value){const wanted=String(value||'Other').trim().toLowerCase();for(let i=0;i<TF_CATEGORIES.length;i++)if(TF_CATEGORIES[i].toLowerCase()===wanted)return TF_CATEGORIES[i];return'Other';}
function tfRateLimit_(playerId){const cache=CacheService.getScriptCache(),key='feedback_submit:'+String(playerId);if(cache.get(key))throw new Error('Please wait a moment before submitting more feedback.');cache.put(key,'1',10);}
function tfTornGet_(path,apiKey){const joiner=String(path).indexOf('?')>=0?'&':'?',response=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(apiKey),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Feedback/'+TF_VERSION}});let root;try{root=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());return root;}
function tfText_(value,maxLength){return String(value==null?'':value).trim().replace(/\u0000/g,'').slice(0,maxLength);}
function tfSafe_(value){const text=String(value==null?'':value);return/^[=+\-@]/.test(text)?"'"+text:text;}
function tfHash_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value||''),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function tfDb_(){const id=PropertiesService.getScriptProperties().getProperty('FEEDBACK_SHEET_ID');if(!id)throw new Error('Feedback backend is not configured. Run setupTornFcaFeedbackBackend() first.');return SpreadsheetApp.openById(id);}
function tfEnsureSheet_(ss,name,headers){let sheet=ss.getSheetByName(name);if(!sheet)sheet=ss.insertSheet(name);if(sheet.getLastRow()===0)sheet.appendRow(headers);return sheet;}
function tfJson_(obj){return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);}
