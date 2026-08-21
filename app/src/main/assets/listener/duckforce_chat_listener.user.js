// ==UserScript==
// @name         TornFCA Banking Companion
// @namespace    tornfca.app
// @version      0.9.13
// @description  Queues visible faction-chat banking requests only while the player is actively using Torn.
// @author       TornFCA
// @match        https://www.torn.com/*
// @run-at       document-end
// ==/UserScript==

(() => {
  'use strict';

  const VERSION = '0.9.13';
  const BACKEND_URL = '###TORNFCA-FACTION-BACKEND-URL###';
  const LISTENER_TOKEN = '###TORNFCA-LISTENER-TOKEN###';
  const SEEN_KEY = 'tornfca_banking_seen_v1';
  const MAX_SEEN = 1200;
  const ACTIVE_WINDOW_MS = 90_000;
  const inflight = new Set();
  let activeUntil = 0;
  let scanTimer = null;

  const REQUEST_PATTERNS = [
    /^\s*!bank(?:\s|$)/i,
    /^\s*!balance(?:\s|$)/i,
    /\bbanker\b/i,
    /\bbank\s+(?:me|please|pls|plz)\b/i,
    /\b(?:bank|withdraw|send|get)\s+(?:me\s+)?(?:\$\s*)?\d+(?:\.\d+)?\s*(?:k|m|b|bn|thousand|million|billion)?\b/i,
    /\b(?:\$\s*)?\d+(?:\.\d+)?\s*(?:k|m|b|bn|thousand|million|billion)\s+(?:bank|withdrawal|withdraw)\b/i,
    /\b(?:can|could|may|would)\s+i\s+(?:please\s+)?(?:get|have|see|check)\s+(?:my\s+)?(?:faction\s+)?balance\b/i,
    /\b(?:balance\s+check|check\s+(?:my\s+)?balance|what(?:'s|\s+is)\s+my\s+(?:faction\s+)?balance|my\s+(?:faction\s+)?balance\s+(?:please|pls|plz)|balance\s+(?:please|pls|plz))\b/i,
    /\b(?:cash\s*out|withdraw(?:al)?)\b/i
  ];
  const REQUEST_WORDS = /(?:!bank|!balance|\bbanker\b|\bbank\b|\bbalance\b|\bcash\s*out\b|\bwithdraw(?:al)?\b|\bfunds\b)/i;

  function configured() { return /^https:\/\//i.test(BACKEND_URL) && !BACKEND_URL.includes('###') && LISTENER_TOKEN && !LISTENER_TOKEN.includes('###'); }
  function normalize(text) { return String(text || '').replace(/\s+/g, ' ').trim(); }
  function isBankingRequest(text) { const clean = normalize(text);return !!clean && clean.length <= 700 && REQUEST_WORDS.test(clean) && REQUEST_PATTERNS.some(p => p.test(clean)); }
  function parseAmount(text) {
    const clean = normalize(text).toLowerCase().replace(/,/g, '');
    const patterns = [/(?:\$\s*)?(\d+(?:\.\d+)?)\s*(b|bn|billion)\b/i,/(?:\$\s*)?(\d+(?:\.\d+)?)\s*(m|mil|million)\b/i,/(?:\$\s*)?(\d+(?:\.\d+)?)\s*(k|thousand)\b/i,/\$\s*(\d{4,})\b/i,/(?:!bank|bank|withdraw|send|get)\s+(?:me\s+)?(\d{4,})\b/i];
    for (const pattern of patterns) {const m = clean.match(pattern); if (!m) continue;let v = Number(m[1]); if (!Number.isFinite(v) || v <= 0) continue;const suffix = String(m[2] || '').toLowerCase();if (suffix.startsWith('b')) v *= 1_000_000_000;else if (suffix.startsWith('m')) v *= 1_000_000;else if (suffix.startsWith('k') || suffix.startsWith('t')) v *= 1_000;return Math.round(v);}return null;
  }
  function visible(element) {if (!element || !(element instanceof Element)) return false;const style = getComputedStyle(element);if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0) return false;const rect = element.getBoundingClientRect(); return rect.width > 0 && rect.height > 0;}
  function factionChatIsOpen() {const selectors = ['[role="tab"][aria-selected="true"]','[class*="chat"] [class*="active"]','[class*="chat"] [class*="title"]','[class*="chat"] [class*="header"]','[data-channel]','[data-chat-type]'];for (const selector of selectors) {let rows=[]; try { rows=document.querySelectorAll(selector); } catch (_) {}for (const element of rows) {if (!visible(element)) continue;const value = `${element.textContent || ''} ${element.getAttribute('data-channel') || ''} ${element.getAttribute('data-chat-type') || ''}`;if (/\bfaction\b/i.test(value)) return true;}}return false;}
  function activeSession() {return configured() && !document.hidden && document.hasFocus() && Date.now() <= activeUntil && factionChatIsOpen();}
  function markActive() { activeUntil = Date.now() + ACTIVE_WINDOW_MS; scheduleScan(180); }
  function getSeen() { try { const v=JSON.parse(localStorage.getItem(SEEN_KEY)||'[]'); return Array.isArray(v)?v:[]; } catch (_) { return []; } }
  function remember(fp) { const seen=getSeen().filter(x=>x!==fp);seen.push(fp);while(seen.length>MAX_SEEN)seen.shift();localStorage.setItem(SEEN_KEY,JSON.stringify(seen)); }
  function wasSeen(fp) { return getSeen().includes(fp); }
  function hash(input) { let h=2166136261;for(let i=0;i<input.length;i++){h^=input.charCodeAt(i);h=Math.imul(h,16777619);}return(h>>>0).toString(16); }
  function profileIdFrom(node) {const anchors=node.querySelectorAll?node.querySelectorAll('a[href*="XID="]'):[];for(const a of anchors){const href=a.getAttribute('href')||'';const m=href.match(/[?&#]XID=(\d+)/i);if(m)return{id:Number(m[1]),name:normalize(a.textContent)};}return null;}
  function messageTimestamp(node) {const time=node.querySelector&&node.querySelector('time[datetime]');if(time){const p=Date.parse(time.getAttribute('datetime'));if(Number.isFinite(p))return Math.floor(p/1000);}const t=node.querySelector&&node.querySelector('[data-timestamp]');if(t){const raw=Number(t.getAttribute('data-timestamp'));if(Number.isFinite(raw)&&raw>0)return raw>1e12?Math.floor(raw/1000):Math.floor(raw);}return 0;}
  function explicitMessageId(node) { for(const attr of ['data-message-id','data-chat-message-id','data-id','data-messageid']){const v=node.getAttribute&&node.getAttribute(attr);if(v)return String(v);}return ''; }
  function candidateMessages() {const nodes=new Set();for(const selector of ['[data-message-id]','[data-chat-message-id]','[class*="chatMessage"]','[class*="chat-message"]','[class*="message_"]','[class*="message-"]']){try{document.querySelectorAll(selector).forEach(n=>nodes.add(n));}catch(_){}}document.querySelectorAll('a[href*="XID="]').forEach(a=>{const p=a.closest('[data-message-id],[data-chat-message-id],[class*="message"],li');if(p)nodes.add(p);});return [...nodes].filter(node=>{if(!visible(node))return false;const text=normalize(node.innerText||node.textContent);return text.length>=3&&text.length<=900&&REQUEST_WORDS.test(text);});}
  async function submitRequest(payload) {if(!activeSession())return false;try {await fetch(BACKEND_URL,{method:'POST',mode:'no-cors',cache:'no-store',headers:{'Content-Type':'text/plain;charset=UTF-8'},body:JSON.stringify({action:'listener_event',listener_token:LISTENER_TOKEN,listener_version:VERSION,...payload})});return true;} catch(error) { console.warn('[TornFCA Banking] Queue failed:',error); return false; }}
  async function scan() {if(!activeSession())return;for(const node of candidateMessages()){if(!activeSession())return;const text=normalize(node.innerText||node.textContent);if(!isBankingRequest(text))continue;const profile=profileIdFrom(node);if(!profile||!profile.id)continue;const messageId=explicitMessageId(node),timestamp=messageTimestamp(node);const fingerprint=messageId?`msg:${messageId}`:`fp:${hash(`${profile.id}|${timestamp}|${text.toLowerCase()}`)}`;if(wasSeen(fingerprint)||inflight.has(fingerprint))continue;inflight.add(fingerprint);try {const amount=parseAmount(text);const sent=await submitRequest({fingerprint,message_id:messageId,requester_id:profile.id,requester_name:profile.name||`ID ${profile.id}`,request_text:text,requested_amount:amount,request_mode:amount?'AMOUNT':'FULL_BALANCE',message_timestamp:timestamp||null,detected_timestamp:Math.floor(Date.now()/1000),source:'FACTION_CHAT_ACTIVE'});if(sent)remember(fingerprint);} finally { inflight.delete(fingerprint); }}}
  function scheduleScan(delay=250){clearTimeout(scanTimer);scanTimer=setTimeout(scan,delay);}
  const observer=new MutationObserver(()=>{if(activeSession())scheduleScan(220);});observer.observe(document.documentElement,{childList:true,subtree:true});
  ['pointerdown','mousedown','touchstart','keydown','input','wheel'].forEach(type=>document.addEventListener(type,markActive,{capture:true,passive:true}));
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)markActive();});window.addEventListener('focus',markActive);
  if(!configured())console.info('[TornFCA Banking] Configure the faction backend URL and listener token before installing this companion.');
})();
