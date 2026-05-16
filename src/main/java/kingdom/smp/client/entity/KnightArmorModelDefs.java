package kingdom.smp.client.entity;

import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.geom.PartPose;

/**
 * Custom 3D helmet geometry ported byte-for-byte from Epic Knights: Shields & Weapons & Armor
 * (Fabric 1.21, jar epic-knights-1.21.1-fabric-9.30.jar).
 *
 * The original Fabric mod used Fabric-API's ArmorRenderer which calls
 * HumanoidModel.copyPropertiesTo(custom) — this overwrites the custom head's PartPose
 * with the vanilla head's pose at render time, so the original .createLayer() definitions
 * carry "decorative" head offsets like PartPose.offset(-4,-8,-4) that get neutralized at runtime.
 *
 * NeoForge 1.26 has NO copyPropertiesTo equivalent — HumanoidArmorLayer renders the model
 * with whatever PartPose was baked in. So this port reproduces the *runtime* state of the
 * original models: head at PartPose.ZERO, cubes/sub-bones unchanged.
 *
 * Two structural differences from the Fabric original:
 *   1. "hat" must be a child of "head" (in MC 1.26, HumanoidModel reads
 *      this.hat = head.getChild("hat"); in 1.21 it was root.getChild("hat")).
 *   2. The decorative head PartPose offsets are dropped (replaced with PartPose.ZERO),
 *      since copyPropertiesTo no longer exists to neutralize them.
 *
 * Empty body/arm/leg parts are required by the HumanoidModel constructor — they are
 * never rendered for a helmet item because the equipment JSON only references the
 * "humanoid" head layer, but HumanoidModel still calls root.getChild() on each.
 */
public class KnightArmorModelDefs {

