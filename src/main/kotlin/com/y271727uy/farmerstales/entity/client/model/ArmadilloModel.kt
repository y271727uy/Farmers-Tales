package com.y271727uy.farmerstales.entity.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

/** Port of the 26.1.2 adult armadillo geometry; the separate cube is its rolled-up shell. */
class ArmadilloModel(root: ModelPart) : HierarchicalModel<ArmadilloEntity>() {
    private val rootPart = root
    private val body = root.getChild("body")
    private val head = body.getChild("head")
    private val tail = body.getChild("tail")
    private val rightHindLeg = root.getChild("right_hind_leg")
    private val leftHindLeg = root.getChild("left_hind_leg")
    private val rightFrontLeg = root.getChild("right_front_leg")
    private val leftFrontLeg = root.getChild("left_front_leg")
    private val rolledShell = root.getChild("cube")

    override fun root(): ModelPart = rootPart

    override fun setupAnim(
        entity: ArmadilloEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        if (entity.isRolledUp) {
            body.visible = false
            leftHindLeg.visible = false
            rightHindLeg.visible = false
            leftFrontLeg.visible = false
            rightFrontLeg.visible = false
            tail.visible = false
            rolledShell.visible = true
            return
        }

        body.visible = true
        leftHindLeg.visible = true
        rightHindLeg.visible = true
        leftFrontLeg.visible = true
        rightFrontLeg.visible = true
        tail.visible = true
        rolledShell.visible = false
        head.xRot = Mth.clamp(headPitch, -22.5F, 25.0F) * Mth.DEG_TO_RAD
        head.yRot = Mth.clamp(netHeadYaw, -32.5F, 32.5F) * Mth.DEG_TO_RAD

        val stride = Mth.cos(limbSwing * 0.9F) * limbSwingAmount * 0.75F
        leftFrontLeg.xRot = stride
        rightHindLeg.xRot = stride
        rightFrontLeg.xRot = -stride
        leftHindLeg.xRot = -stride
        tail.xRot = 0.5061F + Mth.cos(limbSwing * 0.45F) * limbSwingAmount * 0.08F
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
    ) = rootPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)

    companion object {
        @JvmField
        val LAYER_LOCATION = ModelLayerLocation(ResourceLocation(FTMod.MODID, "armadillo"), "main")

        @JvmStatic
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val root = mesh.root
            val body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                    .texOffs(0, 20).addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, CubeDeformation(0.3F))
                    .texOffs(0, 40).addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, 21.0F, 4.0F),
            )
            body.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(44, 53).addBox(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F),
            )
            val head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -11.0F))
            head.addOrReplaceChild(
                "head_cube",
                CubeListBuilder.create().texOffs(43, 15).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F),
            )
            val rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, 0.0F))
            rightEar.addOrReplaceChild(
                "right_ear_cube",
                CubeListBuilder.create().texOffs(43, 10).addBox(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F),
                PartPose.offsetAndRotation(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F),
            )
            val leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0F, -2.0F, 0.0F))
            leftEar.addOrReplaceChild(
                "left_ear_cube",
                CubeListBuilder.create().texOffs(47, 10).addBox(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F),
                PartPose.offsetAndRotation(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F),
            )
            root.addOrReplaceChild(
                "right_hind_leg",
                CubeListBuilder.create().texOffs(51, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 21.0F, 4.0F),
            )
            root.addOrReplaceChild(
                "left_hind_leg",
                CubeListBuilder.create().texOffs(42, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 21.0F, 4.0F),
            )
            root.addOrReplaceChild(
                "right_front_leg",
                CubeListBuilder.create().texOffs(51, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(-2.0F, 21.0F, -4.0F),
            )
            root.addOrReplaceChild(
                "left_front_leg",
                CubeListBuilder.create().texOffs(42, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 21.0F, -4.0F),
            )
            root.addOrReplaceChild(
                "cube",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F),
            )
            return LayerDefinition.create(mesh, 64, 64)
        }
    }
}
