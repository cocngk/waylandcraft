//! Pure-logic IME relay: serial discipline, FIFO batches, focus lifecycle.
//!
//! Zero Wayland types. Wire adapters (text_input_v3 / input_method_v2 / host
//! backends) translate protocol events into [`ImeOp`] / [`RelayCommand`] and
//! apply [`FlushBatch`] results.
//!
//! # Serial rules (must hold)
//!
//! - **App side (`done_serial`)**: incremented each time we flush a batch to the
//!   app (`done(serial)`). Matches text-input-v3 "number of commits received".
//! - **IME side (`ime_done_count`)**: incremented on every activate / deactivate /
//!   state push. IME `commit(serial)` must equal this count or the batch is dropped.
//! - Focus A→B: always `focus_lost` (deactivate + clear pending) before a new
//!   activate for B.

use std::collections::VecDeque;

/// Operations queued from an IME (or host passthrough) before a validated commit.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum ImeOp {
    /// Replace preedit (composing) text. Empty string clears preedit.
    Preedit {
        text: String,
        /// Cursor offset inside preedit in bytes, if known.
        cursor: Option<i32>,
    },
    /// Commit final text to the application.
    Commit { text: String },
    /// Delete surrounding text relative to the cursor (before, after) in bytes.
    DeleteSurrounding { before: u32, after: u32 },
}

/// Outbound commands the wire layer must send to the current IME endpoint.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RelayCommand {
    /// Start an IME session for the focused text field.
    Activate,
    /// End the IME session; discard any incomplete composition on the IME side.
    Deactivate,
}

/// A batch ready to apply on the app (text-input) side, in protocol order.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct FlushBatch {
    /// Operations in FIFO order: delete → commit → preedit within one cycle.
    pub ops: Vec<ImeOp>,
    /// Serial for `zwp_text_input_v3.done(serial)`.
    pub done_serial: u32,
}

/// Result of feeding an IME `commit(serial)`.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum CommitOutcome {
    /// Serial matched; apply this batch then send `done`.
    Apply(FlushBatch),
    /// Serial mismatched; pending cleared, app must not change state.
    Dropped {
        expected: u32,
        got: u32,
    },
}

/// Endpoint that currently drives the relay (in-compositor IME vs host bridge).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RelayEndpoint {
    #[default]
    None,
    /// In-game input-method-v2 client (e.g. fcitx5 connected to our compositor).
    InCompositor,
    /// Host passthrough (dbus-fcitx5 / dbus-ibus / host ti3).
    Host,
}

/// Pure relay state machine.
#[derive(Debug)]
pub struct Relay {
    /// Whether the focused surface has an active text-input session.
    active: bool,
    /// App-side done serial (commits flushed to the text_input object).
    done_serial: u32,
    /// IME-side count of activate/deactivate/state `done`s sent to the IME.
    ime_done_count: u32,
    /// Pending ops until a matching `commit(serial)`.
    pending: VecDeque<ImeOp>,
    /// Which endpoint owns the session.
    endpoint: RelayEndpoint,
    /// App reported surrounding text (for reverse sync to IME later).
    surrounding: String,
    surrounding_cursor: u32,
    surrounding_anchor: u32,
}

impl Default for Relay {
    fn default() -> Self {
        Self::new()
    }
}

impl Relay {
    pub fn new() -> Self {
        Self {
            active: false,
            done_serial: 0,
            ime_done_count: 0,
            pending: VecDeque::new(),
            endpoint: RelayEndpoint::None,
            surrounding: String::new(),
            surrounding_cursor: 0,
            surrounding_anchor: 0,
        }
    }

    pub fn is_active(&self) -> bool {
        self.active
    }

    pub fn done_serial(&self) -> u32 {
        self.done_serial
    }

    pub fn ime_done_count(&self) -> u32 {
        self.ime_done_count
    }

    pub fn endpoint(&self) -> RelayEndpoint {
        self.endpoint
    }

    pub fn pending_len(&self) -> usize {
        self.pending.len()
    }

