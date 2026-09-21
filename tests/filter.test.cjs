const { chromium } = require('playwright');
const fs = require('fs');
const assert = require('node:assert/strict');
const path = require('path');
const script = fs.readFileSync(path.join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.ATARAXIA_TEST_BROWSER || undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:412,height:915}});
 const errors=[]; page.on('pageerror',error=>errors.push(error.message));
 const html=`<html><head><style>article {height:250px;width:380px;}body{margin:0}</style></head><body>
 <nav><a id="reels" href="/reels/">Reels</a><a id="explore" href="/explore/">Explore</a><a id="inbox" href="/direct/inbox/">Inbox</a></nav>
 <article id="ad"><header><span>Sponsored</span></header><a href="/p/Ad1/">Ad</a></article>
 <article id="first"><header><span>Friend</span></header><a href="/p/First1/">Photo</a><p>I dislike sponsored posts</p></article>
 <article id="second"><header><span>Friend 2</span></header><a href="/p/Second2/">Photo 2</a></article>
 <article id="suggested"><header><button>Follow</button></header><a href="/p/Suggested3/">Suggested</a></article>
 </body></html>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#reels').isVisible(),false);
 assert.equal(await page.locator('#explore').isVisible(),false);
 assert.equal(await page.locator('#inbox').isVisible(),true);
 assert.equal(await page.locator('#ad').isVisible(),false);
 assert.equal(await page.locator('#first').isVisible(),true);
 assert.equal(await page.locator('#suggested').isVisible(),false);
 let state=await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
 assert.deepEqual(state.ids,['First1','Second2']);
 assert.equal(state.hiddenAds,1);
 await page.evaluate(script);state=await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
 assert.equal(state.hiddenAds,1); // idempotent; no new observers/state resets
 await page.evaluate(()=>{const a=document.createElement('a');a.id='dynamic';a.href='/reel/xyz/';a.textContent='Watch';document.body.append(a);});
 await page.waitForFunction(()=>document.querySelector('#dynamic').dataset.quietHidden==='true');
 await page.evaluate(()=>{history.pushState({},'', '/direct/inbox/'); document.querySelector('#first header span').textContent='Sponsored'; window.__ataraxiaStillness.scan();});
 assert.equal(await page.locator('#first').isVisible(),true); // do not inspect/filter messages
 assert.deepEqual(await page.evaluate(()=>window.__ataraxiaStillness.snapshot().ids),[]);
 await page.evaluate(()=>{history.pushState({},'', '/'); document.querySelector('#first header span').textContent='Geborg'; window.__ataraxiaStillness.scan();});
 assert.equal(await page.locator('#first').isVisible(),false);
 // Real-layout regression: posts skipped entirely between scroll events still count.
 const fastHtml = '<style>body{margin:0}article{height:600px;width:380px}</style>' +
   Array.from({length:30},(_,i)=>`<article><header>Friend</header><a href="/p/Fast${i}/">Post</a></article>`).join('');
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:fastHtml}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 await page.evaluate(()=>{window.scrollTo(0,6000);window.dispatchEvent(new Event('scroll'));});
 let fast = await page.evaluate(()=>window.__ataraxiaStillness.snapshot());
 assert.equal(fast.ids.length,12); // posts 0..11 intersect the swept viewport
 await page.evaluate(()=>{window.scrollTo(0,0);window.dispatchEvent(new Event('scroll'));});
 assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,12);
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 await page.evaluate(()=>{window.__ataraxiaStillness.configure({limit:5,ids:[]});window.scrollTo(0,6000);window.dispatchEvent(new Event('scroll'));});
 assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.length,5);
 assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),'');
 assert.equal(await page.locator('article').first().isVisible(),false);
 await page.evaluate(()=>{history.pushState({},'', '/direct/inbox/');window.__ataraxiaStillness.scan();});
 assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),null);
 // Accessible metadata outside a header, split labels, and caption preservation.
 const adsHtml = `<article id="modern" role="article"><span aria-label="Sponsored">Sponsor</span><img alt="Photo" width="300" height="200"><a href="/p/Modern/">Photo</a></article>
 <article id="split"><span><span>Spon</span><span>sored</span></span><img alt="Photo" width="300" height="200"><a href="/p/Split/">Photo</a></article>
 <article id="caption"><img alt="Photo" width="300" height="200"><span>Sponsored</span><a href="/p/Caption/">Photo</a></article>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:adsHtml}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#modern').isVisible(),false);
 assert.equal(await page.locator('#split').isVisible(),false);
 assert.equal(await page.locator('#caption').isVisible(),true);
 // A new native session can reset the cap without a full page reload.
 await page.evaluate(()=>window.__ataraxiaStillness.configure({limit:1,ids:['PriorSession']}));
 assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),'');
 await page.evaluate(()=>{
   history.pushState({},'', '/direct/inbox/');
   window.__ataraxiaStillness.configure({reset:true,limit:10,ids:[]});
 });
 assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),null);
 await page.evaluate(()=>{history.pushState({},'', '/');window.__ataraxiaStillness.scan();});
 assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.includes('PriorSession'),false);
 // Nested scrolling must count posts completely crossed in one jump.
 const nestedHtml='<style>body{margin:0}#feed{height:500px;overflow:auto}article{height:600px;width:380px}</style><div id="feed">'+
   Array.from({length:20},(_,i)=>`<article><header>Friend</header><a href="/p/Nested${i}/">Post</a></article>`).join('')+'</div>';
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:nestedHtml}));
 await page.goto('https://www.instagram.com/');await page.evaluate(script);
 assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,['Nested0']);
 await page.evaluate(()=>{const feed=document.querySelector('#feed');feed.scrollTop=6000;feed.dispatchEvent(new Event('scroll'));});
 assert.deepEqual((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids,Array.from({length:11},(_,i)=>'Nested'+i));
 // Screenshot-style metadata: small avatar before the Ad label, no semantic header.
 const screenshotHtml = `<article id="short-ad"><img alt="IG" style="width:40px;height:40px"><div><span>ig.australia</span><div>Ad</div></div><img alt="Trade shares" width="350" height="500"></article>
 <article id="organic"><img width="350" height="500"><span>Ad</span><a href="/p/Organic/">Post</a></article>
 <article id="late"><span id="late-label">Friend</span><img width="350" height="500"><a href="/p/Late/">Post</a></article>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:screenshotHtml}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#short-ad').isVisible(),false);
 assert.equal(await page.locator('#organic').isVisible(),true);
 await page.evaluate(()=>document.querySelector('#late-label').textContent='Ad');
 await page.waitForFunction(()=>document.querySelector('#late').dataset.quietHidden==='true');
 await page.evaluate(()=>document.querySelector('#late-label').textContent='Friend again');
 await page.waitForFunction(()=>document.querySelector('#late').dataset.quietHidden!=='true');
 // Scroll hot path must never enumerate labels or all document links.
 await page.evaluate(()=>{
   const original=Element.prototype.querySelectorAll;
   const originalDocument=document.querySelectorAll;
   Element.prototype.querySelectorAll=function(){throw Error('Element subtree scanned on scroll');};
   document.querySelectorAll=function(){throw Error('Document scanned on scroll');};
   try { for(let i=0;i<100;i++) window.dispatchEvent(new Event('scroll')); }
   finally {Element.prototype.querySelectorAll=original;document.querySelectorAll=originalDocument;}
 });
 // Focused/ Balanced mode UI routes, SPA navigation and preserved counts.
 await page.goto('https://www.instagram.com/');await page.evaluate(script);
 await page.evaluate(()=>{
   const a=document.createElement('a');a.id='home-route';a.href='/';a.textContent='Home';document.body.append(a);
   window.__ataraxiaStillness.configure({focused:true,limit:10,ids:['SavedPost']});
 });
 assert.equal(await page.locator('html').getAttribute('data-quiet-focused'),'');
 assert.equal(await page.locator('#home-route').getAttribute('data-quiet-home-hidden'),'true');
 await page.evaluate(()=>history.pushState({},'', '/direct/inbox/'));
 assert.equal(await page.locator('html').getAttribute('data-quiet-focused'),null);
 await page.evaluate(()=>history.pushState({},'', '/movinglogs/'));
 assert.equal(await page.locator('html').getAttribute('data-quiet-focused'),null);
 await page.evaluate(()=>history.pushState({},'', '/'));
 assert.equal(await page.locator('html').getAttribute('data-quiet-focused'),'');
 await page.evaluate(()=>window.__ataraxiaStillness.configure({focused:false}));
 assert.equal(await page.locator('html').getAttribute('data-quiet-focused'),null);
 assert.equal(await page.locator('#home-route').getAttribute('data-quiet-home-hidden'),null);
 assert.ok((await page.evaluate(()=>window.__ataraxiaStillness.snapshot())).ids.includes('SavedPost'));
 // Whole-script guard: never operate on look-alike domains.
 await page.route('https://instagram.com.evil.test/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://instagram.com.evil.test/'); await page.evaluate(script);
 assert.equal(await page.evaluate(()=>typeof window.__ataraxiaStillness),'undefined');
 console.log('PASS: browser fixtures — route hiding, dynamic content, ad filtering, caption preservation, unique counting, idempotence, inbox isolation, Afrikaans label, origin guard, fast scroll, immediate cap, split/accessible ad metadata');
 assert.deepEqual(errors,[], 'no script errors, including the scroll hot path');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
