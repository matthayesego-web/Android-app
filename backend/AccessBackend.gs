/**
 * TornFCA shared faction backend v1.1.0.
 * API keys verify requests and are never stored. Shared data is tenant-scoped by verified Torn faction ID.
 * Faction membership and position are read fresh from Torn on every authenticated request; only stable basic player
 * identity may be cached briefly by a SHA-256 API-key fingerprint.
 *
 * Existing single-faction deployments remain compatible: legacy Settings restrictions and the legacy listener token
 * are honored when present. New deployments default to unrestricted multi-faction tenancy, with every shared row
 * keyed by faction_id.
 */
const DF_TOOLKIT_VERSION='1.1.0';
const DF_SHEETS=Object.freeze({
  SETTINGS:'Settings',
  RANKS:'RankAccess',
  USERS:'UserOverrides',
  POSITIONS:'FactionPositions',
  NOTICES:'Notices',
  BANKING:'BankingRequests',
  LISTENERS:'ListenerTokens'
});
const DF_TOOLS=Object.freeze([
  ['ARMORY','Faction Armory Auditor'],
  ['TRAIN','Company Train Calculator']
]);
const DF_BANKING_STATUSES=Object.freeze(['PENDING','LIKELY_HANDLED','PAID','HANDLED','CANCELLED']);
const DF_LIKELY_HANDLED_THRESHOLD=1000000;

function setupTornFcaFactionBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet(),props=PropertiesService.getScriptProperties();
  props.setProperty('SHEET_ID',ss.getId());
  const settings=ensureSheet_(ss,DF_SHEETS.SETTINGS,['key','value']);
  setSettingIfMissing_(settings,'faction_name','');
  setSettingIfMissing_(settings,'faction_id','0');
  setSettingIfMissing_(settings,'restrict_faction','false');
  setSetting_(settings,'schema_version','4');
  const legacyFactionId=Number(getSetting_(settings,'faction_id')||0);

  ensureTenantSheet_(ss,DF_SHEETS.RANKS,['faction_id','rank_name','tool_id','allowed','updated_at','updated_by_id','updated_by_name'],legacyFactionId);
  ensureTenantSheet_(ss,DF_SHEETS.USERS,['faction_id','user_id','tool_id','allowed','updated_at','updated_by_id','updated_by_name'],legacyFactionId);
  ensureSheet_(ss,DF_SHEETS.POSITIONS,['faction_id','position_name','abilities_json','updated_at','updated_by_id','updated_by_name']);
  ensureSheet_(ss,DF_SHEETS.NOTICES,['id','faction_id','title','message','created_at','expires_at','author_id','author_name','active']);
  ensureSheet_(ss,DF_SHEETS.BANKING,['id','faction_id','requester_id','requester_name','requested_amount','request_mode','note','request_text','source','fingerprint','message_id','requested_at','detected_at','status','likely_handled','handled_at','handled_by_id','handled_by_name','updated_at']);
  ensureSheet_(ss,DF_SHEETS.LISTENERS,['faction_id','token_hash','updated_at','updated_by_id','updated_by_name']);

  return{ok:true,sheet_id:ss.getId(),schema_version:4,next:'Deploy as a web app. Existing faction restrictions remain if already configured; new installs are multi-faction by default. Run setup again after upgrading legacy sheets so RankAccess/UserOverrides receive faction_id.'};
}

/** Backward-compatible setup alias for existing deployments/documentation. */
function setupDuckForceBackend(){return setupTornFcaFactionBackend();}

/** Legacy single-faction listener token. Existing browser listeners may continue using it. */
function getDuckForceListenerToken(){return getTornFcaLegacyListenerToken();}
function getTornFcaLegacyListenerToken(){
  const props=PropertiesService.getScriptProperties();let token=props.getProperty('LISTENER_TOKEN');
  if(!token){token=Utilities.getUuid().replace(/-/g,'');props.setProperty('LISTENER_TOKEN',token);}
  return token;
}
function rotateDuckForceListenerToken(){return rotateTornFcaLegacyListenerToken();}
function rotateTornFcaLegacyListenerToken(){const token=Utilities.getUuid().replace(/-/g,'');PropertiesService.getScriptProperties().setProperty('LISTENER_TOKEN',token);return token;}

