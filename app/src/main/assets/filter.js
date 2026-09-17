/* Ataraxia 0.1.0 — local Instagram rules. No network calls, credentials or message collection. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) { window.__ataraxiaStillness.scan(); return; }
  const seen = new Set();
  let hiddenAds = 0;
  let scheduled = false;
  const blockedPath = p => /^\/(reels?|explore|tv)(\/|$)/i.test(p.replace(/\/+/g, '/'));
  const isFeed = () => location.pathname === '/';
  const labels = new Set(['sponsored', 'geborg', 'geborgde', 'suggested for you', 'voorgestel vir jou']);
  const style = document.createElement('style');
  style.textContent = '[data-quiet-hidden="true"]{display:none!important}html{scroll-behavior:auto!important}';
  (document.head || document.documentElement).appendChild(style);
  const hide = el => { if (el.dataset.quietHidden !== 'true') el.dataset.quietHidden = 'true'; };
  function scan() {
    scheduled = false;
    for (const a of document.querySelectorAll('a[href]')) {
      try {
        const u = new URL(a.getAttribute('href'), location.href);
        if (/(^|\.)instagram\.com$/i.test(u.hostname) && blockedPath(u.pathname)) hide(a);
      } catch (_) { }
    }
    // Never inspect message bodies: sponsored/suggestion filtering is restricted to the home feed.
    if (!isFeed()) return;
    for (const article of document.querySelectorAll('article')) {
      let sponsored = false;
      // Restrict label matching to header-like elements; don't hide posts for captions discussing ads.
      for (const el of article.querySelectorAll('header span, header a, [role="button"] span, a[href*="/ads/"]')) {
        if (labels.has((el.textContent || '').trim().toLowerCase()) || (el.getAttribute('href') || '').includes('/ads/')) {
          sponsored = true; break;
        }
      }
      if (sponsored && article.dataset.quietHidden !== 'true') { hide(article); hiddenAds++; }
      if (article.dataset.quietHidden === 'true') continue;
      const bounds = article.getBoundingClientRect();
      if (bounds.bottom <= 80 || bounds.top >= innerHeight * 0.8 || bounds.width <= 0) continue;
      const link = article.querySelector('a[href^="/p/"],a[href*="instagram.com/p/"]');
      if (link) {
        try {
          const match = new URL(link.href, location.href).pathname.match(/^\/p\/([^/]+)/);
          if (match) seen.add(match[1]);
        } catch (_) { }
      }
    }
  }
  function queueScan() { if (!scheduled) { scheduled = true; requestAnimationFrame(scan); } }
  new MutationObserver(queueScan).observe(document.documentElement, {subtree:true, childList:true, attributes:true, attributeFilter:['href']});
  addEventListener('scroll', queueScan, {passive:true});
  addEventListener('popstate', queueScan);
  document.addEventListener('click', e => {
    const a = e.target.closest && e.target.closest('a[href]');
    if (!a) return;
    try {
      const u = new URL(a.href, location.href);
      if (/(^|\.)instagram\.com$/i.test(u.hostname) && blockedPath(u.pathname)) {
        e.preventDefault(); e.stopImmediatePropagation();
      }
    } catch (_) { }
  }, true);
  window.__ataraxiaStillness = {
    scan,
    snapshot: () => ({ ids: isFeed() ? Array.from(seen).slice(0,500) : [], hiddenAds, feed:isFeed() })
  };
  scan();
})();
