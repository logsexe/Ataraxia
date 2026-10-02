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
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'an ad stays in the feed');
assert.equal(f.articles[0].parentElement.dataset.quietHidden, undefined, 'an ad slot is not collapsed');
assert.equal(f.articles[1].dataset.quietHidden, undefined, 'caption text is left alone');
assert.equal(f.articles[2].dataset.quietHidden, undefined, 'a sponsored header is not removed');
f = boot([{ aria: 'Sponsored', media: true, before: true, author: 'nike' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends'], people: { nike: 'influencer' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'a filed brand is still not pulled out of the feed');

f = boot([{ author: 'maya' }, { author: 'jonah' }, { author: 'ada' }, { label: 'Friend' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends'], people: { maya: 'celebrity', jonah: 'influencer', ada: 'friend' } });
assert.equal(f.articles[0].dataset.quietHidden, 'true', 'a celebrity counts as an influencer');
assert.equal(f.articles[1].dataset.quietHidden, 'true', 'an influencer leaves the friends side');
assert.equal(f.articles[2].dataset.quietHidden, undefined, 'a friend stays on the friends side');
assert.equal(f.articles[3].dataset.quietHidden, undefined, 'someone not filed yet stays');
f.state.__ataraxiaStillness.configure({ rooms: ['influencers'], people: { maya: 'celebrity', jonah: 'influencer', ada: 'friend' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'switching to influencers brings Maya back');
assert.equal(f.articles[1].dataset.quietHidden, undefined, 'Jonah is on the influencer side');
assert.equal(f.articles[2].dataset.quietHidden, 'true', 'a friend leaves the influencer side');
assert.equal(f.articles[3].dataset.quietHidden, undefined, 'someone not filed yet stays on this side too');

f = boot([{ author: 'maya', label: 'Follows you' }, { author: 'jonah', label: '12.4k followers' }, { author: 'ada', label: 'Ada' }, { author: 'lee', label: 'Lee', aria: 'Verified' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends'], people: {} });
assert.equal(f.state.__ataraxiaStillness.snapshot().sorted.maya, 'friend', 'follows you is a friend');
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'a friend stays on the friends side');
assert.equal(f.state.__ataraxiaStillness.snapshot().sorted.jonah, 'influencer', '10,000 followers is an influencer');
assert.equal(f.articles[1].dataset.quietHidden, 'true', 'an influencer leaves the friends side');
assert.equal(f.state.__ataraxiaStillness.snapshot().sorted.ada, undefined, 'a plain name is not guessed');
assert.equal(f.articles[2].dataset.quietHidden, undefined, 'an unsorted account stays');
assert.equal(f.state.__ataraxiaStillness.snapshot().sorted.lee, 'influencer', 'verified and not following you is an influencer');
f.state.__ataraxiaStillness.configure({ rooms: ['influencers'], people: { maya: 'influencer' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'a manual mark wins over follows-you');
f = boot([{ author: 'mina', label: 'Follows you 2.1m followers' }]);
f.state.__ataraxiaStillness.configure({ rooms: ['friends'], people: {} });
assert.equal(f.state.__ataraxiaStillness.snapshot().sorted.mina, 'influencer', 'a large audience stays on the influencer side');

f = boot([{ author: 'maya' }]);
f.state.location.pathname = '/direct/inbox/';
f.state.__ataraxiaStillness.configure({ rooms: [], people: { maya: 'celebrity' } });
assert.equal(f.articles[0].dataset.quietHidden, undefined, 'messages are not filtered');
assert.equal(f.state.__ataraxiaStillness.snapshot().ids.length, 0);
console.log('PASS: feed scroll is left alone, reel posts stay, groups can be chosen');
