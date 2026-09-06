# WaylandCraft IME (Input Method) Architecture

> Status: **P0 scaffold + P1 relay logic** (branch `feat/ime-scaffold`).  
> Wayland globals are still **not** advertised; `handle_key` does not consume keys.  
> Runtime keyboard path is unchanged until wire adapters (ti3/im2) land.

## Why IME fails today

WaylandCraft is a **nested compositor** (Minecraft on the host compositor,
apps on an inner Wayland server). Upstream EVV1E only implements raw
`wl_keyboard`. There is no `zwp_text_input_v3` / `zwp_input_method_v2`.

Fcitx5 (and Lotus as an Fcitx5 engine) needs:

1. Apps to speak **text-input-v3** (or an IM module / XIM path).
2. The compositor (or a host bridge) to speak **input-method-v2** or DBus.
3. Correct **serial / `done` / focus lifecycle** so commit/preedit are not dropped.

Nested focus is also isolated: the **host** IME often never sees focus
changes inside WaylandCraft. That is an ecosystem limitation; a **DBus
Fcitx5 backend** bypasses it by talking to Fcitx5 directly.

## Module layout

```
native/src/ime/
  mod.rs              # ImeState facade — DONE (scaffold)
  relay.rs            # Pure logic: serial, FIFO, lifecycle — DONE (unit tests)
  text_input_v3.rs    # Server wire — TODO
  input_method_v2.rs  # Server wire — TODO
  host/               # Passthrough backends — TODO (P2)
    dbus_fcitx5.rs    # Preferred for Lotus / Fcitx5
    dbus_ibus.rs
    wayland_ti3.rs
```

## Data flow (target)

### In-compositor IME (endpoint A)

```
App enable → Relay Activate → im2 activate + done
IME preedit/commit/delete + commit(serial)
  → Relay checks serial → flush FIFO → ti3 events + done(commit_count)
```

### Host passthrough (endpoint B, default for nested)

```
App enable → host backend FocusIn / Activate
Keys → ProcessKeyEvent (DBus) or host ti3
Host Commit/Preedit → flush_host_batch → ti3 + done
```

### Key path integration points

| Location | Role |
|----------|------|
| `bridge::keyboard_input` | Ask `ime.handle_key` before `seat.keyboard_key` |
| `bridge::keyboard_focus` | `ime.set_focus` / `clear_focus` after seat focus |
| `seat::keyboard_key` | Later: skip raw key if IME grab consumed it |

Apply bridge hooks from `docs/IME_BRIDGE_PATCH.md` if not already applied.

## Serial rules (implemented in `relay.rs`)

| Direction | Counter | Meaning |
|-----------|---------|---------|
| Compositor → App | `done_serial` on [`FlushBatch`] | for `ti3.done(serial)` |
| IME → Compositor | `ime_done_count` | IME `commit(serial)` must match or batch is **dropped** |
| Focus A→B | `focus_lost` then new enable | pending cleared; deactivate bumps IME serial |

Missing `done` after commit/preedit is a classic cause of swallowed characters.

### Run relay unit tests

```bash
cd native && cargo test --lib ime::relay
```

## Implementation phases

### P0 — Scaffold

- [x] `docs/IME.md`
- [x] `native/src/ime/mod.rs` with `ImeState`
- [x] `WLCState.ime` + `create_globals` hook in `lib.rs`
- [x] Bridge focus/key hook instructions (`IME_BRIDGE_PATCH.md`)

### P1 — Protocol + relay

- [x] `relay.rs` pure state machine + unit tests
- [ ] Advertise `zwp_text_input_manager_v3` and `zwp_input_method_manager_v2`
- [ ] Do **not** advertise v1 managers
- [ ] Wire enable/disable, surrounding text, cursor rect to relay
- [ ] Integration tests with mock ti3 + im2 clients (optional)

### P2 — `dbus-fcitx5` (highest value for Lotus)

- [ ] Backend using `org.fcitx.Fcitx5` DBus
- [ ] Probe order: `wayland-ti3` → `dbus-fcitx5` → `dbus-ibus` → Unsupported
- [ ] Async key submit without blocking the render thread

### P3 — UX

- [ ] `waylandcraft-ime.log`
- [ ] Clear Unsupported diagnostics
- [ ] Optional prefer-XWayland launch hints
- [ ] Candidate popup (host short-term; in-game later)

## Known limitations

1. **Nested host focus**: pure host Wayland IME without DBus may miss nested surfaces; DBus backends mitigate this.
2. **IME popup surfaces** need extra rendering to appear inside Minecraft.
3. **X11/XWayland apps** often work earlier via XIM than pure Wayland clients.

## References

- [Using Fcitx 5 on Wayland](https://www.fcitx-im.org/index.php?title=Using_Fcitx_5_on_Wayland)
- text-input-unstable-v3 / input-method-unstable-v2 protocol specs
