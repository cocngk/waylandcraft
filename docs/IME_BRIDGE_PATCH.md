# Bridge hooks (apply to `native/src/bridge.rs`)

The full `bridge.rs` is large; the scaffold only needs these two edits.

## 1. `keyboard_focus`

Replace:

```rust
    match surface {
        Some(s) => instance.state.seat.keyboard_focus(s),
        None => instance.state.seat.keyboard_unfocus(),
    };
```

With:

```rust
    match surface {
        Some(s) => {
            instance.state.seat.keyboard_focus(s.clone());
            // IME scaffold: track text focus (no-op protocol until P1)
            instance.state.ime.set_focus(s);
        }
        None => {
            instance.state.seat.keyboard_unfocus();
            instance.state.ime.clear_focus();
        }
    };
```

## 2. `keyboard_input`

Replace:

```rust
    instance.state.seat.keyboard_key(scancode, action);
```

With:

```rust
    // IME scaffold: if handle_key returns true, skip raw wl_keyboard delivery.
    // Currently always false (no grab / no host backend).
    if !instance.state.ime.handle_key(scancode, action) {
        instance.state.seat.keyboard_key(scancode, action);
    }
```

Without these hooks the module still builds; keys continue to use the existing path only.