function doGet(){return json_({ok:true,app:'TornFCA Faction Backend',version:DF_TOOLKIT_VERSION,authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}'),action=String(body.action||'');
    if(action==='listener_event')return json_(handleListenerEvent_(body));

    const apiKey=String(body.apiKey||'').trim();if(!apiKey)throw new Error('API key required.');
    const user=verifyFactionUser_(apiKey);

    if(action==='config'){
      try{syncFactionPositions_(apiKey,user);}catch(_){}
      return json_({
        ok:true,
        user:publicUser_(user),
        can_manage:isLeaderOrCoLeader_(user.position),
        can_manage_banking:canManageBankingQueue_(apiKey,user),
        permissions:readPositionPermissions_(user.faction_id,user.position),
        rank_rules:readRankRules_(user.faction_id),
        user_overrides:readUserOverrides_(user.faction_id,user.id),
        capabilities:{notices:true,banking:true,listener:true,multi_tenant:true}
      });
    }

    if(action==='notices')return json_({ok:true,user:publicUser_(user),notices:readActiveNotices_(user.faction_id)});

    if(action==='positions'||action==='sync_positions'){
      if(!isLeaderOrCoLeader_(user.position))throw new Error('Leader or Co-leader required.');
      return json_({ok:true,user:publicUser_(user),positions:syncFactionPositions_(apiKey,user)});
    }

    if(action==='post_notice'){
      if(!hasCurrentPermission_(apiKey,user,'Announcement Changes'))throw new Error('Announcement Changes permission (or Leader/Co-leader) required.');
      return json_({ok:true,notice:createNotice_(user,body)});
    }

    if(action==='banking_submit')return json_({ok:true,request:createAppBankingRequest_(user,body)});

    if(action==='banking_list'){
      const canManage=canManageBankingQueue_(apiKey,user);let reconciliation=null,reconcileError='';
      if(canManage&&toBoolean_(body.reconcile)){
        try{reconciliation=reconcileBanking_(apiKey,user);}catch(err){reconcileError=String(err&&err.message||err);}
      }
      return json_({ok:true,user:publicUser_(user),can_manage:canManage,reconciliation:reconciliation,reconcile_error:reconcileError,requests:readBankingRequests_(user.faction_id,canManage?0:user.id)});
    }

    if(action==='banking_update'){
      if(!canManageBankingQueue_(apiKey,user))throw new Error('Money Giving / Balance Adjustment permission (or Leader/Co-leader) required.');
      return json_({ok:true,request:updateBankingRequest_(user,body)});
    }

    if(action==='banking_reconcile'){
      if(!canManageBankingQueue_(apiKey,user))throw new Error('Money Giving / Balance Adjustment permission (or Leader/Co-leader) required.');
      return json_({ok:true,reconciliation:reconcileBanking_(apiKey,user),requests:readBankingRequests_(user.faction_id,0)});
    }

    if(action==='listener_token_rotate'){
      if(!canManageBankingQueue_(apiKey,user))throw new Error('Banking management permission is required to configure the faction banking listener.');
      return json_({ok:true,faction_id:user.faction_id,listener_token:rotateFactionListenerToken_(user)});
    }

    if(!isLeaderOrCoLeader_(user.position))throw new Error('Leader or Co-leader required.');

    if(action==='save_rank_rules'){
      saveRankRules_(user.faction_id,String(body.rank_name||''),body.tools||{},user);
      return json_({ok:true,rank_rules:readRankRules_(user.faction_id)});
    }

    if(action==='save_user_overrides'){
      const userId=Number(body.user_id||0);if(!userId)throw new Error('Valid user_id required.');
      saveUserOverrides_(user.faction_id,userId,body.tools||{},user);
      return json_({ok:true,user_id:userId,user_overrides:readUserOverrides_(user.faction_id,userId)});
    }

    throw new Error('Unknown action.');
  }catch(err){return json_({ok:false,error:String(err&&err.message||err)});}
}

