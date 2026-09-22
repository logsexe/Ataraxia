// Real browser regressions for the reported gaps, backwards jumps and zero counter.
// FILTER_SCRIPT can point at the previous build to verify these fail before the fix.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const script = fs.readFileSync(process.env.FILTER_SCRIPT || path.join(__dirname, '../app/src/main/assets/filter.js'), 'utf8');
const card = (id, label = 'Friend') => `<article><header>${label}</header><img width="350" height="400" alt="Photo"><a href="/p/${id}/">Post</a></article>`;
const base = '<style>body{margin:0}article{height:600px;width:380px;box-sizing:border-box}img{display:block}#feed{overflow:auto;height:500px}</style>';
(async () => {
  const browser = await chromium.launch({headless:true, executablePath:process.env.ATARAXIA_TEST_BROWSER || undefined, args:['--no-sandbox']});
  const failures = [];
  async function check(name, html, verify) {
    const page = await browser.newPage({viewport:{width:412,height:915}});
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    await page.route('https://www.instagram.com/**', r => r.fulfill({contentType:'text/html',body:base + html}));
    try {
      await page.goto('https://www.instagram.com/');
      await page.evaluate(script);
      await verify(page);
      assert.deepEqual(errors, []);
      console.log('PASS:', name);
    } catch (e) { failures.push(name); console.error('FAIL:', name, e.message); }
    finally { await page.close(); }
  }
  await check('collapse the whole padded item, not just its article',
    `<main style="display:flex;flex-direction:column;gap:24px"><div id="ad-slot" style="overflow:hidden;min-height:680px;padding:20px;margin-bottom:30px">${card('Ad','Sponsored')}<div style="height:40px"></div></div><div id="organic-slot">${card('Organic')}</div></main>`,
    async page => {
      assert.equal(await page.locator('#ad-slot').evaluate(el => el.getBoundingClientRect().height), 0);
      assert.equal(await page.locator('#organic-slot').evaluate(el => el.getBoundingClientRect().top), 0);
    });
  for (const nested of [false,true]) {
    await check(`late ad above viewport preserves anchor without writing ${nested?'nested':'window'} scroll position`,
      `${nested?'<div id="feed">':'<main>'}${Array.from({length:20},(_,i)=>`<div style="height:600px">${card('Anchor'+i)}</div>`).join('')}${nested?'</div>':'</main>'}`,
      async page => {
        await page.evaluate(nested => {const port=nested?document.querySelector('#feed'):window;port.scrollTo(0,4200);}, nested);
        await page.waitForTimeout(100);
        const before = await page.locator('article').nth(7).evaluate(el => el.getBoundingClientRect().top);
        await page.evaluate(() => {
          window.scrollWrites = 0;
          window.scrollTo = () => {window.scrollWrites++;};
          const descriptor = Object.getOwnPropertyDescriptor(Element.prototype, 'scrollTop');
          Object.defineProperty(Element.prototype,'scrollTop',{...descriptor,set(value){window.scrollWrites++;descriptor.set.call(this,value);}});
          document.querySelector('article header').textContent = 'Sponsored';
        });
        await page.waitForTimeout(150);
        assert.equal(await page.evaluate(() => window.scrollWrites), 0, 'filter must not fight native anchoring or cancel a fling');
        const after = await page.locator('article').nth(7).evaluate(el => el.getBoundingClientRect().top);
        assert.ok(Math.abs(after-before)<=1, `anchor moved ${after-before}px`);
      });
  }
  await check('username permalinks and absolute reel links count',
    `<article><header>Friend</header><a href="/friend/p/Modern123/">Post</a></article><article><header>Friend</header><a href="https://www.instagram.com/reel/Reel123/">Video</a></article>`,
    async page => assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Modern123','Reel123']));
  await check('linkless semantic posts count once, including after permalink hydration',
    `<article id="hydrating"><header>Friend</header><img src="https://www.instagram.com/media/photo.jpg?token=secret" width="350" height="400"></article>`,
    async page => {
      const initial = await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
      assert.equal(initial.ids.length,1);
      assert.match(initial.ids[0],/^[A-Za-z0-9_-]{1,80}$/);
      await page.evaluate(()=>document.querySelector('article').insertAdjacentHTML('beforeend','<a href="/friend/p/Hydrated/">Post</a>'));
      await page.waitForTimeout(50);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,1);
    });
  await check('non-semantic feed cards are discovered, not just article tags',
    `<main><div id="modern-card"><div><header><a href="/friend/">Friend</a></header><img width="350" height="400"><a href="/friend/p/DivPost/">Post</a></div></div></main>`,
    async page => assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['DivPost']));
  await check('blocking Reel navigation does not hide an organic post’s media',
    `<article><header>Friend</header><a id="media" href="/reel/OrganicReel/"><img width="350" height="400"></a></article>`,
    async page => assert.equal(await page.locator('#media').isVisible(),true));
  await check('actual scroll frames and native snapshots do no DOM scans or geometry reads',
    Array.from({length:60},(_,i)=>card('Fast'+i)).join(''), async page => {
      await page.waitForTimeout(100); // settle initial layout and ResizeObserver
      const result = await page.evaluate(async () => {
        const qsa = Element.prototype.querySelectorAll, qs = Element.prototype.querySelector;
        const rect = Element.prototype.getBoundingClientRect, dqs = document.querySelectorAll;
        let scans=0, geometry=0;
        Element.prototype.querySelectorAll=function(...args){scans++;return qsa.apply(this,args);};
        Element.prototype.querySelector=function(...args){scans++;return qs.apply(this,args);};
        document.querySelectorAll=function(...args){scans++;return dqs.apply(this,args);};
        Element.prototype.getBoundingClientRect=function(...args){geometry++;return rect.apply(this,args);};
        window.scrollTo(0,6000);
        for(let i=0;i<100;i++) window.dispatchEvent(new Event('scroll'));
        await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
        const state=window.__ataraxiaStillness.snapshot();
        Element.prototype.querySelectorAll=qsa;Element.prototype.querySelector=qs;
        Element.prototype.getBoundingClientRect=rect;document.querySelectorAll=dqs;
        return {scans,geometry,ids:state.ids};
      });
      assert.equal(result.scans,0);assert.equal(result.geometry,0);
      assert.deepEqual(result.ids,Array.from({length:12},(_,i)=>'Fast'+i));
    });
  await check('recycled cards change identity and restore filtered wrappers',
    `<main><div id="slot">${card('RecycledAd','Sponsored')}</div></main>`, async page => {
      assert.equal(await page.locator('#slot').isVisible(),false);
      await page.evaluate(()=>{document.querySelector('article header').textContent='Friend';document.querySelector('article a').href='/friend/p/Replacement/';});
      await page.waitForTimeout(50);
      assert.equal(await page.locator('#slot').isVisible(),true);
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Replacement']);
      await page.evaluate(()=>document.querySelector('article a').href='/friend/p/NextPost/');
      await page.waitForTimeout(50);
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Replacement','NextPost']);
    });
  await check('a filtered wrapper cannot hide a newly inserted organic sibling',
    `<main><div id="slot">${card('Ad','Sponsored')}</div></main>`, async page => {
      await page.evaluate(html=>document.querySelector('#slot').insertAdjacentHTML('beforeend',html),card('Sibling'));
      await page.waitForTimeout(50);
      assert.equal(await page.locator('#slot').isVisible(),true);
      assert.equal(await page.locator('article').first().isVisible(),false);
      assert.equal(await page.locator('article').nth(1).isVisible(),true);
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Sibling']);
    });
  await check('nested offscreen feeds do not count, and all clipping ancestors apply',
    `<div style="height:1100px"></div><div style="height:100px;overflow:hidden"><div id="feed">${Array.from({length:20},(_,i)=>card('Clipped'+i)).join('')}</div></div><div style="height:2000px"></div>`, async page => {
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,[]);
      await page.evaluate(()=>document.querySelector('#feed').scrollTop=6000);
      await page.waitForTimeout(50);
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,[]);
      await page.evaluate(()=>window.scrollTo(0,1100));
      await page.waitForTimeout(50);
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Clipped10']);
    });
  await check('skeleton hydration, carousel dedupe and linkless recycling',
    '<article id="skeleton"></article>',async page => {
      assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,[]);
      await page.evaluate(()=>document.querySelector('article').innerHTML='<header><a href="/alice/">Alice</a></header><img width="350" height="400" src="/media/one.jpg">');
      await page.waitForTimeout(50);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,1);
      await page.evaluate(()=>document.querySelector('img').src='/media/carousel-two.jpg');
      await page.waitForTimeout(50);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,1);
      await page.evaluate(()=>{document.querySelector('header a').href='/bob/';document.querySelector('img').src='/media/new-post.jpg';});
      await page.waitForTimeout(50);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,2);
    });
  await check('inbox mutations never inspect message metadata or media',card('BeforeInbox'),async page => {
    const result=await page.evaluate(async()=>{
      history.pushState({},'', '/direct/inbox/');
      const article=document.querySelector('article');
      article.querySelectorAll=()=>{throw Error('message body inspected');};
      article.querySelector=()=>{throw Error('message body inspected');};
      article.innerHTML='<header>Sponsored private message</header><img width="350">';
      await new Promise(r=>requestAnimationFrame(r));
      return window.__ataraxiaStillness.snapshot();
    });
    assert.equal(result.feed,false);assert.equal(result.containers,0);assert.deepEqual(result.ids,[]);
  });
  await check('configuration is applied before initial counting',card('ShouldNotCount'),async page=>{
    await page.goto('https://www.instagram.com/');
    await page.evaluate(()=>window.__ataraxiaConfig={focused:true,limit:1,ids:[]});
    await page.evaluate(script);
    assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,[]);
    await page.goto('https://www.instagram.com/');
    await page.evaluate(()=>window.__ataraxiaConfig={focused:false,limit:1,ids:['Saved']});
    await page.evaluate(script);
    assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Saved']);
  });
  await check('appending a post scans only new content, not the existing feed',
    `<main>${Array.from({length:30},(_,i)=>card('Existing'+i)).join('')}</main>`, async page=>{
      await page.waitForTimeout(100);
      await page.evaluate(html=>{
        for(const article of document.querySelectorAll('article')) article.querySelectorAll=()=>{throw Error('existing card rescanned during append');};
        document.querySelector('main').insertAdjacentHTML('beforeend',html);
      },card('NewCard'));
      await page.waitForTimeout(100);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).containers,31);
    });
  await browser.close();
  assert.deepEqual(failures,[], 'feed stability regressions');
})().catch(e=>{console.error(e);process.exitCode=1;});
