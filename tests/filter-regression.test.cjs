// Deterministic DOM/geometry harness. Does not replace Android or live-site testing.
const vm = require('node:vm');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const script = fs.readFileSync(require('node:path').join(__dirname, '../app/src/main/assets/filter.js'), 'utf8');
function fixture(posts) {
 const attrs = new Set(), listeners = {};
 const state = {scrollY:0, innerHeight:500, location:{href:'https://www.instagram.com/', pathname:'/'}};
 const articles = posts.map((p,i) => {
  const media = p.media ? {} : null;
  const label = {textContent:p.label||'', getBoundingClientRect:()=>({width:100,height:20}),
   closest:()=>p.header?{}:null, compareDocumentPosition:()=>p.before?4:2, contains:()=>false,
   getAttribute:k=>k==='aria-label'?(p.aria||''):null};
  return {dataset:{}, querySelectorAll:q=>q==='img'?[]:[label],
   getBoundingClientRect:()=>({top:i*600-state.scrollY,bottom:(i+1)*600-state.scrollY,width:400,height:600}),
   querySelector:q=>q.startsWith('video')?media:{href:`https://www.instagram.com/p/Post${i}/`}};
 });
 const root = {appendChild(){},toggleAttribute(k,on){if(on)attrs.add(k);else attrs.delete(k);}};
 Object.assign(state,{top:state, window:state, URL, Set, Node:{DOCUMENT_POSITION_FOLLOWING:4},
  document:{documentElement:root,head:root,createElement:()=>({}),addEventListener(){},
   querySelectorAll:q=>q==='a[href]'?[]:articles},
  MutationObserver:class{observe(){}},addEventListener:(n,f)=>listeners[n]=f,
  requestAnimationFrame:f=>f()});
 vm.runInNewContext(script,state);
 return {state,articles,attrs,scroll(y){state.scrollY=y;listeners.scroll();}};
}
let f=fixture(Array.from({length:20},()=>({})));
assert.deepEqual(Array.from(f.state.__ataraxiaStillness.snapshot().ids),['Post0']);
f.scroll(6000);
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length,11,'all ten crossed posts counted on a large scroll jump');
f.scroll(0);
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length,11,'reverse scrolling does not double count');
f=fixture(Array.from({length:20},()=>({})));
f.state.__ataraxiaStillness.configure({limit:5,ids:[]});
f.scroll(6000);
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length,5);
assert.ok(f.attrs.has('data-quiet-capped'),'local shield appears without waiting for native polling');
f.state.location.pathname='/direct/inbox/';f.state.__ataraxiaStillness.scan();
assert.ok(!f.attrs.has('data-quiet-capped'),'inbox is never capped');
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length,0);
f=fixture([{label:'Spon\u200bsored',media:true,before:true},{label:'Sponsored',media:true,before:false},{label:'Geborg',header:true}]);
assert.equal(f.articles[0].dataset.quietHidden,'true','split/zero-width metadata recognised outside header');
assert.equal(f.articles[1].dataset.quietHidden,undefined,'caption text preserved');
assert.equal(f.articles[2].dataset.quietHidden,'true');
f=fixture([{aria:'Sponsored',media:true,before:true}]);
assert.equal(f.articles[0].dataset.quietHidden,'true','accessible ad label recognised');
f=fixture(Array.from({length:20},()=>({})));
f.state.__ataraxiaStillness.configure({limit:3,ids:['Older1','Older2']});
assert.ok(f.attrs.has('data-quiet-capped'),'restored native counts contribute to cap');
console.log('PASS: 12 regression assertions (mock DOM): fast jump, reverse scroll, immediate cap, inbox isolation, metadata labels, caption preservation, restored count');

f=fixture([{label:'Ad',media:true,before:true},{label:'Ad',media:true,before:false}]);
assert.equal(f.articles[0].dataset.quietHidden,'true');
assert.equal(f.articles[1].dataset.quietHidden,undefined);
for (const article of f.articles) article.querySelectorAll=()=>{throw Error('metadata rescanned during scroll');};
f.scroll(100);
console.log('PASS: Ad label, caption preservation and zero metadata scans on scroll');

f=fixture(Array.from({length:20},()=>({})));
f.state.__ataraxiaStillness.configure({limit:3,ids:['Previous1','Previous2']});
assert.ok(f.attrs.has('data-quiet-capped'));
f.state.location.pathname='/direct/inbox/';
f.state.__ataraxiaStillness.configure({reset:true,limit:3,ids:[]});
assert.ok(!f.attrs.has('data-quiet-capped'));
f.state.location.pathname='/';f.state.__ataraxiaStillness.scan();
assert.deepEqual(Array.from(f.state.__ataraxiaStillness.snapshot().ids),['Post0']);
assert.equal(f.state.__ataraxiaStillness.snapshot().containers,20);
console.log('PASS: session reset while in inbox, return to feed and container status');
f=fixture(Array.from({length:20},()=>({})));
f.state.__ataraxiaStillness.configure({focused:true,limit:10,ids:[]});
assert.ok(f.attrs.has('data-quiet-focused'));
const focusedCount=f.state.__ataraxiaStillness.snapshot().ids.length;
f.scroll(6000);
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length,focusedCount);
f.state.location.pathname='/direct/inbox/';f.state.__ataraxiaStillness.scan();
assert.ok(!f.attrs.has('data-quiet-focused'));
f.state.location.pathname='/';f.state.__ataraxiaStillness.configure({focused:false});
assert.ok(!f.attrs.has('data-quiet-focused'));
console.log('PASS: Focused feed shield, no feed counting while focused, inbox access and Balanced restoration');
