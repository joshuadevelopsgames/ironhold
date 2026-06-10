function admin:assign_slot
function admin:compute_coords
# Brand-new slot? Lay down its two empty barrels.
execute if score #new admin.calc matches 1 run function admin:place_barrels with storage admin:io
# Snapshot state, stash items, then wipe + creative.
function admin:save_state with storage admin:io
function admin:save_items with storage admin:io
clear @s
gamemode creative @s
scoreboard players set @s admin.state 1
tellraw @s [{"text":"[Admin] ","color":"gold","bold":true},{"text":"Build mode ON — position & inventory saved, switched to creative. Run ","color":"yellow"},{"text":"/function admin:toggle","color":"aqua"},{"text":" to return.","color":"yellow"}]
