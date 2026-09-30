/* Still, the practice. Friends and Known never share a feed. Instagram is not loaded here. */
(function () {
  var L = window.StillLogic;
  var screen = document.getElementById('screen');
  var phone = document.querySelector('.phone');
  var params = new URLSearchParams(location.search);
  var state = {
    welcomed: localStorage.getItem('still.welcomed') === '1' && !params.has('welcome'),
    step: 1,
    screen: 'friends',
    threadId: null,
    profileId: null,
    backTo: 'friends',
    backLabel: 'Friends',
    query: '',
    caption: '',
    reply: '',
    photo: '',
    focus: null,
    commenting: null,
    likes: read('still.likes', {}),
    comments: read('still.comments', {}),
    extra: read('still.extra', {}),
    mine: read('still.mine', [])
  };

  document.querySelector('.tabs').addEventListener('click', function (e) {
    var tab = e.target.closest('[data-tab]');
    if (!tab) return;
    openRoom(tab.dataset.tab);
  });
  screen.addEventListener('click', onClick);
  screen.addEventListener('input', onInput);
  screen.addEventListener('change', onChange);

  var time = loadTime();
  save('still.time', time);
  var lastWall = Date.now();

  var requested = params.get('room');
  var rooms = ['friends', 'known', 'messages', 'you', 'compose', 'search'];
  if (params.has('welcome')) {
    var step = Number(params.get('step'));
    if (step >= 1 && step <= 3) state.step = step;
    render();
  }
  else if (requested) {
    state.welcomed = true;
    if (params.get('q')) state.query = params.get('q');
    localStorage.setItem('still.welcomed', '1');
    if (requested === 'thread') {
      state.screen = 'thread';
      state.threadId = params.get('who') || 'maya';
    } else state.screen = rooms.indexOf(requested) === -1 ? 'friends' : requested;
    render({ reset: true });
  } else if (!state.welcomed) render();
  else openRoom('friends');

  renderHistory();
  renderRail();
  setInterval(tick, 1000);
  document.addEventListener('visibilitychange', function () { lastWall = Date.now(); });
  window.addEventListener('pagehide', function () { save('still.time', time); });

  function openRoom(name) {
    state.welcomed = true;
    localStorage.setItem('still.welcomed', '1');
    state.screen = name;
    state.threadId = null;
    state.profileId = null;
    state.focus = null;
    render({ reset: true });
  }

  function render(opts) {
    var reset = opts && opts.reset;
    var top = reset ? 0 : screen.scrollTop;
    phone.dataset.chrome = state.welcomed ? 'app' : 'onboard';
    screen.classList.toggle('locked', state.screen === 'thread');
    var html = '';
    if (!state.welcomed) html = onboard();
    else if (state.screen === 'friends') html = room('friends');
    else if (state.screen === 'known') html = room('known');
    else if (state.screen === 'messages') html = messages();
    else if (state.screen === 'thread') html = thread();
    else if (state.screen === 'you') html = profile('you');
    else if (state.screen === 'profile') html = profile(state.profileId);
    else if (state.screen === 'compose') html = compose();
    else if (state.screen === 'search') html = search();
    screen.innerHTML = html;
    markTabs();
    if (reset) screen.scrollTop = 0;
    else screen.scrollTop = top;
    if (state.focus) {
      var el = screen.querySelector(state.focus);
      if (el) el.focus();
      state.focus = null;
    }
  }

  function onboard() {
    var head = '<div class="pad"><div class="top"><p class="step">' + state.step + ' of 3</p>'
      + '<button type="button" class="skip" data-act="skip">Skip</button></div>';
    if (state.step === 1) {
      return head + mark()
        + '<p class="wordmark">Still.</p>'
        + '<h1>Instagram, quietly.</h1>'
        + '<p class="lede">Friends in one room. Famous people in another.</p>'
        + '<p class="lede">Read what’s waiting. Then leave.</p>'
        + '<div class="stack"><button type="button" class="btn primary" data-act="next">Continue</button></div></div>';
    }
    if (state.step === 2) {
      return head + '<h1>What you keep.</h1>'
        + '<p class="kicker">Stays</p><ul class="keep">'
        + '<li>Friends’ posts</li><li>A Known room</li><li>Messages</li><li>Posting</li></ul>'
        + '<p class="kicker">Never loads</p>'
        + '<p class="absent">Reels, ads, tracking, Explore, Shop, and Live.</p>'
        + '<div class="stack"><button type="button" class="btn primary" data-act="next">Continue</button></div></div>';
    }
    return head + '<h1>Why are you opening it?</h1>'
      + '<div class="stack">'
      + choice('messages', 'Check messages')
      + choice('reply', 'Reply to someone')
      + choice('post', 'Post a photo')
      + choice('search', 'Look someone up')
      + choice('unsure', 'I’m not sure yet')
      + '</div></div>';
  }

  function choice(id, label) {
    return '<button type="button" class="btn secondary" data-act="intent" data-choice="' + id + '">' + label + '</button>';
  }

  function room(which) {
    var knownRoom = which === 'known';
    var title = knownRoom ? 'Known' : 'Friends';
    var lede = knownRoom ? 'Famous people. Another room.' : 'Only people you know.';
    var posts = L.feed(which).map(function (post) {
      var who = L.person(post.author);
      return '<article class="post">'
        + '<img src="' + esc(post.image) + '" alt="' + esc(post.alt) + '">'
        + '<button type="button" class="name" data-act="profile" data-id="' + esc(who.id) + '">' + esc(who.name) + '</button>'
        + (who.field ? '<p class="field">' + esc(who.field) + '</p>' : '')
        + '<p class="caption">' + esc(post.caption) + '</p>'
        + actions(post.id)
        + commentLine(post.id)
        + '</article>';
    }).join('');
    return '<div class="pad"><header>'
      + '<h1>' + title + '</h1><p class="lede">' + lede + '</p>'
      + (knownRoom ? '' : '<button type="button" class="quiet lookup" data-act="lookup">Someone you know.</button>')
      + todayLine()
      + '</header>' + posts
      + '<p class="end">' + (knownRoom ? 'That’s the other room.' : 'That’s the room.') + '</p></div>';
  }

  function actions(id) {
    var liked = !!state.likes[id];
    return '<div class="actions">'
      + '<button type="button" class="quiet" data-act="like" data-id="' + esc(id) + '" aria-pressed="' + liked + '">'
      + (liked ? 'Liked' : 'Like') + '</button>'
      + '<button type="button" class="quiet" data-act="comment" data-id="' + esc(id) + '">Comment</button>'
      + '</div>';
  }

  function commentLine(id) {
    var line = state.comments[id] ? '<p class="comment"><b>You</b> ' + esc(state.comments[id]) + '</p>' : '';
    if (state.commenting !== id || state.comments[id]) return line;
    return '<form class="composer" data-act="note" data-id="' + esc(id) + '">'
      + '<input id="note" type="text" maxlength="240" placeholder="A short note" autocomplete="off">'
      + '<button class="btn primary narrow" type="submit">Leave it</button></form>';
  }

  function messages() {
    var rows = L.threads.map(function (t) {
      var lines = t.lines.concat(state.extra[t.id] || []);
      var last = lines[lines.length - 1];
      return '<button type="button" class="row" data-act="thread" data-id="' + t.id + '">'
        + '<strong>' + esc(t.name) + '</strong><span>' + esc(last.text) + '</span></button>';
    }).join('');
    return '<div class="pad"><h1>Messages</h1><p class="lede">Read what’s waiting. Then leave.</p>'
      + todayLine() + '<div class="list">' + rows + '</div></div>';
  }

  function thread() {
    var t = L.threads.filter(function (item) { return item.id === state.threadId; })[0];
    if (!t) return messages();
    var lines = t.lines.concat(state.extra[t.id] || []).map(function (line) {
      var mine = line.from === 'you';
      return '<p class="bubble' + (mine ? ' you' : '') + '">' + esc(line.text) + '</p>';
    }).join('');
    return '<div class="thread"><button type="button" class="back" data-act="messages">Messages</button>'
      + '<h1>' + esc(t.name) + '</h1>'
      + '<div class="thread-log">' + lines + '</div>'
      + '<form class="composer" data-act="send">'
      + '<input id="reply" type="text" maxlength="240" placeholder="Write back" value="' + esc(state.reply) + '" autocomplete="off">'
      + '<button class="btn primary narrow" type="submit">Send</button></form></div>';
  }

  function profile(id) {
    var who = L.person(id);
    if (!who) return room('friends');
    var posts = (id === 'you' ? state.mine : []).concat(who.posts).map(function (post) {
      return '<article class="post"><img src="' + esc(post.image) + '" alt="' + esc(post.alt || 'Your photo') + '">'
        + '<p class="caption">' + esc(post.caption || '') + '</p>'
        + actions(post.id) + commentLine(post.id) + '</article>';
    }).join('');
    var back = id === 'you'
      ? ''
      : '<button type="button" class="back" data-act="back">' + esc(state.backLabel) + '</button>';
    var real = id === 'you'
      ? '<div class="real"><p class="kicker">Your real Instagram</p>'
        + '<p>This leaves Still and opens Instagram in the browser. The site is not shown here.</p>'
        + '<a class="btn secondary" href="https://www.instagram.com/" target="_blank" rel="noopener noreferrer">Open Instagram</a></div>'
      : '';
    return '<div class="pad">' + back
      + (id === 'you' ? mark() + '<p class="wordmark">Still.</p>' : '')
      + '<h1 class="person-name">' + esc(who.name) + '</h1>'
      + (who.field ? '<p class="field">' + esc(who.field) + '</p>' : '')
      + '<p class="lede">' + esc(who.bio) + '</p>'
      + (id === 'you' ? '<div class="stack"><button type="button" class="btn secondary" data-act="compose">Post a photo</button></div>' : '')
      + real
      + todayLine() + posts + '</div>';
  }

  function compose() {
    var frame = state.photo
      ? '<img class="preview" src="' + esc(state.photo) + '" alt="The photo you chose">'
      : '<label class="chooser" for="photo">One photo.</label>';
    return '<div class="pad"><button type="button" class="back" data-act="you">You</button>'
      + '<h1>One photo.</h1>'
      + frame
      + '<input id="photo" type="file" accept="image/*" hidden>'
      + (state.photo ? '<button type="button" class="quiet" data-act="rechoose" style="margin-top:8px">Choose another</button>' : '')
      + '<textarea id="caption" maxlength="240" placeholder="A short caption.">' + esc(state.caption) + '</textarea>'
      + '<button type="button" class="btn primary" data-act="publish"' + (state.photo ? '' : ' disabled') + '>Post</button></div>';
  }

  function search() {
    var found = L.searchPeople(state.query);
    var body = '';
    if (!state.query.trim()) body = '<p class="hint">A name you already know.</p>';
    else if (found.people.length) {
      body = found.people.map(function (p) {
        return '<button type="button" class="row" data-act="profile" data-id="' + p.id + '"><strong>' + esc(p.name) + '</strong></button>';
      }).join('');
    } else if (found.knownRoom) {
      body = '<p class="hint">That’s the other room.</p>'
        + '<button type="button" class="btn secondary" data-act="known">Open Known</button>';
    } else body = '<p class="hint">If you don’t already know them, they aren’t here.</p>';
    return '<div class="pad"><button type="button" class="back" data-act="friends">Friends</button>'
      + '<h1>Someone you know.</h1>'
      + '<input id="lookup" type="search" placeholder="A name" value="' + esc(state.query) + '" autocomplete="off">'
      + '<div id="results">' + body + '</div></div>';
  }

  function onClick(e) {
    var btn = e.target.closest('[data-act]');
    if (!btn || btn.tagName === 'FORM') return;
    var act = btn.dataset.act;
    if (act === 'skip') return openRoom('friends');
    if (act === 'next') { state.step += 1; return render({ reset: true }); }
    if (act === 'intent') {
      var dest = L.routeIntent(btn.dataset.choice);
      state.welcomed = true;
      localStorage.setItem('still.welcomed', '1');
      state.screen = dest.screen;
      state.threadId = dest.threadId || null;
      state.focus = dest.focus || null;
      return render({ reset: true });
    }
    if (act === 'like') {
      state.likes[btn.dataset.id] = !state.likes[btn.dataset.id];
      save('still.likes', state.likes);
      return render();
    }
    if (act === 'comment') {
      if (state.comments[btn.dataset.id]) return;
      state.commenting = btn.dataset.id;
      state.focus = '#note';
      return render();
    }
    if (act === 'lookup') {
      state.screen = 'search';
      state.query = '';
      return render({ reset: true });
    }
    if (act === 'profile') {
      state.backTo = state.screen === 'search' ? 'search' : (L.roomOf(btn.dataset.id) === 'known' ? 'known' : 'friends');
      state.backLabel = state.backTo === 'known' ? 'Known' : (state.backTo === 'search' ? 'Search' : 'Friends');
      state.profileId = btn.dataset.id;
      state.screen = 'profile';
      return render({ reset: true });
    }
    if (act === 'back') {
      state.screen = state.backTo || 'friends';
      return render({ reset: true });
    }
    if (act === 'thread') {
      state.threadId = btn.dataset.id;
      state.screen = 'thread';
      state.reply = '';
      return render({ reset: true });
    }
    if (act === 'messages') { state.screen = 'messages'; return render({ reset: true }); }
    if (act === 'friends') { return openRoom('friends'); }
    if (act === 'known') { return openRoom('known'); }
    if (act === 'you' || act === 'compose') {
      state.screen = act === 'compose' ? 'compose' : 'you';
      state.welcomed = true;
      return render({ reset: true });
    }
    if (act === 'rechoose') {
      var input = document.getElementById('photo');
      if (input) input.click();
      return;
    }
    if (act === 'publish') publish();
    if (act === 'send') return;
  }

  function onInput(e) {
    if (e.target.id === 'lookup') {
      state.query = e.target.value;
      var box = document.getElementById('results');
      if (!box) return;
      var hold = state.screen;
      state.screen = 'search';
      var fresh = search();
      var wrap = document.createElement('div');
      wrap.innerHTML = fresh;
      var next = wrap.querySelector('#results');
      if (next) box.innerHTML = next.innerHTML;
      state.screen = hold;
    } else if (e.target.id === 'caption') state.caption = e.target.value;
    else if (e.target.id === 'reply') state.reply = e.target.value;
  }

  function onChange(e) {
    if (e.target.id !== 'photo' || !e.target.files || !e.target.files[0]) return;
    var file = e.target.files[0];
    if (file.type.indexOf('image/') !== 0) return;
    var reader = new FileReader();
    reader.onload = function () {
      state.photo = String(reader.result || '');
      render();
    };
    reader.readAsDataURL(file);
  }

  screen.addEventListener('submit', function (e) {
    var note = e.target.closest('[data-act="note"]');
    if (note) {
      e.preventDefault();
      var written = (note.querySelector('input').value || '').trim();
      if (!written) return;
      state.comments[note.dataset.id] = written.slice(0, 240);
      save('still.comments', state.comments);
      state.commenting = null;
      render();
      return;
    }
    var form = e.target.closest('[data-act="send"]');
    if (!form) return;
    e.preventDefault();
    var text = ((form.querySelector('input') || {}).value || '').trim();
    if (!text || !state.threadId) return;
    state.extra[state.threadId] = (state.extra[state.threadId] || []).concat([{ from: 'you', text: text.slice(0, 240) }]);
    save('still.extra', state.extra);
    state.reply = '';
    state.focus = '#reply';
    render();
    var log = screen.querySelector('.thread-log');
    if (log) log.scrollTop = log.scrollHeight;
  });

  function publish() {
    if (!state.photo) return;
    state.mine.unshift({
      id: 'mine-' + Date.now(),
      image: state.photo,
      alt: 'A photo you posted.',
      caption: state.caption.trim().slice(0, 240)
    });
    save('still.mine', state.mine);
    state.photo = '';
    state.caption = '';
    state.screen = 'you';
    render({ reset: true });
  }

  function markTabs() {
    var current = state.screen === 'profile'
      ? (L.roomOf(state.profileId) === 'you' ? 'you' : L.roomOf(state.profileId))
      : (state.screen === 'thread' ? 'messages' : (state.screen === 'compose' ? 'you' : (state.screen === 'search' ? 'friends' : state.screen)));
    document.querySelectorAll('.tabs button').forEach(function (btn) {
      if (btn.dataset.tab === current) btn.setAttribute('aria-current', 'page');
      else btn.removeAttribute('aria-current');
    });
  }

  function todayLine() {
    return '<p class="today-line">' + esc(L.clock(time.seconds)) + ' in Still today</p>';
  }

  function mark() {
    return '<svg class="mark" viewBox="0 0 64 36" aria-hidden="true">'
      + '<circle cx="32" cy="8" r="3.1" fill="none" stroke="#3E5A46" stroke-width="1.4"></circle>'
      + '<path d="M4 28 H60" fill="none" stroke="#171512" stroke-width="1.4"></path></svg>';
  }

  function tick() {
    var now = Date.now();
    var delta = Math.min(5000, Math.max(0, now - lastWall));
    lastWall = now;
    if (!document.hidden) {
      var today = L.localIso(new Date());
      if (time.today !== today) {
        time.history = time.history.concat([{ date: time.today, seconds: Math.round(time.seconds) }]).slice(-4);
        time.today = today;
        time.seconds = 0;
        renderHistory();
      }
      time.seconds += delta / 1000;
      save('still.time', time);
    }
    renderRail();
    var line = screen.querySelector('.today-line');
    if (line) line.textContent = L.clock(time.seconds) + ' in Still today';
  }

  function renderRail() {
    document.getElementById('clock').textContent = L.clock(time.seconds);
    document.getElementById('today-mark').style.width = (L.stillShare(time.seconds, L.TYPICAL_SECONDS) * 100) + '%';
  }

  function renderHistory() {
    document.getElementById('days').innerHTML = (time.history || []).slice(-4).map(function (day) {
      var width = L.stillShare(day.seconds, L.TYPICAL_SECONDS) * 100;
      return '<div class="day"><div class="day-label"><span>' + esc(L.weekday(day.date)) + '</span><span>'
        + esc(L.spentLabel(day.seconds)) + '</span></div><div class="measure" aria-hidden="true"><i style="width:'
        + width + '%"></i></div></div>';
    }).join('');
  }

  function loadTime() {
    var today = L.localIso(new Date());
    var data = read('still.time', null);
    if (!data || !data.today) return { today: today, seconds: 0, history: L.seedHistory(today) };
    if (data.today !== today) {
      var history = (data.history || []).concat([{ date: data.today, seconds: Math.round(data.seconds || 0) }]).slice(-4);
      return { today: today, seconds: 0, history: history };
    }
    data.history = data.history && data.history.length ? data.history : L.seedHistory(today);
    return data;
  }

  function read(key, fallback) {
    try {
      var raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : fallback;
    } catch (e) { return fallback; }
  }
  function save(key, value) {
    try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* A full disk stays local. */ }
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (ch) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch];
    });
  }
})();