    /// App enabled text-input on the focused surface.
    ///
    /// Returns [`RelayCommand::Activate`] so the wire layer can notify the IME.
    pub fn on_app_enable(&mut self, endpoint: RelayEndpoint) -> Option<RelayCommand> {
        if endpoint == RelayEndpoint::None {
            return None;
        }
        // Re-enable on same endpoint: still push activate semantics for IME.
        self.endpoint = endpoint;
        self.active = true;
        self.ime_done_count = self.ime_done_count.wrapping_add(1);
        Some(RelayCommand::Activate)
    }

    /// App disabled text-input (without full focus loss).
    pub fn on_app_disable(&mut self) -> Option<RelayCommand> {
        if !self.active {
            return None;
        }
        self.active = false;
        self.pending.clear();
        self.ime_done_count = self.ime_done_count.wrapping_add(1);
        Some(RelayCommand::Deactivate)
    }

    /// Keyboard focus left the surface: tear down session, drop pending.
    pub fn focus_lost(&mut self) -> Option<RelayCommand> {
        self.pending.clear();
        if !self.active && self.endpoint == RelayEndpoint::None {
            return None;
        }
        let was_active = self.active;
        self.active = false;
        self.endpoint = RelayEndpoint::None;
        if was_active {
            self.ime_done_count = self.ime_done_count.wrapping_add(1);
            Some(RelayCommand::Deactivate)
        } else {
            None
        }
    }

    /// Queue an IME operation (preedit / commit / delete) before serial commit.
    pub fn push_op(&mut self, op: ImeOp) {
        if self.active {
            self.pending.push_back(op);
        }
        // If inactive, ignore — IME should not be sending ops.
    }

    /// IME finished a batch with `commit(serial)`.
    ///
    /// On match: take pending FIFO as a [`FlushBatch`] and bump `done_serial`.
    /// On mismatch: clear pending and report [`CommitOutcome::Dropped`].
    pub fn on_ime_commit(&mut self, serial: u32) -> CommitOutcome {
        if serial != self.ime_done_count {
            self.pending.clear();
            return CommitOutcome::Dropped {
                expected: self.ime_done_count,
                got: serial,
            };
        }

        let ops: Vec<ImeOp> = self.pending.drain(..).collect();
        // text-input-v3: done serial equals number of commit requests from the app.
        // We model app commits as "we finished applying one IME batch".
        self.done_serial = self.done_serial.wrapping_add(1);

        CommitOutcome::Apply(FlushBatch {
            ops,
            done_serial: self.done_serial,
        })
    }

    /// Host passthrough path: batch already validated by the host compositor.
    ///
    /// Apply unconditionally (no IME serial check) and assign a done serial.
    pub fn flush_host_batch(&mut self, ops: Vec<ImeOp>) -> Option<FlushBatch> {
        if !self.active {
            return None;
        }
        self.pending.clear();
        self.done_serial = self.done_serial.wrapping_add(1);
        Some(FlushBatch {
            ops,
            done_serial: self.done_serial,
        })
    }

    /// App reported surrounding text (reverse sync toward IME in P1 wire).
    pub fn set_surrounding(&mut self, text: String, cursor: u32, anchor: u32) {
        self.surrounding = text;
        self.surrounding_cursor = cursor;
        self.surrounding_anchor = anchor;
    }

    pub fn surrounding(&self) -> (&str, u32, u32) {
        (
            self.surrounding.as_str(),
            self.surrounding_cursor,
            self.surrounding_anchor,
        )
    }

    /// Switch endpoint (e.g. in-compositor IME appeared). Resets per-endpoint
    /// counters used for the *new* endpoint's first activate.
    pub fn switch_endpoint(&mut self, endpoint: RelayEndpoint) -> Option<RelayCommand> {
        let cmd = self.focus_lost();
        self.endpoint = endpoint;
        // Counters intentionally keep done_serial (app object may be same);
        // ime_done_count continues — new IME object should start from 0 on the
        // wire side; callers that create a new im2 object must reset via
        // `reset_ime_serial`.
        cmd
    }