    /** Kettlehat — used by knight_recruit. Wide flat brim around the helmet. */
    public static LayerDefinition createKettlehat() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(1.0f)),
            PartPose.ZERO);
        head.addOrReplaceChild("helmet_r1",
            CubeListBuilder.create()
                .texOffs(0, 18).addBox(-3.0f, 3.7f, -3.0f, 14.0f, -1.9f, 14.0f, new CubeDeformation(1.0f)),
            PartPose.offset(-4.0f, -8.0f, -4.0f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Bascinet — used by knight_man_at_arms and knight_armored. Closed-face helmet with cheek panels. */
    public static LayerDefinition createBascinet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -7.95f, -3.2f, 8.0f, 7.0f, 7.0f, new CubeDeformation(1.0f))
                .texOffs(6, 6).addBox(-4.0f, -7.95f, -3.7f, 8.0f, 7.0f, 1.0f, new CubeDeformation(0.97f)),
            PartPose.ZERO);
        head.addOrReplaceChild("cube_r1",
            CubeListBuilder.create()
                .texOffs(34, 0).addBox(-4.0f, 0.05f, -6.0f, 8.0f, 0.0f, 7.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, -5.0f, -4.7f, 0.5672f, 0.0f, 0.0f));
        head.addOrReplaceChild("cube_r2",
            CubeListBuilder.create()
                .texOffs(36, 8).addBox(-4.0f, -0.078f, -1.5337f, 8.0f, 0.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, -1.872f, -7.8663f, -0.2618f, 0.0f, 0.0f));
        head.addOrReplaceChild("cube_r3",
            CubeListBuilder.create()
                .texOffs(0, 14).addBox(-4.0f, 0.85f, -2.0f, 8.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, -7.0f, -5.5f, 0.7854f, 0.0f, 0.0f));
        head.addOrReplaceChild("cube_r4",
            CubeListBuilder.create()
                .texOffs(50, 21).addBox(-0.4f, -1.45f, -6.3f, 0.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(4.0f, -3.5f, -5.0f, 0.0f, 0.6615f, 0.0f));
        head.addOrReplaceChild("cube_r5",
            CubeListBuilder.create()
                .mirror().texOffs(50, 21).addBox(-5.95f, -1.45f, -1.37f, 0.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(4.0f, -3.5f, -5.0f, 0.0f, -0.6615f, 0.0f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Barbute — used by knight_crossbowman. Italian open-face helmet with T-shaped opening. */
    public static LayerDefinition createBarbute() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -7.9f, -4.2f, 8.0f, 7.0f, 8.0f, new CubeDeformation(1.0f)),
            PartPose.ZERO);

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Crusader — used by knight_crusader. Great helm with flat front face plate. */
    public static LayerDefinition createCrusader() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.7f))
                .texOffs(33, 7).addBox(-4.5f, -8.5f, -3.7f, 9.0f, 8.0f, 1.0f, new CubeDeformation(1.3f)),
            PartPose.ZERO);

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Armet — used by knight_gothic and knight_gold. Full-face helmet with articulated visor. */
    public static LayerDefinition createArmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("Armet",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.0f, 0.05f, 0.8f, 8.0f, 7.0f, 7.0f, new CubeDeformation(1.0f))
                .texOffs(6, 6).addBox(0.0f, 0.05f, 0.3f, 8.0f, 7.0f, 1.0f, new CubeDeformation(0.97f)),
            PartPose.offset(-4.0f, -8.0f, -4.0f));
        head.addOrReplaceChild("VisorTopLeft",
            CubeListBuilder.create()
                .texOffs(47, 0).addBox(0.4f, 0.65f, -2.6f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(-4.0f, -8.0f, -4.0f, -0.7285f, -0.6829f, 0.0f));
        head.addOrReplaceChild("VisorTopRight",
            CubeListBuilder.create()
                .mirror().texOffs(47, 0).addBox(-1.1f, -2.6f, 1.1f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(-4.0f, -8.0f, -4.0f, -0.7285f, 0.6374f, 0.0f));
        head.addOrReplaceChild("VisorBottomLeft",
            CubeListBuilder.create()
                .mirror().texOffs(30, 0).addBox(0.15f, -1.5f, -6.9f, 7.0f, 6.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(-4.0f, -8.0f, -4.0f, 0.6829f, -0.6829f, 0.0f));
        head.addOrReplaceChild("VisorBottomRight",
            CubeListBuilder.create()
                .texOffs(30, 0).addBox(-0.95f, 1.75f, -2.9f, 7.0f, 6.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(-4.0f, -8.0f, -4.0f, 0.6829f, 0.6829f, 0.0f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Sallet — late-medieval helmet with a sloped tail at the back. */
    public static LayerDefinition createSallet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0f, -7.9f, -4.2f, 8.0f, 7.0f, 8.0f, new CubeDeformation(1.0f)),
            PartPose.ZERO);
        head.addOrReplaceChild("Helmet_r1",
            CubeListBuilder.create()
                .texOffs(52, 26).addBox(-4.7f, -2.6f, -4.7f, 3.0f, 3.0f, 3.0f, new CubeDeformation(-0.2f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7854f, 0.0f));
        head.addOrReplaceChild("Helmet_r2",
            CubeListBuilder.create()
                .texOffs(0, 10).addBox(-9.3f, 5.6f, 1.67f, 8.0f, 0.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(6.25f, 0.0f, 0.0f, -1.789f, 0.1309f, 1.5708f));
        head.addOrReplaceChild("Helmet_r3",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-9.3f, 6.9f, -1.806f, 8.0f, 0.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(-0.275f, 0.0f, 0.0f, -1.3526f, 0.1309f, 1.5708f));
        // Tail / neck guard sub-bone with several flap pieces hanging off it.
        PartDefinition bone = head.addOrReplaceChild("bone",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(-4.0f, 0.6f, -2.9f, 0.1309f, 0.0f, 0.0f));
        bone.addOrReplaceChild("Helmet_r4",
            CubeListBuilder.create()
                .mirror().texOffs(12, 6).addBox(-4.0f, -2.0f, 0.0f, 8.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(9.3301f, -6.3606f, 0.4988f, -1.5708f, -0.2618f, -1.5708f));
        bone.addOrReplaceChild("Helmet_r5",
            CubeListBuilder.create()
                .texOffs(12, 6).addBox(-4.0f, -2.0f, 0.0f, 8.0f, 4.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(-1.3301f, -6.3606f, 0.4988f, -1.5708f, 0.2618f, 1.5708f));
        bone.addOrReplaceChild("Helmet_r6",
            CubeListBuilder.create()
                .texOffs(20, 11).addBox(-3.3f, -9.5f, -2.6f, 1.0f, 11.0f, 10.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.2618f, 1.5708f));
        bone.addOrReplaceChild("Helmet_r7",
            CubeListBuilder.create()
                .texOffs(0, 22).addBox(-2.501f, -4.7f, -6.6f, 1.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 3.0f, -2.0071f, 0.2618f, 1.5708f));
        bone.addOrReplaceChild("Helmet_r8",
            CubeListBuilder.create()
                .texOffs(0, 22).addBox(-2.5f, -6.4f, -3.01f, 1.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(4.0f, 0.0f, 3.0f, -1.1345f, 0.2618f, 1.5708f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Stechhelm — jousting helm with massive frog-mouth front. */
    public static LayerDefinition createStechhelm() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(34, 0).addBox(-4.0f, -8.0f, -3.2f, 8.0f, 7.0f, 7.0f, new CubeDeformation(1.0f))
                .texOffs(40, 6).addBox(-4.0f, -7.95f, -3.7f, 8.0f, 7.0f, 1.0f, new CubeDeformation(0.97f)),
            PartPose.ZERO);
        head.addOrReplaceChild("helmet_r1",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-10.001f, -4.2f, -2.46f, 8.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 1.0472f, -0.4363f, 1.5708f));
        head.addOrReplaceChild("helmet_r2",
            CubeListBuilder.create()
                .texOffs(0, 12).addBox(-10.0f, -4.2f, -4.54f, 8.0f, 4.0f, 7.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 2.0944f, -0.4363f, 1.5708f));
        head.addOrReplaceChild("helmet_r3",
            CubeListBuilder.create()
                .texOffs(0, 23).addBox(-4.0f, -9.3f, -4.0f, 8.0f, 1.0f, 8.0f, new CubeDeformation(0.3f)),
            PartPose.ZERO);
        head.addOrReplaceChild("helmet_r4",
            CubeListBuilder.create()
                .mirror().texOffs(54, 8).addBox(-9.96f, -4.16f, 4.0f, 2.0f, 3.0f, 0.0f, new CubeDeformation(0.03f)).mirror(false),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 1.0472f, -0.4363f, 1.5708f));
        head.addOrReplaceChild("helmet_r5",
            CubeListBuilder.create()
                .texOffs(56, 8).addBox(-9.96f, -4.16f, -4.0f, 2.0f, 3.0f, 0.0f, new CubeDeformation(0.03f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 2.0944f, -0.4363f, 1.5708f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Maximilian Helmet — fluted late-Gothic close-helm with hinged visor flaps. */
    public static LayerDefinition createMaximilianHelmet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0f, -7.9f, -3.2f, 8.0f, 7.0f, 7.0f, new CubeDeformation(1.0f))
                .texOffs(38, 6).addBox(-4.0f, -7.9f, -3.7f, 8.0f, 7.0f, 1.0f, new CubeDeformation(0.97f)),
            PartPose.ZERO);
        head.addOrReplaceChild("Helmet_r1",
            CubeListBuilder.create()
                .texOffs(0, 5).addBox(-3.6f, -7.2f, -3.75f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.552f, -0.4648f, 0.0f));
        head.addOrReplaceChild("Helmet_r2",
            CubeListBuilder.create()
                .mirror().texOffs(0, 5).addBox(-3.4f, -7.2f, -3.75f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.552f, 0.4648f, 0.0f));
        head.addOrReplaceChild("Helmet_r3",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.6f, -4.9f, -8.1f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.552f, -0.4648f, 0.0f));
        head.addOrReplaceChild("Helmet_r4",
            CubeListBuilder.create()
                .mirror().texOffs(0, 0).addBox(-3.4f, -4.9f, -8.1f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.552f, 0.4648f, 0.0f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Grand Bascinet — bascinet with flared lower edges plus extra visor panels. */
    public static LayerDefinition createGrandBascinet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0f, -7.9f, -3.2f, 8.0f, 7.0f, 7.0f, new CubeDeformation(1.0f))
                .texOffs(38, 6).addBox(-4.0f, -7.9f, -3.7f, 8.0f, 7.0f, 1.0f, new CubeDeformation(0.97f)),
            PartPose.ZERO);
        head.addOrReplaceChild("cube_r1",
            CubeListBuilder.create()
                .texOffs(0, 10).addBox(-0.0354f, -0.5f, 0.0f, 5.0f, 1.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0354f, -5.2f, -8.1841f, 0.0f, -0.4648f, 0.0f));
        head.addOrReplaceChild("cube_r2",
            CubeListBuilder.create()
                .mirror().texOffs(0, 10).addBox(-5.0354f, -0.5f, 0.0f, 5.0f, 1.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(0.0354f, -5.2f, -8.1841f, 0.0f, 0.4648f, 0.0f));
        head.addOrReplaceChild("cube_r3",
            CubeListBuilder.create()
                .texOffs(23, 19).addBox(0.2f, -2.6856f, -2.5f, 0.0f, 5.0f, 5.0f, new CubeDeformation(0.0f))
                .texOffs(23, 19).addBox(9.0f, -2.6856f, -2.5f, 0.0f, 5.0f, 5.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(-4.6f, -4.9144f, -3.9244f, -0.829f, 0.0f, 0.0f));
        head.addOrReplaceChild("cube_r4",
            CubeListBuilder.create()
                .texOffs(0, 5).addBox(-3.8f, -8.1f, -3.75f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.552f, -0.4648f, 0.0f));
        head.addOrReplaceChild("cube_r5",
            CubeListBuilder.create()
                .mirror().texOffs(0, 5).addBox(-3.2f, -8.1f, -3.75f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.552f, 0.4648f, 0.0f));
        head.addOrReplaceChild("cube_r6",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5f, -4.931f, -0.1089f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)),
            PartPose.offsetAndRotation(2.9849f, -5.569f, -6.6221f, -0.7266f, -0.4648f, 0.0f));
        head.addOrReplaceChild("cube_r7",
            CubeListBuilder.create()
                .mirror().texOffs(0, 0).addBox(-3.5f, -4.8762f, -0.108f, 7.0f, 5.0f, 0.0f, new CubeDeformation(0.0f)).mirror(false),
            PartPose.offsetAndRotation(-2.9697f, -5.6238f, -6.592f, -0.7266f, 0.4648f, 0.0f));

        addEmptyHumanoidParts(root, head);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Adds the placeholder parts that HumanoidModel(ModelPart) requires.
     *
     * MC 1.26 layout (was different in 1.21 — see class javadoc):
     *   root
     *   ├── head  ← caller fills with helmet geometry
     *   │   └── hat        (empty placeholder; this is the only part not on root)
     *   ├── body           (empty placeholder)
     *   ├── right_arm      (empty placeholder)
     *   ├── left_arm       (empty placeholder)
     *   ├── right_leg      (empty placeholder)
     *   └── left_leg       (empty placeholder)
     */
    private static void addEmptyHumanoidParts(PartDefinition root, PartDefinition head) {
        head.addOrReplaceChild("hat",       CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body",      CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm",  CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg",  CubeListBuilder.create(), PartPose.ZERO);
    }
}
