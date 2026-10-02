const { chromium } = require('playwright');
const fs = require('fs');
const assert = require('node:assert/strict');
const path = require('path');
const script = fs.readFileSync(path.join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.ATARAXIA_TEST_BROWSER || undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:412,height:915}});
 const html=`<html><head><style>article {height:250px;width:380px;}body{margin:0}</style></head><body>
 <nav><a id="reels" href="/reels/">Reels</a><a id="explore" href="/explore/">Explore</a><a id="shop" href="/shop/">Shop</a><a id="live" href="/maya/live/">Live</a><a id="friend" href="/maya/">Maya</a><a id="inbox" href="/direct/inbox/">Inbox</a></nav>
 <article id="ad"><header><span>Sponsored</span></header><a href="/p/Ad1/">Ad</a></article>
 <article id="first"><header><span>Friend</span></header><a href="/p/First1/">Photo</a><p>I dislike sponsored posts</p></article>
 <article id="second"><header><span>Friend 2</span></header><a href="/p/Second2/">Photo 2</a></article>
 </body></html>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#reels').isVisible(),false);
 assert.equal(await page.locator('#explore').isVisible(),false);
 assert.equal(await page.locator('#shop').isVisible(),false);
 assert.equal(await page.locator('#live').isVisible(),false);
 assert.equal(await page.locator('#friend').isVisible(),true);
 assert.equal(await page.locator('#inbox').isVisible(),true);
 assert.equal(await page.locator('#ad').isVisible(),true);
 assert.equal(await page.locator('#first').isVisible(),true);
 let state=await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
 assert.deepEqual(state.ids,[]);
 assert.equal(state.hiddenAds,0);
 await page.evaluate(script);state=await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
 assert.equal(state.hiddenAds,0); // idempotent; no new observers/state resets
 await page.evaluate(()=>{
   const post=document.createElement('article'); post.id='reelpost';
   post.innerHTML='<a id="reelmedia" href="/reel/xyz/">Clip</a>';
   document.body.append(post);
   const a=document.createElement('a'); a.id='dynamic'; a.href='/explore/'; a.textContent='Explore';
   document.body.append(a);
 });
 await page.waitForFunction(()=>document.querySelector('#dynamic').dataset.quietHidden==='true');
 assert.equal(await page.locator('#reelpost').isVisible(),true);
 assert.equal(await page.locator('#reelmedia').isVisible(),true);
 await page.evaluate(()=>{history.pushState({},'', '/direct/inbox/'); document.querySelector('#first header span').textContent='Sponsored'; window.__ataraxiaStillness.scan();});
 assert.equal(await page.locator('#first').isVisible(),true); // do not inspect/filter messages
 assert.deepEqual(await page.evaluate(()=>window.__ataraxiaStillness.snapshot().ids),[]);
 await page.evaluate(()=>{history.pushState({},'', '/'); document.querySelector('#first header span').textContent='Geborg'; window.__ataraxiaStillness.scan();});
 assert.equal(await page.locator('#first').isVisible(),true);
 const fastHtml = '<style>body{margin:0}article{height:600px;width:380px}</style>' +
   Array.from({length:30},(_,i)=>`<article><header><a href="/friend${i}/">Friend</a></header><a href="/reel/Clip${i}/">Post</a></article>`).join('');
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:fastHtml}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 await page.evaluate(()=>{window.scrollTo(0,6000);window.dispatchEvent(new Event('scroll'));});
 assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),null);
 assert.equal(await page.locator('article').first().isVisible(),true);
 assert.equal(await page.locator('a[href="/reel/Clip0/"]').isVisible(),true);
 await page.evaluate(()=>window.__ataraxiaStillness.configure({rooms:['friends'],people:{friend1:'celebrity'}}));
 assert.equal(await page.locator('article').nth(1).isVisible(),false);
 assert.equal(await page.locator('article').first().isVisible(),true);
 // Accessible metadata outside a header, split labels, and caption preservation.
 const adsHtml = `<article id="modern" role="article"><span aria-label="Sponsored">Sponsor</span><img alt="Photo" width="300" height="200"><a href="/p/Modern/">Photo</a></article>
 <article id="split"><span><span>Spon</span><span>sored</span></span><img alt="Photo" width="300" height="200"><a href="/p/Split/">Photo</a></article>
 <article id="caption"><img alt="Photo" width="300" height="200"><span>Sponsored</span><a href="/p/Caption/">Photo</a></article>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:adsHtml}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#modern').isVisible(),true);
 assert.equal(await page.locator('#split').isVisible(),true);
 assert.equal(await page.locator('#caption').isVisible(),true);
 // Whole-script guard: never operate on look-alike domains.
 await page.route('https://instagram.com.evil.test/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://instagram.com.evil.test/'); await page.evaluate(script);
 assert.equal(await page.evaluate(()=>typeof window.__ataraxiaStillness),'undefined');
 console.log('PASS: browser fixtures — nav hiding, reel posts stay, ads left in place, groups, inbox isolation, origin guard');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
