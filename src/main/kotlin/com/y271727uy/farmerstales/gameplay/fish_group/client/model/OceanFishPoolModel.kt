package com.y271727uy.farmerstales.gameplay.fish_group.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.entity.OceanFishPoolAnimation
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
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

class OceanFishPoolModel(root: ModelPart) : HierarchicalModel<OceanFishPoolEntity>() {
    private val oceanSwarm = root.getChild("ocean_swarm")

    override fun setupAnim(
        entity: OceanFishPoolEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        animate(entity.idleAnimationState, OceanFishPoolAnimation.idle, ageInTicks, 1.0F)
    }

    override fun root(): ModelPart = oceanSwarm

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
        oceanSwarm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    companion object {
        @JvmField
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "ocean_fish_pool"),
            "main",
        )

        @JvmStatic
        fun getTexturedModelData(): LayerDefinition {
            val mesh = MeshDefinition()
            val oceanSwarm = mesh.root.addOrReplaceChild(
                "ocean_swarm",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 22.0F, 0.0F),
            )
            addLargePuffer(oceanSwarm)
            addMediumPuffer(
                oceanSwarm,
                "salmon_2",
                PartPose.offset(12.0F, 17.0F, -3.0F),
                "body_mid",
                "5",
                "",
            )
            addMediumPuffer(
                oceanSwarm,
                "salmon_3",
                PartPose.offset(-17.0F, 24.0F, 14.0F),
                "body_mid2",
                "6",
                "3",
            )
            addOceanFish(
                oceanSwarm,
                3,
                "body",
                "3",
                "3",
                PartPose.offset(5.0F, 5.0F, -4.1667F),
                PartPose.offset(-0.5F, 2.0F, -0.8333F)
            )
            addOceanFish(
                oceanSwarm,
                2,
                "body2",
                "2",
                "2",
                PartPose.offset(9.0F, 10.0F, 10.0F),
                PartPose.offset(-0.5F, 0.0F, 4.0F)
            )
            addOceanFish(
                oceanSwarm,
                1,
                "body3",
                "4",
                "7",
                PartPose.offset(-10.0F, 6.0F, 3.8333F),
                PartPose.offset(-0.5F, 2.0F, 0.1667F)
            )
            return LayerDefinition.create(mesh, 48, 48)
        }

        private fun addLargePuffer(parent: PartDefinition) {
            val salmon = parent.addOrReplaceChild(
                "salmon_1",
                CubeListBuilder.create(),
                PartPose.offset(-7.0F, 15.0F, 20.0F),
            )
            val body = salmon.addOrReplaceChild(
                "body_large",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 4.0F, -1.0F),
            )
            body.addOrReplaceChild(
                "leftFin4",
                CubeListBuilder.create().texOffs(24, 3)
                    .addBox(0.0F, 0.0F, -5.9904F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offset(4.0F, -7.0F, 3.0F)
            )
            body.addOrReplaceChild(
                "rightFin4",
                CubeListBuilder.create().texOffs(24, 0)
                    .addBox(-1.9968F, 0.0F, -3.992F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, -7.0F, 1.0F)
            )
            body.addOrReplaceChild(
                "spines_top_front2",
                CubeListBuilder.create().texOffs(14, 16)
                    .addBox(0.0F, -1.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, -8.0F, -4.0F, 0.7854F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_top_mid",
                CubeListBuilder.create().texOffs(14, 16)
                    .addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -8.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_top_back2",
                CubeListBuilder.create().texOffs(14, 16)
                    .addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.0F, 4.0F, -0.7854F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_bottom_front2",
                CubeListBuilder.create().texOffs(14, 19)
                    .addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.7854F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_bottom_mid",
                CubeListBuilder.create().texOffs(14, 19)
                    .addBox(-4.0F, -1.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_bottom_back2",
                CubeListBuilder.create().texOffs(14, 19)
                    .addBox(-4.0F, 0.0F, 0.0F, 8.0F, 1.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.7854F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_left_front2",
                CubeListBuilder.create().texOffs(0, 16)
                    .addBox(0.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, -4.0F, 0.0F, 0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_left_mid",
                CubeListBuilder.create().texOffs(4, 16).mirror()
                    .addBox(0.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(4.0F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_left_back2",
                CubeListBuilder.create().texOffs(8, 16).mirror()
                    .addBox(0.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(4.0F, 0.0F, 4.0F, 0.0F, -0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_right_front2",
                CubeListBuilder.create().texOffs(4, 16)
                    .addBox(-1.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, -4.0F, 0.0F, -0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_right_mid",
                CubeListBuilder.create().texOffs(8, 16)
                    .addBox(-1.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_right_back2",
                CubeListBuilder.create().texOffs(8, 16)
                    .addBox(-1.0F, -8.0F, 0.0F, 1.0F, 8.0F, 1.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.0F, 0.0F, 4.0F, 0.0F, 0.7854F, 0.0F)
            )
        }

        private fun addMediumPuffer(
            parent: PartDefinition,
            salmonName: String,
            salmonPose: PartPose,
            bodyName: String,
            finSuffix: String,
            spineSuffix: String,
        ) {
            val salmon = parent.addOrReplaceChild(salmonName, CubeListBuilder.create(), salmonPose)
            val body = salmon.addOrReplaceChild(
                bodyName,
                CubeListBuilder.create().texOffs(12, 22)
                    .addBox(-2.5F, -6.0F, -2.5F, 5.0F, 5.0F, 5.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 4.0F, 0.0F),
            )
            body.addOrReplaceChild(
                "leftFin$finSuffix",
                CubeListBuilder.create().texOffs(24, 3)
                    .addBox(0.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offset(2.5F, -5.0F, 0.5F)
            )
            body.addOrReplaceChild(
                "rightFin$finSuffix",
                CubeListBuilder.create().texOffs(24, 0)
                    .addBox(-2.0F, 0.0F, -2.0F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offset(-2.5F, -5.0F, 0.5F)
            )
            body.addOrReplaceChild(
                "spines_top_front$spineSuffix",
                CubeListBuilder.create().texOffs(19, 17)
                    .addBox(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -6.0F, -2.5F)
            )
            body.addOrReplaceChild(
                "spines_top_back$spineSuffix",
                CubeListBuilder.create().texOffs(11, 17)
                    .addBox(-2.5F, -1.0F, 0.0F, 5.0F, 1.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -6.0F, 2.5F)
            )
            body.addOrReplaceChild(
                "spines_bottom_front$spineSuffix",
                CubeListBuilder.create().texOffs(18, 20)
                    .addBox(-2.5F, 0.0F, 0.0F, 5.0F, 1.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -1.0F, -2.5F)
            )
            body.addOrReplaceChild(
                "spines_bottom_back$spineSuffix",
                CubeListBuilder.create().texOffs(18, 20)
                    .addBox(-2.5F, 0.0F, 0.0F, 5.0F, 1.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -1.0F, 2.5F, 0.7854F, 0.0F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_left_front$spineSuffix",
                CubeListBuilder.create().texOffs(1, 17)
                    .addBox(0.0F, -6.0F, 0.0F, 1.0F, 5.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.5F, 0.0F, -2.5F, 0.0F, 0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_left_back$spineSuffix",
                CubeListBuilder.create().texOffs(1, 17)
                    .addBox(0.0F, -6.0F, 0.0F, 1.0F, 5.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.5F, 0.0F, 2.5F, 0.0F, -0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_right_front$spineSuffix",
                CubeListBuilder.create().texOffs(5, 17)
                    .addBox(-1.0F, -6.0F, 0.0F, 1.0F, 5.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.5F, 0.0F, -2.5F, 0.0F, -0.7854F, 0.0F)
            )
            body.addOrReplaceChild(
                "spines_right_back$spineSuffix",
                CubeListBuilder.create().texOffs(9, 17)
                    .addBox(-1.0F, -6.0F, 0.0F, 1.0F, 5.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.5F, 0.0F, 2.5F, 0.0F, 0.7854F, 0.0F)
            )
        }

        private fun addOceanFish(
            parent: PartDefinition,
            index: Int,
            bodyName: String,
            tailSuffix: String,
            finSuffix: String,
            fishPose: PartPose,
            bodyPose: PartPose,
        ) {
            val fish = parent.addOrReplaceChild("cod_$index", CubeListBuilder.create(), fishPose)
            val body = fish.addOrReplaceChild(
                bodyName,
                CubeListBuilder.create().texOffs(32, 0)
                    .addBox(-0.5F, -3.0F, -3.0F, 2.0F, 3.0F, 6.0F, CubeDeformation(0.0F))
                    .texOffs(36, 9)
                    .addBox(0.5F, -7.0F, -2.9992F, 0.0F, 4.0F, 6.0F, CubeDeformation(0.0F)),
                bodyPose,
            )
            body.addOrReplaceChild(
                "tailfin$tailSuffix",
                CubeListBuilder.create().texOffs(40, 5).mirror()
                    .addBox(0.0F, -3.0F, 0.0F, 0.0F, 3.0F, 4.0F, CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(0.5F, 0.0F, 3.0F)
            )
            body.addOrReplaceChild(
                "leftFin$finSuffix",
                CubeListBuilder.create().texOffs(32, 15)
                    .addBox(-0.164F, -2.0F, -1.1059F, 2.0F, 2.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, 0.0F, -0.6109F, 0.0F)
            )
            body.addOrReplaceChild(
                "rightFin$finSuffix",
                CubeListBuilder.create().texOffs(32, 19)
                    .addBox(-1.836F, -2.0F, -1.1059F, 2.0F, 2.0F, 0.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 0.6109F, 0.0F)
            )
        }
    }
}
