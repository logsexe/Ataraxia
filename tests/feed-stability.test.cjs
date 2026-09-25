// Real Chromium fixtures. They do not certify live Instagram/device behaviour.
// FILTER_SCRIPT + FEED_TEST_ONLY=fling reproduces the alpha10 mid-motion failure.
const { chromium } = require('playwright');
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const script = fs.readFileSync(process.env.FILTER_SCRIPT || path.join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
const card = (id,label='Friend') => '<article><header>'+label+'</header><img width="350" height="400" alt="Photo"><a href="/p/'+id+'/">Post</a><p class="caption">A caption</p></article>';
const base = '<style>body{margin:0}article{height:600px;width:380px;box-sizing:border-box}img{display:block}#feed{height:500px;overflow:auto}</style>';
const settle = page => page.waitForTimeout(220);
(async()=>{
  const browser=await chromium.launch({headless:true,executablePath:process.env.ATARAXIA_TEST_BROWSER || undefined,args:['--no-sandbox']});
  const failures=[];
  let passed=0;
  async function check(name,html,verify){
    if(process.env.FEED_TEST_ONLY && !name.includes(process.env.FEED_TEST_ONLY)) return;
    const page=await browser.newPage({viewport:{width:412,height:915}});
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await page.route('https://www.instagram.com/**',r=>r.fulfill({contentType:'text/html',body:base+html}));
    try{
      await page.goto('https://www.instagram.com/');await page.evaluate(script);
      await verify(page);assert.deepEqual(errors,[]);passed++;console.log('PASS:',name);
    }catch(e){failures.push(name);console.error('FAIL:',name,e.message);}
    finally{await page.close();}
  }
  await check('whole padded ad slot collapses, including the outer gap',
    '<main style="display:flex;flex-direction:column;gap:24px"><div id="ad-slot" style="overflow:hidden;min-height:680px;padding:20px;margin-bottom:30px">'+card('Ad','Sponsored')+'<div style="height:40px"></div></div><div id="organic-slot">'+card('Organic')+'</div></main>',
    async page=>{
      assert.equal(await page.locator('#ad-slot').evaluate(el=>el.getBoundingClientRect().height),0);
      assert.equal(await page.locator('#organic-slot').evaluate(el=>el.getBoundingClientRect().top),0);
    });
  for(const nested of [false,true]){
    await check('fling holds layout steady, then preserves '+(nested?'nested':'window')+' anchor',
      (nested?'<div id="feed">':'<main>')+Array.from({length:20},(_,i)=>'<div style="height:600px">'+card('Anchor'+i)+'</div>').join('')+(nested?'</div>':'</main>'),
      async page=>{
        await page.evaluate(nested=>(nested?document.querySelector('#feed'):window).scrollTo(0,4200),nested);
        await settle(page);
        const before=await page.locator('article').nth(7).evaluate(el=>el.getBoundingClientRect().top);
        await page.evaluate(()=>{
          window.scrollWrites=0;window.scrollTo=()=>window.scrollWrites++;
          const descriptor=Object.getOwnPropertyDescriptor(Element.prototype,'scrollTop');
          Object.defineProperty(Element.prototype,'scrollTop',{...descriptor,set(v){window.scrollWrites++;descriptor.set.call(this,v);}});
          window.dispatchEvent(new PointerEvent('pointerdown',{pointerId:7,pointerType:'touch'}));
          document.querySelector('article header').textContent='Sponsored';
          for(let i=0;i<40;i++)window.dispatchEvent(new Event('scroll'));
        });
        await page.waitForTimeout(250);
        assert.equal(await page.locator('article').first().evaluate(el=>el.getBoundingClientRect().height),600,'filter collapsed a card under an active gesture');
        await page.evaluate(()=>window.dispatchEvent(new PointerEvent('pointerup',{pointerId:7,pointerType:'touch'})));
        await page.waitForFunction(()=>document.querySelector('article').dataset.quietHidden==='true');
        await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
        const after=await page.locator('article').nth(7).evaluate(el=>el.getBoundingClientRect().top);
        assert.ok(Math.abs(after-before)<=1,'anchor shifted by '+(after-before)+'px');
        assert.equal(await page.evaluate(()=>window.scrollWrites),0);
      });
  }
  await check('inertial fling defers late labels until scroll events stop',
    '<main>'+Array.from({length:20},(_,i)=>card('Inertia'+i)).join('')+'</main>',async page=>{
      await page.evaluate(()=>{
        window.dispatchEvent(new Event('scroll'));
        window.fling=setInterval(()=>window.dispatchEvent(new Event('scroll')),25);
        document.querySelector('article header').textContent='Sponsored';
      });
      await page.waitForTimeout(250);
      assert.equal(await page.locator('article').first().isVisible(),true,'filter changed layout during inertia');
      await page.evaluate(()=>clearInterval(window.fling));
      await page.waitForFunction(()=>document.querySelector('article').dataset.quietHidden==='true');
    });
  await check('scroll and diagnostics perform no DOM scans or geometry measurements',
    Array.from({length:60},(_,i)=>card('Fast'+i)).join(''),async page=>{
      await settle(page);
      const result=await page.evaluate(async()=>{
        const qsa=Element.prototype.querySelectorAll,qs=Element.prototype.querySelector;
        const rect=Element.prototype.getBoundingClientRect,dqs=document.querySelectorAll;
        let scans=0,geometry=0;
        Element.prototype.querySelectorAll=function(...args){scans++;return qsa.apply(this,args);};
        Element.prototype.querySelector=function(...args){scans++;return qs.apply(this,args);};
        document.querySelectorAll=function(...args){scans++;return dqs.apply(this,args);};
        Element.prototype.getBoundingClientRect=function(...args){geometry++;return rect.apply(this,args);};
        window.scrollTo(0,6000);
        for(let i=0;i<100;i++)window.dispatchEvent(new Event('scroll'));
        await new Promise(r=>setTimeout(r,240));
        const status=window.__ataraxiaStillness.status();
        Element.prototype.querySelectorAll=qsa;Element.prototype.querySelector=qs;
        Element.prototype.getBoundingClientRect=rect;document.querySelectorAll=dqs;
        return {scans,geometry,status};
      });
      assert.equal(result.scans,0);assert.equal(result.geometry,0);
      assert.equal('ids' in result.status,false);
      assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),null);
    });
  await check('caption, class and style changes do not rescan post metadata',
    card('Animated'),async page=>{
      await settle(page);
      await page.evaluate(()=>{
        const article=document.querySelector('article'),caption=article.querySelector('.caption');
        article.querySelectorAll=()=>{throw Error('metadata rescanned for unrelated animation');};
        caption.textContent='Updated caption';article.className='animated';article.style.opacity='.99';
      });
      await settle(page);
    });
  await check('new ads wait for gesture end and only new content is scanned',
    '<main>'+Array.from({length:30},(_,i)=>card('Existing'+i)).join('')+'</main>',async page=>{
      await settle(page);
      await page.evaluate(html=>{
        for(const article of document.querySelectorAll('article'))article.querySelectorAll=()=>{throw Error('existing post rescanned');};
        window.dispatchEvent(new PointerEvent('pointerdown',{pointerId:4,pointerType:'touch'}));
        document.querySelector('main').insertAdjacentHTML('beforeend',html);
      },'<div id="new-ad">'+card('NewAd','Sponsored')+'</div>');
      await settle(page);
      assert.equal(await page.locator('#new-ad').evaluate(el=>el.getBoundingClientRect().height),600);
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.status())).pending,true);
      await page.evaluate(()=>window.dispatchEvent(new PointerEvent('pointercancel',{pointerId:4,pointerType:'touch'})));
      await page.waitForFunction(()=>document.querySelector('#new-ad').dataset.quietHidden==='true');
      assert.equal((await page.evaluate(()=>window.__ataraxiaStillness.status())).containers,31);
    });
  await check('recycled ad slots restore and shared wrappers keep organic siblings',
    '<main><div id="slot">'+card('Ad','Sponsored')+'</div></main>',async page=>{
      assert.equal(await page.locator('#slot').isVisible(),false);
      await page.evaluate(html=>document.querySelector('#slot').insertAdjacentHTML('beforeend',html),card('Sibling'));
      await page.waitForFunction(()=>!document.querySelector('#slot').hasAttribute('data-quiet-hidden'));
      assert.equal(await page.locator('article').first().isVisible(),false);
      assert.equal(await page.locator('article').nth(1).isVisible(),true);
      await page.evaluate(()=>document.querySelector('article header').textContent='Friend');
      await page.waitForFunction(()=>!document.querySelector('article').hasAttribute('data-quiet-hidden'));
    });
  await check('modern div cards still filter ads and preserve ordinary Reel media',
    '<main><div id="modern"><header>Sponsored</header><img width="350" height="400"><a href="/friend/p/Ad123/">Post</a></div><article><header>Friend</header><a id="media" href="/reel/OrganicReel/"><img width="350" height="400"></a></article></main>',async page=>{
      assert.equal(await page.locator('#modern').isVisible(),false);
      assert.equal(await page.locator('#media').isVisible(),true);
    });
  await check('collapsed relative-width media never makes an ad reappear on metadata updates',
    '<main><div id="relative-slot"><article><span id="label">Sponsored</span><img style="width:100%;height:400px"><a href="/p/Relative/">Post</a></article></div></main>',async page=>{
      assert.equal(await page.locator('#relative-slot').isVisible(),false);
      await page.evaluate(()=>document.querySelector('#label').textContent='Spon\u200bsored');
      await settle(page);
      assert.equal(await page.locator('#relative-slot').isVisible(),false,'an already filtered card reappeared');
      await page.evaluate(()=>document.querySelector('#label').textContent='Friend');
      await page.waitForFunction(()=>!document.querySelector('#relative-slot').hasAttribute('data-quiet-hidden'));
    });
  await check('legacy post limits cannot enable counting or cover the feed',
    Array.from({length:30},(_,i)=>card('Uncapped'+i)).join(''),async page=>{
      await page.evaluate(()=>window.__ataraxiaStillness.configure({focused:false,limit:1,ids:['OldPost']}));
      await page.evaluate(()=>{window.scrollTo(0,12000);window.dispatchEvent(new Event('scroll'));});
      await settle(page);
      assert.equal(await page.locator('html').getAttribute('data-quiet-capped'),null);
      assert.equal(await page.locator('article').nth(20).isVisible(),true);
      const status=await page.evaluate(()=>window.__ataraxiaStillness.status());
      assert.equal('ids' in status,false);
    });
  await check('inbox and profile mutations never inspect message or post metadata',
    card('BeforeInbox'),async page=>{
      await page.evaluate(()=>{
        history.pushState({},'', '/direct/inbox/');
        const article=document.querySelector('article');
        article.querySelectorAll=()=>{throw Error('message body inspected');};
        article.querySelector=()=>{throw Error('message body inspected');};
        article.innerHTML='<header>Sponsored private message</header><img width="350">';
      });
      await settle(page);
      let status=await page.evaluate(()=>window.__ataraxiaStillness.status());
      assert.equal(status.feed,false);assert.equal(status.containers,0);
      await page.evaluate(()=>history.pushState({},'', '/friend/'));
      status=await page.evaluate(()=>window.__ataraxiaStillness.status());
      assert.equal(status.feed,false);
    });
  await browser.close();
  assert.deepEqual(failures,[], 'feed stability regressions');
  console.log('PASS:',passed,'scroll/filter scenarios');
})().catch(e=>{console.error(e);process.exitCode=1;});
