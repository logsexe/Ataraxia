/* Still — local Instagram rules. No network calls, credentials, or message collection.
   Does not count posts and does not listen to scroll. Hiding a post link was leaving
   the empty media box behind and shoving the feed around. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) { window.__ataraxiaStillness.scan(); return; }
  let hiddenAds = 0;
  let scanTimer = 0;
  let rooms = new Set(['friends', 'influencers', 'celebrities']);
  let people = {};
  const normalise = text => (text || '').replace(/[\u200b-\u200f\u202a-\u202e\u2060-\u206f]/g, '').replace(/\s+/g, ' ').trim().toLowerCase();
  const reserved = new Set(['p', 'reel', 'reels', 'explore', 'tv', 'shop', 'shopping', 'live', 'direct', 'accounts', 'stories', 'about', 'legal', 'privacy']);
  const roomOf = { friend: 'friends', influencer: 'influencers', celebrity: 'celebrities' };
  const isFeed = () => location.pathname === '/';
  const labels = new Set(['sponsored', 'geborg', 'geborgde', 'suggested for you', 'voorgestel vir jou']);
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
    // Collapse only a one-child slot. A wider parent is the feed column, and hiding it blanks the page.
    const parent = el.parentElement;
    if (!parent || parent === document.body || parent === document.documentElement || parent.childElementCount !== 1) return;
    const posts = parent.querySelectorAll('article,[role="article"]');
    if (posts.length === 1 && posts[0].dataset.quietHidden === 'true') parent.dataset.quietHidden = 'true';
  }
  function reveal(el) {
    if (!el) return;
    if (el.dataset.quietHidden === 'true') delete el.dataset.quietHidden;
    let parent = el.parentElement;
    for (let depth = 0; depth < 6 && parent && parent !== document.body && parent !== document.documentElement; depth++) {
      if (parent.dataset.quietHidden !== 'true') break;
      delete parent.dataset.quietHidden;
      parent = parent.parentElement;
    }
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
  function sponsored(article) {
    const media = article.querySelector('video, img:not([alt*="profile" i])');
    for (const el of article.querySelectorAll('header, header span, header a, span, [aria-label], a[href*="/ads/"]')) {
      const header = el.closest && el.closest('header');
      const beforeMedia = media && el.compareDocumentPosition && (el.compareDocumentPosition(media) & Node.DOCUMENT_POSITION_FOLLOWING) && !(el.contains && el.contains(media));
      if (!header && !beforeMedia) continue;
      const text = normalise(el.textContent);
      const aria = normalise(el.getAttribute && el.getAttribute('aria-label'));
      if (labels.has(text) || labels.has(aria)) return true;
    }
    return false;
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
    if (!isFeed()) return;
    for (const article of document.querySelectorAll('article,[role="article"]')) {
      if (sponsored(article)) {
        if (article.dataset.quietHidden !== 'true') { conceal(article); hiddenAds++; }
        continue;
      }
      const name = author(article);
      const filed = people[name];
      const room = roomOf[filed];
      if (room && !rooms.has(room)) conceal(article);
      else reveal(article);
    }
  }
  function queueScan() {
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
      if (config && config.people && typeof config.people === 'object') people = config.people;
      scan();
    },
    snapshot: () => ({ ids: [], hiddenAds, feed: isFeed() })
  };
  scan();
})();
