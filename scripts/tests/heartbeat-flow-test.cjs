const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const pagePath = process.argv[2];
if (!pagePath) throw new Error('Usage: node scripts/tests/heartbeat-flow-test.cjs PATH_TO_NewMarsHeartBeat_index.html');
const html = fs.readFileSync(pagePath, 'utf8');
const connection = html.slice(html.indexOf('    function connect() {'), html.indexOf('    const heartPanel ='));
const restore = html.slice(html.indexOf('    function initAllSettings() {'), html.indexOf("    connectBtn.addEventListener('click', connect)"));

function page(savedAddress) {
  const store = new Map(savedAddress ? [['wsUrl', savedAddress]] : []);
  const listeners = {};
  let now = 1000;
  const context = {
    wsUrl: {value: 'ws://192.168.31.26:8001'},
    ws: null, connectTimeout: null, beatInterval: null, reconnectTimer: null,
    console: {log() {}, warn() {}, error() {}},
    localStorage: {getItem: k => store.get(k) ?? null, setItem: (k,v) => store.set(k,v)},
    bpmEl: {addEventListener: (kind, cb) => listeners[kind] = cb},
    t: key => key, promptCalls: 0, alerts: [], sockets: [], answer: null,
    setStatus() {}, setConnectButton() {}, clearTimeout() {}, clearInterval() {},
    setTimeout: () => 1,
    Date: class { getTime() { return now; } },
    prompt: () => {context.promptCalls++; return context.answer;},
    alert: message => context.alerts.push(message),
    WebSocket: class {
      constructor(address) {
        const url = new URL(address);
        if (!['ws:', 'wss:'].includes(url.protocol) || url.hash) throw new SyntaxError('Invalid WebSocket URL');
        context.sockets.push(address); this.readyState = 0;
      }
      close() {}
      static OPEN = 1;
    },
  };
  vm.createContext(context);
  vm.runInContext(connection + restore, context);
  const click = at => { now = at; listeners.click({preventDefault() {}, stopPropagation() {}}); };
  return {context, store, click};
}

let passed = 0;
function test(name, fn) {fn(); passed++; console.log('PASS ' + name);}
test('two click events inside 300ms call prompt; separated clicks do not', () => {
  const p = page(); p.click(1000); p.click(1400); assert.equal(p.context.promptCalls, 0);
  p.click(1500); assert.equal(p.context.promptCalls, 1);
});
for (const answer of [null, '', '   ']) {
  test('cancel/blank does not save or reconnect: ' + JSON.stringify(answer), () => {
    const p = page('ws://old.local:8001'); p.context.answer = answer; p.click(1000); p.click(1100);
    assert.equal(p.store.get('wsUrl'), 'ws://old.local:8001'); assert.equal(p.context.sockets.length, 0);
    assert.equal(p.context.alerts.length, 0);
  });
}
for (const address of ['ws://192.168.1.2:8001', 'wss://example.test/socket']) {
  test('confirm trims, reconnects, saves, alerts, and restores: ' + address, () => {
    const p = page(); p.context.answer = ' ' + address + ' '; p.click(1000); p.click(1100);
    assert.equal(p.store.get('wsUrl'), address); assert.equal(p.context.sockets[0], address);
    assert.equal(p.context.alerts[0], 'alertAddressSaved');
    p.context.wsUrl.value = 'changed'; vm.runInContext('initAllSettings()', p.context);
    assert.equal(p.context.wsUrl.value, address);
  });
}
test('repeated double click can reopen and cancel without replacing saved address', () => {
  const p = page(); p.context.answer = 'ws://host.local:8001'; p.click(1000); p.click(1100);
  p.context.answer = null; p.click(2000); p.click(2100);
  assert.equal(p.context.promptCalls, 2); assert.equal(p.context.sockets.length, 1);
  assert.equal(p.store.get('wsUrl'), 'ws://host.local:8001');
});
test('existing webpage limitation: malformed non-empty URL is persisted before constructor rejects it', () => {
  const p = page(); p.context.answer = 'not a websocket'; p.click(1000);
  assert.throws(() => p.click(1100), TypeError);
  assert.equal(p.store.get('wsUrl'), 'not a websocket'); assert.equal(p.context.alerts.length, 0);
});
console.log(`${passed} tests passed. WebSocket and DOM adapters are mocked; no Android touch/IME or network validation.`);
