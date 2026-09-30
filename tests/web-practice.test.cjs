const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const L = require('../web/logic.js');

assert.equal(L.roomsOverlap(), false, 'friends and known never share a person or a post');
assert.deepEqual(L.feed('friends').map(p => p.author).sort(), ['ada', 'jonah', 'leah', 'maya']);
assert.deepEqual(L.feed('known').map(p => p.author).sort(), ['edwin', 'june', 'nia']);
const friendIds = new Set(L.feed('friends').map(p => p.id));
assert.equal(L.feed('known').some(p => friendIds.has(p.id)), false);

assert.deepEqual(L.routeIntent('messages'), { screen: 'messages' });
assert.equal(L.routeIntent('reply').screen, 'thread');
assert.equal(L.routeIntent('reply').threadId, 'maya');
assert.equal(L.routeIntent('post').screen, 'compose');
assert.equal(L.routeIntent('search').screen, 'search');
assert.equal(L.routeIntent('unsure').screen, 'friends');
assert.equal(L.routeIntent('skip').screen, 'friends');

const maya = L.searchPeople('maya');
assert.deepEqual(maya.people.map(p => p.id), ['maya']);
assert.equal(maya.knownRoom, false);
const june = L.searchPeople('June Harlow');
assert.deepEqual(june.people, []);
assert.equal(june.knownRoom, true);
assert.equal(L.searchPeople('reel').people.length, 0);
assert.equal(L.searchPeople('').people.length, 0);
assert.equal(L.roomOf('maya'), 'friends');
assert.equal(L.roomOf('june'), 'known');
assert.notEqual(L.roomOf('maya'), L.roomOf('june'));

assert.equal(L.shiftIso('2026-09-30', -1), '2026-09-29');
const history = L.seedHistory('2026-09-30');
assert.equal(history.length, 4);
assert.equal(history[3].date, '2026-09-29');
assert.equal(history[0].date, '2026-09-26');
assert.equal(L.clock(0), '0:00');
assert.equal(L.clock(75), '1:15');
assert.equal(L.spentLabel(30), 'Under a minute');
assert.equal(L.spentLabel(90), '2 min');
assert.equal(L.stillShare(0), 0);
assert.equal(L.stillShare(15 * 60), 0.5);
assert.equal(L.stillShare(60 * 60), 1);

const html = fs.readFileSync(path.join(__dirname, '../web/index.html'), 'utf8');
const app = fs.readFileSync(path.join(__dirname, '../web/app.js'), 'utf8');
const css = fs.readFileSync(path.join(__dirname, '../web/styles.css'), 'utf8');
assert.equal(/<iframe/i.test(html + app), false, 'the practice does not embed another site');
assert.match(app, /target="_blank"/);
assert.match(app, /noopener/);
assert.equal((app.match(/instagram\.com/g) || []).length, 1);
assert.equal(/while you were away|suggested for you|streak/i.test(html + app + css), false);
for (const absent of ['Reels', 'Explore', 'Shop', 'Live']) assert.match(app, new RegExp(absent));
assert.doesNotMatch(css, /#101916|#b9e5c8/i);

console.log('PASS: practice rooms stay separate, intents land, search is not Explore, no embed');
