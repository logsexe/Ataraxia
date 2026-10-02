// Deterministic DOM harness. Scrolling must not hide posts or blank the page.
const vm = require('node:vm');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const script = fs.readFileSync(require('node:path').join(__dirname, '../app/src/main/assets/filter.js'), 'utf8');
assert.equal(/addEventListener\(\s*['"]scroll['"]/.test(script), false, 'scrolling is not observed');
assert.equal(script.includes('getBoundingClientRect'), false, 'scroll geometry is not read');
assert.equal(script.includes('data-quiet-capped'), false, 'the page is not blanked after a post count');

function link(href) {
  return { dataset: {}, href, parentElement: null, children: [],
    getAttribute: k => k === 'href' ? href : null, closest: () => null,
    querySelectorAll: () => [], querySelector: () => null };
}
function article(opts) {
  const label = { textContent: opts.label || '', dataset: {},
    getAttribute: k => k === 'aria-label' ? (opts.aria || null) : null,
    closest: () => opts.header ? {} : null,
    compareDocumentPosition: () => opts.before ? 4 : 2, contains: () => false };
  const author = opts.author ? link('/' + opts.author + '/') : null;
  const node = { dataset: {}, parentElement: { dataset: {}, childElementCount: 1, querySelectorAll: () => [node] },
    querySelector: q => (q.startsWith('video') || q.includes('img')) ? (opts.media ? {} : null) : null,
    querySelectorAll: q => q === 'a[href]' ? (author ? [author] : []) : [label] };
  if (author) author.closest = sel => sel.includes('article') ? node : null;
  return node;
}
function boot(posts, anchors) {
  const articles = posts.map(article);
  const links = (anchors || []).map(link);
  const state = { location: { href: 'https://www.instagram.com/', pathname: '/' },
    URL, Set, Node: { DOCUMENT_POSITION_FOLLOWING: 4 }, requestAnimationFrame: f => f(),
    setTimeout: f => f(), clearTimeout() {} };
  state.top = state; state.window = state;
  state.document = {
    documentElement: { appendChild(){} }, head: { appendChild(){} }, body: {},
    createElement: () => ({}), addEventListener(){},
    querySelectorAll: q => q === 'a[href]' ? links.concat(articles.flatMap(a => a.querySelectorAll('a[href]'))) : articles
  };
  state.MutationObserver = class { observe(){} };
  state.addEventListener = () => {};
  vm.runInNewContext(script, state);
  return { state, articles, links };
}

let f = boot([{ label: 'Friend' }, { author: 'maya' }], ['/reels/', '/reel/abc123/']);
assert.equal(f.links[0].dataset.quietHidden, 'true', 'the Reels tab is hidden');
assert.equal(f.links[1].dataset.quietHidden, undefined, 'a reel post stays in the feed');
assert.equal(f.state.document.documentElement.toggleAttribute, undefined);
assert.equal(typeof f.state.addEventListener, 'function');
f.state.__ataraxiaStillness.scan();
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'a second pass does not punch holes in ordinary posts');

f = boot([{ label: 'Spon\u200bsored', media: true, before: true }, { label: 'Sponsored', media: true, before: false }, { label: 'Geborg', header: true }]);
assert.equal(f.articles[0].dataset.quietHidden, 'true', 'split metadata recognised outside the header');
assert.equal(f.articles[0].parentElement.dataset.quietHidden, 'true', 'an ad slot collapses with the post');
assert.equal(f.articles[1].dataset.quietHidden, undefined, 'caption text is left alone');
assert.equal(f.articles[1].parentElement.dataset.quietHidden, undefined, 'an ordinary post keeps its slot');
assert.equal(f.articles[2].dataset.quietHidden, 'true');
f = boot([{ aria: 'Sponsored', media: true, before: true }]);
assert.equal(f.articles[0].dataset.quietHidden, 'true', 'accessible ad label recognised');

f = boot([{ author: 'maya' }, { author: 'jonah' }, { label: 'Friend' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends'], people: { maya: 'celebrity', jonah: 'influencer' } });
assert.equal(f.articles[0].dataset.quietHidden, 'true', 'a filed celebrity leaves when that group is off');
assert.equal(f.articles[1].dataset.quietHidden, 'true', 'a filed influencer leaves when that group is off');
assert.equal(f.articles[2].dataset.quietHidden, undefined, 'someone not filed yet stays');
f.state.__ataraxiaStillness.configure({ rooms: ['friends', 'celebrities'], people: { maya: 'celebrity', jonah: 'influencer' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'turning celebrities back on brings Maya back');
assert.equal(f.articles[0].parentElement.dataset.quietHidden, undefined, 'the slot comes back with the post');
assert.equal(f.articles[1].dataset.quietHidden, 'true', 'Jonah stays out while influencers are off');
f = boot([{ author: 'maya' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends', 'celebrities'], people: { maya: 'celebrity' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'celebrities stay when that group is on');

f = boot([{ author: 'maya' }]);
f.state.location.pathname = '/direct/inbox/';
f.state.__ataraxiaStillness.configure({ rooms: [], people: { maya: 'celebrity' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'messages are not filtered');
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length, 0);
console.log('PASS: feed scroll is left alone, reel posts stay, groups can be chosen');
