package com.y271727uy.farmerstales.entity.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.BambooRatEntity
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

/** Model translated from BambooratModel.bbmodel. */
class BambooRatModel(root: ModelPart) : HierarchicalModel<BambooRatEntity>() {
    private val bone = root.getChild("bone")
    private val body = bone.getChild("body")
    private val head = bone.getChild("head")
    private val frontLegs = bone.getChild("front_legs")
    private val rearLegs = bone.getChild("rear_legs")

    override fun root(): ModelPart = bone

    override fun setupAnim(
        entity: BambooRatEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD
        head.xRot = headPitch * Mth.DEG_TO_RAD

        body.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.08F
        frontLegs.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.18F
        rearLegs.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * limbSwingAmount * 0.18F
    }

    override fun renderToBuffer(
        poseStack: PoseStack,
        vertexConsumer: VertexConsumer,
        packedLight: Int,
        packedOverlay: Int,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
    ) {
        bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    companion object {
        @JvmField
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "bamboo_rat"),
            "main",
        )

        @JvmStatic
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val bone = mesh.root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0F, 25.0F, 0.0F))

            val body = bone.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                    .texOffs(1, 0).addBox(-2.0F, -5.0F, -3.0F, 4.0F, 5.0F, 8.0F)
                    .texOffs(22, 18).addBox(0.0F, -4.0F, 5.0F, 1.0F, 1.0F, 2.0F),
                PartPose.ZERO,
            )
            body.addOrReplaceChild(
                "tail_left",
                CubeListBuilder.create().texOffs(22, 21).addBox(0.0F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, -3.0F, 7.0F, 0.0F, -Mth.HALF_PI, 0.0F),
            )
            body.addOrReplaceChild(
                "tail_right",
                CubeListBuilder.create().texOffs(14, 14).addBox(0.0F, -1.0F, -2.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-1.0F, -3.0F, 7.0F, 0.0F, -0.9163F, 0.0F),
            )

            bone.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                    .texOffs(0, 14).addBox(-2.0F, -4.0F, -3.0F, 4.0F, 4.0F, 3.0F)
                    .texOffs(16, 22).addBox(-1.0F, -4.0F, -4.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -1.0F, -3.0F),
            )
            bone.addOrReplaceChild(
                "front_legs",
                CubeListBuilder.create()
                    .texOffs(14, 18).addBox(-3.0F, -1.0F, -4.0F, 1.0F, 1.0F, 3.0F)
                    .texOffs(0, 21).addBox(2.0F, -1.0F, -4.0F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO,
            )
            bone.addOrReplaceChild(
                "rear_legs",
                CubeListBuilder.create()
                    .texOffs(8, 22).addBox(-3.0F, -1.0F, 2.0F, 1.0F, 1.0F, 3.0F)
                    .texOffs(22, 14).addBox(2.0F, -1.0F, 2.0F, 1.0F, 1.0F, 3.0F),
                PartPose.ZERO,
            )
            return LayerDefinition.create(mesh, 32, 32)
        }
    }
}
