# MACRO — Arg: slot. Pull this player's record into scratch, then apply it.
$data modify storage admin:io rec set from storage admin:db players.p$(slot)
function admin:do_restore_state with storage admin:io rec
