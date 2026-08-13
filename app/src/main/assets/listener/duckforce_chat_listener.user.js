// ==UserScript==
// @name         Duck Force Banking Chat Listener
// @namespace    duckforce.toolkit
// @version      0.3.0
// @description  Detect loaded Duck Force faction-chat banking requests and queue only matched requests for Duck Force Toolkit.
// @author       Duck Force
// @match        https://www.torn.com/*
// @run-at       document-end
// ==/UserScript==

(() => {
  'use strict';

  const VERSION = '0.3.0';
  const BACKEND_URL = '###DUCKFORCE-BACKEND-URL###';
  const LISTENER_TOKEN = '###DUCKFORCE-LISTENER-TOKEN###';
  const SEEN_KEY = 'duckforce_listener_seen_v1';
  const MAX_SEEN = 600;
  const DEBUG = false;

  const REQUEST_PATTERNS = [
    /\bbanker\b/i,
    /\bbank\s+(?:me|please|pls|plz)\b/i,
    /\b(?:can|could|may|would)\s+i\s+(?:please\s+)?(?:get|have|see|check)\s+(?:my\s+)?(?:faction\s+)?balance\b/i,
    /\b(?:can|could|would)\s+(?:someone|somebody|a\s+banker)\s+(?:please\s+)?(?:get|check|send)\s+(?:me\s+)?(?:my\s+)?(?:faction\s+)?balance\b/i,
    /\b(?:balance\s+check|check\s+(?:my\s+)?balance|what(?:'s|\s+is)\s+my\s+(?:faction\s+)?balance|my\s+(?:faction\s+)?balance\s+(?:please|pls|plz)|balance\s+(?:please|pls|plz))\b/i,
    /\b(?:cash\s*out|withdraw(?:al)?)\b/i,
    /\b(?:can|could|may|would)\s+i\s+(?:please\s+)?get\s+(?:my\s+)?(?:money|funds)\b/i
  ];

  const REQUEST_WORDS = /\b(?:banker|bank|balance|cash\s*out|withdraw|withdrawal|funds)\b/i;

  function log(...args) {
    if (DEBUG) console.log('[DuckForce Listener]', ...args);
  }

  function configured() {
    return /^https:\/\//i.test(BACKEND_URL)
      && !BACKEND_URL.includes('###')
      && LISTENER_TOKEN
      && !LISTENER_TOKEN.includes('###');
  }

  function normalize(text) {
    return String(text || '').replace(/\s+/g, ' ').trim();
  }

  function isBankingRequest(text) {
    const clean = normalize(text);
    if (!clean || clean.length > 700 || !REQUEST_WORDS.test(clean)) return false;
    return REQUEST_PATTERNS.some(pattern => pattern.test(clean));
  }

  function parseAmount(text) {
    const clean = normalize(text).toLowerCase().replace(/,/g, '');
    const patterns = [
      /(?:\$\s*)?(\d+(?:\.\d+)?)\s*(b|bn|billion)\b/i,
      /(?:\$\s*)?(\d+(?:\.\d+)?)\s*(m|mil|million)\b/i,
      /(?:\$\s*)?(\d+(?:\.\d+)?)\s*(k|thousand)\b/i,
      /\$\s*(\d{4,})\b/i,
      /\b(?:bank|withdraw|cash\s*out)\s+(?:me\s+)?(\d{4,})\b/i
    ];

    for (const pattern of patterns) {
      const match = clean.match(pattern);
      if (!match) continue;
      let value = Number(match[1]);
      if (!Number.isFinite(value) || value <= 0) continue;
      const suffix = String(match[2] || '').toLowerCase();
      if (suffix.startsWith('b')) value *= 1_000_000_000;
      else if (suffix.startsWith('m')) value *= 1_000_000;
      else if (suffix.startsWith('k') || suffix.startsWith('t')) value *= 1_000;
      return Math.round(value);
    }
    return null;
  }

  function getSeen() {
    try {
      const value = JSON.parse(localStorage.getItem(SEEN_KEY) || '[]');
      return Array.isArray(value) ? value : [];
    } catch (_) {
      return [];
    }
  }

  function remember(fingerprint) {
    const seen = getSeen().filter(item => item !== fingerprint);
    seen.push(fingerprint);
    while (seen.length > MAX_SEEN) seen.shift();
    localStorage.setItem(SEEN_KEY, JSON.stringify(seen));
  }

  function wasSeen(fingerprint) {
    return getSeen().includes(fingerprint);
  }

  function hash(input) {
    let h = 2166136261;
    for (let i = 0; i < input.length; i++) {
      h ^= input.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    return (h >>> 0).toString(16);
  }

  function visible(element) {
    if (!element || !(element instanceof Element)) return false;
    const style = getComputedStyle(element);
    if (style.display === 'none' || style.visibility === 'hidden' || Number(style.opacity) === 0) return false;
    const rect = element.getBoundingClientRect();
    return rect.width > 0 && rect.height > 0;
  }

  function factionChatIsOpen() {
    const selectors = [
      '[role="tab"][aria-selected="true"]',
      '[class*="chat"] [class*="active"]',
      '[class*="chat"] [class*="title"]',
      '[class*="chat"] [class*="header"]',
      '[data-channel]',
      '[data-chat-type]'
    ];

    for (const selector of selectors) {
      for (const element of document.querySelectorAll(selector)) {
        if (!visible(element)) continue;
        const value = `${element.textContent || ''} ${element.getAttribute('data-channel') || ''} ${element.getAttribute('data-chat-type') || ''}`;
        if (/\bfaction\b/i.test(value)) return true;
      }
    }
    return false;
  }

  function profileIdFrom(node) {
    const anchors = node.querySelectorAll ? node.querySelectorAll('a[href*="XID="]') : [];
    for (const anchor of anchors) {
      const href = anchor.getAttribute('href') || '';
      const match = href.match(/[?&#]XID=(\d+)/i);
      if (match) return { id: Number(match[1]), name: normalize(anchor.textContent) };
    }
    return null;
  }

  function messageTimestamp(node) {
    const time = node.querySelector && node.querySelector('time[datetime]');
    if (time) {
      const parsed = Date.parse(time.getAttribute('datetime'));
      if (Number.isFinite(parsed)) return Math.floor(parsed / 1000);
    }

    const timestampNode = node.querySelector && node.querySelector('[data-timestamp]');
    if (timestampNode) {
      const raw = Number(timestampNode.getAttribute('data-timestamp'));
      if (Number.isFinite(raw) && raw > 0) return raw > 1e12 ? Math.floor(raw / 1000) : Math.floor(raw);
    }
    return 0;
  }

  function explicitMessageId(node) {
    const attrs = ['data-message-id', 'data-chat-message-id', 'data-id', 'data-messageid'];
    for (const attr of attrs) {
      const value = node.getAttribute && node.getAttribute(attr);
      if (value) return String(value);
    }
    return '';
  }

  function candidateMessages() {
    const nodes = new Set();
    const selectors = [
      '[data-message-id]',
      '[data-chat-message-id]',
      '[class*="chatMessage"]',
      '[class*="chat-message"]',
      '[class*="message_"]',
      '[class*="message-"]'
    ];

    selectors.forEach(selector => {
      try {
        document.querySelectorAll(selector).forEach(node => nodes.add(node));
      } catch (_) {}
    });

    document.querySelectorAll('a[href*="XID="]').forEach(anchor => {
      const parent = anchor.closest('[data-message-id],[data-chat-message-id],[class*="message"],li');
      if (parent) nodes.add(parent);
    });

    return [...nodes].filter(node => {
      if (!visible(node)) return false;
      const text = normalize(node.innerText || node.textContent);
      return text.length >= 3 && text.length <= 900 && REQUEST_WORDS.test(text);
    });
  }

  async function submitRequest(payload) {
    if (!configured()) return;
    try {
      await fetch(BACKEND_URL, {
        method: 'POST',
        mode: 'no-cors',
        cache: 'no-store',
        headers: { 'Content-Type': 'text/plain;charset=UTF-8' },
        body: JSON.stringify({
          action: 'listener_event',
          listener_token: LISTENER_TOKEN,
          listener_version: VERSION,
          ...payload
        })
      });
      log('Queued request', payload);
    } catch (error) {
      console.warn('[DuckForce Listener] Could not queue banking request:', error);
    }
  }

  function scan() {
    if (!configured() || !document.hasFocus() || !factionChatIsOpen()) return;

    for (const node of candidateMessages()) {
      const text = normalize(node.innerText || node.textContent);
      if (!isBankingRequest(text)) continue;

      const profile = profileIdFrom(node);
      if (!profile || !profile.id) continue;

      const messageId = explicitMessageId(node);
      const timestamp = messageTimestamp(node);
      const fingerprint = messageId
        ? `msg:${messageId}`
        : `fp:${hash(`${profile.id}|${timestamp}|${text.toLowerCase()}`)}`;

      if (wasSeen(fingerprint)) continue;
      remember(fingerprint);

      const amount = parseAmount(text);
      submitRequest({
        fingerprint,
        message_id: messageId,
        requester_id: profile.id,
        requester_name: profile.name || `ID ${profile.id}`,
        request_text: text,
        requested_amount: amount,
        request_mode: amount ? 'AMOUNT' : 'FULL_BALANCE',
        message_timestamp: timestamp || null,
        detected_timestamp: Math.floor(Date.now() / 1000),
        source: 'FACTION_CHAT_RETROACTIVE'
      });
    }
  }

  let scanTimer = null;
  function scheduleScan(delay = 350) {
    clearTimeout(scanTimer);
    scanTimer = setTimeout(scan, delay);
  }

  const observer = new MutationObserver(() => scheduleScan());
  observer.observe(document.documentElement, { childList: true, subtree: true });

  document.addEventListener('visibilitychange', () => {
    if (!document.hidden) scheduleScan(200);
  });
  window.addEventListener('focus', () => scheduleScan(200));
  window.addEventListener('scroll', () => scheduleScan(450), true);

  setTimeout(scan, 800);
  setTimeout(scan, 2500);
  setInterval(() => {
    if (document.hasFocus()) scan();
  }, 5000);

  if (!configured()) {
    console.info('[DuckForce Listener] Install this script from Duck Force Toolkit after the shared backend is configured.');
  } else {
    log('Listener ready');
  }
})();
