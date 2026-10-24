// the app posts a pack here, the script keeps it on drive and mails the owner
// a pull request is opened only from the review page; needs script property GITHUB_TOKEN

const REPO = 'Restor-0o0/GameReel';
const BASE = 'main';
const MAX_CHARS = 5000000;
const MAX_PER_HOUR = 20;
const FOLDER = 'GameReel submissions';
const IMAGE_SOURCES = [
  'https://cdn.cloudflare.steamstatic.com/', 'https://cdn.akamai.steamstatic.com/',
  'https://community.cloudflare.steamstatic.com/economy/image/',
  'https://community.akamai.steamstatic.com/economy/image/',
  'https://ddragon.leagueoflegends.com/cdn/', 'https://media.valorant-api.com/',
  'https://raw.githubusercontent.com/pubg/api-assets/',
  'https://raw.githubusercontent.com/ByMykel/counter-strike-image-tracker/',
];
const RESERVED = ['fixture', 'steam_broad', 'steam_alltime_owners', 'google_play_alltime_like', 'curated_v5',
  'itch_selected', 'steam_selected', 'personal_games'];

function doPost(e) {
  try {
    return reply({ ok: true, url: submit((e && e.postData && e.postData.contents) || '') });
  } catch (error) {
    return reply({ ok: false, error: String(error && error.message || error) });
  }
}

function doGet(e) {
  const id = e && e.parameter && e.parameter.review;
  if (!id) return reply({ ok: true, service: 'gamereel-submissions' });
  if (e.parameter.key !== reviewKey() || !/^[A-Za-z0-9_-]{10,100}$/.test(id)) return HtmlService.createHtmlOutput('Not found');
  const page = HtmlService.createTemplate(REVIEW_PAGE);
  page.id = id;
  page.key = e.parameter.key;
  page.info = describe(readSubmission(id));
  return page.evaluate().setTitle('GameReel submission');
}

function reply(body) {
  return ContentService.createTextOutput(JSON.stringify(body)).setMimeType(ContentService.MimeType.JSON);
}

function submit(raw) {
  if (raw.length > MAX_CHARS) throw new Error('pack is larger than 5 MB');
  let request;
  try { request = JSON.parse(raw); } catch (_) { throw new Error('invalid JSON'); }
  const pack = clean(request && request.pack);
  const note = typeof request.note === 'string' ? request.note.slice(0, 500) : '';
  const text = JSON.stringify(pack, null, 1) + '\n';

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const cache = CacheService.getScriptCache();
    const digest = Utilities.base64EncodeWebSafe(
      Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, text, Utilities.Charset.UTF_8));
    if (cache.get('sent:' + digest)) throw new Error('this pack was already sent');
    const hour = 'hour:' + Math.floor(Date.now() / 3600000);
    const count = Number(cache.get(hour) || 0);
    if (count >= MAX_PER_HOUR) throw new Error('too many submissions, try again in an hour');
    store(pack, text, note);
    cache.put(hour, String(count + 1), 3600);
    cache.put('sent:' + digest, '1', 21600);
    return '';
  } finally {
    lock.releaseLock();
  }
}

function str(value, limit, label) {
  if (typeof value !== 'string' || !value.trim() || value.length > limit) throw new Error('bad ' + label);
  return value.trim();
}

