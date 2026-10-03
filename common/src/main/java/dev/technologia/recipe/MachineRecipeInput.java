package dev.technologia.recipe;

import dev.technologia.machine.MachineKind;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** One processing lane. {@code mesh} is the sieve mesh installed in the machine; other machines pass zero. */
public record MachineRecipeInput(MachineKind machine, List<ItemStack> items, int mesh) implements RecipeInput {
    public MachineRecipeInput { items = List.copyOf(items); }
    public MachineRecipeInput(MachineKind machine, List<ItemStack> items) { this(machine, items, 0); }
    @Override public ItemStack getItem(int index) { return items.get(index); }
    @Override public int size() { return items.size(); }
}
