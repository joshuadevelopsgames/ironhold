# Runs on every (re)load. Idempotent.
scoreboard objectives add admin.state dummy
scoreboard objectives add admin.slot dummy
scoreboard objectives add admin.calc dummy
scoreboard objectives add admin.data dummy
# Next slot to hand out (only initialise if unset).
execute unless score $next admin.data matches -2147483648.. run scoreboard players set $next admin.data 0
# Keep the vault chunks loaded so item-replace always finds the barrels.
forceload add 2000000 2000000 2000063 2000063
