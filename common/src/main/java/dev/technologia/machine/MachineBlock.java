package dev.technologia.machine;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import dev.technologia.Technologia;

public final class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public final MachineKind kind;
    public MachineBlock(MachineKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return FactoryShapes.get(kind.id, state.getValue(FACING));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Technologia.MACHINE_TYPE, MachineBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (kind == MachineKind.TERMINAL) {
                var core = StorageNetwork.findCore(level, pos);
                if (core == null) player.displayClientMessage(Component.translatable("message.technologia.no_core"), true);
                else player.openMenu(StorageNetwork.access(core, pos));
            } else player.openMenu(machine);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof WrenchItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!(stack.getItem() instanceof TierKitItem kit)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine) || !player.mayBuild() || !machine.mayConfigure(player))
            return ItemInteractionResult.FAIL;
        if (!kind.isTierable()) {
            player.displayClientMessage(Component.translatable("message.technologia.kit_no_tier"), true);
        } else if (machine.tier().index() >= kit.tier) {
            player.displayClientMessage(Component.translatable("message.technologia.kit_already", machine.tier().name()), true);
        } else if (!machine.upgradeTo(kit.tier)) {
            player.displayClientMessage(Component.translatable("message.technologia.kit_order", MachineTier.get(kit.tier - 1).name()), true);
        } else {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.1F);
            player.displayClientMessage(Component.translatable("message.technologia.kit_installed", machine.tier().name()), true);
        }
        return ItemInteractionResult.CONSUME;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player && level.getBlockEntity(pos) instanceof MachineBlockEntity machine) machine.setOwner(placer.getUUID());
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!state.is(oldState.getBlock())) Technologia.topologyChanged();
        super.onPlace(state, level, pos, oldState, moving);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moving) {
        // A block appearing next to a supplier may be a new energy endpoint. A lit-front change of a
        // neighbouring machine is sent without neighbour updates and does not reach this method.
        if (kind.suppliesEnergy()) Technologia.topologyChanged();
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            Technologia.topologyChanged();
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) Containers.dropContents(level, pos, machine);
            super.onRemove(state, level, pos, newState, moving);
        }
    }
    /** The dropped block keeps its tier and stored energy; the inventory drops separately. */
    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        List<ItemStack> drops = super.getDrops(state, params);
        if (entity instanceof MachineBlockEntity machine) for (ItemStack drop : drops) if (drop.is(asItem())) machine.saveToItem(drop);
        return drops;
    }
    /**
     * Pick-block returns the same variant, as a shulker box does: the item carries the machine tier.
     * The client does not know the stored energy, so that is left out.
     */
    @Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) machine.saveToItem(stack);
        return stack;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || random.nextInt(3) != 0) return;
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        double spread = random.nextDouble() * 0.5 - 0.25;
        // A point just outside the front face.
        double fx = x + facing.getStepX() * 0.55 + (facing.getAxis() == Direction.Axis.Z ? spread : 0);
        double fz = z + facing.getStepZ() * 0.55 + (facing.getAxis() == Direction.Axis.X ? spread : 0);
        switch (kind) {
            case GENERATOR, BIOMASS_GENERATOR, FURNACE, ALLOY_SMELTER -> {
                level.addParticle(ParticleTypes.SMOKE, fx, y + 0.45, fz, 0, 0.02, 0);
                if (kind != MachineKind.BIOMASS_GENERATOR) level.addParticle(ParticleTypes.FLAME, fx, y + 0.4, fz, 0, 0, 0);
            }
            case CRUSHER, RECYCLER, SIEVE -> level.addParticle(ParticleTypes.CRIT, fx, y + 0.5, fz, 0, -0.05, 0);
            case SAWMILL, METAL_PRESS, COMPACTOR -> top(level, ParticleTypes.CLOUD, x, y, z, random, 0.01);
            case CENTRIFUGE -> top(level, ParticleTypes.EFFECT, x, y, z, random, 0.02);
            case MINER -> level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + spread, y - 0.05, z + spread, 0, -0.1, 0);
            default -> { }
        }
    }
    private static void top(Level level, ParticleOptions particle, double x, double y, double z, RandomSource random, double rise) {
        level.addParticle(particle, x + random.nextDouble() * 0.4 - 0.2, y + 1.02, z + random.nextDouble() * 0.4 - 0.2, 0, rise, 0);
    }
}
