package com.y271727uy.farmerstales.entity.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

/** Direct translation of rat.bbmodel using Blockbench's 1.17+ Java-export coordinates. */
class RatModel(root: ModelPart) : HierarchicalModel<RatEntity>() {
    private val all = root.getChild("all")
    private val upperPart = all.getChild("body").getChild("upper_part")
    private val head = upperPart.getChild("head")
    private val leftArm = upperPart.getChild("left_arm")
    private val rightArm = upperPart.getChild("right_arm")
    private val leftThigh = all.getChild("left_thigh")
    private val rightThigh = all.getChild("right_thigh")

    override fun root(): ModelPart = all

    override fun setupAnim(
        entity: RatEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD
        head.xRot = headPitch * Mth.DEG_TO_RAD

        val swing = Mth.cos(limbSwing * 0.8F) * limbSwingAmount * 0.45F
        leftArm.xRot = swing
        rightArm.xRot = -swing
        leftThigh.xRot = -swing
        rightThigh.xRot = swing
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
        all.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    companion object {
        @JvmField
        val LAYER_LOCATION = ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "rat"), "main")

        @JvmStatic
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val all = mesh.root.addOrReplaceChild("all", CubeListBuilder.create(), PartPose.offset(0.0F, 27.0F, 0.0F))
            val body = all.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -6.0F, 3.0F))
            val upperPart = body.addOrReplaceChild(
                "upper_part",
                CubeListBuilder.create()
                    .texOffs(14, 0).addBox(-2.0F, 0.0F, -4.6F, 4.0F, 3.0F, 2.0F, CubeDeformation(-0.2F))
                    .texOffs(14, 0).addBox(-2.0F, 0.0F, -5.0F, 4.0F, 3.0F, 2.0F, CubeDeformation(-0.2F))
                    .texOffs(0, 0).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 4.0F, 3.0F, CubeDeformation(-0.2F))
                    .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.6F, 4.0F, 4.0F, 3.0F, CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, -2.0F, -2.0F),
            )
            val head = upperPart.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 2.5F, -4.0F),
            )
            val bone3 = head.addOrReplaceChild(
                "bone3",
                CubeListBuilder.create().texOffs(0, 7).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 4.0F, CubeDeformation(-0.1F)),
                PartPose.offset(0.0F, -1.5F, -3.0F),
            )
            val bone4 = bone3.addOrReplaceChild(
                "bone4",
                CubeListBuilder.create().texOffs(15, 5).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -1.0F),
            )
            bone4.addOrReplaceChild(
                "nose",
                CubeListBuilder.create()
                    .texOffs(26, 6).addBox(-0.5F, 0.0F, -5.5F, 1.0F, 1.0F, 1.0F)
                    .texOffs(14, 28).addBox(-2.5F, -1.0F, -5.75F, 5.0F, 3.0F, 1.0F, CubeDeformation(-0.5F)),
                PartPose.offset(0.0F, -0.5F, 4.0F),
            )
            head.addOrReplaceChild(
                "right_ear",
                CubeListBuilder.create().texOffs(0, 26).addBox(1.5F, -2.0F, -1.0F, 2.0F, 2.0F, 1.0F, CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, -2.0F, 0.0F),
            )
            head.addOrReplaceChild(
                "left_ear",
                CubeListBuilder.create().texOffs(6, 24).addBox(-3.5F, -2.0F, -1.0F, 2.0F, 2.0F, 1.0F, CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, -2.0F, 0.0F),
            )

            val leftArm = upperPart.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(22, 21).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offset(2.0F, 1.0F, -4.0F),
            )
            leftArm.addOrReplaceChild(
                "left_paw",
                CubeListBuilder.create().texOffs(23, 14).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 1.0F, 2.0F, CubeDeformation(-0.02F)),
                PartPose.offset(0.0F, 4.0F, 0.0F),
            )
            val rightArm = upperPart.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(0, 20).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offset(-2.0F, 1.0F, -4.0F),
            )
            rightArm.addOrReplaceChild(
                "right_paw",
                CubeListBuilder.create().texOffs(23, 10).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 1.0F, 2.0F, CubeDeformation(-0.02F)),
                PartPose.offset(0.0F, 4.0F, 0.0F),
            )

            val lowerPart = body.addOrReplaceChild(
                "lower_part",
                CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-2.0F, -8.0F, 1.0F, 4.0F, 4.0F, 3.0F, CubeDeformation(-0.2F))
                    .texOffs(14, 23).addBox(-1.5F, -7.25F, 3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 6.0F, -3.0F),
            )
            val tail = lowerPart.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(6, 20).addBox(-0.5F, -0.25F, -1.0F, 1.0F, 1.0F, 3.0F, CubeDeformation(-0.15F)),
                PartPose.offset(0.0F, -6.75F, 4.5F),
            )
            tail.addOrReplaceChild(
                "sec_tail",
                CubeListBuilder.create().texOffs(22, 17).addBox(-0.5F, -0.5F, -0.8F, 1.0F, 1.0F, 3.0F, CubeDeformation(-0.2F)),
                PartPose.offset(0.0F, 0.25F, 1.8F),
            )

            val leftThigh = all.addOrReplaceChild(
                "left_thigh",
                CubeListBuilder.create()
                    .texOffs(14, 9).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 3.0F, 3.0F)
                    .texOffs(15, 10).addBox(-0.5F, 1.0F, -1.0F, 1.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, -6.0F, 2.5F),
            )
            leftThigh.addOrReplaceChild(
                "left_foot",
                CubeListBuilder.create().texOffs(26, 0).addBox(0.5F, -1.0F, -2.0F, 1.0F, 1.0F, 2.0F, CubeDeformation(-0.02F)),
                PartPose.offset(-1.0F, 3.0F, 0.0F),
            )
            val rightThigh = all.addOrReplaceChild(
                "right_thigh",
                CubeListBuilder.create()
                    .texOffs(14, 16).addBox(-0.5F, -1.0F, -1.0F, 1.0F, 3.0F, 3.0F)
                    .texOffs(15, 17).addBox(-0.5F, 2.0F, -1.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offset(-2.0F, -6.0F, 2.5F),
            )
            rightThigh.addOrReplaceChild(
                "right_foot",
                CubeListBuilder.create().texOffs(26, 3).addBox(-0.5F, -1.0F, -2.0F, 1.0F, 1.0F, 2.0F, CubeDeformation(-0.02F)),
                PartPose.offset(0.0F, 3.0F, 0.0F),
            )
            return LayerDefinition.create(mesh, 32, 32)
        }
    }
}
