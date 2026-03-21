package com.trm.sightline.core.common

enum class PermissionStatus {
  // Not yet requested this session — safe to auto-launch the dialog.
  Unknown,
  // Granted
  Granted,
  // Denied once; rationale can be shown. Dialog can still be launched.
  Denied,
  // Denied permanently; only Settings can unblock.
  PermanentlyDenied,
}
