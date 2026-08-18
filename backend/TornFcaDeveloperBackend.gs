/**
 * TornFCA Developer Control Plane v1.4.0.
 * Deploy as its OWN Google Apps Script web app.
 *
 * Developer-channel security:
 * - hidden Android entry is not an authorization boundary.
 * - delegated developer access uses username + strong password + per-account TOTP.
 * - passwords are salted and HMAC-hashed with a server-only pepper; plaintext is never stored.
 * - TOTP secrets are stored in Script Properties, not the spreadsheet.
 * - successful login issues a short-lived 2-hour device-bound session token.
 * - failed logins progressively lock the account; access changes and privileged mutations are audited.
 * - Root/Admin may invite/revoke developers. Root cannot be revoked through the app.
 *
 * Existing remote product-policy writes remain Torn-owner verified until cross-service delegated
 * authorization is explicitly enabled. This keeps developer-console delegation separate from
 * faction, premium, banking and moderation authority.
 */
const TD_VERSION='1.4.0';
const TD_DEVELOPER_PLAYER_ID=3987363;
const TD_CONFIG='DeveloperConfig';
const TD_AUDIT='DeveloperAudit';
const TD_USERS='DeveloperUsers';
const TD_ACCESS='DeveloperAccess';
const TD_INVITES='DeveloperInvites';
const TD_SESSIONS='DeveloperSessions';
const TD_ENROLL='DeveloperEnrollments';
const TD_CURRENT_WINDOW_SECONDS=24*60*60;
const TD_HEARTBEAT_SECONDS=6*60*60;
const TD_SESSION_SECONDS=2*60*60;
const TD_INVITE_SECONDS=24*60*60;
const TD_ENROLL_SECONDS=15*60;
const TD_ALLOWED_CONFIG=Object.freeze(['maintenance_mode','minimum_version_code','beta_message','disable_activity','disable_war','disable_chain','disable_oc','disable_pulse','disable_lookup','disable_premium']);

function setupTornFcaDeveloperBackend(){
  const ss=SpreadsheetApp.getActiveSpreadsheet(),props=PropertiesService.getScriptProperties();
  props.setProperty('DEVELOPER_SHEET_ID',ss.getId());
  if(!props.getProperty('TELEMETRY_SALT'))props.setProperty('TELEMETRY_SALT',Utilities.getUuid()+Utilities.getUuid());
  if(!props.getProperty('DEVELOPER_PASSWORD_PEPPER'))props.setProperty('DEVELOPER_PASSWORD_PEPPER',Utilities.getUuid()+Utilities.getUuid()+Utilities.getUuid());
  const config=tdEnsureSheet_(ss,TD_CONFIG,['key','value','updated_at','updated_by_id','updated_by_name']);
  tdSetIfMissing_(config,'maintenance_mode','false',0,'setup');tdSetIfMissing_(config,'minimum_version_code','0',0,'setup');tdSetIfMissing_(config,'beta_message','',0,'setup');
  ['activity','war','chain','oc','pulse','lookup','premium'].forEach(v=>tdSetIfMissing_(config,'disable_'+v,'false',0,'setup'));
  tdEnsureSheet_(ss,TD_AUDIT,['id','timestamp','actor_id','actor_name','action','details_json']);
  tdEnsureSheet_(ss,TD_USERS,['user_hash','first_seen','last_seen','last_version_code','last_version_name']);
  tdEnsureSheet_(ss,TD_ACCESS,['id','username','display_name','role','active','password_salt','password_hash','totp_enabled','created_at','created_by','updated_at','last_login','failed_count','lock_until']);
  tdEnsureSheet_(ss,TD_INVITES,['id','code_hash','username','display_name','role','created_at','created_by','expires_at','consumed_at','target_id']);
  tdEnsureSheet_(ss,TD_SESSIONS,['token_hash','developer_id','username','role','device_hash','created_at','expires_at','revoked_at','last_seen']);
  tdEnsureSheet_(ss,TD_ENROLL,['token_hash','invite_id','developer_id','device_hash','expires_at','completed_at']);
  return{ok:true,version:TD_VERSION,sheet_id:ss.getId(),next:'Set DEVELOPER_ROOT_USERNAME_SETUP and DEVELOPER_ADMIN_PASSWORD_SETUP Script Properties, run bootstrapTornFcaDeveloperRoot(), save the returned authenticator secret, then deploy as a web app.'};
}

