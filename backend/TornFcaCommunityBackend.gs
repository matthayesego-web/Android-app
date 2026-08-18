/**
 * TornFCA Community Backend v1.7.0.
 * Deploy this file as its OWN Google Apps Script web app, separate from faction, premium and developer backends.
 * Torn API keys are used only to verify the current request and are never written to Sheets/Properties/Cache.
 * Faction membership/position is read fresh from Torn on every request; only stable basic player identity may be
 * cached briefly behind a SHA-256 API-key fingerprint.
 *
 * Moderation policy is configurable. Until the final capability matrix is approved, the default is owner-only.
 * Later, Script Properties can enable Leader/Co-leader access and/or qualifying Torn abilities without hard-coding
 * custom faction position names.
 *
 * Scale note: chat polling reads bounded recent chunks rather than the entire historical chat sheet. Exact message
 * and report lookups use TextFinder so old history does not make each active poll progressively more expensive.
 * War Prep configuration and completion rows are always keyed by freshly verified faction_id + war_id.
 */
const TC_VERSION='1.7.0';
const TC_CHAT='ChatMessages';
const TC_REPORTS='ChatReports';
const TC_DEVICES='PushDevices';
const TC_TRAINING_RULES='TrainingRules';
const TC_TRAINING_GUIDES='TrainingGuides';
const TC_WAR_PREP_CONFIG='WarPrepConfig';
const TC_WAR_PREP_STATUS='WarPrepStatus';
const TC_CHANNELS=['general','war','oc','leadership'];
const TC_MODERATOR_PLAYER_ID=3987363;
const TC_CHAT_SCAN_LIMIT=5000;
const TC_CHAT_CHUNK=500;

function setupTornFcaCommunityBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet(),props=PropertiesService.getScriptProperties();
  props.setProperty('COMMUNITY_SHEET_ID',ss.getId());
  if(props.getProperty('MODERATION_ALLOW_LEADERS')===null)props.setProperty('MODERATION_ALLOW_LEADERS','false');
  if(props.getProperty('MODERATION_ABILITIES')===null)props.setProperty('MODERATION_ABILITIES','');
  tcEnsureSheet_(ss,TC_CHAT,['id','faction_id','channel','author_id','author_name','message','created_at']);
  tcEnsureSheet_(ss,TC_REPORTS,['id','faction_id','reporter_id','reporter_name','message_id','author_id','author_name','channel','reason','message_snapshot','created_at','status','resolved_by_id','resolved_by_name','resolved_at','resolution']);
  tcEnsureSheet_(ss,TC_DEVICES,['token_hash','faction_id','player_id','token','preferences_json','platform','updated_at','active']);
  tcEnsureSheet_(ss,TC_TRAINING_RULES,['faction_id','stat_gain_target','xanax_target','notes','updated_by_id','updated_by_name','updated_at']);
  tcEnsureSheet_(ss,TC_TRAINING_GUIDES,['id','faction_id','title','category','body','author_id','author_name','updated_at','active']);
  tcEnsureSheet_(ss,TC_WAR_PREP_CONFIG,['faction_id','items_json','updated_by_id','updated_by_name','updated_at']);
  tcEnsureSheet_(ss,TC_WAR_PREP_STATUS,['faction_id','war_id','player_id','player_name','completed_json','first_seen_at','updated_at']);
  return{ok:true,sheet_id:ss.getId(),schema_version:7,next:'Deploy this Apps Script as a web app. Moderation remains owner-only until MODERATION_ALLOW_LEADERS and/or MODERATION_ABILITIES are deliberately configured. Store Firebase service-account values only in Script Properties if cloud push is desired.'};
}

