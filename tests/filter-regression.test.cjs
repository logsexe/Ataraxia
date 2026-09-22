// Source-level safety contracts only. Layout/counting behaviour is tested in
// Chromium by filter.test.cjs and feed-stability.test.cjs, not a fake DOM.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const source = fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
assert.doesNotMatch(source, /\.(?:scrollTo|scrollBy|scrollIntoView)\s*\(/, 'filter never moves the viewport');
assert.doesNotMatch(source, /\.scroll(?:Top|Left)\s*=/, 'filter never writes a scroll offset');
assert.doesNotMatch(source, /scroll-behavior\s*:/, 'filter does not override the site scrolling behaviour');
assert.match(source, /cancelAnimationFrame\(frame\)/, 'native snapshots cancel duplicate queued work');
assert.match(source, /if \(window\.top !== window/, 'top-level Instagram origin gate');
assert.match(source, /configure\(window\.__ataraxiaConfig \|\| \{\}\)/, 'native limits precede the first count');
assert.doesNotMatch(source, /localStorage|sessionStorage|cookie|XMLHttpRequest|sendBeacon|fetch\(/, 'no page storage or network APIs');
console.log('PASS: source safety contracts; behavioural regressions run in Chromium');
