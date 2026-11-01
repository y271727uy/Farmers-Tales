package com.y271727uy.farmerstales.gameplay.fauna.client;
import net.minecraft.client.model.WolfModel;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
public final class WolfArmorModel extends WolfModel<Wolf>{
    public WolfArmorModel(ModelPart root){super(root);}
   public static LayerDefinition createArmorLayer() {
      CubeDeformation g=new CubeDeformation(0.2F);
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      float headHeight = 13.5F;
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(-1.0F, 13.5F, -7.0F));
      head.addOrReplaceChild(
         "real_head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, g)
            .texOffs(16, 14)
            .addBox(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, g)
            .texOffs(16, 14)
            .addBox(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, g)
            .texOffs(0, 10)
            .addBox(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, g),
         PartPose.ZERO
      );
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(18, 14).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, g),
         PartPose.offsetAndRotation(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      root.addOrReplaceChild(
         "upper_body",
         CubeListBuilder.create().texOffs(21, 0).addBox(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, g),
         PartPose.offsetAndRotation(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
      );
      CubeListBuilder leftLeg = CubeListBuilder.create().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g);
      CubeListBuilder rightLeg = CubeListBuilder.create().mirror().texOffs(0, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g);
      root.addOrReplaceChild("right_hind_leg", rightLeg, PartPose.offset(-2.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("left_hind_leg", leftLeg, PartPose.offset(0.5F, 16.0F, 7.0F));
      root.addOrReplaceChild("right_front_leg", rightLeg, PartPose.offset(-2.5F, 16.0F, -4.0F));
      root.addOrReplaceChild("left_front_leg", leftLeg, PartPose.offset(0.5F, 16.0F, -4.0F));
      PartDefinition tail = root.addOrReplaceChild(
         "tail", CubeListBuilder.create(), PartPose.offsetAndRotation(-1.0F, 12.0F, 8.0F, (float) (Math.PI / 5), 0.0F, 0.0F)
      );
      tail.addOrReplaceChild("real_tail", CubeListBuilder.create().texOffs(9, 18).addBox(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, g), PartPose.ZERO);
      return LayerDefinition.create(mesh,64,32);
   }

}
