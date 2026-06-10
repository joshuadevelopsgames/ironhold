scoreboard players operation #slot admin.calc = @s admin.slot
scoreboard players set #cols admin.calc 32
scoreboard players set #two admin.calc 2
# col = slot % COLS ; row = slot / COLS
scoreboard players operation #col admin.calc = #slot admin.calc
scoreboard players operation #col admin.calc %= #cols admin.calc
scoreboard players operation #row admin.calc = #slot admin.calc
scoreboard players operation #row admin.calc /= #cols admin.calc
# ax = VX + col*2 ; bx = ax + 1 ; az = VZ + row ; y = VY
scoreboard players operation #ax admin.calc = #col admin.calc
scoreboard players operation #ax admin.calc *= #two admin.calc
scoreboard players add #ax admin.calc 2000000
scoreboard players operation #bx admin.calc = #ax admin.calc
scoreboard players add #bx admin.calc 1
scoreboard players operation #az admin.calc = #row admin.calc
scoreboard players add #az admin.calc 2000000
scoreboard players set #y admin.calc 260
# Publish to storage for the macro functions.
execute store result storage admin:io ax int 1 run scoreboard players get #ax admin.calc
execute store result storage admin:io ay int 1 run scoreboard players get #y admin.calc
execute store result storage admin:io az int 1 run scoreboard players get #az admin.calc
execute store result storage admin:io bx int 1 run scoreboard players get #bx admin.calc
execute store result storage admin:io by int 1 run scoreboard players get #y admin.calc
execute store result storage admin:io bz int 1 run scoreboard players get #az admin.calc
execute store result storage admin:io slot int 1 run scoreboard players get @s admin.slot
