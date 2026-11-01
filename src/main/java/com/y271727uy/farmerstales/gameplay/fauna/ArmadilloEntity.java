package com.y271727uy.farmerstales.gameplay.fauna;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.*;
public class ArmadilloEntity extends Animal {
    private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(ArmadilloEntity.class,EntityDataSerializers.INT);
    private int stateTicks,scuteTime=6000,calmTicks;
    public final AnimationState rollUp=new AnimationState(),rollOut=new AnimationState(),peek=new AnimationState();
    public ArmadilloEntity(EntityType<? extends Animal> type,Level level){super(type,level);scuteTime=6000+random.nextInt(6000);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,12).add(Attributes.MOVEMENT_SPEED,0.14);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(STATE,0);}
    public int state(){return entityData.get(STATE);}
    public boolean rolled(){return state()==1||state()==2;}
    public void changeState(int state){if(state()!=state){entityData.set(STATE,state);stateTicks=0;}}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor){super.onSyncedDataUpdated(accessor);if(STATE.equals(accessor)){stateTicks=0;rollUp.stop();rollOut.stop();peek.stop();if(state()==1)rollUp.start(tickCount);else if(state()==3)rollOut.start(tickCount);else if(state()==2)peek.start(tickCount);}}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.5){@Override public boolean canUse(){return !rolled()&&super.canUse();}});
        goalSelector.addGoal(2,new BreedGoal(this,1));goalSelector.addGoal(3,new TemptGoal(this,1.25,Ingredient.of(Items.SPIDER_EYE),false));goalSelector.addGoal(4,new FollowParentGoal(this,1.25));goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,6));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    public boolean canRoll(){return !isInWaterOrBubble()&&!isInLava()&&!isLeashed()&&!isPassenger()&&!isVehicle()&&!isOnFire();}
    public boolean scaredBy(LivingEntity entity){return getBoundingBox().inflate(7,2,7).intersects(entity.getBoundingBox())&&(entity.getMobType()==MobType.UNDEAD||entity==getLastHurtByMob()||entity instanceof Player player&&!player.isSpectator()&&(player.isSprinting()||player.isPassenger()));}
    public void roll(){if((state()==0||state()==3)&&canRoll()){changeState(1);resetLove();getNavigation().stop();playSound(FaunaContent.soundEvent("entity.armadillo.roll"),1,1);}calmTicks=80;}
    @Override public void tick(){
        super.tick();stateTicks++;
        if(!level().isClientSide){
            if(!canRoll()){if(state()!=0)changeState(0);}
            else {
                if(tickCount%5==0&&level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(7,2,7),e->e!=this&&scaredBy(e)).size()>0)roll();
                if(rolled()){
                    getNavigation().stop();setDeltaMovement(0,getDeltaMovement().y,0);
                    if(state()==1&&stateTicks>=10)changeState(2);
                    if(--calmTicks<=0){changeState(3);playSound(FaunaContent.soundEvent("entity.armadillo.unroll_start"),1,1);}
                    else if(state()==2&&stateTicks%50==0){level().broadcastEntityEvent(this,(byte)64);playSound(FaunaContent.soundEvent("entity.armadillo.peek"),1,1);}
                }else if(state()==3&&stateTicks>=30){changeState(0);playSound(FaunaContent.soundEvent("entity.armadillo.unroll_finish"),1,1);}
            }
            if(isAlive()&&!isBaby()&&--scuteTime<=0){if(level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)){spawnAtLocation(FaunaContent.SCUTE.get());playSound(FaunaContent.soundEvent("entity.armadillo.scute_drop"),1,1);}scuteTime=6000+random.nextInt(6000);}
        }
    }
    @Override public void handleEntityEvent(byte id){if(id==64)peek.start(tickCount);else super.handleEntityEvent(id);}
    @Override protected boolean isImmobile(){return state()!=0||super.isImmobile();}
    @Override public boolean isFood(ItemStack item){return item.is(Items.SPIDER_EYE);}
    @Override public boolean canFallInLove(){return state()==0&&super.canFallInLove();}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){return FaunaContent.ARMADILLO.get().create(level);}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(stack.is(Items.BRUSH)&&!isBaby()){
            if(!level().isClientSide){spawnAtLocation(FaunaContent.SCUTE.get());playSound(FaunaContent.soundEvent("entity.armadillo.brush"),1,1);stack.hurtAndBreak(16,player,p->p.broadcastBreakEvent(hand));}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return state()!=0?InteractionResult.FAIL:super.mobInteract(player,hand);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        float adjusted=rolled()?Math.max(0,(amount-1)/2):amount;boolean hurt=super.hurt(source,adjusted);
        if(hurt&&!level().isClientSide&&source.getEntity() instanceof LivingEntity&&canRoll())roll();return hurt;
    }
    @Override protected SoundEvent getAmbientSound(){return state()==0?FaunaContent.soundEvent("entity.armadillo.ambient"):null;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return FaunaContent.soundEvent(rolled()?"entity.armadillo.hurt_reduced":"entity.armadillo.hurt");}
    @Override protected SoundEvent getDeathSound(){return FaunaContent.soundEvent("entity.armadillo.death");}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("ArmadilloState",state());tag.putInt("ScuteTime",scuteTime);tag.putInt("CalmTicks",calmTicks);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);changeState(Math.max(0,Math.min(3,tag.getInt("ArmadilloState"))));if(tag.contains("ScuteTime"))scuteTime=tag.getInt("ScuteTime");else if(tag.contains("NextScuteTick"))scuteTime=Math.max(1,Math.min(12000,tag.getInt("NextScuteTick")));if(!tag.contains("ArmadilloState")&&tag.getInt("RollTicks")>0)changeState(2);calmTicks=tag.getInt("CalmTicks");}
    public static boolean spawnRules(EntityType<? extends ArmadilloEntity> type,LevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){return (level.getBlockState(pos.below()).is(BlockTags.DIRT)||level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.RED_SAND))&&level.getRawBrightness(pos,0)>8;}
}
