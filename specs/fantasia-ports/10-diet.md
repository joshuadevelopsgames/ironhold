# Diet Variety  (feature ⑪)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑪. **Reward-only (carrot), never a
penalty.** Tied to the Cooking profession.

## 1. Goal
Eating a varied/balanced diet grants temporary buffs; eating poorly just means no bonus (never a
penalty). Home-cooked meals + Cooking rank make the buffs stronger — giving Cooking a real payoff.

## 2. Food groups
- Define food-group item tags: `#ironhold:diet/protein`, `/grain`, `/vegetable`, `/fruit`, `/sweet`
  (⚠️ exact groups TBD; ~5). Populate from vanilla + mod foods (data tags; large but mechanical, cf.
  Fantasia's diet tag sheets).
- A food not in any group still feeds hunger normally — it just doesn't advance diet.

## 3. Tracking
- `ModAttachments.DIET` — per-player rolling record of recently-eaten groups with timestamps/decay
  (e.g. each group "satisfied" for N in-game time after eating from it). Serialized, copyOnDeath,
  synced for a small HUD.
- On `LivingEntityUseItemEvent.Finish` / food-eaten (cf. how `food.KnownRecipes`/cooking already hooks
  eating), stamp the eaten item's group(s).

## 4. Reward
- A `PlayerTickEvent` (every ~1s) computes **how many distinct groups are currently satisfied** and
  grants a scaling **"Well Fed"** buff while ≥K groups are active (⚠️ K, durations):
  | Groups satisfied | Buff |
  |---|---|
  | 3 | minor: +1 heart (health boost) |
  | 4 | + Regeneration-lite / saturation retention |
  | 5 (balanced) | + small Strength/Resistance or a stamina-ish bonus |
- Buffs are temporary and refresh while the diet stays varied. Dropping below K just lets them lapse —
  **no debuff**.

## 5. Cooking tie-in (decision ⑪)
- **Cooked meals** (items made via Cooking, tagged `#ironhold:diet/cooked` or carrying a "cooked"
  component) grant a **longer/stronger** diet contribution than raw ingredients.
- **Cooking `ProfessionRank`** scales the Well-Fed buff tier (higher rank → better diet bonuses).
  Reads `SkillSavedData` like the other rank systems. Ties into `06-rank-gated-crafting.md` (Cooking gate).

## 6. Files (new)
- diet group item tags (`data/ironhold/tags/item/diet/*.json`)
- `food/DietState.java` + `ModAttachments.DIET` · `food/DietHandler.java` (eat stamp + tick reward)
- (optional) `client` Well-Fed HUD icon · lang

## 7. Open / TBD
- Final group list + per-group decay window. K threshold + buff values. "Cooked" detection mechanism.
