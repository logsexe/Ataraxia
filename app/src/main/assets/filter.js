/* Local feed rules. No network calls, credentials, captions or messages are collected. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) {
    if (window.__ataraxiaConfig) window.__ataraxiaStillness.configure(window.__ataraxiaConfig);
    return;
  }
  const VERSION = 'alpha11', SEMANTIC = 'article,[role="article"]';
  const cards = new Map(), dirty = new Set(), roots = new Set(), hiddenOwners = new Map();
  const pointers = new Set();
  const SETTLE_MS = 160;
  let hiddenAds = 0, focused = false, frame = 0, full = true, settleTimer = 0;
  let lastMotion = -Infinity;
  const isFeed = () => location.pathname === '/';
  const normalise = text => (text || '').replace(/[\u200b-\u200f\u202a-\u202e\u2060-\u206f]/g,'').replace(/\s+/g,' ').trim().toLowerCase();
  const labels = new Set(['ad','advertisement','sponsored','geborg','geborgde','suggested for you','voorgestel vir jou','follow']);
  const blockedPath = p => /^\/(reels?|explore|tv)(\/|$)/i.test(p.replace(/\/+/g,'/'));
  const instagram = u => u.protocol === 'https:' && /^(www\.)?instagram\.com$/i.test(u.hostname) && (!u.port || u.port === '443');
  const flag = (el,name,on) => { if (el.hasAttribute(name) !== on) el.toggleAttribute(name,on); };
  function updateMode() {
    flag(document.documentElement,'data-quiet-focused',isFeed() && focused);
  }
  const style = document.createElement('style');
  style.textContent = '[data-quiet-hidden="true"],[data-quiet-home-hidden="true"]{display:none!important}' +
    'html[data-quiet-focused] body{visibility:hidden!important;pointer-events:none!important}' +
    'html[data-quiet-focused]::after{content:"Focused mode. Open Inbox or Home below.";position:fixed;inset:0;z-index:2147483647;background:#101916;color:#edf5ee;padding:48px 24px;font:18px sans-serif}';
  (document.head || document.documentElement).appendChild(style);

  function elements(root,selector) {
    const result = root.nodeType === 1 && root.matches(selector) ? [root] : [];
    if (root.querySelectorAll) result.push(...root.querySelectorAll(selector));
    return result;
  }
  function permalink(a) {
    try {
      const u = new URL(a.getAttribute('href'),location.href);
      if (!instagram(u)) return null;
      const m = u.pathname.match(/^\/(?:[A-Za-z0-9_.]+\/)?(?:p|reel)\/([A-Za-z0-9_-]{1,80})(?:\/|$)/);
      return m ? m[1] : null;
    } catch (_) { return null; }
  }
  function owner(node) {
    for (let el = node; el; el = el.parentElement) if (cards.has(el)) return cards.get(el);
    return null;
  }
  function mainMedia(el) {
    return Array.from(el.querySelectorAll('video,img')).find(media => {
      if (media.closest('header') || /profile|avatar/i.test(media.getAttribute('alt') || '')) return false;
      if (media.tagName === 'VIDEO') return true;
      const width = Number(media.getAttribute('width')) || parseFloat(getComputedStyle(media).width);
      return width > 100;
    });
  }
  function register(el) {
    if (owner(el)) return;
    // Semantic cards take precedence over previously inferred inner cards.
    for (const [child,record] of cards) if (el.contains(child)) { restore(record); cards.delete(child); dirty.delete(record); }
    const record = {el,media:null,hidden:false,hiddenNodes:new Set()};
    cards.set(el,record); dirty.add(record);
  }
  function inferCard(node,needsHeader) {
    for (let el = node.parentElement; el && !el.matches('main,body,html,nav,[role="feed"],[role="main"]'); el = el.parentElement) {
      if (owner(el)) return;
      if (!mainMedia(el)) continue;
      const links = Array.from(el.querySelectorAll('a[href]'));
      const ids = new Set(links.map(permalink).filter(Boolean));
      if (ids.size > 1 || el.querySelector(SEMANTIC)) return;
      if (needsHeader && !el.querySelector('header')) continue;
      if (ids.size === 1 || (needsHeader && links.some(a => /^\/[A-Za-z0-9_.]+\/$/.test(a.getAttribute('href') || '')))) {
        register(el); return;
      }
    }
  }
  function discover(root) {
    // Never enumerate or inspect message bodies, including on native refresh.
    if (isFeed()) {
      for (const el of elements(root,SEMANTIC)) register(el);
      for (const a of elements(root,'a[href]')) if (permalink(a) && !owner(a)) inferCard(a,false);
      for (const media of elements(root,'img,video')) if (!owner(media)) inferCard(media,true);
    }
    const linkRoots = isFeed() ? [root] : elements(root,'nav,[role="navigation"]');
    for (const linkRoot of linkRoots) for (const a of elements(linkRoot,'a[href]')) {
      try {
        const u = new URL(a.getAttribute('href'),location.href);
        if (!instagram(u)) continue;
        // A Reel permalink can wrap real feed media. Preserve it; block its click.
        const hide = blockedPath(u.pathname) && !owner(a);
        if (hide) { if (a.dataset.quietHidden !== 'true') a.dataset.quietHidden = 'true'; }
        else if (!hiddenOwners.has(a)) delete a.dataset.quietHidden;
        if (focused && u.pathname === '/') a.dataset.quietHomeHidden = 'true';
        else delete a.dataset.quietHomeHidden;
      } catch (_) { }
    }
  }
  function sponsored(record,media) {
    for (const el of record.el.querySelectorAll('header,span,div,button,[aria-label]')) {
      const header = el.closest('header');
      const beforeMedia = media && !el.contains(media) && Boolean(el.compareDocumentPosition(media) & Node.DOCUMENT_POSITION_FOLLOWING);
      if (!header && !beforeMedia) continue;
      if (!labels.has(normalise(el.textContent)) && !labels.has(normalise(el.getAttribute('aria-label')))) continue;
      // Collapsed ancestors have zero geometry: don't oscillate based on rects.
      if (el.hidden || el.getAttribute('aria-hidden') === 'true' || getComputedStyle(el).display === 'none') continue;
      return true;
    }
    return false;
  }
  function emptySpacer(el) {
    return !el.textContent.trim() && !el.matches('a,button,input,video,img,iframe,canvas,svg,article,[role]') &&
      !el.querySelector('a,button,input,video,img,iframe,canvas,svg,article,[role]');
  }
  function collapseRoot(record) {
    let root = record.el;
    for (let parent=root.parentElement; parent; parent=parent.parentElement) {
      if (!parent.matches('div,section,li') || parent.matches('[role]:not([role="listitem"])') || parent.id === 'root') break;
      if (/auto|scroll|overlay/.test(getComputedStyle(parent).overflowY)) break;
      if (Array.from(parent.childNodes).some(n => n.nodeType === 3 && n.textContent.trim())) break;
      if (Array.from(parent.children).some(sibling => sibling !== root && !emptySpacer(sibling))) break;
      root = parent;
    }
    return root;
  }
  function restore(record) {
    for (const el of record.hiddenNodes) {
      if (hiddenOwners.get(el) === record) { delete el.dataset.quietHidden; hiddenOwners.delete(el); }
    }
    record.hiddenNodes.clear();
  }
  function applyHidden(record,hidden,root) {
    const next = hidden ? new Set([record.el,root]) : new Set();
    for (const el of record.hiddenNodes) if (!next.has(el)) { delete el.dataset.quietHidden; hiddenOwners.delete(el); }
    for (const el of next) { if (el.dataset.quietHidden !== 'true') el.dataset.quietHidden = 'true'; hiddenOwners.set(el,record); }
    if (hidden && !record.hidden) hiddenAds++;
    record.hidden = hidden; record.hiddenNodes = next;
    // NO scroll-position writes. Chromium anchors surviving content; a manual
    // correction double-adjusts the position and cancels an in-flight fling.
  }

  function hasWork() { return full || roots.size > 0 || dirty.size > 0; }
  function moving() { return pointers.size > 0 || performance.now()-lastMotion < SETTLE_MS; }
  function mutations(changes) {
    if (!isFeed()) {
      for (const change of changes) {
        const el = change.target.nodeType===1 ? change.target : change.target.parentElement;
        const nav = el && el.closest('nav,[role="navigation"]');
        if (nav) roots.add(nav);
      }
      if (roots.size) schedule();
      return;
    }
    for (const change of changes) {
      const el = change.target.nodeType===1 ? change.target : change.target.parentElement;
      if (!el) continue;
      const record = owner(el);
      if (record) {
        const media=record.media;
        // Caption edits and animated controls after the media cannot change an
        // ad header. Ignore them instead of rescanning a whole post.
        if (!media || !media.isConnected || el===record.el || el.contains(media) || el.closest('header') ||
            (el.compareDocumentPosition(media) & Node.DOCUMENT_POSITION_FOLLOWING)) dirty.add(record);
      }
      else if (change.type === 'attributes' && ['href','role'].includes(change.attributeName)) roots.add(el);
      // Restore a formerly exclusive wrapper if another card is inserted in it.
      for (let p=el;p;p=p.parentElement) if (hiddenOwners.has(p)) dirty.add(hiddenOwners.get(p));
      if (!record) for (const node of change.addedNodes || []) if (node.nodeType===1) roots.add(node);
    }
    if (hasWork()) schedule();
  }
  const observer = new MutationObserver(mutations);
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,
    attributeFilter:['href','aria-label','hidden','role']});
  function flush() {
    if (frame) { cancelAnimationFrame(frame); frame=0; }
    const pending = observer.takeRecords();
    if (pending.length) mutations(pending);
    if (frame) { cancelAnimationFrame(frame); frame=0; }
    if (moving()) { schedule(); return; }
    if (full) { discover(document); for (const record of cards.values()) dirty.add(record); }
    else for (const root of roots) if (root.isConnected && !Array.from(roots).some(other => other!==root && other.contains(root))) discover(root);
    roots.clear(); full=false;
    if (isFeed()) {
      for (const [el,record] of cards) if (!el.isConnected) { restore(record); cards.delete(el); dirty.delete(record); }
      // Read/classify metadata first, then batch all layout writes.
      const decisions = [];
      for (const record of dirty) {
        // Percentage widths stop resolving to pixels under display:none. Keep
        // the known media boundary while collapsed, until that node is replaced.
        const media = record.hidden && record.media && record.el.contains(record.media)
          ? record.media : mainMedia(record.el);
        record.media=media;
        const hidden = sponsored(record,media);
        decisions.push({record,hidden,root:hidden?collapseRoot(record):null});
      }
      for (const decision of decisions) applyHidden(decision.record,decision.hidden,decision.root);
    }
    dirty.clear(); updateMode();
  }
  function schedule() {
    if (!hasWork()) return;
    clearTimeout(settleTimer);
    if (moving()) {
      if (frame) { cancelAnimationFrame(frame); frame=0; }
      if (!pointers.size) settleTimer=setTimeout(schedule,Math.max(16,SETTLE_MS-(performance.now()-lastMotion)));
      return;
    }
    if (!frame) frame=requestAnimationFrame(flush);
  }
  function motion() {
    // No DOM reads, layout writes, geometry sampling or counting on scroll.
    lastMotion=performance.now();
    if (frame) { cancelAnimationFrame(frame); frame=0; }
    schedule();
  }
  function releasePointer(event) { pointers.delete(event.pointerId); motion(); }
  addEventListener('scroll',motion,{passive:true,capture:true});
  addEventListener('pointerdown',event => { pointers.add(event.pointerId); motion(); },{passive:true,capture:true});
  addEventListener('pointerup',releasePointer,{passive:true,capture:true});
  addEventListener('pointercancel',releasePointer,{passive:true,capture:true});
  addEventListener('blur',() => {pointers.clear();motion();});
  function routeChanged() { full=true;updateMode();schedule(); }
  function mediaReady(event) {
    if (!isFeed() || !event.target.matches || !event.target.matches('img,video')) return;
    const record=owner(event.target);
    if (record) dirty.add(record); else roots.add(event.target);
    schedule();
  }
  addEventListener('load',mediaReady,{passive:true,capture:true});
  addEventListener('loadedmetadata',mediaReady,{passive:true,capture:true});
  addEventListener('popstate',routeChanged);
  for (const method of ['pushState','replaceState']) {
    const original=history[method];
    history[method]=function(...args) { const result=original.apply(this,args);routeChanged();return result; };
  }
  document.addEventListener('click',e => {
    const a=e.target.closest && e.target.closest('a[href]');
    if (!a) return;
    try {const u=new URL(a.href,location.href);if (instagram(u) && (blockedPath(u.pathname) || (focused && u.pathname==='/'))) {e.preventDefault();e.stopImmediatePropagation();}} catch (_) { }
  },true);
  function configure(config) {
    if (typeof config.focused==='boolean') focused=config.focused;
    updateMode(); full=true;flush();
  }
  window.__ataraxiaStillness = {
    scan:() => {full=true;flush();},
    configure,
    status:() => {
      // On-demand diagnostics only; never force a layout update mid-fling.
      if (hasWork()) flush();
      return {version:VERSION,hiddenAds,feed:isFeed(),containers:isFeed()?cards.size:0,pending:hasWork()};
    }
  };
  configure(window.__ataraxiaConfig || {});
})();