/** First and only root bootstrap. Returns the TOTP setup secret once. */
function bootstrapTornFcaDeveloperRoot(){
  const props=PropertiesService.getScriptProperties(),username=tdNormalizeUsername_(props.getProperty('DEVELOPER_ROOT_USERNAME_SETUP')||'root'),plain=String(props.getProperty('DEVELOPER_ADMIN_PASSWORD_SETUP')||'');
  if(plain.length<14)throw new Error('Set DEVELOPER_ADMIN_PASSWORD_SETUP to at least 14 characters.');
  const sheet=tdDb_().getSheetByName(TD_ACCESS);if(tdFindRoot_())throw new Error('A Root Admin already exists.');
  const id=Utilities.getUuid(),salt=Utilities.getUuid().replace(/-/g,''),secret=tdNewTotpSecret_(),now=tdNow_();
  const hash=tdCredentialHash_(username,salt,plain);sheet.appendRow([id,username,'Root Admin','root',true,salt,hash,true,now,'bootstrap',now,0,0,0]);props.setProperty('DEV_TOTP_'+id,secret);
  try{tdAuditActor_({id:id,username:username,role:'root'},'developer_root_bootstrap',{});}finally{props.deleteProperty('DEVELOPER_ROOT_USERNAME_SETUP');props.deleteProperty('DEVELOPER_ADMIN_PASSWORD_SETUP');}
  return{ok:true,username:username,role:'root',totp_secret:secret,otpauth_uri:tdOtpUri_(username,secret),warning:'Store this authenticator secret now. It is not returned by normal API calls.'};
}

/** Legacy helper retained only to migrate old setup scripts; it no longer creates an app-embedded password. */
function bootstrapTornFcaDeveloperAdminPassword(){return bootstrapTornFcaDeveloperRoot();}
function setTornFcaDeveloperAdminPassword(){throw new Error('Use per-developer credentials. Root/password bootstrap is handled by bootstrapTornFcaDeveloperRoot().');}

function doGet(){return tdJson_({ok:true,app:'TornFCA Developer Control Plane',version:TD_VERSION,authenticated_actions:'POST only'});}

function doPost(e){
  try{
    const body=JSON.parse((e&&e.postData&&e.postData.contents)||'{}'),action=String(body.action||'').trim();
    if(action==='developer_login')return tdJson_(tdDeveloperLogin_(body));
    if(action==='developer_session')return tdJson_(tdDeveloperSessionStatus_(body));
    if(action==='developer_logout')return tdJson_(tdDeveloperLogout_(body));
    if(action==='developer_enroll_begin')return tdJson_(tdDeveloperEnrollBegin_(body));
    if(action==='developer_enroll_complete')return tdJson_(tdDeveloperEnrollComplete_(body));
    if(action==='developer_access_list')return tdJson_(tdDeveloperAccessList_(body));
    if(action==='developer_invite_create')return tdJson_(tdDeveloperInviteCreate_(body));
    if(action==='developer_access_revoke')return tdJson_(tdDeveloperAccessRevoke_(body));
    if(action==='developer_reset_enrollment')return tdJson_(tdDeveloperResetEnrollment_(body));

    const apiKey=String(body.apiKey||'').trim();if(!apiKey)throw new Error('API key required.');
    if(action==='public_config'){const user=tdVerifyUser_(apiKey);try{tdTrackUser_(user,body);}catch(_){}return tdJson_({ok:true,user:tdPublicUser_(user),version:TD_VERSION,config:tdReadConfig_()});}

    // Sensitive remote product policy remains owner/Torn verified for this release.
    const user=tdVerifyDeveloper_(apiKey);
    if(action==='status'||action==='config_read')return tdJson_({ok:true,user:tdPublicUser_(user),version:TD_VERSION,config:tdReadConfig_(),user_stats:tdUserStats_()});
    if(action==='audit_list'){tdRequireLegacyAdmin_(String(body.admin_password||''));return tdJson_({ok:true,audit:tdReadAudit_()});}
    if(action==='config_write'){tdRequireLegacyAdmin_(String(body.admin_password||''));const updates=body.config&&typeof body.config==='object'?body.config:{};const applied=tdWriteConfig_(updates,user);tdAudit_(user,'config_write',{keys:Object.keys(applied)});return tdJson_({ok:true,config:tdReadConfig_(),applied:applied,user_stats:tdUserStats_()});}
    throw new Error('Unknown action.');
  }catch(err){return tdJson_({ok:false,error:String(err&&err.message||err)});}
}

