package dev.technologia.recipe;

import dev.technologia.machine.MachineKind;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record MachineRecipeInput(MachineKind machine, List<ItemStack> items) implements RecipeInput {
    public MachineRecipeInput { items = List.copyOf(items); }
    @Override public ItemStack getItem(int index) { return items.get(index); }
    @Override public int size() { return items.size(); }
}