function doGet(){return tcJson_({ok:true,app:'TornFCA Community Backend',version:TC_VERSION,authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}'),action=String(body.action||''),key=String(body.apiKey||'').trim();
    if(!key)throw new Error('API key required.');
    const user=tcVerifyUser_(key);

    if(action==='config')return tcJson_({ok:true,user:tcPublicUser_(user),capabilities:{chat:true,chat_reporting:true,moderation:tcCanModerate_(key,user),training:true,war_prep:true,push:tcFirebaseConfigured_()}});
    if(action==='chat_list')return tcJson_({ok:true,user:tcPublicUser_(user),messages:tcReadChat_(user,body)});
    if(action==='chat_send')return tcJson_({ok:true,message:tcSendChat_(user,body)});
    if(action==='chat_report')return tcJson_({ok:true,report:tcReportChat_(user,body)});
    if(action==='moderation_list'){
      tcRequireModerator_(key,user);
      return tcJson_({ok:true,reports:tcModerationList_(user,tcOwnerModerator_(user))});
    }
    if(action==='moderation_resolve'){
      tcRequireModerator_(key,user);
      return tcJson_({ok:true,result:tcModerationResolve_(user,body,tcOwnerModerator_(user))});
    }
    if(action==='training_library')return tcJson_({ok:true,user:tcPublicUser_(user),trainingRules:tcReadTrainingRules_(user.faction_id),guides:tcReadTrainingGuides_(user.faction_id)});
    if(action==='training_rules_save'){
      if(!tcLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required to change training rules.');
      return tcJson_({ok:true,trainingRules:tcSaveTrainingRules_(user,body)});
    }
    if(action==='training_guide_save'){
      if(!tcLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required to publish training guides.');
      return tcJson_({ok:true,guide:tcSaveTrainingGuide_(user,body)});
    }
    if(action==='training_guide_archive'){
      if(!tcLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required to archive training guides.');
      return tcJson_({ok:true,archived:tcArchiveTrainingGuide_(user,body)});
    }
    if(action==='warprep_state')return tcJson_({ok:true,warPrep:tcWarPrepState_(user,body)});
    if(action==='warprep_status_save')return tcJson_({ok:true,warPrep:tcSaveWarPrepStatus_(user,body)});
    if(action==='warprep_leadership'){
      if(!tcLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required to review War Prep.');
      return tcJson_({ok:true,warPrep:tcWarPrepLeadership_(user,body)});
    }
    if(action==='warprep_config_save'){
      if(!tcLeader_(user.position))throw new Error('Faction Leader or Co-leader access is required to change War Prep options.');
      return tcJson_({ok:true,warPrep:tcSaveWarPrepConfig_(user,body)});
    }
    if(action==='push_register')return tcJson_({ok:true,device:tcRegisterDevice_(user,body)});
    if(action==='push_unregister')return tcJson_({ok:true,removed:tcUnregisterDevice_(user,body)});
    if(action==='push_test'){
      tcRate_(user.id,'push_test',10);
      return tcJson_({ok:true,push:tcPushToPlayer_(user.faction_id,user.id,'personal','TornFCA cloud push is ready','This device is registered for TornFCA push notifications.',{})});
    }
    if(action==='announcement_push'){
      if(!tcCanAnnounce_(key,user))throw new Error('Announcement Changes permission is required.');
      const title=String(body.title||'Faction update').trim().slice(0,120),message=String(body.message||'').trim().slice(0,2000);
      if(!message)throw new Error('Announcement message required.');
      return tcJson_({ok:true,push:tcPushToFaction_(user.faction_id,0,'faction',title,message,{})});
    }
    throw new Error('Unknown action.');
  }catch(err){return tcJson_({ok:false,error:String(err&&err.message||err)});}
}

function tcVerifyUser_(apiKey){
  const factionData=tcTornGet_('/user/faction',apiKey),faction=factionData&&factionData.faction;
  if(!faction||!Number(faction.id||0))throw new Error('Torn account is not currently in a faction.');
  const fp=tcHash_(apiKey),cache=CacheService.getScriptCache(),ck='basic:'+fp,cached=cache.get(ck);let profile=null;
  if(cached){try{profile=JSON.parse(cached);}catch(_){} }
  if(!profile||!Number(profile.id||0)){
    const basic=tcTornGet_('/user/basic',apiKey);profile=basic&&basic.profile||{};
    if(!Number(profile.id||0))throw new Error('Unable to verify Torn player identity.');
    profile={id:Number(profile.id),name:String(profile.name||'Unknown')};
    cache.put(ck,JSON.stringify(profile),120);
  }
  return{id:Number(profile.id),name:String(profile.name||'Unknown'),faction_id:Number(faction.id||0),faction_name:String(faction.name||''),position:String(faction.position||'')};
}

function tcTornGet_(path,key){
  const joiner=String(path).indexOf('?')>=0?'&':'?',response=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(key),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Community/'+TC_VERSION}});
  let data;try{data=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(data&&data.error)throw new Error(data.error.error||('Torn API error '+data.error.code));
  if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());
  return data;
}

function tcPublicUser_(u){return{id:u.id,name:u.name,faction_id:u.faction_id,faction_name:u.faction_name,position:u.position};}
function tcLeader_(position){const n=String(position||'').toLowerCase().replace(/[-_\s]/g,'');return n==='leader'||n==='coleader';}
function tcOwnerModerator_(user){return Number(user&&user.id||0)===TC_MODERATOR_PLAYER_ID;}

function tcModerationPolicy_(){
  const p=PropertiesService.getScriptProperties(),allowLeaders=tcBool_(p.getProperty('MODERATION_ALLOW_LEADERS')),raw=String(p.getProperty('MODERATION_ABILITIES')||'').trim();
  let abilities=[];
  if(raw){try{const parsed=JSON.parse(raw);if(Array.isArray(parsed))abilities=parsed;}catch(_){abilities=raw.split(/[|,\n]/);}}
  abilities=abilities.map(v=>String(v||'').trim().toLowerCase()).filter(Boolean);
  return{allow_leaders:allowLeaders,abilities:abilities};
}

function tcPositionAbilities_(apiKey,user){
  const data=tcTornGet_('/faction/positions',apiKey),positions=data&&data.positions,current=String(user.position||'').trim().toLowerCase();
  if(!Array.isArray(positions))return[];
  for(let i=0;i<positions.length;i++){
    const p=positions[i]||{};
    if(String(p.name||'').trim().toLowerCase()!==current)continue;
    return Array.isArray(p.abilities)?p.abilities.map(v=>String(v||'').trim()):[];
  }
  return[];
}

function tcCanModerate_(apiKey,user){
  if(tcOwnerModerator_(user))return true;
  const policy=tcModerationPolicy_();
  if(policy.allow_leaders&&tcLeader_(user.position))return true;
  if(!policy.abilities.length)return false;
  const actual=tcPositionAbilities_(apiKey,user).map(v=>v.toLowerCase());
  return policy.abilities.some(v=>actual.indexOf(v)>=0);
}
function tcRequireModerator_(apiKey,user){if(!tcCanModerate_(apiKey,user))throw new Error('Community moderation permission is required.');}
function tcCanAnnounce_(apiKey,user){if(tcLeader_(user.position))return true;return tcPositionAbilities_(apiKey,user).some(a=>String(a||'').trim().toLowerCase()==='announcement changes');}

function tcChannel_(user,raw){
  const c=String(raw||'general').toLowerCase();
  if(TC_CHANNELS.indexOf(c)<0)throw new Error('Unknown chat channel.');
  if(c==='leadership'&&!tcLeader_(user.position))throw new Error('Leadership chat is restricted to Leader/Co-leader.');
  return c;
}

function tcReadChat_(user,body){
  tcRate_(user.id,'chat_list',2);
  const channel=tcChannel_(user,body.channel),sheet=tcDb_().getSheetByName(TC_CHAT),out=[];
  let end=sheet.getLastRow(),scanned=0;
  while(end>=2&&scanned<TC_CHAT_SCAN_LIMIT&&out.length<75){
    const count=Math.min(TC_CHAT_CHUNK,end-1,TC_CHAT_SCAN_LIMIT-scanned),start=end-count+1,values=sheet.getRange(start,1,count,7).getValues();
    for(let i=values.length-1;i>=0&&out.length<75;i--){
      if(Number(values[i][1])!==user.faction_id||String(values[i][2])!==channel)continue;
      out.push({id:String(values[i][0]||''),faction_id:Number(values[i][1]||0),channel:String(values[i][2]||'general'),author_id:Number(values[i][3]||0),author_name:String(values[i][4]||'Member'),message:String(values[i][5]||''),created_at:Number(values[i][6]||0)});
    }
    scanned+=count;end=start-1;
  }
  out.reverse();return out;
}

function tcSendChat_(user,body){
  tcRate_(user.id,'chat_send',2);
  const channel=tcChannel_(user,body.channel),message=String(body.message||'').trim();
  if(!message)throw new Error('Message required.');
  if(message.length>1000)throw new Error('Messages are limited to 1,000 characters.');
  const row={id:Utilities.getUuid(),faction_id:user.faction_id,channel:channel,author_id:user.id,author_name:user.name,message:message,created_at:Math.floor(Date.now()/1000)};
  tcDb_().getSheetByName(TC_CHAT).appendRow([row.id,row.faction_id,row.channel,row.author_id,tcSafe_(row.author_name),tcSafe_(row.message),row.created_at]);
  if(channel!=='leadership')tcPushToFaction_(user.faction_id,user.id,'chat',user.name+' • '+channel,message,{channel:channel,author_id:String(user.id)});
  return row;
}

function tcReportChat_(user,body){
  tcRate_(user.id,'chat_report',5);
  const messageId=String(body.messageId||'').trim(),reason=String(body.reason||'').trim().slice(0,1000);
  if(!messageId)throw new Error('Message ID required.');
  const ss=tcDb_(),chat=ss.getSheetByName(TC_CHAT),chatRow=tcFindExactRow_(chat,1,messageId);let target=null;
  if(chatRow>0){
    const row=chat.getRange(chatRow,1,1,7).getValues()[0];
    if(Number(row[1]||0)!==user.faction_id)throw new Error('Message does not belong to your current faction.');
    target={id:String(row[0]||''),channel:String(row[2]||'general'),author_id:Number(row[3]||0),author_name:String(row[4]||'Member'),message:String(row[5]||''),created_at:Number(row[6]||0)};
  }
  if(!target)throw new Error('Message not found.');
  if(target.author_id===user.id)throw new Error('You cannot report your own message.');
  const reports=tcEnsureSheet_(ss,TC_REPORTS,['id','faction_id','reporter_id','reporter_name','message_id','author_id','author_name','channel','reason','message_snapshot','created_at','status','resolved_by_id','resolved_by_name','resolved_at','resolution']);
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const existing=tcTailRows_(reports,500,16);
    for(let i=existing.length-1;i>=0;i--){
      if(Number(existing[i][1])===user.faction_id&&Number(existing[i][2])===user.id&&String(existing[i][4])===messageId&&String(existing[i][11])==='open')return{id:String(existing[i][0]||''),status:'open',duplicate:true};
    }
    const id=Utilities.getUuid(),now=Math.floor(Date.now()/1000);
    reports.appendRow([id,user.faction_id,user.id,tcSafe_(user.name),messageId,target.author_id,tcSafe_(target.author_name),tcSafe_(target.channel),tcSafe_(reason||'No reason supplied'),tcSafe_(target.message.slice(0,2000)),now,'open','','','','']);
    return{id:id,status:'open',duplicate:false};
  }finally{lock.releaseLock();}
}

function tcModerationList_(user,globalScope){
  const sheet=tcEnsureSheet_(tcDb_(),TC_REPORTS,['id','faction_id','reporter_id','reporter_name','message_id','author_id','author_name','channel','reason','message_snapshot','created_at','status','resolved_by_id','resolved_by_name','resolved_at','resolution']),values=sheet.getDataRange().getValues(),out=[];
  for(let i=1;i<values.length;i++){
    if(String(values[i][11]||'open')!=='open')continue;
    const factionId=Number(values[i][1]||0);if(!globalScope&&factionId!==Number(user.faction_id))continue;
    out.push({id:String(values[i][0]||''),faction_id:factionId,reporter_id:Number(values[i][2]||0),reporter_name:String(values[i][3]||'Member'),message_id:String(values[i][4]||''),author_id:Number(values[i][5]||0),author_name:String(values[i][6]||'Member'),channel:String(values[i][7]||'general'),reason:String(values[i][8]||'No reason supplied'),message_snapshot:String(values[i][9]||''),created_at:Number(values[i][10]||0),status:'open'});
  }
  out.sort((a,b)=>b.created_at-a.created_at);return out.slice(0,200);
}

function tcModerationResolve_(user,body,globalScope){
  tcRate_(user.id,'moderation_resolve',2);
  const reportId=String(body.reportId||'').trim(),resolution=String(body.resolution||'').trim();
  if(!reportId)throw new Error('Report ID required.');
  if(['dismiss','remove_message'].indexOf(resolution)<0)throw new Error('Unknown moderation resolution.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const ss=tcDb_(),reports=tcEnsureSheet_(ss,TC_REPORTS,['id','faction_id','reporter_id','reporter_name','message_id','author_id','author_name','channel','reason','message_snapshot','created_at','status','resolved_by_id','resolved_by_name','resolved_at','resolution']),reportRow=tcFindExactRow_(reports,1,reportId);
    if(reportRow<2)throw new Error('Report not found.');
    const target=reports.getRange(reportRow,1,1,16).getValues()[0],targetFaction=Number(target[1]||0),messageId=String(target[4]||'');
    if(String(target[11]||'open')!=='open')throw new Error('Report is already resolved.');
    if(!globalScope&&targetFaction!==Number(user.faction_id))throw new Error('Moderation report belongs to another faction.');
    const now=Math.floor(Date.now()/1000),label=resolution==='remove_message'?'message_removed':'dismissed';let closed=0;
    if(resolution==='remove_message'){
      const chat=ss.getSheetByName(TC_CHAT),chatRow=tcFindExactRow_(chat,1,messageId);
      if(chatRow>0){const chatMeta=chat.getRange(chatRow,1,1,2).getValues()[0];if(Number(chatMeta[1]||0)===targetFaction)chat.getRange(chatRow,6).setValue('[Removed by TornFCA moderation]');}
      const reportRows=tcFindAllExactRows_(reports,5,messageId);
      reportRows.forEach(row=>{
        const meta=reports.getRange(row,2,1,11).getValues()[0];
        if(Number(meta[0]||0)!==targetFaction||String(meta[10]||'open')!=='open')return;
        reports.getRange(row,12,1,5).setValues([['resolved',user.id,tcSafe_(user.name),now,label]]);closed++;
      });
    }else{reports.getRange(reportRow,12,1,5).setValues([['resolved',user.id,tcSafe_(user.name),now,label]]);closed=1;}
    return{report_id:reportId,resolution:label,closed_reports:closed,message_id:messageId,faction_id:targetFaction};
  }finally{lock.releaseLock();}
}

function tcTrainingSheets_(){
  const ss=tcDb_();
  return{rules:tcEnsureSheet_(ss,TC_TRAINING_RULES,['faction_id','stat_gain_target','xanax_target','notes','updated_by_id','updated_by_name','updated_at']),guides:tcEnsureSheet_(ss,TC_TRAINING_GUIDES,['id','faction_id','title','category','body','author_id','author_name','updated_at','active'])};
}

function tcReadTrainingRules_(factionId){
  const sheet=tcTrainingSheets_().rules,values=sheet.getDataRange().getValues();
  for(let i=values.length-1;i>=1;i--)if(Number(values[i][0])===Number(factionId))return{faction_id:Number(values[i][0]||0),stat_gain_target:String(values[i][1]||''),xanax_target:String(values[i][2]||''),notes:String(values[i][3]||''),updated_by_id:Number(values[i][4]||0),updated_by_name:String(values[i][5]||''),updated_at:Number(values[i][6]||0)};
  return{};
}

function tcReadTrainingGuides_(factionId){
  const sheet=tcTrainingSheets_().guides,values=sheet.getDataRange().getValues(),out=[];
  for(let i=1;i<values.length;i++){
    if(Number(values[i][1])!==Number(factionId)||!tcBool_(values[i][8]))continue;
    out.push({id:String(values[i][0]||''),faction_id:Number(values[i][1]||0),title:String(values[i][2]||''),category:String(values[i][3]||'Guide'),body:String(values[i][4]||''),author_id:Number(values[i][5]||0),author_name:String(values[i][6]||''),updated_at:Number(values[i][7]||0)});
  }
  out.sort((a,b)=>b.updated_at-a.updated_at);return out.slice(0,100);
}

function tcSaveTrainingRules_(user,body){
  tcRate_(user.id,'training_rules_save',2);
  const statGain=String(body.statGainTarget||'').trim().slice(0,160),xanax=String(body.xanaxTarget||'').trim().slice(0,160),notes=String(body.notes||'').trim().slice(0,3000);
  if(!statGain&&!xanax&&!notes)throw new Error('Add at least one training expectation before saving.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const sheet=tcTrainingSheets_().rules,values=sheet.getDataRange().getValues(),now=Math.floor(Date.now()/1000),row=[user.faction_id,tcSafe_(statGain),tcSafe_(xanax),tcSafe_(notes),user.id,tcSafe_(user.name),now];
    for(let i=1;i<values.length;i++)if(Number(values[i][0])===user.faction_id){sheet.getRange(i+1,1,1,7).setValues([row]);return{faction_id:user.faction_id,stat_gain_target:statGain,xanax_target:xanax,notes:notes,updated_by_id:user.id,updated_by_name:user.name,updated_at:now};}
    sheet.appendRow(row);return{faction_id:user.faction_id,stat_gain_target:statGain,xanax_target:xanax,notes:notes,updated_by_id:user.id,updated_by_name:user.name,updated_at:now};
  }finally{lock.releaseLock();}
}

function tcSaveTrainingGuide_(user,body){
  tcRate_(user.id,'training_guide_save',2);
  const requested=String(body.id||'').trim(),title=String(body.title||'').trim().slice(0,160),category=String(body.category||'Guide').trim().slice(0,80)||'Guide',text=String(body.body||'').trim().slice(0,8000);
  if(!title)throw new Error('Guide title required.');if(!text)throw new Error('Guide content required.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const sheet=tcTrainingSheets_().guides,values=sheet.getDataRange().getValues(),now=Math.floor(Date.now()/1000),id=requested||Utilities.getUuid(),row=[id,user.faction_id,tcSafe_(title),tcSafe_(category),tcSafe_(text),user.id,tcSafe_(user.name),now,true];
    if(requested){
      for(let i=1;i<values.length;i++){
        if(String(values[i][0])!==requested)continue;
        if(Number(values[i][1])!==user.faction_id)throw new Error('Guide does not belong to your current faction.');
        sheet.getRange(i+1,1,1,9).setValues([row]);return{id:id,faction_id:user.faction_id,title:title,category:category,body:text,author_id:user.id,author_name:user.name,updated_at:now};
      }
      throw new Error('Guide not found.');
    }
    sheet.appendRow(row);return{id:id,faction_id:user.faction_id,title:title,category:category,body:text,author_id:user.id,author_name:user.name,updated_at:now};
  }finally{lock.releaseLock();}
}

function tcArchiveTrainingGuide_(user,body){
  tcRate_(user.id,'training_guide_archive',2);
  const id=String(body.id||'').trim();if(!id)throw new Error('Guide ID required.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const sheet=tcTrainingSheets_().guides,values=sheet.getDataRange().getValues();
    for(let i=1;i<values.length;i++){
      if(String(values[i][0])!==id)continue;
      if(Number(values[i][1])!==user.faction_id)throw new Error('Guide does not belong to your current faction.');
      sheet.getRange(i+1,9).setValue(false);return true;
    }
    throw new Error('Guide not found.');
  }finally{lock.releaseLock();}
}

function tcWarPrepSheets_(){
  const ss=tcDb_();
  return{config:tcEnsureSheet_(ss,TC_WAR_PREP_CONFIG,['faction_id','items_json','updated_by_id','updated_by_name','updated_at']),status:tcEnsureSheet_(ss,TC_WAR_PREP_STATUS,['faction_id','war_id','player_id','player_name','completed_json','first_seen_at','updated_at'])};
}
function tcDefaultWarPrepItems_(){return[
  {id:'item1',title:'Reviewed current war mode and timing'},
  {id:'item2',title:'Checked travel'},
  {id:'item3',title:'Checked cooldowns & refills'},
  {id:'item4',title:'Reviewed faction resources'},
  {id:'item5',title:'Reviewed current instructions'}
];}
function tcWarId_(body){const warId=Math.floor(Number(body&&body.warId||0));if(warId<=0)throw new Error('A current or upcoming ranked-war ID is required for shared War Prep.');return warId;}
function tcReadWarPrepConfig_(factionId){
  const sheet=tcWarPrepSheets_().config,row=tcFindExactRow_(sheet,1,String(factionId));if(row<2)return tcDefaultWarPrepItems_();
  let parsed=[];try{parsed=JSON.parse(String(sheet.getRange(row,2).getValue()||'[]'));}catch(_){}
  if(!Array.isArray(parsed)||!parsed.length)return tcDefaultWarPrepItems_();return parsed.slice(0,8).map((v,i)=>({id:String(v&&v.id||('item'+(i+1))),title:String(v&&v.title||'').slice(0,120)})).filter(v=>v.title);
}
function tcFindWarPrepStatusRow_(sheet,factionId,warId,playerId){
  const rows=tcFindAllExactRows_(sheet,3,String(playerId));for(let i=0;i<rows.length;i++){const meta=sheet.getRange(rows[i],1,1,3).getValues()[0];if(Number(meta[0])===Number(factionId)&&Number(meta[1])===Number(warId))return rows[i];}return 0;
}
function tcReadWarPrepStatus_(user,warId){
  const sheet=tcWarPrepSheets_().status,row=tcFindWarPrepStatusRow_(sheet,user.faction_id,warId,user.id);if(row<2)return{war_id:warId,player_id:user.id,player_name:user.name,completed:{},first_seen_at:0,updated_at:0};
  const v=sheet.getRange(row,1,1,7).getValues()[0];let completed={};try{completed=JSON.parse(String(v[4]||'{}'));}catch(_){}
  return{war_id:Number(v[1]||warId),player_id:Number(v[2]||user.id),player_name:String(v[3]||user.name),completed:completed,first_seen_at:Number(v[5]||0),updated_at:Number(v[6]||0)};
}
function tcTouchWarPrep_(user,warId){
  const lock=LockService.getScriptLock();lock.waitLock(10000);try{const sheet=tcWarPrepSheets_().status,row=tcFindWarPrepStatusRow_(sheet,user.faction_id,warId,user.id);if(row>=2)return;const now=Math.floor(Date.now()/1000);sheet.appendRow([user.faction_id,warId,user.id,tcSafe_(user.name),'{}',now,now]);}finally{lock.releaseLock();}
}
function tcWarPrepState_(user,body){const warId=tcWarId_(body);tcTouchWarPrep_(user,warId);return{war_id:warId,items:tcReadWarPrepConfig_(user.faction_id),status:tcReadWarPrepStatus_(user,warId),scope:'verified_faction_only'};}
function tcSanitizeCompleted_(raw){
  let value=raw;if(typeof raw==='string'){try{value=JSON.parse(raw);}catch(_){value={};}}if(!value||typeof value!=='object'||Array.isArray(value))value={};const out={};Object.keys(value).slice(0,16).forEach(k=>{const key=String(k).slice(0,40);if(key)out[key]=tcBool_(value[k]);});return out;
}
function tcSaveWarPrepStatus_(user,body){
  tcRate_(user.id,'warprep_status_save',1);const warId=tcWarId_(body),completed=tcSanitizeCompleted_(body.completed),sheet=tcWarPrepSheets_().status,lock=LockService.getScriptLock();lock.waitLock(10000);
  try{const now=Math.floor(Date.now()/1000),row=tcFindWarPrepStatusRow_(sheet,user.faction_id,warId,user.id),json=JSON.stringify(completed);if(row>=2){const first=Number(sheet.getRange(row,6).getValue()||now);sheet.getRange(row,1,1,7).setValues([[user.faction_id,warId,user.id,tcSafe_(user.name),tcSafe_(json),first,now]]);}else sheet.appendRow([user.faction_id,warId,user.id,tcSafe_(user.name),tcSafe_(json),now,now]);return{war_id:warId,player_id:user.id,completed:completed,updated_at:now};}finally{lock.releaseLock();}
}
function tcWarPrepLeadership_(user,body){
  const warId=tcWarId_(body),sheet=tcWarPrepSheets_().status,values=sheet.getDataRange().getValues(),members=[];for(let i=1;i<values.length;i++){if(Number(values[i][0])!==user.faction_id||Number(values[i][1])!==warId)continue;let completed={};try{completed=JSON.parse(String(values[i][4]||'{}'));}catch(_){}members.push({player_id:Number(values[i][2]||0),player_name:String(values[i][3]||'Member'),completed:completed,first_seen_at:Number(values[i][5]||0),updated_at:Number(values[i][6]||0)});}members.sort((a,b)=>a.player_name.localeCompare(b.player_name));return{war_id:warId,items:tcReadWarPrepConfig_(user.faction_id),members:members,app_users_only:true,scope:'verified_faction_only'};
}
function tcSaveWarPrepConfig_(user,body){
  let input=body&&body.items;if(typeof input==='string'){try{input=JSON.parse(input);}catch(_){input=[];}}if(!Array.isArray(input))throw new Error('War Prep items must be an array.');const clean=[];for(let i=0;i<input.length&&clean.length<8;i++){const raw=input[i],title=String(raw&&typeof raw==='object'?raw.title:raw||'').trim().slice(0,120);if(title)clean.push({id:'item'+(clean.length+1),title:title});}if(!clean.length)throw new Error('Add at least one War Prep checklist item.');
  const sheet=tcWarPrepSheets_().config,lock=LockService.getScriptLock();lock.waitLock(10000);try{const now=Math.floor(Date.now()/1000),row=tcFindExactRow_(sheet,1,String(user.faction_id)),values=[user.faction_id,tcSafe_(JSON.stringify(clean)),user.id,tcSafe_(user.name),now];if(row>=2)sheet.getRange(row,1,1,5).setValues([values]);else sheet.appendRow(values);return{items:clean,updated_by_id:user.id,updated_by_name:user.name,updated_at:now};}finally{lock.releaseLock();}
}

function tcRegisterDevice_(user,body){
  tcRate_(user.id,'push_register',5);
  const token=String(body.token||'').trim();if(token.length<20||token.length>4096)throw new Error('Valid FCM token required.');
  let prefs=String(body.preferences||'{}');if(prefs.length>4000)prefs='{}';
  const platform=String(body.platform||'android').slice(0,40),hash=tcHash_(token),sheet=tcDb_().getSheetByName(TC_DEVICES),now=Math.floor(Date.now()/1000);
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const values=sheet.getDataRange().getValues();
    for(let i=1;i<values.length;i++){
      if(String(values[i][0])!==hash)continue;
      sheet.getRange(i+1,1,1,8).setValues([[hash,user.faction_id,user.id,tcSafe_(token),tcSafe_(prefs),platform,now,true]]);return{registered:true,updated:true};
    }
    sheet.appendRow([hash,user.faction_id,user.id,tcSafe_(token),tcSafe_(prefs),platform,now,true]);return{registered:true,updated:false};
  }finally{lock.releaseLock();}
}

function tcUnregisterDevice_(user,body){
  const hash=tcHash_(String(body.token||'')),sheet=tcDb_().getSheetByName(TC_DEVICES),values=sheet.getDataRange().getValues();let removed=0;
  for(let i=1;i<values.length;i++){
    if(String(values[i][0])!==hash||Number(values[i][1])!==user.faction_id||Number(values[i][2])!==user.id)continue;
    sheet.getRange(i+1,8).setValue(false);removed++;
  }
  return removed;
}

function tcDevices_(factionId,playerId,type,excludePlayer){
  const sheet=tcDb_().getSheetByName(TC_DEVICES),values=sheet.getDataRange().getValues(),out=[],cutoff=Math.floor(Date.now()/1000)-90*86400;
  for(let i=1;i<values.length;i++){
    if(!tcBool_(values[i][7])||Number(values[i][1])!==Number(factionId)||Number(values[i][6]||0)<cutoff)continue;
    const pid=Number(values[i][2]||0);if(playerId&&pid!==Number(playerId))continue;if(excludePlayer&&pid===Number(excludePlayer))continue;
    let prefs={};try{prefs=JSON.parse(String(values[i][4]||'{}'));}catch(_){}
    if(prefs.master===false)continue;if(type&&prefs[type]===false)continue;
    out.push({token:String(values[i][3]||''),player_id:pid});if(out.length>=100)break;
  }
  return out;
}

function tcPushToFaction_(factionId,excludePlayer,type,title,body,data){return tcPush_(tcDevices_(factionId,0,type,excludePlayer),factionId,type,title,body,data);}
function tcPushToPlayer_(factionId,playerId,type,title,body,data){const scoped=Object.assign({},data||{},{target_player_id:String(playerId)});return tcPush_(tcDevices_(factionId,playerId,type,0),factionId,type,title,body,scoped);}

function tcPush_(devices,factionId,type,title,body,data){
  if(!tcFirebaseConfigured_())return{configured:false,sent:0,failed:0};
  let access;try{access=tcFirebaseAccessToken_();}catch(err){return{configured:true,sent:0,failed:devices.length,error:String(err&&err.message||err)};}
  let sent=0,failed=0;
  devices.forEach(d=>{
    try{
      const payload={type:String(type||'personal'),title:String(title||'TornFCA'),body:String(body||''),faction_id:String(factionId||0)};
      Object.keys(data||{}).forEach(k=>payload[k]=String(data[k]));
      const project=PropertiesService.getScriptProperties().getProperty('FIREBASE_PROJECT_ID'),response=UrlFetchApp.fetch('https://fcm.googleapis.com/v1/projects/'+encodeURIComponent(project)+'/messages:send',{method:'post',muteHttpExceptions:true,contentType:'application/json',headers:{Authorization:'Bearer '+access},payload:JSON.stringify({message:{token:d.token,data:payload,android:{priority:'high'}}})});
      if(response.getResponseCode()>=200&&response.getResponseCode()<300)sent++;else failed++;
    }catch(_){failed++;}
  });
  return{configured:true,sent:sent,failed:failed};
}

function tcFirebaseConfigured_(){const p=PropertiesService.getScriptProperties();return!!(p.getProperty('FIREBASE_PROJECT_ID')&&p.getProperty('FIREBASE_CLIENT_EMAIL')&&p.getProperty('FIREBASE_PRIVATE_KEY'));}
function tcFirebaseAccessToken_(){
  const cache=CacheService.getScriptCache(),cached=cache.get('firebase_access_token');if(cached)return cached;
  const p=PropertiesService.getScriptProperties(),email=p.getProperty('FIREBASE_CLIENT_EMAIL'),privateKey=String(p.getProperty('FIREBASE_PRIVATE_KEY')||'').replace(/\\n/g,'\n');
  if(!email||!privateKey)throw new Error('Firebase service account is not configured.');
  const now=Math.floor(Date.now()/1000),header=tcB64_(JSON.stringify({alg:'RS256',typ:'JWT'})),claim=tcB64_(JSON.stringify({iss:email,scope:'https://www.googleapis.com/auth/firebase.messaging',aud:'https://oauth2.googleapis.com/token',iat:now,exp:now+3600})),unsigned=header+'.'+claim,signature=Utilities.computeRsaSha256Signature(unsigned,privateKey),jwt=unsigned+'.'+Utilities.base64EncodeWebSafe(signature).replace(/=+$/,'');
  const response=UrlFetchApp.fetch('https://oauth2.googleapis.com/token',{method:'post',muteHttpExceptions:true,payload:{grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion:jwt}});let data;
  try{data=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Firebase OAuth response.');}
  if(response.getResponseCode()<200||response.getResponseCode()>=300||!data.access_token)throw new Error(data.error_description||'Firebase OAuth failed.');
  cache.put('firebase_access_token',String(data.access_token),Math.max(60,Math.min(3300,Number(data.expires_in||3600)-60)));return String(data.access_token);
}

function tcB64_(value){return Utilities.base64EncodeWebSafe(value,Utilities.Charset.UTF_8).replace(/=+$/,'');}
function tcRate_(playerId,action,seconds){const cache=CacheService.getScriptCache(),key='rate:'+playerId+':'+action;if(cache.get(key))throw new Error('Please wait a moment before trying again.');cache.put(key,'1',Math.max(1,seconds));}
function tcHash_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value||''),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function tcFindExactRow_(sheet,column,value){const last=sheet?sheet.getLastRow():0;if(last<2)return 0;const found=sheet.getRange(2,column,last-1,1).createTextFinder(String(value)).matchEntireCell(true).findNext();return found?found.getRow():0;}
function tcFindAllExactRows_(sheet,column,value){const last=sheet?sheet.getLastRow():0;if(last<2)return[];return sheet.getRange(2,column,last-1,1).createTextFinder(String(value)).matchEntireCell(true).findAll().map(r=>r.getRow());}
function tcTailRows_(sheet,maxRows,width){const last=sheet?sheet.getLastRow():0;if(last<2)return[];const count=Math.min(Math.max(1,maxRows),last-1),start=last-count+1;return sheet.getRange(start,1,count,width).getValues();}
function tcDb_(){const id=PropertiesService.getScriptProperties().getProperty('COMMUNITY_SHEET_ID');if(!id)throw new Error('Community backend is not configured. Run setupTornFcaCommunityBackend() first.');return SpreadsheetApp.openById(id);}
function tcEnsureSheet_(ss,name,headers){let sheet=ss.getSheetByName(name);if(!sheet)sheet=ss.insertSheet(name);if(sheet.getLastRow()===0)sheet.appendRow(headers);return sheet;}
function tcSafe_(value){const text=String(value==null?'':value);return/^[=+\-@]/.test(text)?"'"+text:text;}
function tcBool_(value){return value===true||String(value).toLowerCase()==='true'||Number(value)===1;}
function tcJson_(obj){return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);}
