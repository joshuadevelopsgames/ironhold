Convert a Blockbench mob model to a Minecraft NeoForge entity model.

## Required inputs (provide file paths as arguments)
1. **Converted Bedrock .geo.json** — exported from Blockbench via "File > Convert > Bedrock Entity"
2. **Texture PNG** — the painted texture matching the geo.json UV layout
3. **Entity name** — e.g. "mom_pink_deer" (snake_case)

## What this command does

Given a Blockbench geo.json and texture, generate:
- A Java entity model class (`<Name>Model.java`) in `src/main/java/kingdom/smp/client/entity/`
- Copy the texture to `src/main/resources/assets/ironhold/textures/entity/`
- Register the model layer in `IronholdClient.java`

## Conversion rules (learned from mom_pink_deer)

### UV dimensions
- **FLOOR all non-integer dimensions** (7.5→7, 4.5→4, 1.5→1) for UV correctness
- MC uses exact float UV coordinates with `texOffs`, but Blockbench paints textures using floor'd pixel allocation
- Float dimensions cause faces to sample transparent pixels at part boundaries
- Verify zero UV overlaps: for each part, UV footprint = `2*(d+w)` wide × `(d+h)` tall

### Coordinate conversion (Bedrock geo.json → MC entity model)
- Bedrock Y=0 is ground, entity model Y=24 is ground (Y inverted)
- Entity pivot Y = `24 - bedrock_origin_y`
- addBox offset Y = `-(bedrock_to_y - bedrock_origin_y)` (inverted)
- addBox offset X/Z = `bedrock_from - bedrock_origin` (same sign)
- Child pivot offsets are relative: `parent_origin - child_origin` for Y
- Rotations: convert degrees to radians, negate X rotation

### Positioning
- Legs should extend from their pivot down to Y=24 (ground)
- Body pivot Y should be set so body bottom meets leg tops (no floating)
- Neck/tail are children of body — offsets relative to body pivot

### UV offsets
- Use the `"uv": [u, v]` values directly from the geo.json cubes
- These are the texOffs values for MC's `CubeListBuilder.texOffs(u, v)`

### Verification steps
1. Run overlap check: no two parts should share UV pixels
2. Check texture for transparent pixels in UV regions
3. Ensure body bottom meets leg top (no gap)
4. Ensure all child parts attach to their parents

### Model class structure
- Extend `EntityModel<LivingEntityRenderState>`
- Define `LAYER_LOCATION` and `BABY_LAYER` ModelLayerLocations
- Store ModelPart references for animated parts (legs, head, ears, tail)
- `createBodyLayer()` and `createBabyLayer()` static methods
- `setupAnim()` with walk cycle, idle animations, ear/tail movement

### Registration
- Add layer definitions in `IronholdClient.registerLayerDefinitions()`
- Create/update renderer to use the new model
- Register renderer in `IronholdClient.registerEntityRenderers()`
