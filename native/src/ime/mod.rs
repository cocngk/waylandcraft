//! Input Method (IME) scaffold for WaylandCraft.
//!
//! Current status: **no-op safe**. Methods exist so seat/bridge can call into
//! a stable API while text-input-v3, input-method-v2, and dbus-fcitx5 are
//! implemented in later PRs. See `docs/IME.md`.
//!
//! # Planned modules
//! - `relay` — serial / FIFO / done discipline
//! - `text_input_v3` — server globals for apps
//! - `input_method_v2` — server globals for IME clients (e.g. fcitx5)
//! - `host` — dbus-fcitx5 / dbus-ibus / host ti3 passthrough

use smithay::reexports::wayland_server::{
    DisplayHandle,
    protocol::wl_keyboard::KeyState,
    protocol::wl_surface::WlSurface,
};

/// Backend selected for host-side IME passthrough (P2+).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ImeBackendKind {
    /// No host backend; only in-compositor paths (none active in scaffold).
    #[default]
    None,
    /// Host Wayland text-input-v3 client (when host exposes the global).
    WaylandTextInputV3,
    /// Fcitx5 over session DBus — preferred for fcitx5-lotus.
    DbusFcitx5,
    /// IBus over session DBus.
    DbusIbus,
}

/// Central IME state attached to [`crate::WLCState`].
#[derive(Debug)]
pub struct ImeState {
    /// Surface that currently holds text focus, if any.
    focused: Option<WlSurface>,
    /// Whether an IME keyboard grab would consume keys (always false in scaffold).
    grab_active: bool,
    /// Selected host backend after probe (always None until P2).
    backend: ImeBackendKind,
}

impl Default for ImeState {
    fn default() -> Self {
        Self::new()
    }
}

impl ImeState {
    pub fn new() -> Self {
        Self {
            focused: None,
            grab_active: false,
            backend: ImeBackendKind::None,
        }
    }

    /// Register Wayland globals for text-input / input-method.
    ///
    /// Scaffold: intentionally empty so we do not advertise incomplete
    /// protocols. P1 will create manager_v3 / manager_v2 here.
    pub fn create_globals(&mut self, _dh: &DisplayHandle) {
        // P1: text_input_v3::create_global + input_method_v2::create_global
        // Do not advertise v1 managers.
    }

    /// Called when seat keyboard focus moves to `surface`.
    pub fn set_focus(&mut self, surface: WlSurface) {
        self.focused = Some(surface);
        // P1: ti3.enter for all text_input objects of this client
        // P1/P2: Relay activate / host FocusIn
    }

    /// Called when seat keyboard focus is cleared.
    pub fn clear_focus(&mut self) {
        self.focused = None;
        self.grab_active = false;
        // P1: ti3.leave, Relay deactivate, clear pending batches
        // P2: host FocusOut
    }

    /// Whether the IME layer wants exclusive handling of this key.
    ///
    /// Scaffold always returns `false` so existing `wl_keyboard` path runs.
    /// P1/P2: return true when grab is active or host backend consumed the key.
    pub fn handle_key(&mut self, _key: u32, _state: KeyState) -> bool {
        if !self.grab_active {
            return false;
        }
        // P1: forward to input_method keyboard grab
        // P2: HostBackend::submit_key → maybe consume
        false
    }

    pub fn focused_surface(&self) -> Option<&WlSurface> {
        self.focused.as_ref()
    }

    pub fn backend(&self) -> ImeBackendKind {
        self.backend
    }

    pub fn grab_active(&self) -> bool {
        self.grab_active
    }
}
