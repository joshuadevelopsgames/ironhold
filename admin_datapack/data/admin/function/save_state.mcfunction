# MACRO — Arg: slot. Snapshot where/how the player currently is.
$data modify storage admin:db players.p$(slot).x set from entity @s Pos[0]
$data modify storage admin:db players.p$(slot).y set from entity @s Pos[1]
$data modify storage admin:db players.p$(slot).z set from entity @s Pos[2]
$data modify storage admin:db players.p$(slot).yaw set from entity @s Rotation[0]
$data modify storage admin:db players.p$(slot).pitch set from entity @s Rotation[1]
$data modify storage admin:db players.p$(slot).dim set from entity @s Dimension
$data modify storage admin:db players.p$(slot).gm set value "survival"
$execute if entity @s[gamemode=creative] run data modify storage admin:db players.p$(slot).gm set value "creative"
$execute if entity @s[gamemode=adventure] run data modify storage admin:db players.p$(slot).gm set value "adventure"
$execute if entity @s[gamemode=spectator] run data modify storage admin:db players.p$(slot).gm set value "spectator"
