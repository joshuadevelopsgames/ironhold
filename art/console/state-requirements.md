# State Requirements

How many variants the painter must produce per sprite. The atlas slot
spec already reserves rectangles for every state listed here.

## Interactive elements

| Sprite | States | Notes |
|--------|--------|-------|
| Stepper button (`-` and `+`) | **4**: idle, hover, pressed, disabled | Pressed = inset bevel; disabled = grey-out at ~50% saturation |
| Toggle button (heraldic shield) | **4**: off-idle, off-hover, on-idle, on-hover | "On" version has the green checkmark zone darkened so the runtime checkmark reads |
| X / cancel button | **2**: idle, hover | Hover = brighter red |
| Decree stamp (slot button) | **2**: idle, hover | The stamp item itself is rendered separately on top |

## Static chrome (1 state each)

| Sprite | Notes |
|--------|-------|
| Outer panel frame | 9-slice |
| Inner box frame (tax/toll boxes) | 9-slice |
| Title bar plate | 9-slice with gilded edge |
| Section banner ribbon | Tileable middle, fixed end caps |
| Parchment panel (decree BG) | 9-slice |
| Wax seal medallion | Single sprite with ribbon |
| Hotbar items (coin, emerald, scroll, book, crown) | 16×16 each, single state |

## Icons (1 state each)

| Sprite | Notes |
|--------|-------|
| icon_coin | Used in titles, portal toll values, gate toll value |
| icon_gold_ingot | Treasury row 1 |
| icon_emerald | Treasury row 2 + trade tax box icon |
| icon_land | Land tax box icon (grass-block face) |
| icon_crown | Title-bar decoration, hotbar |
| icon_gate | Gate Toll header (small portcullis) |
| icon_portal | Portal Toll header + portal row indicators |
| ~~icon_skull~~ | **REMOVED** — unrest section deleted from screen |
| icon_wax_seal | Decree right-hand seal |
| icon_decree_stamp | Stamp button face |

## Bars & meters (kept code-drawn)

The traffic-light bar (Total Burden, Unrest) stays drawn in code. It's
4 fixed-color segments with a marker — no painted version needed.

## Realm map / Unrest

Both **removed from the screen**. Don't paint:
- `icon_skull` — was the unrest indicator
- Any realm-map sprite — the section is deleted from the layout

If either is added back later, atlas slot `(112, 0)` is reserved for the
skull, and the bottom half of the atlas (`y ≥ 132`) is reserved for the
map.