function handleListenerEvent_(body){
  const supplied=String(body.listener_token||''),requestedFaction=Number(body.faction_id||0);if(!supplied)throw new Error('Listener token required.');
  let factionId=0;
  if(requestedFaction&&validateFactionListenerToken_(requestedFaction,supplied))factionId=requestedFaction;
  if(!factionId){
    const props=PropertiesService.getScriptProperties(),legacy=String(props.getProperty('LISTENER_TOKEN')||''),settings=settings_();
    if(legacy&&supplied===legacy)factionId=Number(settings.faction_id||0);
  }
  if(!factionId)throw new Error('Invalid listener token or faction scope.');
  const result=ingestListenerBankingRequest_(factionId,body);
  return{ok:true,request:result.request,duplicate:result.duplicate};
}

function rotateFactionListenerToken_(actor){
  const token=Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,''),hash=sha256_(token),sheet=db_().getSheetByName(DF_SHEETS.LISTENERS),row=[actor.faction_id,hash,Math.floor(Date.now()/1000),actor.id,safeCellText_(actor.name)];
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const values=sheet.getDataRange().getValues();
    for(let i=1;i<values.length;i++){
      if(Number(values[i][0])!==Number(actor.faction_id))continue;
      sheet.getRange(i+1,1,1,5).setValues([row]);return token;
    }
    sheet.appendRow(row);return token;
  }finally{lock.releaseLock();}
}

function validateFactionListenerToken_(factionId,token){
  const values=db_().getSheetByName(DF_SHEETS.LISTENERS).getDataRange().getValues(),hash=sha256_(token);
  for(let i=1;i<values.length;i++)if(Number(values[i][0])===Number(factionId)&&String(values[i][1]||'')===hash)return true;
  return false;
}

function verifyFactionUser_(apiKey){
  // Tenant membership/position is authorization-sensitive and is always fresh.
  const factionData=tornGet_('/user/faction',apiKey),faction=factionData&&factionData.faction;
  if(!faction||!Number(faction.id||0))throw new Error('Torn account is not currently in a faction.');

  // Numeric player identity is stable and can be cached briefly without caching faction authorization.
  const fp=sha256_(apiKey),cache=CacheService.getScriptCache(),cacheKey='faction_basic:'+fp,cached=cache.get(cacheKey);let profile=null;
  if(cached){try{profile=JSON.parse(cached);}catch(_){} }
  if(!profile||!Number(profile.id||0)){
    const basicData=tornGet_('/user/basic',apiKey);profile=basicData&&basicData.profile||{};
    if(!Number(profile.id||0))throw new Error('Unable to verify Torn player identity.');
    profile={id:Number(profile.id),name:String(profile.name||'Unknown')};
    cache.put(cacheKey,JSON.stringify(profile),120);
  }
  const user={id:Number(profile.id),name:String(profile.name||'Unknown'),faction_id:Number(faction.id||0),faction_name:String(faction.name||''),position:String(faction.position||'')};

  const settings=settings_(),restrict=String(settings.restrict_faction||'false').toLowerCase()==='true',expectedId=Number(settings.faction_id||0),expectedName=String(settings.faction_name||'').trim();
  if(restrict){
    if(expectedId&&Number(user.faction_id)!==expectedId)throw new Error('This backend is restricted to the configured faction.');
    if(expectedName&&String(user.faction_name||'').toLowerCase()!==expectedName.toLowerCase())throw new Error('This backend is restricted to the configured faction.');
  }
  return user;
}

function isLeaderOrCoLeader_(position){const n=String(position||'').toLowerCase().replace(/[-_\s]/g,'');return n==='leader'||n==='coleader';}

function syncFactionPositions_(apiKey,actor){
  const result=tornGet_('/faction/positions',apiKey),positions=Array.isArray(result.positions)?result.positions:[],sheet=db_().getSheetByName(DF_SHEETS.POSITIONS);
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const values=sheet.getDataRange().getValues();
    for(let i=values.length-1;i>=1;i--)if(Number(values[i][0])===Number(actor.faction_id))sheet.deleteRow(i+1);
    if(positions.length){
      const now=new Date(),rows=positions.map(p=>[actor.faction_id,safeCellText_(String(p.name||'')),JSON.stringify(Array.isArray(p.abilities)?p.abilities:[]),now,actor.id,safeCellText_(actor.name)]);
      sheet.getRange(sheet.getLastRow()+1,1,rows.length,rows[0].length).setValues(rows);
    }
  }finally{lock.releaseLock();}
  return positions;
}

