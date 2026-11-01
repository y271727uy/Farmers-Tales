package com.y271727uy.farmerstales.gameplay.creaking.client;
import com.y271727uy.farmerstales.gameplay.creaking.CreakingEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
public final class CreakingModel extends HierarchicalModel<CreakingEntity> {
    private final ModelPart root, head;
    public CreakingModel(ModelPart root) {
        this.root = root;
        head = root.getChild("root").getChild("upper_body").getChild("head");
    }
    @Override public ModelPart root() { return root; }
    @Override public void setupAnim(CreakingEntity entity, float limbSwing, float limbSpeed, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.xRot = pitch * ((float)Math.PI / 180);
        head.yRot = yaw * ((float)Math.PI / 180);
        if (entity.canMove()) animateWalk(CreakingAnimation.CREAKING_WALK, limbSwing, limbSpeed, 1, 1);
        animate(entity.attackAnimationState, CreakingAnimation.CREAKING_ATTACK, age);
        animate(entity.invulnerabilityAnimationState, CreakingAnimation.CREAKING_INVULNERABLE, age);
        animate(entity.deathAnimationState, CreakingAnimation.CREAKING_DEATH, age);
    }
   private static MeshDefinition createMesh() {
      MeshDefinition meshDefinition = new MeshDefinition();
      PartDefinition partDefinition = meshDefinition.getRoot();
      PartDefinition root = partDefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
      PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
      upperBody.addOrReplaceChild(
         "head",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
            .texOffs(28, 31)
            .addBox(-3.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F)
            .texOffs(12, 40)
            .addBox(3.0F, -13.0F, 0.0F, 9.0F, 14.0F, 0.0F)
            .texOffs(34, 12)
            .addBox(-12.0F, -14.0F, 0.0F, 9.0F, 14.0F, 0.0F),
         PartPose.offset(-3.0F, -11.0F, 0.0F)
      );
      upperBody.addOrReplaceChild(
         "body",
         CubeListBuilder.create().texOffs(0, 16).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F).texOffs(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F),
         PartPose.offset(0.0F, -7.0F, 1.0F)
      );
      upperBody.addOrReplaceChild(
         "right_arm",
         CubeListBuilder.create().texOffs(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F).texOffs(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F),
         PartPose.offset(-7.0F, -9.5F, 1.5F)
      );
      upperBody.addOrReplaceChild(
         "left_arm",
         CubeListBuilder.create()
            .texOffs(30, 40)
            .addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
            .texOffs(52, 12)
            .addBox(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
            .texOffs(52, 19)
            .addBox(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F),
         PartPose.offset(6.0F, -9.0F, 0.5F)
      );
      root.addOrReplaceChild(
         "left_leg",
         CubeListBuilder.create().texOffs(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F).texOffs(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F),
         PartPose.offset(1.5F, -16.0F, 0.5F)
      );
      root.addOrReplaceChild(
         "right_leg",
         CubeListBuilder.create()
            .texOffs(0, 34)
            .addBox(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
            .texOffs(45, 46)
            .addBox(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
            .texOffs(12, 34)
            .addBox(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F),
         PartPose.offset(-1.0F, -17.5F, 0.5F)
      );
      return meshDefinition;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = createMesh();
      return LayerDefinition.create(mesh, 64, 64);
   }


}
