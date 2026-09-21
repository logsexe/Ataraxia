/* Ataraxia stability candidate — local Instagram rules. No network calls, credentials or message collection. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) { window.__ataraxiaStillness.scan(); return; }
  const seen = new Set();
  let hiddenAds = 0;
  let scheduled = false;
  let previousY = null;
  let previousBounds = new WeakMap();
  let limit = 500;
  let focused = false;
  let articles = [];
  const dirty = new Set();
  const checked = new WeakSet();
  const scrollers = new WeakMap();
  const normalise = text => (text || '').replace(/[\u200b-\u200f\u202a-\u202e\u2060-\u206f]/g, '').replace(/\s+/g, ' ').trim().toLowerCase();
  const updateCap = () => {
    document.documentElement.toggleAttribute('data-quiet-capped', isFeed() && seen.size >= limit);
    document.documentElement.toggleAttribute('data-quiet-focused', focused && isFeed());
  };
  const blockedPath = p => /^\/(reels?|explore|tv)(\/|$)/i.test(p.replace(/\/+/g, '/'));
  const isFeed = () => location.pathname === '/';
  const labels = new Set(['ad', 'advertisement', 'sponsored', 'geborg', 'geborgde', 'suggested for you', 'voorgestel vir jou']);
  const style = document.createElement('style');
  style.textContent = '[data-quiet-hidden="true"]{display:none!important}[data-quiet-home-hidden="true"]{display:none!important}html[data-quiet-focused] body{visibility:hidden!important;pointer-events:none!important}html[data-quiet-focused]::after{content:"Focused mode. Open Inbox or Home below.";position:fixed;inset:0;z-index:2147483647;background:#101916;color:#edf5ee;padding:48px 24px;font:18px sans-serif}html{scroll-behavior:auto!important}html[data-quiet-capped] body{visibility:hidden!important;pointer-events:none!important}html[data-quiet-capped]::after{content:"Session post limit reached. Use Inbox below.";position:fixed;inset:0;z-index:2147483647;background:#101916;color:#edf5ee;padding:48px 24px;font:18px sans-serif}';
  (document.head || document.documentElement).appendChild(style);
  const hide = el => { if (el.dataset.quietHidden !== 'true') el.dataset.quietHidden = 'true'; };
  function scan() {
    scheduled = false;
    for (const a of document.querySelectorAll('a[href]')) {
      try {
        const u = new URL(a.getAttribute('href'), location.href);
        if (/(^|\.)instagram\.com$/i.test(u.hostname)) {
          if (blockedPath(u.pathname)) hide(a);
          if (focused && u.pathname === '/') a.dataset.quietHomeHidden = 'true';
          else delete a.dataset.quietHomeHidden;
        }
      } catch (_) { }
    }
    // Never inspect message bodies: sponsored/suggestion filtering is restricted to the home feed.
    if (!isFeed()) { previousY = null; previousBounds = new WeakMap(); updateCap(); return; }
    articles = Array.from(document.querySelectorAll('article,[role="article"]'));
    for (const article of articles) {
      if (!dirty.has(article) && checked.has(article)) continue;
      checked.add(article);
      let scroller = null;
      if (typeof window.getComputedStyle === 'function') {
        for (let parent = article.parentElement; parent && parent !== document.body; parent = parent.parentElement) {
          if (/^(auto|scroll|overlay)$/.test(getComputedStyle(parent).overflowY)) { scroller = parent; break; }
        }
      }
      scrollers.set(article,scroller);
      // Instagram can reuse a post container for different content.
      const wasHidden = article.dataset.quietHidden === 'true';
      if (wasHidden) delete article.dataset.quietHidden;
      let sponsored = false;
      // Match metadata before the main media, never arbitrary caption text.
      const media = article.querySelector('video') || Array.from(article.querySelectorAll('img')).find(img =>
        !/profile|avatar/i.test(img.alt || '') && !img.closest('header') &&
        !((Number(img.getAttribute('width')) || img.getBoundingClientRect().width) > 0 &&
          (Number(img.getAttribute('width')) || img.getBoundingClientRect().width) <= 100));
      for (const el of article.querySelectorAll('header, header span, header a, span, div, [aria-label], a[href*="/ads/"]')) {
        const header = el.closest('header');
        const beforeMedia = media && Boolean(el.compareDocumentPosition(media) & Node.DOCUMENT_POSITION_FOLLOWING)
          && !el.contains(media);
        if (!header && !beforeMedia) continue;
        const text = normalise(el.textContent);
        const aria = normalise(el.getAttribute('aria-label'));
        if (labels.has(text) || labels.has(aria)) {
          const rect = el.getBoundingClientRect();
          if (rect.width > 0 && rect.height > 0) { sponsored = true; break; }
        }
      }
      if (sponsored && article.dataset.quietHidden !== 'true') { hide(article); if (!wasHidden) hiddenAds++; }
    }
    dirty.clear();
    countPosts();
  }
  function countPosts() {
    if (!isFeed()) { previousY = null; previousBounds = new WeakMap(); updateCap(); return; }
    if (focused) { updateCap(); return; }
    const currentY = window.scrollY;
    const low = Math.min(previousY === null ? currentY : previousY, currentY) + 80;
    const high = Math.max(previousY === null ? currentY : previousY, currentY) + innerHeight;
    previousY = currentY;
    const clips = new Map();
    for (const article of articles) {
      if (article.dataset.quietHidden === 'true') continue;
      const bounds = article.getBoundingClientRect();
      const scroller = scrollers.get(article);
      if (scroller && !clips.has(scroller)) clips.set(scroller,scroller.getBoundingClientRect());
      const clip = scroller ? clips.get(scroller) : null;
      const viewTop = clip ? Math.max(0,clip.top) : 80;
      const viewBottom = clip ? Math.min(innerHeight,clip.bottom) : innerHeight;
      const prior = previousBounds.get(article);
      previousBounds.set(article, {top:bounds.top, bottom:bounds.bottom});
      const crossed = prior && ((prior.top >= viewBottom && bounds.bottom <= viewTop)
        || (prior.bottom <= viewTop && bounds.top >= viewBottom));
      if (bounds.width <= 0 || bounds.height <= 0) continue;
      if (viewBottom <= viewTop) continue;
      if (!crossed && (clip
        ? bounds.bottom <= viewTop || bounds.top >= viewBottom
        : bounds.bottom + currentY <= low || bounds.top + currentY >= high)) continue;
      const link = article.querySelector('a[href^="/p/"],a[href*="instagram.com/p/"],a[href^="/reel/"]');
      if (link) {
        try {
          const match = new URL(link.href, location.href).pathname.match(/^\/(?:p|reel)\/([A-Za-z0-9_-]{1,80})(?:\/|$)/);
          if (match && seen.size < limit) seen.add(match[1]);
        } catch (_) { }
      }
    }
    updateCap();
  }
  function queueScan() { if (!scheduled) { scheduled = true; requestAnimationFrame(scan); } }
  new MutationObserver(records => {
    for (const record of records) {
      const el = record.target.nodeType === 1 ? record.target : record.target.parentElement;
      const article = el && el.closest('article,[role="article"]');
      if (article) dirty.add(article);
    }
    queueScan();
  }).observe(document.documentElement, {subtree:true, childList:true, attributes:true, characterData:true, attributeFilter:['href','aria-label','src','alt']});
  // Read geometry on the scroll event: a deferred frame can lose a fast swipe.
  addEventListener('scroll', countPosts, {passive:true, capture:true});
  addEventListener('popstate', () => { updateCap(); queueScan(); });
  for (const method of ['pushState','replaceState']) {
    if (typeof history !== 'undefined') {
      const original = history[method];
      history[method] = function(...args) { const result=original.apply(this,args); updateCap(); queueScan(); return result; };
    }
  }
  document.addEventListener('click', e => {
    const a = e.target.closest && e.target.closest('a[href]');
    if (!a) return;
    try {
      const u = new URL(a.href, location.href);
      if (/(^|\.)instagram\.com$/i.test(u.hostname) && (blockedPath(u.pathname) || (focused && u.pathname === '/'))) {
        e.preventDefault(); e.stopImmediatePropagation();
      }
    } catch (_) { }
  }, true);
  window.__ataraxiaStillness = {
    scan: () => { for (const article of articles) dirty.add(article); scan(); },
    configure: config => {
      if (typeof config.focused === 'boolean') focused = config.focused;
      if (config.reset === true) { seen.clear(); previousY = null; previousBounds = new WeakMap(); }
      limit = Math.max(1, Math.min(500, Number(config.limit) || 10));
      for (const id of (config.ids || [])) if (/^[A-Za-z0-9_-]{1,80}$/.test(id)) seen.add(id);
      scan();
    },
    snapshot: () => ({ ids: isFeed() ? Array.from(seen).slice(0,500) : [], hiddenAds, feed:isFeed(), containers:isFeed() ? articles.length : 0 })
  };
  scan();
})();