function readPositionPermissions_(factionId,positionName){
  const values=db_().getSheetByName(DF_SHEETS.POSITIONS).getDataRange().getValues();
  for(let i=1;i<values.length;i++){
    if(Number(values[i][0])!==Number(factionId)||String(values[i][1]).toLowerCase()!==String(positionName||'').toLowerCase())continue;
    try{const p=JSON.parse(String(values[i][2]||'[]'));return Array.isArray(p)?p:[];}catch(_){return[];}
  }
  return[];
}

function currentPositionAbilities_(apiKey,user){
  if(isLeaderOrCoLeader_(user.position))return['*'];
  const result=tornGet_('/faction/positions',apiKey),positions=result&&result.positions,current=String(user.position||'').trim().toLowerCase();
  if(!Array.isArray(positions))return[];
  for(let i=0;i<positions.length;i++){
    const p=positions[i]||{};if(String(p.name||'').trim().toLowerCase()!==current)continue;
    return Array.isArray(p.abilities)?p.abilities:[];
  }
  return[];
}

function hasCurrentPermission_(apiKey,user,permission){
  if(isLeaderOrCoLeader_(user.position))return true;
  try{return currentPositionAbilities_(apiKey,user).some(v=>String(v).trim().toLowerCase()===String(permission).trim().toLowerCase());}catch(_){return false;}
}

function canManageBankingQueue_(apiKey,user){
  if(isLeaderOrCoLeader_(user.position))return true;
  try{
    const abilities=currentPositionAbilities_(apiKey,user).map(v=>String(v).trim().toLowerCase());
    return abilities.indexOf('money giving')>=0||abilities.indexOf('balance adjustment')>=0;
  }catch(_){return false;}
}

function createNotice_(user,body){
  const title=String(body.title||'').trim(),message=String(body.message||'').trim();
  if(!title||!message)throw new Error('Notice title and message are required.');
  if(title.length>120)throw new Error('Notice title is too long.');
  if(message.length>2000)throw new Error('Notice message is too long.');
  const now=Math.floor(Date.now()/1000);let expiresAt=Number(body.expires_at||0);if(!expiresAt||expiresAt<=now)expiresAt=now+72*3600;
  const notice={id:Utilities.getUuid(),faction_id:user.faction_id,title:title,message:message,created_at:now,expires_at:expiresAt,author_id:user.id,author_name:user.name,active:true};
  db_().getSheetByName(DF_SHEETS.NOTICES).appendRow([notice.id,notice.faction_id,safeCellText_(notice.title),safeCellText_(notice.message),notice.created_at,notice.expires_at,notice.author_id,safeCellText_(notice.author_name),true]);
  return notice;
}

function readActiveNotices_(factionId){
  const values=db_().getSheetByName(DF_SHEETS.NOTICES).getDataRange().getValues(),now=Math.floor(Date.now()/1000),out=[];
  for(let i=1;i<values.length;i++){
    const active=toBoolean_(values[i][8]),expires=Number(values[i][5]||0);if(Number(values[i][1])!==Number(factionId)||!active||(expires&&expires<=now))continue;
    out.push({id:String(values[i][0]||''),faction_id:Number(values[i][1]||0),title:String(values[i][2]||''),message:String(values[i][3]||''),created_at:Number(values[i][4]||0),expires_at:expires,author_id:Number(values[i][6]||0),author_name:String(values[i][7]||'Leadership')});
  }
  out.sort((a,b)=>b.created_at-a.created_at);return out.slice(0,25);
}

