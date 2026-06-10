#!/usr/bin/env python3
"""Generate the standalone `/admin` vanilla datapack under `admin_datapack/`.

The datapack mirrors the Ironhold mod's `/admin` command, but in pure vanilla.
A single op-only entry point — `/function admin:toggle` — flips a player between
their normal state and a clean creative "build mode", restoring everything on the
way back out.

WHY A GENERATOR: vanilla cannot bulk-copy an inventory. Players reject
`/data modify entity @s ...` and `/give` can't splat full item-component NBT, so
the only component-perfect way to save/restore an inventory is `/item replace
... from ...` — one command per slot, 41 slots, each direction. Rather than
hand-maintain ~90 nearly-identical lines, we generate them. Re-run this script
after editing the layout.

DESIGN
  * Op gate: `/function` already requires permission level 2, so non-ops can
    neither see nor run it. No extra check needed.
  * Per-player storage: each player is assigned a stable integer slot on first
    use (`admin.slot`). That slot maps to two barrels (27 + 14 = 41 usable slots)
    in a forceloaded vault far out at y=260, laid out 32 pairs per row.
  * Saved state (position, rotation, dimension, game mode) lives in the
    `admin:db` storage under `players.p<slot>`, keyed by the slot via macros.
  * Toggle is driven by the `admin.state` score (1 = in build mode).
"""

from __future__ import annotations

import os
import textwrap

# ── Vault geometry ─────────────────────────────────────────────────────────
VX, VY, VZ = 2_000_000, 260, 2_000_000   # vault origin (far, forceloaded, in air)
COLS = 32                                # barrel-pairs per row
ROWS_FORCELOADED = 64                    # vault depth in blocks (== rows) → 32*64 = 2048 players
BARREL = "minecraft:barrel"

# ── Player inventory slot order → barrel container index ────────────────────
# Barrel A holds 27 slots, barrel B holds the remaining 14. Names are vanilla
# player slot identifiers accepted by `/item replace entity`.
SLOTS_A: list[str] = (
    [f"hotbar.{i}" for i in range(9)]        # 0..8  -> container.0..8
    + [f"inventory.{i}" for i in range(18)]  # 9..26 -> container.9..26
)
SLOTS_B: list[str] = (
    [f"inventory.{i}" for i in range(18, 27)]  # container.0..8
    + ["armor.feet", "armor.legs", "armor.chest", "armor.head"]  # container.9..12
    + ["weapon.offhand"]                                          # container.13
)
assert len(SLOTS_A) == 27 and len(SLOTS_B) == 14, "must total 41 player slots"

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "admin_datapack")
FUNC_DIR = os.path.join(ROOT, "data", "admin", "function")
LOAD_TAG_DIR = os.path.join(ROOT, "data", "minecraft", "tags", "function")


def write(path: str, text: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text.rstrip("\n") + "\n")
    print(f"  wrote {os.path.relpath(path, ROOT)}")


def func(name: str, body: str) -> None:
    write(os.path.join(FUNC_DIR, f"{name}.mcfunction"), textwrap.dedent(body).lstrip("\n"))


def gen_save_items() -> str:
    lines = ["# MACRO — copy every player slot into the two vault barrels (component-perfect).",
             "# Args: ax ay az bx by bz (barrel coordinates)."]
    for c, slot in enumerate(SLOTS_A):
        lines.append(f"$item replace block $(ax) $(ay) $(az) container.{c} from entity @s {slot}")
    for c, slot in enumerate(SLOTS_B):
        lines.append(f"$item replace block $(bx) $(by) $(bz) container.{c} from entity @s {slot}")
    return "\n".join(lines)


def gen_restore_items() -> str:
    lines = ["# MACRO — copy every vault barrel slot back into the player.",
             "# Args: ax ay az bx by bz (barrel coordinates)."]
    for c, slot in enumerate(SLOTS_A):
        lines.append(f"$item replace entity @s {slot} from block $(ax) $(ay) $(az) container.{c}")
    for c, slot in enumerate(SLOTS_B):
        lines.append(f"$item replace entity @s {slot} from block $(bx) $(by) $(bz) container.{c}")
    return "\n".join(lines)


