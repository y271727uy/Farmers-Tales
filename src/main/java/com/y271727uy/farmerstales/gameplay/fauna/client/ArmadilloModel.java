package com.y271727uy.farmerstales.gameplay.fauna.client;
import com.y271727uy.farmerstales.gameplay.fauna.ArmadilloEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
public final class ArmadilloModel extends HierarchicalModel<ArmadilloEntity> {
    private final ModelPart root,body,cube,head;
    public ArmadilloModel(ModelPart root){this.root=root;body=root.getChild("body");cube=root.getChild("cube");head=body.getChild("head");}
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(ArmadilloEntity entity,float swing,float speed,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);boolean hidden=entity.state()==2;
        body.visible=!hidden;head.visible=!hidden;root.getChild("right_hind_leg").visible=!hidden;root.getChild("left_hind_leg").visible=!hidden;
        root.getChild("right_front_leg").visible=!hidden;root.getChild("left_front_leg").visible=!hidden;cube.visible=hidden;
        head.xRot=net.minecraft.util.Mth.clamp(pitch,-22.5F,25)*(float)Math.PI/180;head.yRot=net.minecraft.util.Mth.clamp(yaw,-32.5F,32.5F)*(float)Math.PI/180;
        if(entity.state()==0)animateWalk(ArmadilloAnimation.ARMADILLO_WALK,swing,speed,16.5F,2.5F);
        animate(entity.rollUp,ArmadilloAnimation.ARMADILLO_ROLL_UP,age);animate(entity.rollOut,ArmadilloAnimation.ARMADILLO_ROLL_OUT,age);animate(entity.peek,ArmadilloAnimation.ARMADILLO_PEEK,age);
    }
   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition body = root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 20)
            .addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.3F))
            .texOffs(0, 40)
            .addBox(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 21.0F, 4.0F)
      );
      body.addOrReplaceChild(
         "tail",
         CubeListBuilder.create().texOffs(44, 53).addBox(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F)
      );
      PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, -2.0F, -11.0F));
      head.addOrReplaceChild(
         "head_cube",
         CubeListBuilder.create().texOffs(43, 15).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F)
      );
      PartDefinition rightEar = head.addOrReplaceChild("right_ear", CubeListBuilder.create(), PartPose.offset(-1.0F, -1.0F, 0.0F));
      rightEar.addOrReplaceChild(
         "right_ear_cube",
         CubeListBuilder.create().texOffs(43, 10).addBox(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F)
      );
      PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create(), PartPose.offset(1.0F, -2.0F, 0.0F));
      leftEar.addOrReplaceChild(
         "left_ear_cube",
         CubeListBuilder.create().texOffs(47, 10).addBox(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
         PartPose.offsetAndRotation(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F)
      );
      root.addOrReplaceChild(
         "right_hind_leg",
         CubeListBuilder.create().texOffs(51, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 21.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "left_hind_leg",
         CubeListBuilder.create().texOffs(42, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 21.0F, 4.0F)
      );
      root.addOrReplaceChild(
         "right_front_leg",
         CubeListBuilder.create().texOffs(51, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(-2.0F, 21.0F, -4.0F)
      );
      root.addOrReplaceChild(
         "left_front_leg",
         CubeListBuilder.create().texOffs(42, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
         PartPose.offset(2.0F, 21.0F, -4.0F)
      );
      root.addOrReplaceChild(
         "cube",
         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -6.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 64, 64);
   }
}
