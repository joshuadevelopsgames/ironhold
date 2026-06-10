# Villager Relationship Lifecycle  (feature ⑲)

**Status:** spec — large. Decisions: `fantasia_port_decisions.md` ⑲. Extends the existing befriending
system with **Hire + Recruit + Marry** (all three). Marriage = mechanical benefits, not flavor-only.

## 1. Goal
On top of `npc.PlayerNpcBonds` (Stardew rapport + gifts = befriending, already built), add the
lifecycle stages: employ NPCs for pay, take them adventuring, and marry a high-rapport NPC for real perks.

## 2. Reuses
- `npc.PlayerNpcBonds` (rapport gate per stage), `entity.goal.NpcFollowOwnerGoal`,
  `entity.goal.NpcStation*` (chest/flower/sleep/wander/station), `entity.KingdomVillagerEntity`
  (+personality/profession/temperament), coin economy (`GOLD_COIN`/purse), lock system (`LOCK_OWNER`).

## 3. Hire (employment)
- At sufficient rapport, an NPC offers a **hire** option (via interaction menu / dialogue). Pay **coins**
  (daily/periodic wage debited from a purse or a village coffer) → the NPC works your base:
  reuse `NpcStation*` goals (tend `NpcStationChestGoal`/`NpcStationFlowerGoal`, etc.). Stop working /
  quit if unpaid (rapport hit).
- State: `ModAttachments`/BE link `{ employerUuid, wage, lastPaidDay, assignedStationPos }` on the NPC.
- Turns villages into a labor economy and a coin sink.

## 4. Recruit (combat companion)
- At higher rapport, **recruit** an NPC as a following fighting companion: enable `NpcFollowOwnerGoal`
  + give it combat AI (reuse the knight/guard goals — `KnightShieldBlockGoal`, melee/ranged goals).
  A simple command set (follow / stay / wait here). Downs/retreats at low HP rather than perma-death
  (⚠️ decide death model — knock-out vs die).
- State: `{ ownerUuid, mode: FOLLOW|STAY }` on the NPC; one or few active recruits per player (⚠️ cap).

## 5. Marry (spouse) — mechanical benefits  (decision ⑲)
- **Courtship → marriage:** at max rapport + a ritual/ring item, marry the NPC. One spouse per player (⚠️).
- **Perks (the point — not flavor):**
  - **Shared base access** — spouse is auto-authorized on the player's locks (`LOCK_OWNER`/`LOCK_KEY_ID`):
    can open the player's locked chests/doors. (Direct tie to the lock system.)
  - **Home buff** — being near your spouse / home grants a small "Beloved" buff (regen-lite/comfort).
  - **Chores & defense** — spouse does station work for free (no wage) and defends the home.
  - **Gifts** — periodically gives the player coins/items (reverse of the gift loop).
- State: `ModAttachments.SPOUSE` `{ npcUuid }` (copyOnDeath) + reciprocal link on the NPC.

## 6. Interaction surface
- Extend the NPC `mobInteract` → a relationship menu (cf. `VillagerDialogueScreen` / `OpenVillagerScreenPayload`)
  showing rapport + the available stage actions (gift / hire / recruit / propose) gated by rapport thresholds.

## 7. Files (new)
- `npc/EmploymentData.java`, `npc/RecruitData.java`, `ModAttachments.SPOUSE` (+NPC-side links)
- relationship menu/screen + payload (cf. villager dialogue screen) · ring/ritual item for marriage
- companion combat goal wiring · lock-system authorization hook for spouses · wage/coffer logic

## 8. Open / TBD
- Recruit death model (knock-out vs die) + cap. One-spouse rule + divorce. Wage cadence/source.
- How hire/recruit/marry compose with voiced AI NPCs (Kangarude/Halric) vs generic kingdom villagers.
