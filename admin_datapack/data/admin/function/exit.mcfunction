function admin:compute_coords
# Drop whatever creative junk is held, give the saved gear back, go home.
clear @s
function admin:restore_items with storage admin:io
function admin:restore_state with storage admin:io
scoreboard players set @s admin.state 0
tellraw @s [{"text":"[Admin] ","color":"gold","bold":true},{"text":"Build mode OFF — items restored and teleported to your saved spot.","color":"green"}]
