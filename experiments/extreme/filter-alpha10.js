/* Local feed rules. No network calls, credentials, captions or messages are collected. */
(function () {
  'use strict';
  if (window.top !== window || !/^https:\/\/(www\.)?instagram\.com(?::443)?\//i.test(location.href)) return;
  if (window.__ataraxiaStillness) {
    if (window.__ataraxiaConfig) window.__ataraxiaStillness.configure(window.__ataraxiaConfig);
    return;
  }
  const VERSION = 'alpha10', SEMANTIC = 'article,[role="article"]';
  const seen = new Set(), aliases = new Map(), cards = new Map();
  const dirty = new Set(), roots = new Set(), hiddenOwners = new Map(), ports = new Map();
  const nonce = Math.random().toString(36).slice(2,12);
  let serial = 0, hiddenAds = 0, limit = 500, focused = false;
  let frame = 0, full = true, layoutDirty = true, viewportHeight = innerHeight;
  let measuredY = 0, measuredX = 0, wasFeed = false;
  const isFeed = () => location.pathname === '/';
  const normalise = text => (text || '').replace(/[\u200b-\u200f\u202a-\u202e\u2060-\u206f]/g,'').replace(/\s+/g,' ').trim().toLowerCase();
  const labels = new Set(['ad','advertisement','sponsored','geborg','geborgde','suggested for you','voorgestel vir jou','follow']);
  const blockedPath = p => /^\/(reels?|explore|tv)(\/|$)/i.test(p.replace(/\/+/g,'/'));
  const instagram = u => u.protocol === 'https:' && /^(www\.)?instagram\.com$/i.test(u.hostname) && (!u.port || u.port === '443');
  const flag = (el,name,on) => { if (el.hasAttribute(name) !== on) el.toggleAttribute(name,on); };
  function updateCap() {
    flag(document.documentElement,'data-quiet-capped',isFeed() && seen.size >= limit);
    flag(document.documentElement,'data-quiet-focused',isFeed() && focused);
  }
  const style = document.createElement('style');
  style.textContent = '[data-quiet-hidden="true"],[data-quiet-home-hidden="true"]{display:none!important}' +
    'html[data-quiet-focused] body,html[data-quiet-capped] body{visibility:hidden!important;pointer-events:none!important}' +
    'html[data-quiet-focused]::after,html[data-quiet-capped]::after{position:fixed;inset:0;z-index:2147483647;background:#101916;color:#edf5ee;padding:48px 24px;font:18px sans-serif}' +
    'html[data-quiet-focused]::after{content:"Focused mode. Open Inbox or Home below."}' +
    'html[data-quiet-capped]::after{content:"Session post limit reached. Use Inbox below."}';
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
    for (const [child,record] of cards) if (el.contains(child)) { restore(record); cards.delete(child); dirty.delete(record); resize.unobserve(child); }
    const record = {el,id:null,canonical:null,mediaKey:null,hidden:false,hiddenNodes:new Set(),box:null,prior:null};
    cards.set(el,record); dirty.add(record); resize.observe(el); layoutDirty = true;
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
  function hash(value) {
    // Opaque local identity: never return or persist a URL or signed query.
    let a = 2166136261, b = 5381;
    for (let i=0;i<value.length;i++) { a = Math.imul(a ^ value.charCodeAt(i),16777619); b = Math.imul(b,33) ^ value.charCodeAt(i); }
    return 'local_' + (a>>>0).toString(36) + '_' + (b>>>0).toString(36);
  }
  function identify(record,media) {
    const links = Array.from(record.el.querySelectorAll('a[href]'));
    const dataId = record.el.getAttribute('data-media-id') || record.el.getAttribute('data-post-id');
    const canonical = links.map(permalink).find(Boolean) || (dataId && /^[A-Za-z0-9_-]{1,70}$/.test(dataId) ? 'media_'+dataId : null);
    const authorLink = links.find(a => /^\/[A-Za-z0-9_.]+\/$/.test(a.getAttribute('href') || ''));
    const author = authorLink ? authorLink.getAttribute('href') : '';
    const time = record.el.querySelector('time[datetime]');
    const stamp = time ? time.getAttribute('datetime') : '';
    const recycled = record.author && author && record.author !== author || record.stamp && stamp && record.stamp !== stamp;
    // Loading skeletons are not posts. Count after media or a permalink arrives.
    if (!canonical && !media) { record.id=null; record.canonical=null; return; }
    let mediaKey = null;
    if (media) {
      const raw = media.getAttribute('poster') || media.getAttribute('src');
      if (raw && !raw.startsWith('data:') && !raw.startsWith('blob:')) {
        try {
          const u = new URL(raw,location.href);
          mediaKey = hash(u.pathname + '|' + author);
        } catch (_) { }
      }
    }
    let id = canonical && aliases.get(canonical);
    if (!id && canonical && seen.has(canonical)) id = canonical;
    if (!id && mediaKey && seen.has(mediaKey)) id = mediaKey;
    // Permalink hydration and carousel changes must not count the card twice.
    if (!id && !recycled && record.id && (!canonical || !record.canonical || canonical === record.canonical)) id = record.id;
    if (!id) id = canonical || mediaKey || 'local_' + nonce + '_' + (++serial);
    record.id = id; record.canonical = canonical; record.mediaKey = mediaKey;
    record.author = author; record.stamp = stamp;
    if (canonical) aliases.set(canonical,id);
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
    if (hidden !== record.hidden || next.size !== record.hiddenNodes.size || [...next].some(el => !record.hiddenNodes.has(el))) layoutDirty = true;
    record.hidden = hidden; record.hiddenNodes = next;
    // NO scroll-position writes. Chromium anchors surviving content; a manual
    // correction double-adjusts the position and cancels an in-flight fling.
  }

  function chainFor(el) {
    const chain = [];
    for (let p=el.parentElement; p && p!==document.body && p!==document.documentElement; p=p.parentElement) {
      if (/auto|scroll|overlay|hidden|clip/.test(getComputedStyle(p).overflowY)) {
        if (!ports.has(p)) ports.set(p,{el:p,y:p.scrollTop,x:p.scrollLeft,rect:p.getBoundingClientRect(),chain:null});
        chain.push(ports.get(p));
      }
    }
    return chain;
  }
  function measure() {
    const oldPorts = Array.from(ports.keys());
    ports.clear(); measuredY = window.scrollY; measuredX = window.scrollX; viewportHeight = innerHeight;
    for (const record of cards.values()) {
      record.prior = null;
      record.box = record.hidden ? null : {rect:record.el.getBoundingClientRect(),chain:chainFor(record.el)};
    }
    for (const port of ports.values()) { port.chain = chainFor(port.el); resize.observe(port.el); }
    for (const el of oldPorts) if (!ports.has(el)) resize.unobserve(el);
    layoutDirty = false;
  }
  function countPosts() {
    if (!isFeed() || focused || seen.size>=limit) { updateCap(); return; }
    const dy = window.scrollY-measuredY, dx = window.scrollX-measuredX;
    const deltas = new Map();
    for (const port of ports.values()) deltas.set(port,{y:port.el.scrollTop-port.y,x:port.el.scrollLeft-port.x});
    function translated(rect,chain) {
      let y=dy,x=dx;
      for (const port of chain) { y+=deltas.get(port).y; x+=deltas.get(port).x; }
      return {top:rect.top-y,bottom:rect.bottom-y,left:rect.left-x,right:rect.right-x};
    }
    const clips = new Map();
    for (const port of ports.values()) clips.set(port,translated(port.rect,port.chain));
    for (const record of cards.values()) {
      if (!record.box || !record.id || record.hidden || seen.has(record.id)) continue;
      const {rect,chain} = record.box;
      if (rect.width<=0 || rect.height<=0) continue;
      const bounds = translated(rect,chain);
      let top=0,bottom=viewportHeight,left=0,right=innerWidth;
      for (const port of chain) { const clip=clips.get(port);top=Math.max(top,clip.top);bottom=Math.min(bottom,clip.bottom);left=Math.max(left,clip.left);right=Math.min(right,clip.right); }
      const prior = record.prior;
      record.prior = {...bounds,viewTop:top,viewBottom:bottom};
      if (bottom<=top || right<=left || bounds.right<=left || bounds.left>=right) continue;
      const visible = bounds.bottom>top && bounds.top<bottom;
      const crossed = prior && prior.viewBottom>prior.viewTop &&
        ((prior.top>=prior.viewBottom && bounds.bottom<=top) || (prior.bottom<=prior.viewTop && bounds.top>=bottom));
      if ((visible || crossed) && seen.size<limit) seen.add(record.id);
    }
    updateCap();
  }
  const resize = new ResizeObserver(() => { if (isFeed()) { layoutDirty=true; schedule(); } });
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
      if (record) dirty.add(record);
      else if (change.type === 'attributes' && ['href','src','poster','role','data-media-id','data-post-id'].includes(change.attributeName)) roots.add(el);
      // Restore a formerly exclusive wrapper if another card is inserted in it.
      for (let p=el;p;p=p.parentElement) if (hiddenOwners.has(p)) dirty.add(hiddenOwners.get(p));
      for (const node of change.addedNodes || []) if (node.nodeType===1) roots.add(node);
      layoutDirty = true;
    }
    schedule();
  }
  const observer = new MutationObserver(mutations);
  observer.observe(document.documentElement,{subtree:true,childList:true,characterData:true,attributes:true,
    attributeFilter:['href','aria-label','src','poster','width','height','style','class','hidden','role','datetime','data-media-id','data-post-id']});
  function flush() {
    if (frame) { cancelAnimationFrame(frame); frame=0; }
    const pending = observer.takeRecords();
    if (pending.length) mutations(pending);
    if (frame) { cancelAnimationFrame(frame); frame=0; }
    // Count the fling against the old layout first. Reflow isn't a scroll:
    // after filtering/recycling, start fresh bounds without sweeping the shift.
    countPosts();
    if (full) { discover(document); for (const record of cards.values()) dirty.add(record); }
    else for (const root of roots) if (root.isConnected && !Array.from(roots).some(other => other!==root && other.contains(root))) discover(root);
    roots.clear(); full=false;
    if (isFeed()) {
      for (const [el,record] of cards) if (!el.isConnected) { restore(record); resize.unobserve(el); cards.delete(el); dirty.delete(record); layoutDirty=true; }
      // Read/classify metadata first, then batch all layout writes.
      const decisions = [];
      for (const record of dirty) {
        const media = mainMedia(record.el);
        identify(record,media);
        const hidden = sponsored(record,media);
        decisions.push({record,hidden,root:hidden?collapseRoot(record):null});
      }
      for (const decision of decisions) applyHidden(decision.record,decision.hidden,decision.root);
      if (layoutDirty) measure();
      countPosts();
    }
    dirty.clear(); updateCap();
  }
  function schedule() { if (!frame) frame=requestAnimationFrame(flush); }
  function routeChanged() {
    if (wasFeed !== isFeed()) { for (const record of cards.values()) record.prior=null; layoutDirty=true; }
    wasFeed=isFeed(); full=true; updateCap(); schedule();
  }
  addEventListener('scroll',schedule,{passive:true,capture:true});
  addEventListener('resize',() => {layoutDirty=true;schedule();},{passive:true});
  function mediaReady(event) {
    if (!isFeed() || !event.target.matches || !event.target.matches('img,video')) return;
    const record=owner(event.target);
    if (record) dirty.add(record); else roots.add(event.target);
    layoutDirty=true;schedule();
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
    if (config.reset===true) { seen.clear(); aliases.clear(); for (const record of cards.values()) record.prior=null; }
    if (config.limit!==undefined) limit=Math.max(1,Math.min(500,Number(config.limit)||10));
    for (const id of (config.ids || [])) if (typeof id==='string' && /^[A-Za-z0-9_-]{1,80}$/.test(id) && seen.size<500) seen.add(id);
    full=true;layoutDirty=true;flush();
  }
  window.__ataraxiaStillness = {
    scan:() => {full=true;layoutDirty=true;flush();},
    configure,
    snapshot:() => {
      // Flush once before the native read, cancelling any queued duplicate frame.
      // Steady-state snapshots and scrolling use cached geometry, not DOM scans.
      flush();
      return {version:VERSION,ids:isFeed()?Array.from(seen).slice(0,500):[],hiddenAds,feed:isFeed(),
        containers:isFeed()?cards.size:0,linkless:isFeed()?Array.from(cards.values()).filter(r=>!r.canonical && !r.hidden).length:0};
    }
  };
  wasFeed=isFeed();resize.observe(document.documentElement);
  if (document.body) resize.observe(document.body);
  // Apply native settings BEFORE the first count, never afterwards.
  configure(window.__ataraxiaConfig || {});
})();
