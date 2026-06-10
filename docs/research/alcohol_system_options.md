# Alcohol System Options

## Goal

Add beer, ale, wine, mead, and related drinks in a way that strengthens
Ironhold's taverns, NPC relationships, economy, cooking progression, and
placeable chalice instead of adding a disconnected set of potion-like items.

## Existing Ironhold Hooks

Ironhold already has most of the systems needed to make alcohol meaningful:

- `ChaliceBlock` can hold, render, persist, and serve colored liquids.
- `ChaliceLiquids` already maps container items to drink behavior.
- Mira is the innkeeper of The Wandering Wolf and her manifest says she keeps
  "the ale honest."
- Beren's rapport milestone already says the player keeps buying him ale, and
  his cheese reaction refers to drinking it with ale.
- Bram refers to patrons crying into their mead.
- Gold coins, NPC gifts, rapport milestones, seasons, Cooking ranks, Alchemy
  ranks, and known cooking recipes are already implemented.

This means alcohol should be treated as a social and production system first,
and as a combat buff system second.

## Patterns Used by Other Mods

### 1. Simple consumable drinks

Each beverage is a bottle or mug with one or more status effects. This is the
lowest-cost implementation and works well when drinks are mostly flavor items.

Advantages:

- Small implementation and art scope.
- Easy to balance with standard `Consumable` effects.
- Immediately works with loot, NPC gifts, shops, and the chalice.

Risks:

- Drinks feel like renamed potions.
- Players may ignore them unless their buffs are strong.
- Strong buffs can turn drinking into mandatory combat preparation.

### 2. Compact keg or fermentation cask

Brewin' and Chewin' centers fermentation on a keg, while Vinery uses a
fermentation barrel that combines stored juice, ingredients, and containers.
This creates a readable production loop without needing a large factory.

Advantages:

- Fits a tavern, farmhouse, cellar, or castle kitchen.
- Gives ingredients and recipes economic value.
- A batch can produce several servings.
- Can plug into Cooking rank and known-recipe progression.

Risks:

- Requires a block entity, menu, recipe type, syncing, and processing state.
- A slow timer is dull if there is no visible or audible progress.

### 3. Active brewing minigame

Legacy Brewery uses drying, a multipart brewing station, fuel, heat, and
mid-process events such as adding fuel or cooling an overheating kettle.

Advantages:

- Brewing is an activity rather than a waiting timer.
- Skill can affect quality or yield.
- Strong fit for a dedicated brewer profession or festival.

Risks:

- Large code, model, UI, and balancing scope.
- Too industrial for the first version of a medieval tavern feature.
- Repetitive intervention can become chores after the novelty wears off.

### 4. Crop-to-cellar production

Vinery builds a full chain around regional grapes, pressing juice, fermenting,
bottling, aging, bottle display, and wine racks.

Advantages:

- Strong farming, exploration, season, building, and collecting loop.
- Regional ingredients can produce distinct drinks.
- Cellars and vineyards gain gameplay purpose.

Risks:

- Very high content scope.
- Adding grapes, hops, trellises, presses, barrels, bottles, racks, and many
  wines at once would delay the core tavern feature.

### 5. Cumulative intoxication

Every alcoholic serving adds to a hidden or visible alcohol level. Benefits can
appear at low levels, while coordination penalties and a possible hangover
appear at high levels.

Advantages:

- Stops players from stacking every drink buff without consequence.
- Makes weak and strong drinks mechanically distinct.
- Creates social comedy and risk without making one drink purely harmful.

Risks:

- Screen distortion and forced movement can cause motion sickness or annoyance.
- Long hangovers punish roleplay.
- A visible meter can feel too clinical for Ironhold's presentation.

## Recommended Direction

Use a compact fermentation cask plus a light cumulative intoxication system.
Make tavern and NPC interactions the main reward, with modest situational
effects rather than powerful universal combat buffs.

### Initial drinks

| Drink | Ingredients | Identity | Dose |
| --- | --- | --- | ---: |
| Small Ale | Wheat, water, hops | Common tavern drink; cheapest and weakest | 20 |
| Honey Mead | Honey, water | Warm, valuable, slightly stronger | 28 |
| Apple Cider | Apples, sugar, water | Low-strength harvest drink | 14 |
| Berry Wine | Sweet berries, sugar, water | Cellar drink; avoids adding grapes in v1 | 24 |

Use existing crops for the first release. Hops are the only new crop worth
adding immediately because they give ale a distinct supply chain and can use
the existing seasonal crop tags. Grapes and dedicated vineyards can be a later
expansion if the base loop proves worthwhile.

### Drink effects

Avoid direct Strength, Regeneration, or Resistance on common drinks. Those make
alcohol optimal before every fight. Prefer small identity effects:

- Small Ale: short knockback resistance or a custom `Courage` effect.
- Honey Mead: small temporary absorption, not repeat-stackable.
- Apple Cider: small hunger and saturation restoration.
- Berry Wine: short Luck effect for social/exploration flavor.

