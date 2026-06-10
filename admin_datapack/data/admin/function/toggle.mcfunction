# Op-gated implicitly: /function requires permission level 2.
execute unless entity @s[type=player] run return fail
# In build mode already? Leave it. Otherwise enter it.
execute if score @s admin.state matches 1 run return run function admin:exit
function admin:enter
