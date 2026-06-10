# Coin Purse  (feature ⑦)

**Status:** spec. Decisions: `fantasia_port_decisions.md` ⑦. Currency already exists
(`ModItems.GOLD_COIN` + `trade.GoldCoinTradeHandler` re-skins villager trades emerald→coin).
This adds only the **purse** convenience layer. No denominations, no trade rework.

## 1. Goal
A purse item that banks loose gold coins into a stored count so coins don't clutter the inventory,
withdrawable any time.

## 2. The purse
- `item.CoinPurseItem extends Item` (stacksTo 1). Stored balance in a **data component**
  `ironhold:coin_balance` (int), declared in `IronholdItemComponents` (cf. existing gear components).
- **Deposit (decision ⑦ = right-click purse):** right-click (use) with the purse →
  pull all `GOLD_COIN` stacks from the player inventory into the purse's balance (server-side),
  play a coin clink. Optional convenience: also auto-absorb coins picked up while a purse is in the
  hotbar (⚠️ off by default to keep it explicit).
- **Withdraw:** sneak-right-click → withdraw a configurable amount (v1: one stack / 64) back as
  `GOLD_COIN` items; or open a tiny amount-picker screen (⚠️ v1 = sneak withdraws 64, no UI).
- **Tooltip / HUD:** tooltip shows `◉ {balance} coins`. Optional: small coin-count HUD when holding the purse.

## 3. Integration
- Reuses existing `GOLD_COIN` item; no change to `GoldCoinTradeHandler` (trades stay vanilla-leveled).
- Balance is per-item-stack (component), so multiple purses are allowed and tradeable. ⚠️ Decide
  whether the purse persists balance on death (it's a normal item → drops with balance unless Soulbound'd).

## 4. Files (new)
- `item/CoinPurseItem.java` (+`ModItems`, recipe) · `IronholdItemComponents` — `COIN_BALANCE` component
- (optional) `client` HUD/tooltip · lang entries

## 5. Open / TBD
- Withdraw UX (fixed 64 vs amount picker). Auto-absorb on/off. Max purse capacity (cap or unlimited).