function tdDeveloperLogin_(body){
  const username=tdNormalizeUsername_(body.username),password=String(body.password||''),otp=String(body.otp||'').replace(/\D/g,''),device=tdDeviceHash_(body.device_id);if(!username||!password||otp.length!==6)throw new Error('Developer authorization failed.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);try{
    const rec=tdFindAccessByUsername_(username);if(!rec){Utilities.sleep(250);throw new Error('Developer authorization failed.');}
    const now=tdNow_();if(!rec.active)throw new Error('Developer account is disabled.');if(rec.lock_until>now)throw new Error('Developer account is temporarily locked. Try again later.');
    const expected=tdCredentialHash_(rec.username,rec.password_salt,password),secret=String(PropertiesService.getScriptProperties().getProperty('DEV_TOTP_'+rec.id)||'');
    if(!tdConstantTime_(expected,rec.password_hash)||!secret||!tdVerifyTotp_(secret,otp)){tdFailedLogin_(rec);throw new Error('Developer authorization failed.');}
    tdResetFailures_(rec,now);const session=tdIssueSession_(rec,device);tdAuditActor_(rec,'developer_login',{device_hash:device});return{ok:true,version:TD_VERSION,developer:tdPublicDeveloper_(rec),developer_session:session.token,expires_at:session.expires_at};
  }finally{lock.releaseLock();}
}
function tdDeveloperSessionStatus_(body){const rec=tdRequireSession_(body,'developer');return{ok:true,version:TD_VERSION,developer:tdPublicDeveloper_(rec.access),expires_at:rec.session.expires_at};}
function tdDeveloperLogout_(body){const token=String(body.developer_session||'');if(!token)throw new Error('Developer session required.');const hash=tdSha256_(token),sheet=tdDb_().getSheetByName(TD_SESSIONS),row=tdFindExactRow_(sheet,1,hash);if(row>1)sheet.getRange(row,8).setValue(tdNow_());return{ok:true};}

function tdDeveloperInviteCreate_(body){
  const actor=tdRequireSession_(body,'admin').access,username=tdNormalizeUsername_(body.username),display=tdSafeLabel_(body.display_name||username),role=tdNormalizeRole_(body.role);if(!username)throw new Error('Developer username is required.');if(role==='root')throw new Error('Root role cannot be invited.');if(role==='admin'&&actor.role!=='root')throw new Error('Only Root Admin may invite another Admin.');if(tdFindAccessByUsername_(username))throw new Error('That developer username already exists.');
  const invite=tdCreateInvite_(username,display,role,actor.id,'');tdAuditActor_(actor,'developer_invite_create',{username:username,role:role});return{ok:true,invite_code:invite.code,expires_at:invite.expires_at,username:username,display_name:display,role:role};
}
function tdDeveloperAccessList_(body){const actor=tdRequireSession_(body,'admin').access,sheet=tdDb_().getSheetByName(TD_ACCESS),values=sheet.getDataRange().getValues(),out=[];for(let i=1;i<values.length;i++){const r=tdAccessFromValues_(values[i],i+1);if(!r.id)continue;out.push(tdPublicDeveloper_(r));}return{ok:true,actor:tdPublicDeveloper_(actor),developers:out};}
function tdDeveloperAccessRevoke_(body){const actor=tdRequireSession_(body,'admin').access,target=tdFindAccessById_(String(body.developer_id||''));if(!target)throw new Error('Developer account not found.');if(target.role==='root')throw new Error('Root Admin cannot be revoked from the app.');if(target.role==='admin'&&actor.role!=='root')throw new Error('Only Root Admin may revoke another Admin.');tdSetAccessActive_(target,false);tdRevokeSessions_(target.id);tdAuditActor_(actor,'developer_access_revoke',{developer_id:target.id,username:target.username});return{ok:true};}
function tdDeveloperResetEnrollment_(body){const actor=tdRequireSession_(body,'admin').access,target=tdFindAccessById_(String(body.developer_id||''));if(!target)throw new Error('Developer account not found.');if(target.role==='root')throw new Error('Root Admin authenticator reset must be performed from Apps Script recovery.');if(target.role==='admin'&&actor.role!=='root')throw new Error('Only Root Admin may reset another Admin.');tdSetAccessActive_(target,false);tdRevokeSessions_(target.id);PropertiesService.getScriptProperties().deleteProperty('DEV_TOTP_'+target.id);const invite=tdCreateInvite_(target.username,target.display_name,target.role,actor.id,target.id);tdAuditActor_(actor,'developer_reset_enrollment',{developer_id:target.id});return{ok:true,invite_code:invite.code,expires_at:invite.expires_at,username:target.username,role:target.role};}

