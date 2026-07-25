package com.y271727uy.farmerstales.gameplay.fish_group.client.model

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.entity.RiverFishPoolAnimation
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import net.minecraft.client.model.HierarchicalModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.resources.ResourceLocation

class RiverFishPoolModel(root: ModelPart) : HierarchicalModel<RiverFishPoolEntity>() {
    private val riverSwarm = root.getChild("river_swarm")

    override fun setupAnim(
        entity: RiverFishPoolEntity,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        root().allParts.forEach(ModelPart::resetPose)
        animate(entity.idleAnimationState, RiverFishPoolAnimation.idle, ageInTicks, 1.0F)
    }

    override fun root(): ModelPart = riverSwarm

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
        riverSwarm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha)
    }

    companion object {
        @JvmField
        val LAYER_LOCATION = ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "river_fish_pool"),
            "main",
        )

        @JvmStatic
        fun getTexturedModelData(): LayerDefinition {
            val mesh = MeshDefinition()
            val riverSwarm = mesh.root.addOrReplaceChild(
                "river_swarm",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F),
            )

            repeat(3) { index ->
                val suffix = if (index == 0) "" else (index + 1).toString()
                val salmonName = "salmon_${index + 1}"
                val positions = arrayOf(
                    PartPose.offset(-7.0F, 10.0F, 20.0F),
                    PartPose.offset(12.0F, 15.0F, -3.0F),
                    PartPose.offset(-17.0F, 18.0F, 14.0F),
                )
                val salmon = riverSwarm.addOrReplaceChild(
                    salmonName,
                    CubeListBuilder.create().texOffs(0, 11)
                        .addBox(-1.5F, -2.5F, -4.0F, 3.0F, 5.0F, 8.0F, CubeDeformation(0.0F)),
                    positions[index],
                )
                val bodySuffix = if (index == 0) "" else (index + 1).toString()
                val finSuffix = (index + 4).toString()
                val bodyBack = salmon.addOrReplaceChild(
                    "body_back$bodySuffix",
                    CubeListBuilder.create().texOffs(0, 24)
                        .addBox(-1.5F, -8.5F, 0.0F, 3.0F, 5.0F, 8.0F, CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 6.0F, 4.0F),
                )
                bodyBack.addOrReplaceChild(
                    "dorsal_back$bodySuffix",
                    CubeListBuilder.create().texOffs(2, 14)
                        .addBox(0.0F, -5.5F, 0.0F, 0.0F, 2.0F, 3.0F, CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, -5.0F, 0.0F),
                )
                bodyBack.addOrReplaceChild(
                    "tailfin$finSuffix",
                    CubeListBuilder.create().texOffs(20, 21)
                        .addBox(0.0F, -8.5F, 0.0F, 0.0F, 5.0F, 6.0F, CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 0.0F, 8.0F),
                )
                salmon.addOrReplaceChild(
                    "dorsal_front$bodySuffix",
                    CubeListBuilder.create().texOffs(4, 13)
                        .addBox(0.0F, -5.5F, 0.0F, 0.0F, 2.0F, 2.0F, CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 1.0F, 2.0F),
                )
                salmon.addOrReplaceChild(
                    "head$finSuffix",
                    CubeListBuilder.create().texOffs(22, 11)
                        .addBox(-1.0F, -5.5F, -3.0F, 2.0F, 4.0F, 3.0F, CubeDeformation(0.0F)),
                    PartPose.offset(0.0F, 3.0F, -4.0F),
                )
                salmon.addOrReplaceChild(
                    "leftFin$finSuffix",
                    CubeListBuilder.create().texOffs(2, 11)
                        .addBox(-2.0075F, -2.867F, 0.0F, 2.0F, 0.0F, 2.0F, CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(1.5F, 5.0F, -4.0F, 0.0F, 0.0F, 0.6109F),
                )
                salmon.addOrReplaceChild(
                    "rightFin$finSuffix",
                    CubeListBuilder.create().texOffs(-2, 11)
                        .addBox(0.0074F, -2.867F, 0.0F, 2.0F, 0.0F, 2.0F, CubeDeformation(0.0F)),
                    PartPose.offsetAndRotation(-1.5F, 5.0F, -4.0F, 0.0F, 0.0F, -0.6109F),
                )
            }

            addCod(riverSwarm, 3, PartPose.offset(5.0F, 5.0F, -4.1667F), -2.8333F, -3.8333F, 2.0F, 4.1667F)
            addCod(riverSwarm, 2, PartPose.offset(9.0F, 7.0F, 10.0F), 1.0F, 0.0F, 0.0F, 8.0F)
            addCod(riverSwarm, 1, PartPose.offset(-10.0F, 8.0F, 3.8333F), -2.8333F, -3.8333F, 2.0F, 4.1667F)
            return LayerDefinition.create(mesh, 48, 48)
        }

        private fun addCod(
            parent: net.minecraft.client.model.geom.builders.PartDefinition,
            index: Int,
            pose: PartPose,
            bodyZ: Float,
            frontZ: Float,
            tailY: Float,
            tailZ: Float,
        ) {
            val suffix = if (index == 1) "" else index.toString()
            val cod = parent.addOrReplaceChild(
                "cod_$index",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-1.0F, if (index == 2) -4.0F else -2.0F, bodyZ, 2.0F, 4.0F, 7.0F, CubeDeformation(0.0F))
                    .texOffs(20, -6)
                    .addBox(0.0F, if (index == 2) -5.0F else -3.0F, frontZ, 0.0F, 1.0F, 6.0F, CubeDeformation(0.0F))
                    .texOffs(22, -1)
                    .addBox(
                        0.0F,
                        if (index == 2) 0.0F else 2.0F,
                        if (index == 2) 3.0F else -0.8333F,
                        0.0F,
                        1.0F,
                        2.0F,
                        CubeDeformation(0.0F)
                    ),
                pose,
            )
            cod.addOrReplaceChild(
                "head$suffix",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-0.9992F, -2.0008F, -3.0F, 2.0F, 3.0F, 1.0F, CubeDeformation(0.0F))
                    .texOffs(11, 0)
                    .addBox(-1.0F, -2.0F, -2.0F, 2.0F, 4.0F, 3.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, if (index == 2) -2.0F else 0.0F, frontZ),
            )
            cod.addOrReplaceChild(
                "leftFin$suffix",
                CubeListBuilder.create().texOffs(24, 4)
                    .addBox(0.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, if (index == 2) -1.0F else 1.0F, frontZ, 0.0F, 0.0F, 0.6109F),
            )
            cod.addOrReplaceChild(
                "rightFin$suffix",
                CubeListBuilder.create().texOffs(24, 1)
                    .addBox(-2.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, if (index == 2) -1.0F else 1.0F, frontZ, 0.0F, 0.0F, -0.6109F),
            )
            cod.addOrReplaceChild(
                "tailfin$suffix",
                CubeListBuilder.create().texOffs(20, 1)
                    .addBox(0.0F, -4.0F, 0.0F, 0.0F, 4.0F, 6.0F, CubeDeformation(0.0F)),
                PartPose.offset(0.0F, tailY, tailZ),
            )
        }
    }
}
