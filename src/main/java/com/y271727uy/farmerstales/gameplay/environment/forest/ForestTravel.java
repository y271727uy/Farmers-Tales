package com.y271727uy.farmerstales.gameplay.environment.forest;
import com.y271727uy.farmerstales.FTMod;
import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.core.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import java.util.*;
public final class ForestTravel {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,FTMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,FTMod.MODID);
    public static final Map<String,RegistryObject<? extends Block>> BLOCKS_BY_NAME=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> ITEMS_BY_NAME=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<EntityType<?>>> BOATS=new LinkedHashMap<>();
    public static final Map<String,WoodType> WOODS=new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<SignBlockEntity>> SIGN=BES.register("forest_sign",()->BlockEntityType.Builder.of((pos,state)->new SignBlockEntity(ForestTravel.SIGN.get(),pos,state),signBlocks(false)).build(null));
    public static final RegistryObject<BlockEntityType<HangingSignBlockEntity>> HANGING_SIGN=BES.register("forest_hanging_sign",()->BlockEntityType.Builder.of((pos,state)->new HangingSignBlockEntity(ForestTravel.HANGING_SIGN.get(),pos,state),signBlocks(true)).build(null));
    static {
        for(String tree:new String[]{"pale_oak","poplar"}) {
            var wood=WoodType.register(new WoodType("farmerstales:"+tree,BlockSetType.OAK));WOODS.put(tree,wood);
            var standing=BLOCKS.register(tree+"_sign",()->new StandingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SIGN),wood){
                @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SignBlockEntity(SIGN.get(),pos,state);}
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return signTicker(level,type,false);}
            });
            var wall=BLOCKS.register(tree+"_wall_sign",()->new WallSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_SIGN).dropsLike(standing.get()),wood){
                @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SignBlockEntity(SIGN.get(),pos,state);}
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return signTicker(level,type,false);}
            });
            var hanging=BLOCKS.register(tree+"_hanging_sign",()->new CeilingHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_HANGING_SIGN),wood){
                @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HangingSignBlockEntity(HANGING_SIGN.get(),pos,state);}
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return signTicker(level,type,true);}
            });
            var wallHanging=BLOCKS.register(tree+"_wall_hanging_sign",()->new WallHangingSignBlock(BlockBehaviour.Properties.copy(Blocks.OAK_WALL_HANGING_SIGN).dropsLike(hanging.get()),wood){
                @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HangingSignBlockEntity(HANGING_SIGN.get(),pos,state);}
                @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return signTicker(level,type,true);}
            });
            BLOCKS_BY_NAME.put(tree+"_sign",standing);BLOCKS_BY_NAME.put(tree+"_wall_sign",wall);BLOCKS_BY_NAME.put(tree+"_hanging_sign",hanging);BLOCKS_BY_NAME.put(tree+"_wall_hanging_sign",wallHanging);
            ITEMS_BY_NAME.put(tree+"_sign",ITEMS.register(tree+"_sign",()->new SignItem(new Item.Properties().stacksTo(16),standing.get(),wall.get())));
            ITEMS_BY_NAME.put(tree+"_hanging_sign",ITEMS.register(tree+"_hanging_sign",()->new HangingSignItem(hanging.get(),wallHanging.get(),new Item.Properties().stacksTo(16))));
            for(boolean chest:new boolean[]{false,true}) {
                String name=tree+(chest?"_chest_boat":"_boat");
                BOATS.put(name,ENTITIES.register(name,()->EntityType.Builder.<Boat>of((type,level)->chest?new TimberChestBoat(type,level,tree):new TimberBoat(type,level,tree),MobCategory.MISC).sized(1.375F,0.5625F).clientTrackingRange(10).build("farmerstales:"+name)));
                ITEMS_BY_NAME.put(name,ITEMS.register(name,()->new TimberBoatItem(chest,tree,new Item.Properties().stacksTo(1))));
            }
        }
    }
    private static Block[] signBlocks(boolean hanging){return BLOCKS_BY_NAME.entrySet().stream().filter(e->e.getKey().contains("hanging")==hanging).map(e->e.getValue().get()).toArray(Block[]::new);}
    @SuppressWarnings("unchecked") private static <T extends BlockEntity> BlockEntityTicker<T> signTicker(Level level,BlockEntityType<T> type,boolean hanging){
        return !level.isClientSide&&type==(hanging?HANGING_SIGN.get():SIGN.get())?(world,pos,state,be)->SignBlockEntity.tick(world,pos,state,(SignBlockEntity)be):null;
    }
    public static Boat createBoat(Level level,String wood,boolean chest,Vec3 position){var boat=(Boat)BOATS.get(wood+(chest?"_chest_boat":"_boat")).get().create(level);if(boat!=null)boat.setPos(position);return boat;}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);BES.register(bus);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,provider->ITEMS_BY_NAME.keySet().forEach(name->{
            String title=Arrays.stream(name.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "));
            if(name.endsWith("sign"))provider.add("block.farmerstales."+name,title);else provider.add("item.farmerstales."+name,title.replace("Chest Boat","Boat with Chest"));
            if(name.endsWith("boat"))provider.add("entity.farmerstales."+name,title.replace("Chest Boat","Boat with Chest"));
        }));
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->ITEMS_BY_NAME.forEach((name,item)->{
            if(!name.endsWith("boat"))return;
            DispenserBlock.registerBehavior(item.get(),new DefaultDispenseItemBehavior(){
                @Override protected ItemStack execute(BlockSource source,ItemStack stack){
                    var direction=source.getBlockState().getValue(DispenserBlock.FACING);var front=source.getPos().relative(direction);var level=source.getLevel();
                    double offset=level.getFluidState(front).is(FluidTags.WATER)?1:level.isEmptyBlock(front)&&level.getFluidState(front.below()).is(FluidTags.WATER)?0:Double.NaN;
                    if(Double.isNaN(offset))return super.execute(source,stack);
                    String wood=name.startsWith("pale_oak")?"pale_oak":"poplar";
                    var boat=createBoat(level,wood,name.endsWith("chest_boat"),new Vec3(source.x()+direction.getStepX()*1.125,source.y()+direction.getStepY()*1.125+offset,source.z()+direction.getStepZ()*1.125));
                    if(boat!=null){boat.setYRot(direction.toYRot());if(level.addFreshEntity(boat))stack.shrink(1);}return stack;
                }
            });
        })));
    }
}
