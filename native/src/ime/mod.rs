//! Input Method (IME) scaffold for WaylandCraft.
//!
//! - **P0**: no-op-safe API wired from seat/bridge (`handle_key` does not consume).
//! - **P1 (this module + `relay`)**: pure serial/FIFO state machine with tests.
//!   Wayland globals still not advertised until wire adapters land.
//! - **P2**: dbus-fcitx5 / host backends.
//!
//! See `docs/IME.md`.

mod relay;

pub use relay::{
    CommitOutcome, FlushBatch, ImeOp, Relay, RelayCommand, RelayEndpoint,
};

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

impl ImeBackendKind {
    fn to_endpoint(self) -> RelayEndpoint {
        match self {
            ImeBackendKind::None => RelayEndpoint::None,
            ImeBackendKind::WaylandTextInputV3
            | ImeBackendKind::DbusFcitx5
            | ImeBackendKind::DbusIbus => RelayEndpoint::Host,
        }
    }
}

/// Central IME state attached to [`crate::WLCState`].
#[derive(Debug)]
pub struct ImeState {
    /// Surface that currently holds text focus, if any.
    focused: Option<WlSurface>,
    /// Whether an IME keyboard grab would consume keys (always false until wire).
    grab_active: bool,
    /// Selected host backend after probe (always None until P2).
    backend: ImeBackendKind,
    /// Pure protocol relay (serial / FIFO / lifecycle).
    relay: Relay,
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
            relay: Relay::new(),
        }
    }

    /// Register Wayland globals for text-input / input-method.
    ///
    /// Still empty: do not advertise incomplete protocols. Next step is
    /// `text_input_v3` + `input_method_v2` wire modules.
    pub fn create_globals(&mut self, _dh: &DisplayHandle) {
        // P1 wire: text_input_v3::create_global + input_method_v2::create_global
        // Do not advertise v1 managers.
    }

    /// Called when seat keyboard focus moves to `surface`.
    pub fn set_focus(&mut self, surface: WlSurface) {
        // Focus A→B: tear down previous session before tracking the new surface.
        if self.focused.is_some() {
            let _ = self.relay.focus_lost();
        }
        self.focused = Some(surface);
        // Wire (later): ti3.enter for this client's text_input objects.
        // If app already enabled, relay.on_app_enable will run from enable request.
    }

    /// Called when seat keyboard focus is cleared.
    pub fn clear_focus(&mut self) {
        self.focused = None;
        self.grab_active = false;
        let _ = self.relay.focus_lost();
        // Wire (later): ti3.leave; P2 host FocusOut
    }

    /// App signaled text-input enable (will be called from ti3 dispatch).
    pub fn on_text_input_enable(&mut self) -> Option<RelayCommand> {
        let endpoint = match self.backend.to_endpoint() {
            RelayEndpoint::None => RelayEndpoint::InCompositor,
            other => other,
        };
        self.relay.on_app_enable(endpoint)
    }

    /// App signaled text-input disable.
    pub fn on_text_input_disable(&mut self) -> Option<RelayCommand> {
        self.relay.on_app_disable()
    }

    /// Whether the IME layer wants exclusive handling of this key.
    ///
    /// Still always `false` until grab / host backend is implemented.
    pub fn handle_key(&mut self, _key: u32, _state: KeyState) -> bool {
        if !self.grab_active {
            return false;
        }
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

    pub fn relay(&self) -> &Relay {
        &self.relay
    }

    pub fn relay_mut(&mut self) -> &mut Relay {
        &mut self.relay
    }
}