function createAppBankingRequest_(user,body){
  const rawAmount=String(body.requested_amount==null?'':body.requested_amount).trim();let amount=null;
  if(rawAmount){amount=Number(rawAmount.replace(/,/g,''));if(!Number.isFinite(amount)||amount<=0)throw new Error('Requested amount must be a positive number.');amount=Math.round(amount);}
  const note=String(body.note||'').trim();if(note.length>500)throw new Error('Banking note is too long.');
  const now=Math.floor(Date.now()/1000),request={id:Utilities.getUuid(),faction_id:user.faction_id,requester_id:user.id,requester_name:user.name,requested_amount:amount,request_mode:amount?'AMOUNT':'FULL_BALANCE',note:note,request_text:'',source:'ANDROID_APP',fingerprint:'app:'+Utilities.getUuid(),message_id:'',requested_at:now,detected_at:now,status:'PENDING',likely_handled:false,handled_at:0,handled_by_id:0,handled_by_name:'',updated_at:now};
  appendBankingRequest_(request);return request;
}

function ingestListenerBankingRequest_(factionId,body){
  const requesterId=Number(body.requester_id||0),requesterName=String(body.requester_name||'').trim(),fingerprint=String(body.fingerprint||'').trim();
  if(!requesterId||!fingerprint)throw new Error('Listener event is missing requester_id or fingerprint.');
  const rawAmount=body.requested_amount;let amount=null;
  if(rawAmount!==null&&rawAmount!==undefined&&String(rawAmount).trim()!==''){amount=Number(rawAmount);if(!Number.isFinite(amount)||amount<=0)amount=null;else amount=Math.round(amount);}
  const now=Math.floor(Date.now()/1000),requestedAt=Number(body.message_timestamp||0)||Number(body.detected_timestamp||0)||now,detectedAt=Number(body.detected_timestamp||0)||now;
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try{
    const existing=findBankingRequestByFingerprint_(factionId,fingerprint);if(existing)return{request:existing,duplicate:true};
    const request={id:Utilities.getUuid(),faction_id:factionId,requester_id:requesterId,requester_name:requesterName||('ID '+requesterId),requested_amount:amount,request_mode:amount?'AMOUNT':'FULL_BALANCE',note:'',request_text:String(body.request_text||'').trim().slice(0,900),source:String(body.source||'FACTION_CHAT').trim().slice(0,80),fingerprint:fingerprint.slice(0,180),message_id:String(body.message_id||'').trim().slice(0,120),requested_at:requestedAt,detected_at:detectedAt,status:'PENDING',likely_handled:false,handled_at:0,handled_by_id:0,handled_by_name:'',updated_at:now};
    appendBankingRequest_(request);return{request:request,duplicate:false};
  }finally{lock.releaseLock();}
}

function appendBankingRequest_(request){
  db_().getSheetByName(DF_SHEETS.BANKING).appendRow([request.id,request.faction_id,request.requester_id,safeCellText_(request.requester_name),request.requested_amount==null?'':request.requested_amount,request.request_mode,safeCellText_(request.note),safeCellText_(request.request_text),safeCellText_(request.source),safeCellText_(request.fingerprint),safeCellText_(request.message_id),request.requested_at,request.detected_at,request.status,request.likely_handled,request.handled_at||'',request.handled_by_id||'',safeCellText_(request.handled_by_name||''),request.updated_at]);
}

function findBankingRequestByFingerprint_(factionId,fingerprint){
  const values=db_().getSheetByName(DF_SHEETS.BANKING).getDataRange().getValues();
  for(let i=values.length-1;i>=1;i--)if(Number(values[i][1])===Number(factionId)&&String(values[i][9]||'')===String(fingerprint||''))return bankingRowToObject_(values[i]);
  return null;
}

function readBankingRequests_(factionId,requesterId){
  const values=db_().getSheetByName(DF_SHEETS.BANKING).getDataRange().getValues(),out=[];
  for(let i=1;i<values.length;i++){
    if(Number(values[i][1])!==Number(factionId))continue;if(requesterId&&Number(values[i][2])!==Number(requesterId))continue;out.push(bankingRowToObject_(values[i]));
  }
  out.sort((a,b)=>(b.requested_at||b.detected_at)-(a.requested_at||a.detected_at));return out.slice(0,requesterId?50:150);
}