function tdDeveloperEnrollBegin_(body){
  const code=String(body.invite_code||'').trim().toUpperCase(),password=String(body.password||''),device=tdDeviceHash_(body.device_id);if(code.length<12)throw new Error('Enrollment code is invalid.');if(password.length<14)throw new Error('Use a developer password of at least 14 characters.');
  const lock=LockService.getScriptLock();lock.waitLock(10000);try{
    const invite=tdFindInviteByCode_(code);if(!invite||invite.consumed_at||invite.expires_at<tdNow_())throw new Error('Enrollment code is invalid or expired.');let access=invite.target_id?tdFindAccessById_(invite.target_id):tdFindAccessByUsername_(invite.username);const sheet=tdDb_().getSheetByName(TD_ACCESS),now=tdNow_(),salt=Utilities.getUuid().replace(/-/g,''),hash=tdCredentialHash_(invite.username,salt,password),secret=tdNewTotpSecret_();
    if(access){if(access.role==='root')throw new Error('Root enrollment cannot use an invitation.');sheet.getRange(access.row,3,1,12).setValues([[invite.display_name,invite.role,false,salt,hash,false,access.created_at||now,access.created_by||invite.created_by,now,0,0,0]]);access=tdFindAccessById_(access.id);}else{const id=Utilities.getUuid();sheet.appendRow([id,invite.username,invite.display_name,invite.role,false,salt,hash,false,now,invite.created_by,now,0,0,0]);access=tdFindAccessById_(id);}
    PropertiesService.getScriptProperties().setProperty('DEV_TOTP_'+access.id,secret);const token=tdRandomToken_(),expires=now+TD_ENROLL_SECONDS;tdDb_().getSheetByName(TD_ENROLL).appendRow([tdSha256_(token),invite.id,access.id,device,expires,0]);
    return{ok:true,enrollment_token:token,expires_at:expires,username:access.username,display_name:access.display_name,role:access.role,totp_secret:secret,otpauth_uri:tdOtpUri_(access.username,secret)};
  }finally{lock.releaseLock();}
}
function tdDeveloperEnrollComplete_(body){
  const token=String(body.enrollment_token||''),otp=String(body.otp||'').replace(/\D/g,''),device=tdDeviceHash_(body.device_id);if(!token||otp.length!==6)throw new Error('Enrollment verification failed.');const lock=LockService.getScriptLock();lock.waitLock(10000);try{
    const enroll=tdFindEnrollment_(tdSha256_(token));if(!enroll||enroll.completed_at||enroll.expires_at<tdNow_()||enroll.device_hash!==device)throw new Error('Enrollment session expired.');const access=tdFindAccessById_(enroll.developer_id);if(!access)throw new Error('Developer account missing.');const secret=String(PropertiesService.getScriptProperties().getProperty('DEV_TOTP_'+access.id)||'');if(!tdVerifyTotp_(secret,otp))throw new Error('Authenticator code is incorrect.');tdSetAccessActive_(access,true,true);tdConsumeInvite_(enroll.invite_id);tdDb_().getSheetByName(TD_ENROLL).getRange(enroll.row,6).setValue(tdNow_());const fresh=tdFindAccessById_(access.id),session=tdIssueSession_(fresh,device);tdAuditActor_(fresh,'developer_enrollment_complete',{});return{ok:true,developer:tdPublicDeveloper_(fresh),developer_session:session.token,expires_at:session.expires_at};
  }finally{lock.releaseLock();}
}

