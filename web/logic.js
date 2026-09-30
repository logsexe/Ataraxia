/* Practice rules for Still. No network, no Instagram page. */
(function (root, factory) {
  var api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  root.StillLogic = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function () {
  var TYPICAL_SECONDS = 30 * 60;
  var friends = [
    { id: 'maya', name: 'Maya', bio: 'Across the street, most evenings.', posts: [
      { id: 'maya-cup', image: 'media/maya.jpg', alt: 'A cup on a wooden table by a window.', caption: 'Stayed for one more cup.' }
    ]},
    { id: 'jonah', name: 'Jonah', bio: 'Works slowly, on paper.', posts: [
      { id: 'jonah-print', image: 'media/jonah.jpg', alt: 'A black and white print on a wooden table.', caption: 'Come by if you want to see the print.' }
    ]},
    { id: 'ada', name: 'Ada', bio: 'Walks the long way.', posts: [
      { id: 'ada-street', image: 'media/ada.jpg', alt: 'A quiet street in late light.', caption: 'The long way home.' }
    ]},
    { id: 'leah', name: 'Leah', bio: 'Leaves things where you will find them.', posts: [
      { id: 'leah-book', image: 'media/leah.jpg', alt: 'A closed book on a stone step.', caption: 'On the step, under the pot.' }
    ]}
  ];
  var known = [
    { id: 'june', name: 'June Harlow', field: 'Photographer', bio: 'Public figure.', posts: [
      { id: 'june-coast', image: 'media/june.jpg', alt: 'A pale coastline in the morning.', caption: 'Coast, March.' }
    ]},
    { id: 'edwin', name: 'Edwin Cho', field: 'Pianist', bio: 'Public figure.', posts: [
      { id: 'edwin-hall', image: 'media/edwin.jpg', alt: 'A piano alone on a wooden stage.', caption: 'Before the hall opened.' }
    ]},
    { id: 'nia', name: 'Nia Okonkwo', field: 'Novelist', bio: 'Public figure.', posts: [
      { id: 'nia-pages', image: 'media/nia.jpg', alt: 'A stack of pages beside a lamp.', caption: 'Pages from the second draft.' }
    ]}
  ];
  var you = {
    id: 'you', name: 'Alex Hart', bio: 'Keeping it short.', posts: [
      { id: 'alex-page', image: 'media/alex.jpg', alt: 'An open notebook and a pencil.', caption: 'The page for today.' }
    ]
  };
  var threads = [
    { id: 'maya', name: 'Maya', lines: [
      { from: 'maya', text: 'Are you still coming by after work?' },
      { from: 'maya', text: 'No need to stay long.' }
    ]},
    { id: 'jonah', name: 'Jonah', lines: [
      { from: 'jonah', text: 'The print is dry.' },
      { from: 'jonah', text: 'Come see it if you want.' }
    ]},
    { id: 'ada', name: 'Ada', lines: [
      { from: 'ada', text: 'Thursday is better for me.' }
    ]},
    { id: 'leah', name: 'Leah', lines: [
      { from: 'leah', text: 'I left the book on your step.' }
    ]}
  ];

  function routeIntent(choice) {
    switch (choice) {
      case 'messages': return { screen: 'messages' };
      case 'reply': return { screen: 'thread', threadId: 'maya', focus: '#reply' };
      case 'post': return { screen: 'compose' };
      case 'search': return { screen: 'search', focus: '#lookup' };
      default: return { screen: 'friends' };
    }
  }

  function searchPeople(query, friendList, knownList) {
    var q = String(query || '').trim().toLowerCase();
    friendList = friendList || friends;
    knownList = knownList || known;
    if (!q) return { people: [], knownRoom: false };
    var people = friendList.filter(function (p) { return p.name.toLowerCase().indexOf(q) !== -1; });
    var knownHit = knownList.some(function (p) { return p.name.toLowerCase().indexOf(q) !== -1; });
    return { people: people, knownRoom: people.length === 0 && knownHit };
  }

  function roomOf(id) {
    if (friends.some(function (p) { return p.id === id; })) return 'friends';
    if (known.some(function (p) { return p.id === id; })) return 'known';
    if (you.id === id) return 'you';
    return null;
  }

  function person(id) {
    return friends.concat(known).concat([you]).filter(function (p) { return p.id === id; })[0] || null;
  }

  function feed(room) {
    var list = room === 'known' ? known : friends;
    var tag = room === 'known' ? 'known' : 'friends';
    return list.reduce(function (all, p) {
      return all.concat(p.posts.map(function (post) {
        return { id: post.id, image: post.image, alt: post.alt, caption: post.caption, author: p.id, room: tag };
      }));
    }, []);
  }

  function roomsOverlap() {
    var ids = {};
    var names = {};
    friends.forEach(function (p) { ids[p.id] = true; names[p.name.toLowerCase()] = true; });
    return known.some(function (p) { return ids[p.id] || names[p.name.toLowerCase()]; })
      || feed('friends').some(function (post) { return roomOf(post.author) !== 'friends'; })
      || feed('known').some(function (post) { return roomOf(post.author) !== 'known'; });
  }

  function localIso(date) {
    return date.getFullYear() + '-'
      + String(date.getMonth() + 1).padStart(2, '0') + '-'
      + String(date.getDate()).padStart(2, '0');
  }

  function shiftIso(iso, days) {
    var parts = iso.split('-').map(Number);
    var dt = new Date(parts[0], parts[1] - 1, parts[2]);
    dt.setDate(dt.getDate() + days);
    return localIso(dt);
  }

  function seedHistory(today) {
    var minutes = [4, 7, 3, 11];
    return minutes.map(function (min, i) {
      return { date: shiftIso(today, i - minutes.length), seconds: min * 60 };
    });
  }

  function weekday(iso) {
    var parts = iso.split('-').map(Number);
    return ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'][new Date(parts[0], parts[1] - 1, parts[2]).getDay()];
  }

  function clock(seconds) {
    seconds = Math.max(0, Math.floor(Number(seconds) || 0));
    var s = seconds % 60;
    var m = Math.floor(seconds / 60) % 60;
    var h = Math.floor(seconds / 3600);
    var pad = function (n) { return String(n).padStart(2, '0'); };
    return h > 0 ? h + ':' + pad(m) + ':' + pad(s) : m + ':' + pad(s);
  }

  function spentLabel(seconds) {
    var s = Number(seconds) || 0;
    if (s < 60) return 'Under a minute';
    var m = Math.round(s / 60);
    if (m <= 1) return '1 min';
    return m + ' min';
  }

  function stillShare(seconds, typical) {
    var scale = typical > 0 ? typical : TYPICAL_SECONDS;
    return Math.max(0, Math.min(1, (Number(seconds) || 0) / scale));
  }

  return {
    TYPICAL_SECONDS: TYPICAL_SECONDS,
    friends: friends,
    known: known,
    you: you,
    threads: threads,
    routeIntent: routeIntent,
    searchPeople: searchPeople,
    roomOf: roomOf,
    person: person,
    feed: feed,
    roomsOverlap: roomsOverlap,
    localIso: localIso,
    shiftIso: shiftIso,
    seedHistory: seedHistory,
    weekday: weekday,
    clock: clock,
    spentLabel: spentLabel,
    stillShare: stillShare
  };
});