function bankingRowToObject_(row){
  const rawAmount=row[4];return{id:String(row[0]||''),faction_id:Number(row[1]||0),requester_id:Number(row[2]||0),requester_name:String(row[3]||''),requested_amount:rawAmount===''?null:Number(rawAmount||0),request_mode:String(row[5]||'FULL_BALANCE'),note:String(row[6]||''),request_text:String(row[7]||''),source:String(row[8]||''),fingerprint:String(row[9]||''),message_id:String(row[10]||''),requested_at:Number(row[11]||0),detected_at:Number(row[12]||0),status:String(row[13]||'PENDING'),likely_handled:toBoolean_(row[14]),handled_at:Number(row[15]||0),handled_by_id:Number(row[16]||0),handled_by_name:String(row[17]||''),updated_at:Number(row[18]||0)};
}

function updateBankingRequest_(actor,body){
  const requestId=String(body.request_id||'').trim(),status=String(body.status||'').trim().toUpperCase();if(!requestId)throw new Error('request_id required.');if(DF_BANKING_STATUSES.indexOf(status)<0)throw new Error('Invalid banking status.');
  const sheet=db_().getSheetByName(DF_SHEETS.BANKING),values=sheet.getDataRange().getValues(),now=Math.floor(Date.now()/1000);
  for(let i=1;i<values.length;i++){
    if(String(values[i][0]||'')!==requestId)continue;if(Number(values[i][1])!==Number(actor.faction_id))throw new Error('Banking request belongs to another faction.');
    const likelyHandled=status==='LIKELY_HANDLED',finalStatus=status==='PAID'||status==='HANDLED'||status==='CANCELLED';
    sheet.getRange(i+1,14,1,6).setValues([[status,likelyHandled,finalStatus?now:'',finalStatus?actor.id:'',finalStatus?safeCellText_(actor.name):'',now]]);
    return bankingRowToObject_(sheet.getRange(i+1,1,1,19).getValues()[0]);
  }
  throw new Error('Banking request not found.');
}

function reconcileBanking_(apiKey,actor){
  const data=tornGet_('/faction/balance?cat=current',apiKey),balance=data&&data.balance||{},members=Array.isArray(balance.members)?balance.members:[],moneyById={};members.forEach(m=>{moneyById[Number(m.id||0)]=Number(m.money||0);});
  const sheet=db_().getSheetByName(DF_SHEETS.BANKING),values=sheet.getDataRange().getValues(),now=Math.floor(Date.now()/1000);let checked=0,flagged=0;
  for(let i=1;i<values.length;i++){
    if(Number(values[i][1])!==Number(actor.faction_id)||String(values[i][13]||'PENDING')!=='PENDING'||!/^FACTION_CHAT/i.test(String(values[i][8]||'')))continue;
    const requesterId=Number(values[i][2]||0);if(!(requesterId in moneyById))continue;checked++;
    if(moneyById[requesterId]<DF_LIKELY_HANDLED_THRESHOLD){sheet.getRange(i+1,14,1,6).setValues([['LIKELY_HANDLED',true,'','','',now]]);flagged++;}
  }
  return{checked:checked,flagged:flagged,threshold:DF_LIKELY_HANDLED_THRESHOLD,note:'Only retroactive faction-chat requests are auto-flagged; app-submitted requests remain pending until explicitly handled.'};
}

function saveRankRules_(factionId,rankName,tools,actor){
  rankName=rankName.trim();if(!rankName)throw new Error('rank_name required.');
  const sheet=db_().getSheetByName(DF_SHEETS.RANKS),rows=sheet.getDataRange().getValues(),now=new Date();
  DF_TOOLS.forEach(([toolId])=>{
    if(!(toolId in tools))return;const allowed=Boolean(tools[toolId]);let rowIndex=0;
    for(let i=1;i<rows.length;i++)if(Number(rows[i][0])===Number(factionId)&&String(rows[i][1])===rankName&&String(rows[i][2])===toolId){rowIndex=i+1;break;}
    const values=[factionId,safeCellText_(rankName),toolId,allowed,now,actor.id,safeCellText_(actor.name)];if(rowIndex)sheet.getRange(rowIndex,1,1,values.length).setValues([values]);else sheet.appendRow(values);
  });
}