function tdRequireSession_(body,minRole){const token=String(body.developer_session||'');if(!token)throw new Error('Developer session required.');const hash=tdSha256_(token),sheet=tdDb_().getSheetByName(TD_SESSIONS),row=tdFindExactRow_(sheet,1,hash);if(row<2)throw new Error('Developer session is invalid.');const v=sheet.getRange(row,1,1,9).getValues()[0],session={token_hash:String(v[0]||''),developer_id:String(v[1]||''),username:String(v[2]||''),role:String(v[3]||''),device_hash:String(v[4]||''),created_at:Number(v[5]||0),expires_at:Number(v[6]||0),revoked_at:Number(v[7]||0),last_seen:Number(v[8]||0),row:row};if(session.revoked_at||session.expires_at<tdNow_())throw new Error('Developer session expired.');const access=tdFindAccessById_(session.developer_id);if(!access||!access.active)throw new Error('Developer account is disabled.');if(tdRoleRank_(access.role)<tdRoleRank_(minRole))throw new Error('Developer role does not permit this action.');sheet.getRange(row,9).setValue(tdNow_());return{session:session,access:access};}
function tdIssueSession_(access,deviceHash){tdRevokeExpiredSessions_();const token=tdRandomToken_(),now=tdNow_(),expires=now+TD_SESSION_SECONDS;tdDb_().getSheetByName(TD_SESSIONS).appendRow([tdSha256_(token),access.id,access.username,access.role,deviceHash,now,expires,0,now]);return{token:token,expires_at:expires};}
function tdRevokeSessions_(developerId){const sheet=tdDb_().getSheetByName(TD_SESSIONS),values=sheet.getDataRange().getValues(),now=tdNow_();for(let i=1;i<values.length;i++)if(String(values[i][1]||'')===developerId&&!Number(values[i][7]||0))sheet.getRange(i+1,8).setValue(now);}
function tdRevokeExpiredSessions_(){const sheet=tdDb_().getSheetByName(TD_SESSIONS),values=sheet.getDataRange().getValues(),now=tdNow_();for(let i=1;i<values.length;i++)if(!Number(values[i][7]||0)&&Number(values[i][6]||0)<now)sheet.getRange(i+1,8).setValue(now);}

function tdCreateInvite_(username,display,role,actorId,targetId){const code=tdInviteCode_(),now=tdNow_(),expires=now+TD_INVITE_SECONDS,id=Utilities.getUuid();tdDb_().getSheetByName(TD_INVITES).appendRow([id,tdSha256_(code),username,display,role,now,actorId,expires,0,targetId||'']);return{id:id,code:code,expires_at:expires};}
function tdFindInviteByCode_(code){const hash=tdSha256_(code),sheet=tdDb_().getSheetByName(TD_INVITES),row=tdFindExactRow_(sheet,2,hash);if(row<2)return null;const v=sheet.getRange(row,1,1,10).getValues()[0];return{id:String(v[0]||''),code_hash:String(v[1]||''),username:String(v[2]||''),display_name:String(v[3]||''),role:String(v[4]||''),created_at:Number(v[5]||0),created_by:String(v[6]||''),expires_at:Number(v[7]||0),consumed_at:Number(v[8]||0),target_id:String(v[9]||''),row:row};}
function tdConsumeInvite_(id){const sheet=tdDb_().getSheetByName(TD_INVITES),row=tdFindExactRow_(sheet,1,id);if(row>1)sheet.getRange(row,9).setValue(tdNow_());}
function tdFindEnrollment_(hash){const sheet=tdDb_().getSheetByName(TD_ENROLL),row=tdFindExactRow_(sheet,1,hash);if(row<2)return null;const v=sheet.getRange(row,1,1,6).getValues()[0];return{token_hash:String(v[0]||''),invite_id:String(v[1]||''),developer_id:String(v[2]||''),device_hash:String(v[3]||''),expires_at:Number(v[4]||0),completed_at:Number(v[5]||0),row:row};}

function tdFindAccessByUsername_(username){const sheet=tdDb_().getSheetByName(TD_ACCESS),row=tdFindExactRow_(sheet,2,tdNormalizeUsername_(username));if(row<2)return null;return tdAccessFromValues_(sheet.getRange(row,1,1,14).getValues()[0],row);}
function tdFindAccessById_(id){const sheet=tdDb_().getSheetByName(TD_ACCESS),row=tdFindExactRow_(sheet,1,id);if(row<2)return null;return tdAccessFromValues_(sheet.getRange(row,1,1,14).getValues()[0],row);}
function tdFindRoot_(){const sheet=tdDb_().getSheetByName(TD_ACCESS),values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++){const r=tdAccessFromValues_(values[i],i+1);if(r.role==='root')return r;}return null;}
function tdAccessFromValues_(v,row){return{id:String(v[0]||''),username:String(v[1]||''),display_name:String(v[2]||''),role:String(v[3]||'developer'),active:tdBool_(v[4]),password_salt:String(v[5]||''),password_hash:String(v[6]||''),totp_enabled:tdBool_(v[7]),created_at:Number(v[8]||0),created_by:String(v[9]||''),updated_at:Number(v[10]||0),last_login:Number(v[11]||0),failed_count:Number(v[12]||0),lock_until:Number(v[13]||0),row:row};}
function tdPublicDeveloper_(r){return{id:r.id,username:r.username,display_name:r.display_name,role:r.role,active:!!r.active,totp_enabled:!!r.totp_enabled,created_at:r.created_at,last_login:r.last_login,locked_until:r.lock_until};}
function tdSetAccessActive_(r,active,totpEnabled){const sheet=tdDb_().getSheetByName(TD_ACCESS),now=tdNow_();sheet.getRange(r.row,5).setValue(!!active);if(totpEnabled!==undefined)sheet.getRange(r.row,8).setValue(!!totpEnabled);sheet.getRange(r.row,11).setValue(now);}
function tdFailedLogin_(r){const count=r.failed_count+1,now=tdNow_();let lock=0;if(count>=12)lock=60*60;else if(count>=8)lock=5*60;else if(count>=5)lock=60;const sheet=tdDb_().getSheetByName(TD_ACCESS);sheet.getRange(r.row,13).setValue(count);if(lock)sheet.getRange(r.row,14).setValue(now+lock);tdAuditActor_(r,'developer_login_failed',{failed_count:count,lock_seconds:lock});}
function tdResetFailures_(r,now){const sheet=tdDb_().getSheetByName(TD_ACCESS);sheet.getRange(r.row,12,1,3).setValues([[now,0,0]]);}