All drinks should be always consumable, use the drink animation and sound,
return an empty container, and pour into the existing chalice.

### Intoxication thresholds

Store an integer alcohol load on the player and decay it server-side.

| Load | State | Suggested behavior |
| ---: | --- | --- |
| 0-19 | Sober | Drink-specific benefit only |
| 20-39 | Merry | Cosmetic particles or occasional chat flavor; no penalty |
| 40-64 | Tipsy | Mild Mining Fatigue or reduced precision |
| 65-89 | Drunk | Slowness plus brief, infrequent disorientation |
| 90+ | Wasted | Nausea/weakness and no additional positive drink effects |

Recommended decay is about one point every 10 seconds, so one ale clears in
under two minutes and heavy drinking matters for roughly one Minecraft evening.
Food and water can accelerate decay, but milk should not instantly erase the
whole system unless that is an explicit balance choice.

Do not use random forced movement. If camera sway is added, make it client
configurable and disabled by default. A hangover should be brief, only trigger
after sleeping at a high load, and be curable with water plus a cooked meal.

### Blackout event

Reaching the maximum intoxication tier can trigger a rare, short blackout
sequence:

1. Apply Blindness and muffle nearby sound for two to three seconds.
2. Fade the screen to black and temporarily suspend player input.
3. Let the server control the player's body for roughly 10-20 seconds.
4. Fade through a few brief snapshots of what the body is doing.
5. Move the player to a safe bed, advance to morning when appropriate, and
   wake them with a hangover plus a short recap.

The controlled section should select from a whitelist of safe actions:

- Stagger, turn around, jump once, or walk a short nav-checked route.
- Sit near a fire, table, tavern patron, or bard.
- Wave, dance, toast with a held chalice, or mutter a flavor line.
- Ring a bell once or open and close an unlocked door.
- Eat an ordinary food item if the player is hungry.
- Fall asleep in the nearest valid bed.

By default it must never:

- Break or place blocks.
- Attack, steal, trade, gift, or trigger abilities.
- Drop items or rearrange equipment.
- Use weapons, tools, potions, teleport items, mounts, boats, or minecarts.
- Enter fire, lava, deep water, cliffs, hostile combat, protected areas, or
  another player's locked room.

This creates the impression that the character went on an uncontrolled
adventure without actually gambling the player's inventory or builds.

Two capped destructive events are allowed as low-probability options:

- Spend one to five gold coins, including coins banked in a Coin Purse.
- Consume one stackable Rare or Epic item from the main inventory.

Both events must be independently configurable, occur at most once per
blackout, and name the loss in the wake-up recap.

Bed selection should be deterministic:

1. The player's valid respawn bed, if safely reachable or teleport-safe.
2. A tagged public tavern bed within range.
3. The nearest unoccupied public bed.
4. A protected recovery point at The Wandering Wolf.
5. World spawn only as a final fallback.

The blackout should clear most alcohol load, apply a short `Hangover` effect,
and set a cooldown of at least one in-game day before another blackout can
occur. Servers and clients should be able to disable body-control blackouts;
when disabled, maximum intoxication should fade directly to waking in bed.

For presentation, show two or three one-second visual snapshots instead of
making the player watch the full autopilot sequence. Example recap:

> You remember raising a toast, ringing the town bell, and arguing with a
> chair. Mira found you asleep by the hearth.

## Production Loop

### Fermentation cask

A single-block cask is the best first station:

1. Insert base ingredients.
2. Add water bottles or fill an internal water tank.
3. Add a catalyst such as sugar or yeast.
4. Wait one to three in-game days.
5. Draw four servings into bottles, mugs, or a chalice.

The block should visibly change state and make occasional bubble sounds while
active. Batch output makes the wait feel worthwhile.

Use data-driven fermentation recipes with:

- Ingredient list or item tags.
- Required fluid and amount.
- Processing time.
- Output beverage.
- Servings.
- Minimum Cooking or Alchemy rank.
- Optional season or temperature modifier.

### Progression split

- Cooking governs ordinary ale, cider, mead, and wine.
- Alchemy governs magical or fortified drinks added later.
- Known recipes gate production before profession rank is checked.
- Mira or another brewer NPC can teach the first ale recipe.

This matches the existing `CookingRecipe`, `CookingService`, `KnownRecipes`,
and profession-rank model.

## Social and Economy Integration

The first release should wire drinks into systems players already encounter:

- Mira sells Small Ale and buys hops, honey, and finished batches.
- Giving Beren ale should fulfill the fiction already present in his milestone.
- Bram should prefer mead.
- Drinking from a filled chalice at The Wandering Wolf can trigger a small
  "shared toast" rapport bonus once per in-game day.
- Taverns, camps, banquets, weddings, and seasonal festivals can place filled
  chalices as props that are also usable.
