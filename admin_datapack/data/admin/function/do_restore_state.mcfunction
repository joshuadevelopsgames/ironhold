# MACRO — Args: dim x y z yaw pitch gm. Teleport home and restore game mode.
$execute in $(dim) run tp @s $(x) $(y) $(z) $(yaw) $(pitch)
$gamemode $(gm) @s