def main() -> None:
    os.makedirs(ROOT, exist_ok=True)
    print(f"Generating /admin datapack at {ROOT}")

    # pack.mcmeta — wide supported_formats so it loads across recent versions.
    write(os.path.join(ROOT, "pack.mcmeta"),
          '{\n'
          '  "pack": {\n'
          '    "description": "/admin build-mode toggle (op only). Run /function admin:toggle.",\n'
          '    "pack_format": 101,\n'
          '    "supported_formats": { "min_inclusive": 26, "max_inclusive": 9999 }\n'
          '  }\n'
          '}')

    # load tag — run setup on (re)load.
    write(os.path.join(LOAD_TAG_DIR, "load.json"),
          '{\n  "values": [\n    "admin:setup"\n  ]\n}')

    # ── setup: objectives, counter, forceload the vault ─────────────────────
    func("setup", f"""
        # Runs on every (re)load. Idempotent.
        scoreboard objectives add admin.state dummy
        scoreboard objectives add admin.slot dummy
        scoreboard objectives add admin.calc dummy
        scoreboard objectives add admin.data dummy
        # Next slot to hand out (only initialise if unset).
        execute unless score $next admin.data matches -2147483648.. run scoreboard players set $next admin.data 0
        # Keep the vault chunks loaded so item-replace always finds the barrels.
        forceload add {VX} {VZ} {VX + ROWS_FORCELOADED - 1} {VZ + ROWS_FORCELOADED - 1}
    """)

    # ── toggle: the single entry point players type ─────────────────────────
    func("toggle", """
        # Op-gated implicitly: /function requires permission level 2.
        execute unless entity @s[type=player] run return fail
        # In build mode already? Leave it. Otherwise enter it.
        execute if score @s admin.state matches 1 run return run function admin:exit
        function admin:enter
    """)

    # ── slot assignment ─────────────────────────────────────────────────────
    func("assign_slot", """
        scoreboard players set #new admin.calc 0
        execute unless score @s admin.slot matches -2147483648.. run function admin:do_assign
    """)
    func("do_assign", """
        scoreboard players operation @s admin.slot = $next admin.data
        scoreboard players add $next admin.data 1
        scoreboard players set #new admin.calc 1
    """)

    # ── slot -> barrel coordinates, written to admin:io for the macros ──────
    func("compute_coords", f"""
        scoreboard players operation #slot admin.calc = @s admin.slot
        scoreboard players set #cols admin.calc {COLS}
        scoreboard players set #two admin.calc 2
        # col = slot % COLS ; row = slot / COLS
        scoreboard players operation #col admin.calc = #slot admin.calc
        scoreboard players operation #col admin.calc %= #cols admin.calc
        scoreboard players operation #row admin.calc = #slot admin.calc
        scoreboard players operation #row admin.calc /= #cols admin.calc
        # ax = VX + col*2 ; bx = ax + 1 ; az = VZ + row ; y = VY
        scoreboard players operation #ax admin.calc = #col admin.calc
        scoreboard players operation #ax admin.calc *= #two admin.calc
        scoreboard players add #ax admin.calc {VX}
        scoreboard players operation #bx admin.calc = #ax admin.calc
        scoreboard players add #bx admin.calc 1
        scoreboard players operation #az admin.calc = #row admin.calc
        scoreboard players add #az admin.calc {VZ}
        scoreboard players set #y admin.calc {VY}
        # Publish to storage for the macro functions.
        execute store result storage admin:io ax int 1 run scoreboard players get #ax admin.calc
        execute store result storage admin:io ay int 1 run scoreboard players get #y admin.calc
        execute store result storage admin:io az int 1 run scoreboard players get #az admin.calc
        execute store result storage admin:io bx int 1 run scoreboard players get #bx admin.calc
        execute store result storage admin:io by int 1 run scoreboard players get #y admin.calc
        execute store result storage admin:io bz int 1 run scoreboard players get #az admin.calc
        execute store result storage admin:io slot int 1 run scoreboard players get @s admin.slot
    """)

    # ── place the two barrels once, on first assignment (replace = guaranteed) ─
    func("place_barrels", """
        # MACRO — Args: ax ay az bx by bz. Only ever called the first time a slot
        # is handed out, so a plain (replace) setblock can't clobber stored items.
        $setblock $(ax) $(ay) $(az) minecraft:barrel
        $setblock $(bx) $(by) $(bz) minecraft:barrel
    """)

    # ── save / restore the player's location + game mode ────────────────────
    func("save_state", """
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
    """)
    func("restore_state", """
        # MACRO — Arg: slot. Pull this player's record into scratch, then apply it.
        $data modify storage admin:io rec set from storage admin:db players.p$(slot)
        function admin:do_restore_state with storage admin:io rec
    """)
    func("do_restore_state", """
        # MACRO — Args: dim x y z yaw pitch gm. Teleport home and restore game mode.
        $execute in $(dim) run tp @s $(x) $(y) $(z) $(yaw) $(pitch)
        $gamemode $(gm) @s
    """)

    # ── the repetitive item-swap macros (generated) ─────────────────────────
    func("save_items", gen_save_items())
    func("restore_items", gen_restore_items())

    # ── enter / exit orchestration ──────────────────────────────────────────
    func("enter", """
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
    """)
    func("exit", """
        function admin:compute_coords
        # Drop whatever creative junk is held, give the saved gear back, go home.
        clear @s
        function admin:restore_items with storage admin:io
        function admin:restore_state with storage admin:io
        scoreboard players set @s admin.state 0
        tellraw @s [{"text":"[Admin] ","color":"gold","bold":true},{"text":"Build mode OFF — items restored and teleported to your saved spot.","color":"green"}]
    """)

    # ── README ──────────────────────────────────────────────────────────────
    write(os.path.join(ROOT, "README.md"), textwrap.dedent(f"""
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
        forceloaded vault at ~({VX}, {VY}, {VZ}). Saved location/game mode live in
        the `admin:db` command storage. Regenerate with
        `python3 scripts/gen_admin_datapack.py`.

        ## Note on pack_format
        `pack.mcmeta` uses a wide `supported_formats` range so it loads on current
        builds. If your client rejects it, set `pack_format` to your version's
        value (datapacks still run; this only affects the compatibility banner).
    """))

    print("Done.")


if __name__ == "__main__":
    main()