function saveUserOverrides_(factionId,userId,tools,actor){
  const sheet=db_().getSheetByName(DF_SHEETS.USERS),rows=sheet.getDataRange().getValues(),now=new Date();
  DF_TOOLS.forEach(([toolId])=>{
    if(!(toolId in tools))return;const allowed=tools[toolId]===null?'':Boolean(tools[toolId]);let rowIndex=0;
    for(let i=1;i<rows.length;i++)if(Number(rows[i][0])===Number(factionId)&&Number(rows[i][1])===userId&&String(rows[i][2])===toolId){rowIndex=i+1;break;}
    const values=[factionId,userId,toolId,allowed,now,actor.id,safeCellText_(actor.name)];if(rowIndex)sheet.getRange(rowIndex,1,1,values.length).setValues([values]);else sheet.appendRow(values);
  });
}

function readRankRules_(factionId){
  const values=db_().getSheetByName(DF_SHEETS.RANKS).getDataRange().getValues(),out={};
  for(let i=1;i<values.length;i++){
    if(Number(values[i][0])!==Number(factionId))continue;const rank=String(values[i][1]||''),tool=String(values[i][2]||'');if(!rank||!tool)continue;if(!out[rank])out[rank]={};out[rank][tool]=toBoolean_(values[i][3]);
  }
  return out;
}

function readUserOverrides_(factionId,userId){
  const values=db_().getSheetByName(DF_SHEETS.USERS).getDataRange().getValues(),out={};
  for(let i=1;i<values.length;i++){
    if(Number(values[i][0])!==Number(factionId)||Number(values[i][1])!==Number(userId))continue;const tool=String(values[i][2]||'');if(!tool)continue;const raw=values[i][3];out[tool]=raw===''?null:toBoolean_(raw);
  }
  return out;
}

function settings_(){const values=db_().getSheetByName(DF_SHEETS.SETTINGS).getDataRange().getValues(),out={};for(let i=1;i<values.length;i++){const key=String(values[i][0]||'');if(key)out[key]=values[i][1];}return out;}
function setSetting_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key){sheet.getRange(i+1,2).setValue(value);return;}sheet.appendRow([key,value]);}
function setSettingIfMissing_(sheet,key,value){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return;sheet.appendRow([key,value]);}
function getSetting_(sheet,key){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return values[i][1];return'';}

function ensureSheet_(ss,name,headers){let sheet=ss.getSheetByName(name);if(!sheet)sheet=ss.insertSheet(name);if(sheet.getLastRow()===0)sheet.appendRow(headers);return sheet;}
function ensureTenantSheet_(ss,name,headers,legacyFactionId){
  let sheet=ss.getSheetByName(name);if(!sheet){sheet=ss.insertSheet(name);sheet.appendRow(headers);return sheet;}
  if(sheet.getLastRow()===0){sheet.appendRow(headers);return sheet;}
  if(String(sheet.getRange(1,1).getValue()||'').trim().toLowerCase()!=='faction_id'){
    const oldRows=sheet.getLastRow();sheet.insertColumnBefore(1);sheet.getRange(1,1).setValue('faction_id');if(oldRows>1)sheet.getRange(2,1,oldRows-1,1).setValue(Number(legacyFactionId||0));
  }
  return sheet;
}

function db_(){const id=PropertiesService.getScriptProperties().getProperty('SHEET_ID');if(!id)throw new Error('Backend is not configured. Run setupTornFcaFactionBackend() first.');return SpreadsheetApp.openById(id);}

function tornGet_(path,apiKey){
  const joiner=String(path).indexOf('?')>=0?'&':'?',url='https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(apiKey),response=UrlFetchApp.fetch(url,{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Faction-Backend/'+DF_TOOLKIT_VERSION}});let data;
  try{data=JSON.parse(response.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}
  if(data&&data.error)throw new Error(data.error.error||('Torn API error '+data.error.code));if(response.getResponseCode()<200||response.getResponseCode()>=300)throw new Error('Torn API HTTP '+response.getResponseCode());return data;
}

function publicUser_(user){return{id:user.id,name:user.name,faction_id:user.faction_id,faction_name:user.faction_name,position:user.position};}
function safeCellText_(value){const text=String(value==null?'':value);return/^[=+\-@]/.test(text)?"'"+text:text;}
function toBoolean_(value){return value===true||String(value).toLowerCase()==='true'||Number(value)===1;}
function sha256_(value){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value||''),Utilities.Charset.UTF_8);return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function json_(obj){return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);}
