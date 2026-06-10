# MACRO — Args: ax ay az bx by bz. Only ever called the first time a slot
# is handed out, so a plain (replace) setblock can't clobber stored items.
$setblock $(ax) $(ay) $(az) minecraft:barrel
$setblock $(bx) $(by) $(bz) minecraft:barrel