    /// Call when a brand-new input_method object is bound (serial baseline 0).
    pub fn reset_ime_serial(&mut self) {
        self.ime_done_count = 0;
        self.pending.clear();
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn enable_activate_increments_ime_serial() {
        let mut r = Relay::new();
        let cmd = r.on_app_enable(RelayEndpoint::Host);
        assert_eq!(cmd, Some(RelayCommand::Activate));
        assert!(r.is_active());
        assert_eq!(r.ime_done_count(), 1);
    }

    #[test]
    fn matching_commit_flushes_fifo_and_bumps_done_serial() {
        let mut r = Relay::new();
        r.on_app_enable(RelayEndpoint::InCompositor);
        // After activate, ime_done_count == 1; IME commits with serial 1.
        r.push_op(ImeOp::Preedit {
            text: "ni".into(),
            cursor: Some(2),
        });
        r.push_op(ImeOp::Preedit {
            text: "".into(),
            cursor: None,
        });
        r.push_op(ImeOp::Commit {
            text: "你".into(),
        });

        match r.on_ime_commit(1) {
            CommitOutcome::Apply(batch) => {
                assert_eq!(batch.ops.len(), 3);
                assert_eq!(batch.done_serial, 1);
                assert!(r.pending_len() == 0);
            }
            other => panic!("expected Apply, got {other:?}"),
        }
    }

    #[test]
    fn mismatched_serial_drops_pending() {
        let mut r = Relay::new();
        r.on_app_enable(RelayEndpoint::Host);
        r.push_op(ImeOp::Commit {
            text: "x".into(),
        });
        match r.on_ime_commit(99) {
            CommitOutcome::Dropped { expected, got } => {
                assert_eq!(expected, 1);
                assert_eq!(got, 99);
                assert_eq!(r.pending_len(), 0);
            }
            other => panic!("expected Dropped, got {other:?}"),
        }
    }

    #[test]
    fn focus_lost_deactivates_and_clears_pending() {
        let mut r = Relay::new();
        r.on_app_enable(RelayEndpoint::Host);
        r.push_op(ImeOp::Preedit {
            text: "a".into(),
            cursor: None,
        });
        let cmd = r.focus_lost();
        assert_eq!(cmd, Some(RelayCommand::Deactivate));
        assert!(!r.is_active());
        assert_eq!(r.pending_len(), 0);
        assert_eq!(r.endpoint(), RelayEndpoint::None);
    }

    #[test]
    fn deactivate_also_bumps_ime_serial() {
        let mut r = Relay::new();
        r.on_app_enable(RelayEndpoint::InCompositor);
        assert_eq!(r.ime_done_count(), 1);
        r.on_app_disable();
        assert_eq!(r.ime_done_count(), 2);
        // Stale commit with old serial must drop
        r.on_app_enable(RelayEndpoint::InCompositor);
        assert_eq!(r.ime_done_count(), 3);
        r.push_op(ImeOp::Commit {
            text: "nope".into(),
        });
        assert!(matches(
            r.on_ime_commit(1),
            CommitOutcome::Dropped { .. }
        ));
    }

    #[test]
    fn host_flush_skips_ime_serial_check() {
        let mut r = Relay::new();
        r.on_app_enable(RelayEndpoint::Host);
        let batch = r
            .flush_host_batch(vec![ImeOp::Commit {
                text: "xin chào".into(),
            }])
            .expect("active");
        assert_eq!(batch.done_serial, 1);
        assert_eq!(batch.ops.len(), 1);
    }

    #[test]
    fn push_op_ignored_when_inactive() {
        let mut r = Relay::new();
        r.push_op(ImeOp::Commit {
            text: "x".into(),
        });
        assert_eq!(r.pending_len(), 0);
    }

    #[test]
    fn surrounding_roundtrip() {
        let mut r = Relay::new();
        r.set_surrounding("hello".into(), 5, 5);
        let (t, c, a) = r.surrounding();
        assert_eq!(t, "hello");
        assert_eq!((c, a), (5, 5));
    }
}
