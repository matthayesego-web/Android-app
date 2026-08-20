TornFCA Community Backend v1.8.1 — Stale-Faction Push Hardening
Prepared: 2026-08-20
Status: Implemented on `work/v0.10.18-beta-hardening`; locally syntax-tested; not deployed

GOAL
Prevent a stale FCM registration from receiving faction-wide pushes after its Torn player has left that faction,
without making one Torn API request per device.

DESIGN
- One fresh Torn /faction/{factionId}/members request per faction-wide broadcast.
- Build a current-member ID set once.
- Filter PushDevices by BOTH stored faction_id and current member ID.
- Fail closed for the push if the member list cannot be verified.
- Do not roll back the underlying chat/announcement/banking action merely because push verification failed.
- Keep the already-tested FCM token dedupe and Android-side faction check unchanged.

WHY THIS APPROACH
Torn API v2 exposes /faction/{id}/members. Torn documents a service cache of up to 30 seconds and permits a
unique timestamp query parameter to bypass that cache where fresh data is required. This hardening adds one
Torn request per faction broadcast, not one per registered device.

EXPECTED COMMUNITY BACKEND VERSION
Change:
  const TC_VERSION='1.8.0';
to:
  const TC_VERSION='1.8.1';

1) MAKE CURRENT-FACTION AUTHORIZATION FRESH

Replace the first line inside tcVerifyUser_:
  const factionData=tcTornGet_('/user/faction',apiKey),faction=factionData&&factionData.faction;

with:
  const ts=Math.floor(Date.now()/1000),
        factionData=tcTornGet_('/user/faction?timestamp='+ts,apiKey),
        faction=factionData&&factionData.faction;

Replace the first line inside tcPositionAbilities_:
  const data=tcTornGet_('/faction/positions',apiKey),positions=data&&data.positions,current=String(user.position||'').trim().toLowerCase();

with:
  const ts=Math.floor(Date.now()/1000),
        data=tcTornGet_('/faction/positions?timestamp='+ts,apiKey),
        positions=data&&data.positions,
        current=String(user.position||'').trim().toLowerCase();

2) ADD CURRENT MEMBER SET HELPER

Add before tcDevices_:

function tcCurrentFactionMemberIds_(apiKey,factionId){
  const fid=Number(factionId||0);
  if(fid<=0)throw new Error('Valid faction ID required for push recipient verification.');
  const ts=Math.floor(Date.now()/1000);
  const data=tcTornGet_('/faction/'+fid+'/members?striptags=true&timestamp='+ts,apiKey);
  const members=data&&data.members;
  if(!Array.isArray(members))throw new Error('Unable to verify the current faction member list.');
  const ids={};
  members.forEach(m=>{
    const id=Number(m&&m.id||0);
    if(id>0)ids[id]=true;
  });
  if(!Object.keys(ids).length)throw new Error('Current faction member list was empty.');
  return ids;
}

3) FILTER DEVICES AGAINST CURRENT MEMBERS

Change:
function tcDevices_(factionId,playerId,type,excludePlayer){

to:
function tcDevices_(factionId,playerId,type,excludePlayer,currentMemberIds){

Immediately after:
  const pid=Number(values[i][2]||0);

add:
  if(currentMemberIds&&!currentMemberIds[pid])continue;

Keep all existing preference, 90-day staleness and banking authorization checks.

4) HARDEN FACTION PUSH; KEEP PLAYER PUSH DIRECT

Replace:
function tcPushToFaction_(factionId,excludePlayer,type,title,body,data){return tcPush_(tcDevices_(factionId,0,type,excludePlayer),factionId,type,title,body,data);}
function tcPushToPlayer_(factionId,playerId,type,title,body,data){const scoped=Object.assign({},data||{},{target_player_id:String(playerId)});return tcPush_(tcDevices_(factionId,playerId,type,0),factionId,type,title,body,scoped);}

with:

function tcPushToFaction_(apiKey,factionId,excludePlayer,type,title,body,data){
  let currentMembers;
  try{
    currentMembers=tcCurrentFactionMemberIds_(apiKey,factionId);
  }catch(err){
    return{
      configured:tcFirebaseConfigured_(),
      sent:0,
      failed:0,
      blocked:true,
      reason:'membership_verification_failed',
      error:String(err&&err.message||err)
    };
  }
  return tcPush_(
    tcDevices_(factionId,0,type,excludePlayer,currentMembers),
    factionId,type,title,body,data
  );
}

function tcPushToPlayer_(factionId,playerId,type,title,body,data){
  const scoped=Object.assign({},data||{},{target_player_id:String(playerId)});
  return tcPush_(tcDevices_(factionId,playerId,type,0,null),factionId,type,title,body,scoped);
}

5) PASS THE REQUESTING USER'S API KEY TO FACTION BROADCASTS

In doPost change:
  tcSendChat_(user,body)
to:
  tcSendChat_(key,user,body)

Change:
  tcPushToFaction_(user.faction_id,0,'announcement',title,message,{destination:'announcements'})
to:
  tcPushToFaction_(key,user.faction_id,0,'announcement',title,message,{destination:'announcements'})

Change:
  tcPushToFaction_(user.faction_id,user.id,'banking',title,message,{destination:'banking',requester_id:String(user.id),requester_name:String(user.name||'Member')})
to:
  tcPushToFaction_(key,user.faction_id,user.id,'banking',title,message,{destination:'banking',requester_id:String(user.id),requester_name:String(user.name||'Member')})

6) PASS API KEY THROUGH CHAT SEND

Change:
function tcSendChat_(user,body){

to:
function tcSendChat_(apiKey,user,body){

Change:
  tcPushToFaction_(user.faction_id,user.id,'chat',user.name+' • '+channel,message,{channel:channel,author_id:String(user.id)})

to:
  tcPushToFaction_(apiKey,user.faction_id,user.id,'chat',user.name+' • '+channel,message,{channel:channel,author_id:String(user.id)})

TEST MATRIX AFTER DEPLOYMENT
1. Current member, one device: receives one faction announcement.
2. Current member, two devices: each device receives one.
3. Sender exclusion for chat/banking: sender does not get the fan-out intended for other recipients.
4. Device stored under the same faction but player ID absent from /faction/{id}/members: receives nothing.
5. Member-list verification fails: underlying action succeeds, push result reports blocked=true, and no faction push is sent.
6. Existing Cloud Push Test still works (player-targeted test path unchanged).
7. Existing banking authorization filtering still works.
8. Duplicate-notification regression test still produces exactly one notification/device.

DEPLOYMENT NOTE
This is the COMMUNITY backend, not the Faction Backend.
Do not deploy until the exact source is committed/compiled/tested on an isolated branch and the existing Firebase
dedupe/banking behavior has passed regression testing.
