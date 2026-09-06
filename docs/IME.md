# WaylandCraft IME (Input Method) Architecture

> Status: **scaffold only** (branch `feat/ime-scaffold`).  
> Runtime behavior is unchanged until P1/P2 land. This document is the
> roadmap for making Fcitx5 / fcitx5-lotus (and other IMEs) work inside
> nested Wayland sessions.

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

## Module layout (target)

```
native/src/ime/
  mod.rs              # ImeState facade, globals, handle_key / set_focus
  relay.rs            # Pure logic: serial, FIFO batches, done discipline
  text_input_v3.rs    # Server wire: zwp_text_input_manager_v3
  input_method_v2.rs  # Server wire: zwp_input_method_manager_v2
  host/               # Optional passthrough backends (P2+)
    mod.rs
    dbus_fcitx5.rs    # Preferred for Lotus / Fcitx5 users
    dbus_ibus.rs
    wayland_ti3.rs    # Client on host text-input-v3 when available
```

Scaffold in this PR only adds `mod.rs` with no-op methods wired from
`lib.rs` and `bridge.rs`.

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
Host Commit/Preedit → HostEvent FIFO → Relay → ti3 + done
```

### Key path integration points (existing code)

| Location | Role |
|----------|------|
| `bridge::keyboard_input` | Ask `ime.handle_key` before `seat.keyboard_key` |
| `bridge::keyboard_focus` | `ime.set_focus` / `clear_focus` after seat focus |
| `seat::keyboard_key` | Later: skip raw key if IME grab consumed it |

## Serial rules (must not be violated in P1)

| Direction | Counter | Meaning |
|-----------|---------|---------|
| Compositor → App | `done(serial)` | serial = commits received on that text_input object |
| IME → Compositor | `commit(serial)` | must equal dones sent on that input_method object |
| Focus A→B | deactivate A, clear pending, then activate B | never treat new enable as continuation of A |

Missing `done` after `commit_string`/`preedit_string` is a common cause of
"swallowed" characters (client buffers until `done`).

## Implementation phases

### P0 — Scaffold (this PR)

- [x] `docs/IME.md`
- [x] `native/src/ime/mod.rs` with `ImeState`
- [x] `WLCState.ime` + `create_globals` hook in `lib.rs`
- [x] Focus / key hooks in `bridge.rs` (no behavior change)

### P1 — Protocol + relay

- [ ] `relay.rs` pure state machine + unit tests
- [ ] Advertise `zwp_text_input_manager_v3` and `zwp_input_method_manager_v2`
- [ ] Do **not** advertise v1 managers (avoids IBus/Fcitx falling back to dead paths)
- [ ] Wire enable/disable, surrounding text, cursor rect
- [ ] Integration tests with mock ti3 client + mock im2 client if feasible

### P2 — `dbus-fcitx5` (highest value for Lotus)

- [ ] Backend using `org.fcitx.Fcitx5` DBus (InputContext, FocusIn/Out, key, CommitString, preedit signals)
- [ ] Probe order: `wayland-ti3` → `dbus-fcitx5` → `dbus-ibus` → Unsupported + log
- [ ] Async key submit without blocking the render thread (one-frame latency OK)

### P3 — UX

- [ ] `waylandcraft-ime.log`
- [ ] Clear Unsupported diagnostics
- [ ] Optional prefer-XWayland launch hints for stubborn native Wayland apps
- [ ] Candidate popup: short-term on host desktop; in-game render later

## Known limitations (even after full implementation)

1. **Nested host focus**: pure host Wayland IME without DBus may still miss nested surfaces; DBus backends mitigate this.
2. **IME popup surfaces** need extra rendering work to appear inside Minecraft.
3. **X11/XWayland apps** often work earlier via XIM than pure Wayland clients.

## References

- [Using Fcitx 5 on Wayland](https://www.fcitx-im.org/index.php?title=Using_Fcitx_5_on_Wayland)
- text-input-unstable-v3 / input-method-unstable-v2 protocol specs
- Related research in other nested-compositor projects (IBus focus isolation under Mutter/KWin)
