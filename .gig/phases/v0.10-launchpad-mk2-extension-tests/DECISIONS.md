# Decisions

> Append-only log. Never delete entries — amend or revise instead.
> Active decisions for the current phase live here.
> When a phase completes, its decisions are archived to `phases/`.

## 2026-03-11 — Testing Strategy: How to test LaunchpadMk2Extension behavioral logic

**Decision:** Use real constructor with mock ControllerHost, mock all Bitwig API interfaces via Mockito, and test flush/onMidi behavior directly on the extension instance.
**Rationale:** `Extension.getHost()` returns the constructor-injected host (final field). All Bitwig API types (TrackBank, Transport, MidiOut, etc.) are interfaces — fully mockable. The real `LaunchpadMk2ExtensionDefinition` works as-is. No refactoring needed.
**Alternatives considered:** (1) Extract logic into testable helper classes — invasive refactoring for a 569-line class, not justified. (2) Spy on the extension — unnecessary since we can construct it directly with mocks.
**Status:** ACTIVE
**ID:** D-10.1

## 2026-03-11 — Init Wiring: How to handle init() setup in tests

**Decision:** Create a shared test helper method that calls `init()` after configuring all mock stubs. Use `@BeforeEach` to set up the full mock chain: `host.getMidiInPort(0)` → mockMidiIn, `host.getMidiOutPort(0)` → mockMidiOut, `host.createMainTrackBank(8,0,8)` → mockTrackBank with 8 tracks each having 8 clip slots, etc. Use `Strictness.LENIENT` since many mocks exist only for init wiring.
**Rationale:** `init()` calls ~20 factory methods and registers observers on every slot, track, scene, transport, and cursor. All must be stubbed before `init()` runs or it throws NPE. A shared setup that mirrors the real init chain is the cleanest approach.
**Alternatives considered:** (1) Test without calling init — fields like `midiOut`, `trackBank` would be null, making flush/onMidi untestable. (2) Mock only what each test needs — too brittle, every test would need its own init stub set.
**Status:** ACTIVE
**ID:** D-10.2

## 2026-03-11 — Test Scope: Which behavioral areas to cover

**Decision:** Cover three test classes: (1) **FlushTest** — grid LED rendering for all 6 clip states (recording, queued, playing, has-content, armed-empty, empty), scene launch LED logic (allPlaying, anyQueued, anyPlaying, idle), top row LED logic for all 3 utility modes, and dirty-flag/cache optimization. (2) **MidiInputTest** — grid pad press (launch/stop toggle), scene launch button (launch-all vs stop-all), CC navigation (scroll scene bank, select track), utility mode cycling, and all 9 modal actions (3 modes × 3 buttons). (3) **InitTest** — verify markInterested/observer registration count, SysEx startup messages, cache initialization to -1.
**Rationale:** These three areas map directly to the three `@Override` methods (`init`, `flush`, `exit`) plus `onMidi`. Together they cover every code path in the extension. Separating into three classes keeps each focused and readable.
**Alternatives considered:** (1) Single test class — too large (60+ tests). (2) Per-method test classes (flushGrid, flushSceneLaunch, flushTopRow) — over-fragmented, these share setup.
**Status:** ACTIVE
**ID:** D-10.3

## 2026-03-11 — Mock Depth: How deep to stub the Bitwig API chain

**Decision:** Create mock chains 3 levels deep: TrackBank → Track → ClipLauncherSlotBank → ClipLauncherSlot, with each slot providing mockable `BooleanValue` and `ColorValue` objects. Use a helper that creates a pre-wired 8×8 grid of slot mocks with configurable state. Scene mocks get their own ColorValue. Transport and CursorTrack get BooleanValue mocks for all observed properties.
**Rationale:** The extension accesses `trackBank.getItemAt(t).clipLauncherSlotBank().getItemAt(s).isPlaying().get()` — 4 levels of chaining. Each level must return a non-null mock. Pre-building the grid avoids repeating this in every test. BooleanValue/ColorValue mocks need `markInterested()` (no-op) and `addValueObserver()` (capture or no-op) stubbed.
**Alternatives considered:** (1) Shallow mocks + `RETURNS_DEEP_STUBS` — Mockito's deep stubs don't handle `addValueObserver` well and make assertions harder. (2) Mock only accessed slots — fragile if test touches multiple grid positions.
**Status:** ACTIVE
**ID:** D-10.4

## 2026-03-11 — Observer Capture: How to trigger state changes for flush testing

**Decision:** Don't capture observers. Instead, directly configure mock return values before calling `flush()`. Since `flush()` reads state via `.get()` calls (not cached observer values), we can stub `slot.isPlaying().get()` → `true` at any point before flush. Set `ledsDirty = true` via reflection or by calling `init()` which sets it.
**Rationale:** The extension's observers all just call `markDirty()` — they don't cache values. All state reads happen during `flush()` via `.get()`. This means tests can simply configure mock state and call flush directly without needing to capture and fire observers.
**Alternatives considered:** (1) Capture observers via `ArgumentCaptor` and fire them — more realistic but unnecessary complexity since flush reads live values, not cached ones.
**Status:** ACTIVE
**ID:** D-10.5