function clean(pack) {
  const library = pack && pack.library;
  if (!library || typeof library !== 'object') throw new Error('library object is required');
  const id = str(library.id, 100, 'library id');
  if (!/^[A-Za-z0-9_.-]{1,100}$/.test(id) || RESERVED.indexOf(id) >= 0) throw new Error('bad library id');
  const version = Number.isInteger(library.version) && library.version > 0 ? library.version : 1;

  if (!Array.isArray(library.slots) || library.slots.length < 1 || library.slots.length > 50)
    throw new Error('slots must hold 1..50 entries');
  const slots = library.slots.map(function (slot) {
    const out = { id: str(slot && slot.id, 60, 'slot id'), name: str(slot.name, 60, 'slot name') };
    if (!/^[A-Za-z0-9_.-]{1,60}$/.test(out.id)) throw new Error('bad slot id ' + out.id);
    if (typeof slot.icon === 'string' && slot.icon) out.icon = slot.icon.slice(0, 16);
    if (slot.parent) out.parent = str(slot.parent, 60, 'slot parent');
    if (slot.unique === false) out.unique = false;
    return out;
  });
  const parents = {};
  slots.forEach(function (slot) {
    if (slot.id in parents) throw new Error('duplicate slot ' + slot.id);
    parents[slot.id] = slot.parent || null;
  });
  Object.keys(parents).forEach(function (slotId) {
    const seen = {};
    for (let current = parents[slotId]; current; current = parents[current]) {
      if (!(current in parents) || seen[current]) throw new Error('slot ' + slotId + ' has a bad parent');
      seen[current] = true;
    }
  });

  if (!Array.isArray(library.reels) || library.reels.length < 1 || library.reels.length > 100 ||
      library.reels.some(function (reel) { return !(reel in parents); }))
    throw new Error('reels must hold 1..100 declared slot ids');

  const tags = library.standaloneTags;
  if (!Array.isArray(tags) || tags.length > 10000) throw new Error('up to 10000 tags');
  const counts = {};
  const cleanTags = tags.map(function (tag) {
    const out = { name: str(tag && tag.name, 120, 'tag name'), slot: str(tag.slot, 60, 'tag slot') };
    if (!(out.slot in parents)) throw new Error('tag ' + out.name + ' has an unknown slot');
    counts[out.slot] = (counts[out.slot] || 0) + 1;
    if (Array.isArray(tag.requires) && tag.requires.length)
      out.requires = tag.requires.slice(0, 200).map(function (name) { return str(name, 120, 'requires'); });
    if (typeof tag.description === 'string' && tag.description.trim())
      out.description = tag.description.trim().slice(0, 500);
    if (tag.image !== undefined && tag.image !== null) {
      const image = String(tag.image);
      if (image.length > 600 || !IMAGE_SOURCES.some(function (p) { return image.indexOf(p) === 0; }))
        throw new Error('tag ' + out.name + ' has an image outside the allowed servers');
      out.image = image;
    }
    return out;
  });
  library.reels.forEach(function (reel) {
    if (!counts[reel]) throw new Error('slot ' + reel + ' has no tags');
  });

  const result = { library: { id: id, name: str(library.name, 80, 'library name'), version: version,
    slots: slots, reels: library.reels.slice(), standaloneTags: cleanTags } };
  if (typeof pack.source_url === 'string' && /^https:\/\/\S{1,500}$/.test(pack.source_url))
    result.source_url = pack.source_url;
  return result;
}

