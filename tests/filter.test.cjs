const { chromium } = require('playwright');
const fs = require('fs');
const assert = require('node:assert/strict');
const path = require('path');
const script = fs.readFileSync(path.join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.ATARAXIA_TEST_BROWSER || undefined,args:['--no-sandbox']});
 const page=await browser.newPage({viewport:{width:412,height:915}});
 const html=`<html><head><style>article {height:250px;width:380px;}body{margin:0}</style></head><body>
 <nav><a id="reels" href="/reels/">Reels</a><a id="explore" href="/explore/">Explore</a><a id="inbox" href="/direct/inbox/">Inbox</a></nav>
 <article id="ad"><header><span>Sponsored</span></header><a href="/p/Ad1/">Ad</a></article>
 <article id="first"><header><span>Friend</span></header><a href="/p/First1/">Photo</a><p>I dislike sponsored posts</p></article>
 <article id="second"><header><span>Friend 2</span></header><a href="/p/Second2/">Photo 2</a></article>
 </body></html>`;
 await page.route('https://www.instagram.com/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://www.instagram.com/'); await page.evaluate(script);
 assert.equal(await page.locator('#reels').isVisible(),false);
 assert.equal(await page.locator('#explore').isVisible(),false);
 assert.equal(await page.locator('#inbox').isVisible(),true);
 assert.equal(await page.locator('#ad').isVisible(),false);
 assert.equal(await page.locator('#first').isVisible(),true);
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
 // Whole-script guard: never operate on look-alike domains.
 await page.route('https://instagram.com.evil.test/**',route=>route.fulfill({contentType:'text/html',body:html}));
 await page.goto('https://instagram.com.evil.test/'); await page.evaluate(script);
 assert.equal(await page.evaluate(()=>typeof window.__ataraxiaStillness),'undefined');
 console.log('PASS: browser fixtures — route hiding, dynamic content, ad filtering, caption preservation, unique counting, idempotence, inbox isolation, Afrikaans label, origin guard');
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