- Rare regional brews can become quest rewards, fishing treasure, or castle
  cellar loot.
- Advancements should teach the cask instead of requiring a guidebook.

This creates uses for alcohol even for players who do not care about its status
effects.

## Later Expansion Options

### Quality and aging

Add `rough`, `good`, and `fine` quality based on ingredient quality, brewer
rank, processing temperature, and cellar aging. Quality should improve sale
price, duration, and NPC rapport more than combat power.

### Regional and seasonal drinks

- Winter spiced mead.
- Autumn harvest cider.
- Taiga spruce ale.
- Ebonwood berry wine.
- Moonshroom spirits with a magical Alchemy recipe.

Seasonal drinks can use the existing season state without blocking ordinary
drinks year-round.

### Tavern service

Let players place a cask behind a bar and fill mugs or chalices directly. A
later tavern-management loop could track served patrons, tips, and reputation,
but this should not be part of the initial implementation.

### Drinking games and celebrations

Optional social mechanics include toasts, rounds bought for nearby players,
festival contests, and bard reactions. Keep these opt-in and avoid mechanics
that reward rapid real-world alcohol consumption.

## Technical Shape for NeoForge 26.1

### Drinks

NeoForge 26.1 supports drink behavior through the `Consumable` data component,
including drink animation, sound, consume time, and multiple consume effects.
A custom data-driven `AlcoholDoseConsumeEffect` is preferable to a separate
hard-coded item class for every beverage.

Suggested pieces:

- `AlcoholDoseConsumeEffect(int dose, ResourceLocation drinkId)`
- `AlcoholState(int load, long lastDrinkTick, long blackoutCooldownUntil)`
  player attachment
- `AlcoholService` for dose, decay, thresholds, and sleep handling
- Visible `tipsy`, `drunk`, and `hangover` mob effects
- A beverage definition or profile map for color, display name, chalice effect,
  dose, and optional positive effect
- `BlackoutController` for the finite-state sequence, safe action whitelist,
  bed selection, recovery, and recap generation

The alcohol attachment should serialize so logging out does not sober a player,
but should not copy on death.

The blackout controller must be server-authoritative. Do not simulate normal
client key presses. During a blackout it should put the player into a dedicated
state, reject normal interaction packets, and execute only explicit server-side
actions. If the player disconnects, dies, changes dimension, or enters combat,
abort the sequence and recover them safely on their next login.

### Chalice

`ChaliceLiquids` currently hard-codes liquid IDs and stores full
`PotionContents` only for potions. Add a beverage resource ID to `Fill` and the
block entity so a chalice can preserve which drink it contains.

Do not add one switch case per future drink. Resolve color, name, alcohol dose,
and effects through a beverage profile registry or data map.

### Cask

Implement the cask as:

- Block plus block entity.
- Small item inventory and internal water amount.
- Custom fermentation recipe type.
- Server-authoritative processing timer.
- Blockstate flags for `active` and optionally `ready`.
- Menu only if direct slot interactions become too awkward.

Start with fixed-quality outputs. Quality and aging require more item data and
should be added only after the production loop is fun.

## Suggested Delivery Phases

### Phase 1: Tavern drinks

- Four drink items.
- Drink effects and container remainders.
- Chalice support.
- Mira, Beren, and Bram manifest integration.
- Simple crafting or admin recipes for playtesting.

### Phase 2: Fermentation

- Hops crop and seasonal fertility.
- Fermentation cask and JSON recipes.
- Cooking rank and known-recipe gates.
- Advancements and Mira recipe teaching.

### Phase 3: Intoxication

- Persistent alcohol load and decay.
- Threshold effects.
- Safe blackout sequence and wake-up recap.
- Water/food recovery and brief optional hangover.
- Client configuration for any visual distortion.

### Phase 4: Cellar depth

- Quality and aging.
- Bottle racks and tavern service.
- Regional, seasonal, and magical brews.
- Festivals, toasts, and drinking games.

## Recommendation Summary

The strongest Ironhold version is not a full Brewery or Vinery clone. Build a
small cask-based production loop, serve drinks through the existing chalice,
and make NPC rapport and tavern life the primary payoff. Add restrained
cumulative intoxication to control buff stacking. Begin with ale, mead, cider,
and berry wine using mostly existing ingredients, then expand into grapes,
aging, quality, and magical drinks only after the first loop is proven.

## Sources

- [NeoForge 26.1 consumables documentation](https://docs.neoforged.net/docs/items/consumables/)
- [Legacy Let's Do Brewery overview](https://modrinth.com/mod/lets-do-brewery)
- [Let's Do Vinery overview](https://modrinth.com/mod/lets-do-vinery)
- [Vinery wiki](https://lets-do.ch/wiki/vinery/)
- [Brewin' and Chewin' overview](https://modrinth.com/mod/brewin-and-chewin)
- [Brewin' and Chewin' source](https://github.com/ChefsDelights/BrewinAndChewin)
