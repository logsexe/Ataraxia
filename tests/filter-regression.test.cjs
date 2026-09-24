// Source-level safety contracts only. Layout/counting behaviour is tested in
// Chromium by filter.test.cjs and feed-stability.test.cjs, not a fake DOM.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const source = fs.readFileSync(require('node:path').join(__dirname,'../app/src/main/assets/filter.js'),'utf8');
assert.doesNotMatch(source, /\.(?:scrollTo|scrollBy|scrollIntoView)\s*\(/, 'filter never moves the viewport');
assert.doesNotMatch(source, /\.scroll(?:Top|Left)\s*=/, 'filter never writes a scroll offset');
assert.doesNotMatch(source, /scroll-behavior\s*:/, 'filter does not override the site scrolling behaviour');
assert.match(source, /cancelAnimationFrame\(frame\)/, 'queued filtering can be cancelled during motion');
assert.match(source, /if \(window\.top !== window/, 'top-level Instagram origin gate');
assert.match(source, /configure\(window\.__ataraxiaConfig \|\| \{\}\)/, 'native mode precedes initial filtering');
assert.doesNotMatch(source, /localStorage|sessionStorage|cookie|XMLHttpRequest|sendBeacon|fetch\(/, 'no page storage or network APIs');
assert.doesNotMatch(source, /countPosts|new ResizeObserver|new IntersectionObserver|getBoundingClientRect|data-quiet-capped|seen\.add|function identify|function hash/, 'the active filter has no counting or geometry engine');
assert.match(source, /attributeFilter:\['href','aria-label','hidden','role'\]/, 'scroll-related style/class changes do not trigger scans');
console.log('PASS: source safety contracts; behavioural regressions run in Chromium');
