package dev.technologia.recipe;

import com.mojang.serialization.MapCodec;
import dev.technologia.Technologia;
import dev.technologia.machine.MachineBlock;
import dev.technologia.machine.MachineBlockEntity;
import dev.technologia.machine.MachineBlockItem;
import dev.technologia.machine.MachineTier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe that builds one machine out of others. A plain shaped recipe would throw away the
 * tier kits and energy stored on the machines used; here the result keeps the lowest tier among
 * them and their combined energy, up to its own capacity. The JSON is that of a shaped recipe.
 */
public final class MachineCraftingRecipe implements CraftingRecipe {
    public static final RecipeSerializer<MachineCraftingRecipe> SERIALIZER = new Serializer();
    private final ShapedRecipe shaped;
    public MachineCraftingRecipe(ShapedRecipe shaped) { this.shaped = shaped; }

    @Override public boolean matches(CraftingInput input, Level level) { return shaped.matches(input, level); }
    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = shaped.assemble(input, registries);
        if (!(result.getItem() instanceof MachineBlockItem item) || !(item.getBlock() instanceof MachineBlock machine) || !machine.kind.isTierable()) return result;
        int tier = Integer.MAX_VALUE;
        long energy = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!(stack.getItem() instanceof MachineBlockItem part) || !(part.getBlock() instanceof MachineBlock used) || !used.kind.isTierable()) continue;
            tier = Math.min(tier, MachineBlockItem.tier(stack).index());
            energy += MachineBlockItem.energy(stack);
        }
        if (tier == Integer.MAX_VALUE) return result;
        CompoundTag tag = new CompoundTag();
        if (tier > 0) tag.putInt("Tier", tier);
        int kept = (int) Math.min(energy, MachineBlockEntity.scaledCapacity(machine.kind, MachineTier.get(tier)));
        if (kept > 0) tag.putInt("Energy", kept);
        BlockItem.setBlockEntityData(result, Technologia.MACHINE_TYPE, tag);
        return result;
    }
    @Override public boolean canCraftInDimensions(int width, int height) { return shaped.canCraftInDimensions(width, height); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return shaped.getResultItem(registries); }
    @Override public NonNullList<Ingredient> getIngredients() { return shaped.getIngredients(); }
    @Override public String getGroup() { return shaped.getGroup(); }
    @Override public CraftingBookCategory category() { return shaped.category(); }
    @Override public boolean showNotification() { return shaped.showNotification(); }
    @Override public boolean isIncomplete() { return shaped.isIncomplete(); }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    private static final class Serializer implements RecipeSerializer<MachineCraftingRecipe> {
        private static final MapCodec<MachineCraftingRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(MachineCraftingRecipe::new, recipe -> recipe.shaped);
        private static final StreamCodec<RegistryFriendlyByteBuf, MachineCraftingRecipe> STREAM_CODEC =
                ShapedRecipe.Serializer.STREAM_CODEC.map(MachineCraftingRecipe::new, recipe -> recipe.shaped);
        @Override public MapCodec<MachineCraftingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MachineCraftingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
