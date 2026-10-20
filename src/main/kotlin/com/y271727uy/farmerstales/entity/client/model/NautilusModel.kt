package com.y271727uy.farmerstales.entity.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.NautilusEntity
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

/** Port of the 26.1.2 adult nautilus mesh, including its three-part mouth. */
class NautilusModel<T : NautilusEntity>(root: ModelPart) : HierarchicalModel<T>() {
    private val rootPart = root.getChild("root")
    private val body = rootPart.getChild("body")

    override fun root(): ModelPart = rootPart

    override fun setupAnim(
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        body.yRot = Mth.clamp(netHeadYaw, -10.0F, 10.0F) * Mth.DEG_TO_RAD
        body.xRot = Mth.clamp(headPitch, -10.0F, 10.0F) * Mth.DEG_TO_RAD
        rootPart.zRot = Mth.sin(ageInTicks * 0.2F) * 0.04F
        body.xRot += Mth.sin(ageInTicks * 0.2F) * 0.08F
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
        val LAYER_LOCATION = ModelLayerLocation(ResourceLocation(FTMod.MODID, "nautilus"), "main")

        @JvmStatic
        fun createBodyLayer(): LayerDefinition {
            val mesh = MeshDefinition()
            val root = mesh.root.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 29.0F, -6.0F))
            root.addOrReplaceChild(
                "shell",
                CubeListBuilder.create()
                    .texOffs(0, 0).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F)
                    .texOffs(0, 26).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F)
                    .texOffs(48, 26).addBox(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F),
                PartPose.offset(0.0F, -13.0F, 5.0F),
            )
            val body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                    .texOffs(0, 54).addBox(-5.0F, -4.51F, -3.0F, 10.0F, 8.0F, 14.0F)
                    .texOffs(0, 76).addBox(-5.0F, -4.51F, 7.0F, 10.0F, 8.0F, 0.0F),
                PartPose.offset(0.0F, -8.5F, 12.3F),
            )
            body.addOrReplaceChild(
                "upper_mouth",
                CubeListBuilder.create().texOffs(54, 54).addBox(-5.0F, -2.0F, 0.0F, 10.0F, 4.0F, 4.0F, CubeDeformation(-0.001F)),
                PartPose.offset(0.0F, -2.51F, 7.0F),
            )
            body.addOrReplaceChild(
                "inner_mouth",
                CubeListBuilder.create().texOffs(54, 70).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -0.51F, 7.5F),
            )
            body.addOrReplaceChild(
                "lower_mouth",
                CubeListBuilder.create().texOffs(54, 62).addBox(-5.0F, -1.98F, 0.0F, 10.0F, 4.0F, 4.0F, CubeDeformation(-0.001F)),
                PartPose.offset(0.0F, 1.49F, 7.0F),
            )
            return LayerDefinition.create(mesh, 128, 128)
        }
    }
}
