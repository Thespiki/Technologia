package dev.technologia.device;

import dev.technologia.Technologia;
import dev.technologia.machine.MachineKind;
import dev.technologia.machine.MeshItem;
import dev.technologia.recipe.MachineRecipe;
import dev.technologia.recipe.MachineRecipeInput;
import dev.technologia.recipe.RecipeIndex;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the one block being sieved, how many shakes it still needs, and the installed mesh. */
public final class HandSieveBlockEntity extends BlockEntity {
    public static final int SHAKES = 4;
    private ItemStack content = ItemStack.EMPTY;
    private int shakes, mesh;
    public HandSieveBlockEntity(BlockPos pos, BlockState state) { super(Technologia.HAND_SIEVE_TYPE, pos, state); }

    public boolean isEmpty() { return content.isEmpty(); }
    public int mesh() { return mesh; }
    public int installMesh(int level) {
        int previous = mesh;
        mesh = Math.clamp(level, 0, MachineRecipe.MAX_MESH);
        setChanged();
        return previous;
    }
    private MachineRecipe recipe(ItemStack stack) {
        return RecipeIndex.best(level, new MachineRecipeInput(MachineKind.SIEVE, List.of(stack), mesh)).map(holder -> holder.value()).orElse(null);
    }
    /** Takes what one sieve recipe needs from the held stack; false when the sieve cannot use it. */
    public boolean fill(ItemStack held) {
        if (!content.isEmpty() || held.isEmpty() || level == null) return false;
        MachineRecipe recipe = recipe(held);
        // The tray holds one plain block: compressed blocks and recipes that need several items are for the Auto Sieve.
        if (recipe == null || recipe.ingredients().getFirst().count() != 1) return false;
        for (String name : Technologia.COMPRESSED) if (held.is(Technologia.ITEMS.get(name))) return false;
        content = held.copyWithCount(1);
        shakes = SHAKES;
        level.playSound(null, worldPosition, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.7F, 1.0F);
        changed();
        return true;
    }
    public void shake() {
        if (content.isEmpty() || level == null) return;
        level.playSound(null, worldPosition, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + level.random.nextFloat() * 0.3F);
        if (--shakes > 0) { changed(); return; }
        MachineRecipe recipe = recipe(content);
        if (recipe != null) {
            List<ItemStack> outputs = recipe.outputs();
            for (int i = 0; i < outputs.size(); i++) {
                if (i == 1 && recipe.byproductChance() < 1 && level.random.nextFloat() >= recipe.byproductChance()) continue;
                Technologia.drop(level, worldPosition.above(), outputs.get(i));
            }
        }
        content = ItemStack.EMPTY;
        changed();
    }
    /** Drops the unfinished block and the installed mesh when the sieve is broken. */
    public void dropAll() {
        if (level == null) return;
        Technologia.drop(level, worldPosition, content);
        Technologia.drop(level, worldPosition, MeshItem.stack(mesh));
        content = ItemStack.EMPTY; mesh = 0;
    }
    private void changed() {
        setChanged();
        int fill = content.isEmpty() ? 0 : Math.clamp(shakes, 1, SHAKES);
        BlockState state = getBlockState();
        if (state.hasProperty(HandSieveBlock.FILL) && state.getValue(HandSieveBlock.FILL) != fill)
            level.setBlock(worldPosition, state.setValue(HandSieveBlock.FILL, fill), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!content.isEmpty()) tag.put("Content", content.save(registries));
        tag.putInt("Shakes", shakes);
        if (mesh != 0) tag.putInt("Mesh", mesh);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        content = tag.contains("Content") ? ItemStack.parse(registries, tag.getCompound("Content")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        shakes = Math.clamp(tag.getInt("Shakes"), 0, SHAKES);
        mesh = Math.clamp(tag.getInt("Mesh"), 0, MachineRecipe.MAX_MESH);
    }
}
