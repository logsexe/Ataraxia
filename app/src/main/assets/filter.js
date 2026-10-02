/* Still — local Instagram rules. No network calls, credentials, or message collection.
   Does not count posts and does not listen to scroll. Hiding a post link was leaving
   the empty media box behind and shoving the feed around. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) { window.__ataraxiaStillness.scan(); return; }
  let hiddenAds = 0;
  let scanTimer = 0;
  let rooms = new Set(['friends']);
  let marks = {};
  let sorted = {};
  try {
    if (typeof localStorage !== 'undefined') {
      const stored = JSON.parse(localStorage.getItem('still-sorted') || '{}');
      if (stored && typeof stored === 'object') sorted = stored;
    }
  } catch (_) { sorted = {}; }
  const normalise = text => (text || '').replace(/[\u200b-\u200f\u202a-\u202e\u2060-\u206f]/g, '').replace(/\s+/g, ' ').trim().toLowerCase();
  const reserved = new Set(['p', 'reel', 'reels', 'explore', 'tv', 'shop', 'shopping', 'live', 'direct', 'accounts', 'stories', 'about', 'legal', 'privacy']);
  const roomOf = { friend: 'friends', influencer: 'influencers', celebrity: 'influencers' };
  const isFeed = () => location.pathname === '/';
  const adWord = /(^| )(sponsored|geborg|geborgde|suggested for you|voorgestel vir jou)$/;
  const style = document.createElement('style');
  style.textContent = '[data-quiet-hidden="true"]{display:none!important;height:0!important;min-height:0!important;max-height:0!important;margin:0!important;padding:0!important;overflow:hidden!important;border:0!important}html{scroll-behavior:auto!important}';
  (document.head || document.documentElement).appendChild(style);
  const navPath = p => {
    const n = (p || '').replace(/\/+/g, '/');
    if (/^\/(reels|explore|tv|shop|shopping|live)(\/|$)/i.test(n)) return true;
    return /^\/(?!(?:p|reel|reels|explore|tv|direct|accounts|stories)\/)[^/]+\/live(\/|$)/i.test(n);
  };
  function conceal(el) {
    if (!el || el.dataset.quietHidden === 'true') return;
    el.dataset.quietHidden = 'true';
  }
  function reveal(el) {
    if (!el || el.dataset.quietHidden !== 'true') return;
    delete el.dataset.quietHidden;
  }
  function author(article) {
    let found = '';
    for (const a of article.querySelectorAll('a[href]')) {
      const href = a.getAttribute && a.getAttribute('href');
      if (!href) continue;
      try {
        const part = new URL(href, location.href).pathname.toLowerCase().match(/^\/([a-z0-9._]{1,30})\/?$/);
        if (!part || reserved.has(part[1])) continue;
        found = part[1];
        break;
      } catch (_) { }
    }
    return found;
  }
  function leaveAlone(article) {
    // Removing an ad changes the feed height. Instagram inserts it again and the scroll returns to the top.
    for (const el of article.querySelectorAll('header, [aria-label]')) {
      const text = normalise(el.textContent);
      const aria = normalise(el.getAttribute && el.getAttribute('aria-label'));
      if (adWord.test(text) || adWord.test(aria)) return true;
    }
    return false;
  }
  function pageProfile() {
    const match = location.pathname.toLowerCase().match(/^\/([a-z0-9._]{1,30})(?:\/(?:reels|tagged|saved))?\/?$/);
    if (!match || reserved.has(match[1])) return '';
    return match[1];
  }
  function scale(num, suffix) {
    const n = parseFloat(String(num).replace(/,/g, ''));
    if (!isFinite(n)) return null;
    const unit = (suffix || '').toLowerCase();
    if (unit === 'k') return Math.round(n * 1000);
    if (unit === 'm') return Math.round(n * 1000000);
    if (unit === 'b') return Math.round(n * 1000000000);
    return Math.round(n);
  }
  function evidenceText(root) {
    let source = root;
    if (root && root.querySelectorAll) {
      const headers = root.querySelectorAll('header');
      if (headers.length) source = headers[0];
      else if (root.matches && root.matches('article,[role="article"]')) return '';
    }
    let blob = source && source.textContent ? source.textContent : '';
    const own = source && source.getAttribute && (source.getAttribute('aria-label') || source.getAttribute('title'));
    if (own) blob += ' ' + own;
    if (source && source.querySelectorAll) {
      for (const el of source.querySelectorAll('[aria-label], [title]')) {
        blob += ' ' + (el.getAttribute && (el.getAttribute('aria-label') || el.getAttribute('title')) || '');
      }
    }
    return normalise(blob);
  }
  function decide(root) {
    const blob = evidenceText(root);
    const followsYou = /\bfollows you\b|\bvolg jou\b/.test(blob);
    const verified = /\bverified\b/.test(blob);
    const count = blob.match(/(\d[\d.,]*)\s*([kmb])?\s+(followers|volgers)\b/);
    const audience = count ? scale(count[1], count[2]) : null;
    if (audience != null && audience >= 10000) return 'influencer';
    if (verified && !followsYou) return 'influencer';
    if (followsYou) return 'friend';
    return '';
  }
  function remember(name, kind) {
    if (!name || (kind !== 'friend' && kind !== 'influencer') || sorted[name] === kind) return;
    if (!sorted[name] && Object.keys(sorted).length >= 500) return;
    sorted[name] = kind;
    try {
      if (typeof localStorage !== 'undefined') localStorage.setItem('still-sorted', JSON.stringify(sorted));
    } catch (_) { }
  }
  function filedKind(name) {
    const manual = marks[name];
    if (manual === 'friend' || manual === 'influencer' || manual === 'celebrity') return manual;
    return sorted[name] === 'friend' || sorted[name] === 'influencer' ? sorted[name] : '';
  }
  function scan() {
    clearTimeout(scanTimer);
    scanTimer = 0;
    for (const a of document.querySelectorAll('a[href]')) {
      if (a.closest && a.closest('article,[role="article"]')) continue;
      try {
        const u = new URL(a.getAttribute('href'), location.href);
        if (/(^|\.)instagram\.com$/i.test(u.hostname) && navPath(u.pathname)) conceal(a);
      } catch (_) { }
    }
    const who = pageProfile();
    if (who) {
      const root = document.querySelector && (document.querySelector('main') || document.querySelector('header'));
      if (root) remember(who, decide(root));
    }
    if (!isFeed()) return;
    for (const article of document.querySelectorAll('article,[role="article"]')) {
      if (leaveAlone(article)) continue;
      const name = author(article);
      if (name) remember(name, decide(article));
      const filed = filedKind(name);
      const room = roomOf[filed];
      if (room && !rooms.has(room)) conceal(article);
      else reveal(article);
    }
  }
  function queueScan(records) {
    if (records && records.length) {
      let interesting = false;
      for (const record of records) {
        for (const node of record.addedNodes) {
          if (!node || node.nodeType !== 1) continue;
          if ((node.matches && node.matches('article,[role="article"],a[href]'))
            || (node.querySelector && node.querySelector('article,[role="article"],a[href]'))) {
            interesting = true;
            break;
          }
        }
        if (interesting) break;
      }
      if (!interesting && !pageProfile()) return;
    }
    clearTimeout(scanTimer);
    scanTimer = setTimeout(scan, 250);
  }
  new MutationObserver(queueScan).observe(document.documentElement, { subtree: true, childList: true });
  addEventListener('popstate', queueScan);
  document.addEventListener('click', e => {
    const a = e.target.closest && e.target.closest('a[href]');
    if (!a) return;
    try {
      const u = new URL(a.href, location.href);
      if (/(^|\.)instagram\.com$/i.test(u.hostname) && navPath(u.pathname)) {
        e.preventDefault(); e.stopImmediatePropagation();
      }
    } catch (_) { }
  }, true);
  window.__ataraxiaStillness = {
    scan,
    configure: config => {
      if (config && Array.isArray(config.rooms)) rooms = new Set(config.rooms);
      if (config && config.people && typeof config.people === 'object') marks = config.people;
      if (config && config.sorted && typeof config.sorted === 'object') {
        for (const key of Object.keys(config.sorted)) {
          const kind = config.sorted[key];
          if (kind === 'friend' || kind === 'influencer') sorted[key] = kind;
        }
      }
      scan();
    },
    snapshot: () => ({ ids: [], hiddenAds, feed: isFeed(), sorted })
  };
  scan();
})();
