package dev.technologia.device;

import com.mojang.serialization.MapCodec;
import dev.technologia.Technologia;
import dev.technologia.machine.FactoryShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Grows a small tree from one sapling, again and again, with no power and no space. Use a sapling
 * to plant it, bone meal to hurry it, an empty hand to take the wood. A hopper below empties it.
 */
public final class BonsaiPotBlock extends BaseEntityBlock {
    /** 0 empty, 1 planted, 2 half grown, 3 grown. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    public BonsaiPotBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STAGE); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FactoryShapes.get("bonsai_pot", Direction.NORTH);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BonsaiPotBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Technologia.BONSAI_TYPE, BonsaiPotBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        boolean plant = pot.sapling().isEmpty() && BonsaiPotBlockEntity.isSapling(stack);
        boolean feed = stack.is(Items.BONE_MEAL) && !pot.sapling().isEmpty() && !pot.isGrown();
        if (!plant && !feed) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (plant) {
            pot.plant(stack.copyWithCount(1));
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
            pot.feed();
            level.levelEvent(1505, pos, 8);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot)) return InteractionResult.sidedSuccess(level.isClientSide);
        boolean taken = pot.giveResults(player);
        // Crouching with nothing left to take digs the sapling out again.
        if (!taken && player.isShiftKeyDown() && !pot.sapling().isEmpty()) Technologia.giveOrDrop(player, pot.uproot(), pos);
        else if (!taken) player.displayClientMessage(Component.translatable(pot.sapling().isEmpty() ? "message.technologia.bonsai_empty" : "message.technologia.bonsai_growing", pot.percent()), true);
        return InteractionResult.CONSUME;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot) Containers.dropContents(level, pos, pot);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BonsaiPotBlockEntity pot ? pot.comparatorSignal() : 0;
    }
}