function tdCredentialHash_(username,salt,password){const pepper=String(PropertiesService.getScriptProperties().getProperty('DEVELOPER_PASSWORD_PEPPER')||'');if(!pepper)throw new Error('Developer password pepper is not configured. Run setup first.');const bytes=Utilities.computeHmacSha256Signature(tdNormalizeUsername_(username)+'|'+String(salt||'')+'|'+String(password||''),pepper);return tdHex_(bytes);}
function tdConstantTime_(a,b){a=String(a||'');b=String(b||'');if(a.length!==b.length)return false;let x=0;for(let i=0;i<a.length;i++)x|=a.charCodeAt(i)^b.charCodeAt(i);return x===0;}
function tdNewTotpSecret_(){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,Utilities.getUuid()+Utilities.getUuid(),Utilities.Charset.UTF_8);return tdBase32Encode_(bytes.slice(0,20));}
function tdOtpUri_(username,secret){return'otpauth://totp/TornFCA:'+encodeURIComponent(username)+'?secret='+encodeURIComponent(secret)+'&issuer=TornFCA&algorithm=SHA1&digits=6&period=30';}
function tdVerifyTotp_(secret,otp){const now=Math.floor(Date.now()/1000/30);for(let d=-1;d<=1;d++)if(tdTotp_(secret,now+d)===otp)return true;return false;}
function tdTotp_(secret,counter){const key=tdBase32Decode_(secret),msg=[];let n=counter;for(let i=7;i>=0;i--){msg[i]=n&255;n=Math.floor(n/256);}const signedMsg=msg.map(tdSignedByte_),signedKey=key.map(tdSignedByte_),h=Utilities.computeHmacSignature(Utilities.MacAlgorithm.HMAC_SHA_1,signedMsg,signedKey),bytes=h.map(v=>v<0?v+256:v),offset=bytes[bytes.length-1]&15,bin=((bytes[offset]&127)<<24)|((bytes[offset+1]&255)<<16)|((bytes[offset+2]&255)<<8)|(bytes[offset+3]&255),code=String((bin>>>0)%1000000);return('000000'+code).slice(-6);}
function tdBase32Encode_(bytes){const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';let bits=0,value=0,out='';bytes.forEach(raw=>{const b=raw<0?raw+256:raw;value=(value<<8)|b;bits+=8;while(bits>=5){out+=alphabet[(value>>>(bits-5))&31];bits-=5;}});if(bits>0)out+=alphabet[(value<<(5-bits))&31];return out;}
function tdBase32Decode_(text){const alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567',clean=String(text||'').toUpperCase().replace(/[^A-Z2-7]/g,'');let bits=0,value=0,out=[];for(let i=0;i<clean.length;i++){const idx=alphabet.indexOf(clean.charAt(i));if(idx<0)continue;value=(value<<5)|idx;bits+=5;if(bits>=8){out.push((value>>>(bits-8))&255);bits-=8;}}return out;}
function tdSignedByte_(b){return b>127?b-256:b;}
function tdHex_(bytes){return bytes.map(b=>('0'+((b<0?b+256:b)&255).toString(16)).slice(-2)).join('');}
function tdInviteCode_(){const raw=(Utilities.getUuid()+Utilities.getUuid()).replace(/-/g,'').toUpperCase();return raw.slice(0,6)+'-'+raw.slice(6,12)+'-'+raw.slice(12,18);}
function tdRandomToken_(){return Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,'');}
function tdDeviceHash_(v){return tdSha256_(String(v||'unknown-device')).slice(0,32);}
function tdNormalizeUsername_(v){return String(v||'').trim().toLowerCase().replace(/[^a-z0-9._-]/g,'').slice(0,40);}
function tdSafeLabel_(v){return String(v||'').trim().replace(/[\r\n\t]/g,' ').slice(0,80)||'Developer';}
function tdNormalizeRole_(v){const r=String(v||'developer').toLowerCase();return r==='admin'?'admin':'developer';}
function tdRoleRank_(v){return v==='root'?3:v==='admin'?2:1;}
function tdNow_(){return Math.floor(Date.now()/1000);}
function tdFindExactRow_(sheet,column,value){if(!sheet||!value||sheet.getLastRow()<2)return-1;const finder=sheet.getRange(2,column,sheet.getLastRow()-1,1).createTextFinder(String(value)).matchEntireCell(true).findNext();return finder?finder.getRow():-1;}

function tdVerifyUser_(apiKey){const fp=tdSha256_(apiKey),cache=CacheService.getScriptCache(),cacheKey='developer_identity:'+fp,cached=cache.get(cacheKey);if(cached){try{const u=JSON.parse(cached);if(Number(u.id||0)>0)return u;}catch(_){} }const root=tdTornGet_('/user/basic',apiKey),profile=root&&root.profile||{},user={id:Number(profile.id||0),name:String(profile.name||'Unknown')};if(!user.id)throw new Error('Unable to verify Torn player identity.');cache.put(cacheKey,JSON.stringify(user),90);return user;}
function tdVerifyDeveloper_(apiKey){const user=tdVerifyUser_(apiKey);if(user.id!==TD_DEVELOPER_PLAYER_ID)throw new Error('Verified TornFCA owner account required for remote product-policy controls.');return user;}
function tdRequireLegacyAdmin_(password){const root=tdFindRoot_();if(!root)throw new Error('Root developer account is not configured.');const expected=root.password_hash,actual=tdCredentialHash_(root.username,root.password_salt,String(password||''));if(!expected||!tdConstantTime_(actual,expected))throw new Error('Developer authorization failed.');}

function tdWriteConfig_(updates,user){const sheet=tdDb_().getSheetByName(TD_CONFIG),applied={};TD_ALLOWED_CONFIG.forEach(key=>{if(!(key in updates))return;let value=String(updates[key]==null?'':updates[key]).trim();if(key==='maintenance_mode'||key.indexOf('disable_')===0)value=tdBool_(updates[key])?'true':'false';else if(key==='minimum_version_code')value=String(Math.max(0,Math.floor(Number(updates[key])||0)));else if(key==='beta_message')value=value.slice(0,1000);tdSet_(sheet,key,value,user.id,user.name);applied[key]=value;});if(!Object.keys(applied).length)throw new Error('No supported developer configuration keys were supplied.');return applied;}
function tdReadConfig_(){const sheet=tdDb_().getSheetByName(TD_CONFIG),values=sheet.getDataRange().getValues(),out={};for(let i=1;i<values.length;i++){const key=String(values[i][0]||'');if(TD_ALLOWED_CONFIG.indexOf(key)<0)continue;const raw=String(values[i][1]==null?'':values[i][1]);if(key==='maintenance_mode'||key.indexOf('disable_')===0)out[key]=tdBool_(raw);else if(key==='minimum_version_code')out[key]=Math.max(0,Number(raw)||0);else out[key]=raw;}return out;}
function tdTrackUser_(user,body){const hash=tdUserHash_(user.id),cache=CacheService.getScriptCache(),cacheKey='telemetry:'+hash;if(cache.get(cacheKey))return;const sheet=tdDb_().getSheetByName(TD_USERS);if(!sheet)return;const now=tdNow_(),versionCode=Math.max(0,Math.floor(Number(body.version_code||0)||0)),versionName=String(body.version_name||'').trim().slice(0,60),lock=LockService.getScriptLock();lock.waitLock(10000);try{const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++){if(String(values[i][0]||'')!==hash)continue;sheet.getRange(i+1,3,1,3).setValues([[now,versionCode,tdSafe_(versionName)]]);cache.put(cacheKey,'1',TD_HEARTBEAT_SECONDS);return;}sheet.appendRow([hash,now,now,versionCode,tdSafe_(versionName)]);cache.put(cacheKey,'1',TD_HEARTBEAT_SECONDS);}finally{lock.releaseLock();}}
function tdUserStats_(){const sheet=tdDb_().getSheetByName(TD_USERS);if(!sheet)return{total_unique:0,current_total:0,current_window_hours:24,tracking_since:0,updated_at:tdNow_()};const values=sheet.getDataRange().getValues(),now=tdNow_(),cutoff=now-TD_CURRENT_WINDOW_SECONDS;let total=0,current=0,first=0;for(let i=1;i<values.length;i++){if(!String(values[i][0]||''))continue;const firstSeen=Number(values[i][1]||0),lastSeen=Number(values[i][2]||0);total++;if(lastSeen>=cutoff)current++;if(firstSeen>0&&(first===0||firstSeen<first))first=firstSeen;}return{total_unique:total,current_total:current,current_window_hours:24,tracking_since:first,updated_at:now};}
function tdUserHash_(playerId){const props=PropertiesService.getScriptProperties();let salt=String(props.getProperty('TELEMETRY_SALT')||'');if(!salt){salt=Utilities.getUuid()+Utilities.getUuid();props.setProperty('TELEMETRY_SALT',salt);}return tdSha256_(salt+':'+String(Number(playerId)||0));}
function tdAudit_(user,action,details){tdDb_().getSheetByName(TD_AUDIT).appendRow([Utilities.getUuid(),tdNow_(),user.id,tdSafe_(user.name),tdSafe_(action),tdSafe_(JSON.stringify(details||{}))]);}
function tdAuditActor_(actor,action,details){tdDb_().getSheetByName(TD_AUDIT).appendRow([Utilities.getUuid(),tdNow_(),String(actor.id||''),tdSafe_(actor.display_name||actor.username||'Developer'),tdSafe_(action),tdSafe_(JSON.stringify(details||{}))]);}
function tdReadAudit_(){const values=tdDb_().getSheetByName(TD_AUDIT).getDataRange().getValues(),out=[];for(let i=Math.max(1,values.length-200);i<values.length;i++)out.push({id:String(values[i][0]||''),timestamp:Number(values[i][1]||0),actor_id:String(values[i][2]||''),actor_name:String(values[i][3]||''),action:String(values[i][4]||''),details_json:String(values[i][5]||'{}')});out.sort((a,b)=>b.timestamp-a.timestamp);return out;}
function tdSetIfMissing_(sheet,key,value,id,name){const values=sheet.getDataRange().getValues();for(let i=1;i<values.length;i++)if(String(values[i][0])===key)return;sheet.appendRow([key,value,tdNow_(),id,tdSafe_(name)]);}
function tdSet_(sheet,key,value,id,name){const values=sheet.getDataRange().getValues(),now=tdNow_(),row=[key,tdSafe_(value),now,id,tdSafe_(name)];for(let i=1;i<values.length;i++)if(String(values[i][0])===key){sheet.getRange(i+1,1,1,5).setValues([row]);return;}sheet.appendRow(row);}
function tdDb_(){const id=PropertiesService.getScriptProperties().getProperty('DEVELOPER_SHEET_ID');if(!id)throw new Error('Developer backend is not configured. Run setupTornFcaDeveloperBackend() first.');return SpreadsheetApp.openById(id);}
function tdEnsureSheet_(ss,name,headers){let s=ss.getSheetByName(name);if(!s)s=ss.insertSheet(name);if(s.getLastRow()===0)s.appendRow(headers);return s;}
function tdTornGet_(path,key){const joiner=String(path).indexOf('?')>=0?'&':'?',r=UrlFetchApp.fetch('https://api.torn.com/v2'+path+joiner+'key='+encodeURIComponent(key),{method:'get',muteHttpExceptions:true,headers:{'User-Agent':'TornFCA-Developer/'+TD_VERSION}});let root;try{root=JSON.parse(r.getContentText());}catch(_){throw new Error('Unreadable Torn API response.');}if(root&&root.error)throw new Error(root.error.error||('Torn API error '+root.error.code));if(r.getResponseCode()<200||r.getResponseCode()>=300)throw new Error('Torn API HTTP '+r.getResponseCode());return root;}
function tdPublicUser_(u){return{id:u.id,name:u.name};}
function tdBool_(v){return v===true||String(v).toLowerCase()==='true'||Number(v)===1;}
function tdSafe_(v){const t=String(v==null?'':v);return/^[=+\-@]/.test(t)?"'"+t:t;}
function tdSha256_(v){const bytes=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(v||''),Utilities.Charset.UTF_8);return tdHex_(bytes);}
function tdJson_(o){return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);}
