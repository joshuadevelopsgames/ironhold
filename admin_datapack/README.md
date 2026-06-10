
# /admin datapack (vanilla)

A pure-vanilla twin of the Ironhold mod's `/admin` command.

## Use
Run (as an op — `/function` requires permission level 2):

    /function admin:toggle

* **First run** saves your exact position, facing, dimension, game mode, and
  entire inventory; empties your inventory; and puts you in creative.
* **Second run** restores your game mode and items and teleports you back.

`/function admin:status` is not provided in vanilla; check `admin.state`
with `/scoreboard players get <name> admin.state` (1 = in build mode).

## Install
Drop the `admin_datapack` folder into a world's `datapacks/` directory (or
zip its contents) and `/reload`. On load it creates its scoreboards and
forceloads the storage vault.

## How it works
Vanilla can't bulk-copy an inventory, so each of the 41 slots is mirrored
with `/item replace ... from ...` into two per-player barrels kept in a
forceloaded vault at ~(2000000, 260, 2000000). Saved location/game mode live in
the `admin:db` command storage. Regenerate with
`python3 scripts/gen_admin_datapack.py`.

## Note on pack_format
`pack.mcmeta` uses a wide `supported_formats` range so it loads on current
builds. If your client rejects it, set `pack_format` to your version's
value (datapacks still run; this only affects the compatibility banner).