function github(method, path, body) {
  const token = PropertiesService.getScriptProperties().getProperty('GITHUB_TOKEN');
  if (!token) throw new Error('submissions are not configured');
  const response = UrlFetchApp.fetch('https://api.github.com/repos/' + REPO + path, {
    method: method,
    contentType: 'application/json',
    payload: body ? JSON.stringify(body) : undefined,
    headers: { Authorization: 'Bearer ' + token, Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28' },
    muteHttpExceptions: true,
  });
  const code = response.getResponseCode();
  const data = response.getContentText() ? JSON.parse(response.getContentText()) : {};
  return { code: code, data: data };
}

function expect(result, label) {
  if (result.code >= 300) throw new Error(label + ' failed (' + result.code + ')');
  return result.data;
}

function reviewKey() {
  const props = PropertiesService.getScriptProperties();
  let key = props.getProperty('REVIEW_KEY');
  if (!key) {
    key = Utilities.getUuid().replace(/-/g, '');
    props.setProperty('REVIEW_KEY', key);
  }
  return key;
}

function folder() {
  const found = DriveApp.getFoldersByName(FOLDER);
  return found.hasNext() ? found.next() : DriveApp.createFolder(FOLDER);
}

function store(pack, text, note) {
  const file = folder().createFile(pack.library.id + '.json', text, 'application/json');
  file.setDescription(JSON.stringify({ note: note }));
  const info = describe({ pack: pack, note: note });
  const link = ScriptApp.getService().getUrl() + '?review=' + file.getId() + '&key=' + reviewKey();
  const html = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  MailApp.sendEmail({
    to: Session.getEffectiveUser().getEmail(),
    subject: 'GameReel: new pack "' + pack.library.name.slice(0, 80) + '"',
    htmlBody: '<pre>' + html(info) + '</pre><p><a href="' + link + '">Review and publish</a> · ' +
      '<a href="' + file.getUrl() + '">File on Drive</a></p>',
    attachments: [Utilities.newBlob(text, 'application/json', pack.library.id + '.json')],
  });
}

function readSubmission(id) {
  const file = DriveApp.getFileById(id);
  if (!file.getParents().hasNext() || file.getParents().next().getName() !== FOLDER) throw new Error('not found');
  const meta = JSON.parse(file.getDescription() || '{}');
  return { pack: JSON.parse(file.getBlob().getDataAsString('UTF-8')), note: meta.note || '',
    published: meta.published || '' };
}

function describe(data) {
  const library = data.pack.library;
  const reels = library.reels.map(function (reel) {
    const slot = library.slots.filter(function (s) { return s.id === reel; })[0];
    return (slot.icon ? slot.icon + ' ' : '') + slot.name;
  });
  const counts = {};
  library.standaloneTags.forEach(function (tag) { counts[tag.slot] = (counts[tag.slot] || 0) + 1; });
  return [
    'Name: ' + library.name,
    'Id: ' + library.id + ' (version ' + library.version + ')',
    'Reels: ' + reels.join(', '),
    'Tags: ' + library.slots.map(function (s) { return s.name + ' ' + (counts[s.id] || 0); }).join(', '),
    data.note ? 'Note: ' + data.note : null,
    data.published ? 'Published: ' + data.published : null,
  ].filter(function (line) { return line !== null; }).join('\n');
}

function publish(id, key) {
  if (key !== reviewKey()) throw new Error('not allowed');
  const data = readSubmission(id);
  if (data.published) return data.published;
  const pack = clean(data.pack);
  const url = openPullRequest(pack, JSON.stringify(pack, null, 1) + '\n', data.note || '');
  DriveApp.getFileById(id).setDescription(JSON.stringify({ note: data.note, published: url }));
  return url;
}

const REVIEW_PAGE = `<!doctype html><html><body style="font-family:sans-serif;max-width:720px;margin:24px auto">
<pre style="white-space:pre-wrap"><?= info ?></pre>
<button id="go" onclick="go()">Open pull request</button> <span id="out"></span>
<script>
function go() {
  document.getElementById('go').disabled = true;
  document.getElementById('out').textContent = '...';
  google.script.run
    .withSuccessHandler(function (url) {
      var a = document.createElement('a'); a.href = url; a.target = '_blank'; a.textContent = url;
      var out = document.getElementById('out'); out.textContent = ''; out.appendChild(a);
    })
    .withFailureHandler(function (e) {
      document.getElementById('out').textContent = e.message;
      document.getElementById('go').disabled = false;
    })
    .publish('<?!= id ?>', '<?!= key ?>');
}
</script></body></html>`;

function openPullRequest(pack, text, note) {
  const library = pack.library;
  const slug = library.id.toLowerCase().replace(/[^a-z0-9-]+/g, '-').replace(/^-+|-+$/g, '').slice(0, 50) || 'pack';
  const stamp = Utilities.formatDate(new Date(), 'UTC', 'yyyyMMdd-HHmmss');
  const branch = 'submissions/' + slug + '-' + stamp;
  const path = 'community/packs/' + slug + '.json';

  const head = expect(github('get', '/git/ref/heads/' + BASE), 'reading ' + BASE);
  expect(github('post', '/git/refs', { ref: 'refs/heads/' + branch, sha: head.object.sha }), 'creating branch');
  const existing = github('get', '/contents/' + path + '?ref=' + encodeURIComponent(branch));
  const file = { message: 'Add pack ' + library.name, branch: branch,
    content: Utilities.base64Encode(text, Utilities.Charset.UTF_8) };
  if (existing.code === 200 && existing.data.sha) file.sha = existing.data.sha;
  expect(github('put', '/contents/' + path, file), 'writing pack');

  const quiet = function (value) { return value.replace(/@/g, '@​').replace(/[<>]/g, ''); };
  const reels = library.reels.map(function (reel) {
    const slot = library.slots.filter(function (s) { return s.id === reel; })[0];
    return (slot.icon ? slot.icon + ' ' : '') + slot.name;
  });
  const body = [
    'Pack submitted from the app and reviewed.',
    '',
    '- **Name:** ' + quiet(library.name),
    '- **Id:** `' + library.id + '`',
    '- **Reels:** ' + quiet(reels.join(', ')),
    '- **Tags:** ' + library.standaloneTags.length,
    existing.code === 200 ? '- **Replaces** the existing `' + path + '`' : null,
    note ? '\n> ' + quiet(note).replace(/\n/g, '\n> ') : null,
  ].filter(function (line) { return line !== null; }).join('\n');
  const pull = expect(github('post', '/pulls', { title: 'Pack: ' + library.name.slice(0, 80), head: branch,
    base: BASE, body: body, maintainer_can_modify: true }), 'opening pull request');
  github('post', '/issues/' + pull.number + '/labels', { labels: ['pack-submission'] });
  return pull.html_url;
}
