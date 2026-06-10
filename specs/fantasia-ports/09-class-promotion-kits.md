# Class Promotion Kits  (feature ⑭)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑭. Fitted to Ironhold's **earned**
progression (start `PEASANT`, promote up the 27-class tree at class stones) — NOT a spawn-screen pick.

## 1. Goal
When a player promotes into a class at a **major branch point**, grant that class's signature starter
gear + weapon + a one-line backstory. Immediate identity at the moments that matter.

## 2. Hook
- Tie into the existing promotion completion path: `game.ActivePromotionStone` /
  `block.ClassStoneBlockEntity` / `rpg.RpgProgression` (where `PlayerKingdomRpgData.classIndex` changes).
  ⚠️ confirm the exact "promotion finalized" call; grant the kit there, server-side.
- Fire **once per class** (track via existing `rpg.CompletedClasses` attachment, or a new
  `KITS_GRANTED` set) so re-promoting / dying never re-grants.

## 3. Which promotions (decision ⑭ = major branch points only)
Grant kits at the identity moments, not every step:
- Entering each first-tier line: **Knight, Mage/Wizard, Archer/Ranger, Medic/Cleric** (the branch roots).
- The advanced/Divine tiers: **Champion, Sorcerer Supreme, Deadshot, Bishop, Divine Knight/Mage/Ranger** (⚠️ pick the canonical set).
- Minor in-line steps (Squire, intermediate ranks) → stats/skills only, no kit.

## 4. Kit definition
- `rpg.ClassKit` — a data table keyed by `PlayerClass`: `{ List<ItemStack> gear, ItemStack weapon,
  String backstoryKey }`. Reuse `client.screen.ClassGear` if it already models per-class gear.
- Grant = insert stacks into inventory/equip slots; send the backstory line as a styled chat/toast.
- Themed examples (⚠️ author per class): Knight = plate set + sword + kite shield + bread; Mage =
  robe + a `WizardStaffItem`/`ArcaneScepterItem` + mana focus; Archer = leather + `TempestBowItem` + arrows;
  Medic = healer garb + bandages + a heal focus.
- Quality: kit gear rolls **Fine/Good** `ItemQuality` (not Mint) so it's a starting point, not an endgame.

## 5. Files (new)
- `rpg/ClassKit.java` (table) + grant hook in the promotion flow
- `ModAttachments.KITS_GRANTED` (or reuse `CompletedClasses`) · lang keys for backstories
- per-class kit data (gear lists)

## 6. Open / TBD
- The canonical "major branch point" class list. Exact kit contents per class. Whether kits scale
  with the tier you promote into (bigger kits for Divine tiers).
