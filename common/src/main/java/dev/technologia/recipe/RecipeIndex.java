package dev.technologia.recipe;

import dev.technologia.machine.MachineKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Answers "could this machine ever use this item?" without scanning every recipe on each
 * hopper push. The index is rebuilt when the recipe manager swaps its recipe set.
 */
public final class RecipeIndex {
    private static final Map<RecipeManager, RecipeIndex> CACHE = new WeakHashMap<>();
    private final Object token;
    private final Map<MachineKind, List<MachineRecipe>> byMachine = new EnumMap<>(MachineKind.class);
    private final Map<Item, Boolean> smeltable = new HashMap<>();
    private final Map<MachineKind, Map<Item, Boolean>> plainAnswers = new EnumMap<>(MachineKind.class);

    private RecipeIndex(RecipeManager manager, Object token) {
        this.token = token;
        for (RecipeHolder<MachineRecipe> holder : manager.getAllRecipesFor(MachineRecipe.TYPE))
            byMachine.computeIfAbsent(holder.value().machine(), kind -> new ArrayList<>()).add(holder.value());
    }

    /** Changes exactly when recipes are (re)loaded; holders cached against an older token are stale. */
    public static Object token(Level level) { return level.getRecipeManager().getRecipes(); }

    private static synchronized RecipeIndex of(Level level) {
        RecipeManager manager = level.getRecipeManager();
        Object token = manager.getRecipes();
        RecipeIndex index = CACHE.get(manager);
        if (index == null || index.token != token) {
            index = new RecipeIndex(manager, token);
            CACHE.put(manager, index);
        }
        return index;
    }

    /** True when the stack is an ingredient of at least one recipe of that machine. */
    public static boolean isIngredient(Level level, MachineKind kind, ItemStack stack) {
        if (stack.isEmpty()) return false;
        RecipeIndex index = of(level);
        if (kind == MachineKind.FURNACE) {
            synchronized (index.smeltable) {
                return index.smeltable.computeIfAbsent(stack.getItem(), item -> level.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(item)), level).isPresent());
            }
        }
        // Hoppers ask every tick; a stack without changed components always gets the same answer.
        if (stack.getComponentsPatch().isEmpty()) {
            synchronized (index.plainAnswers) {
                return index.plainAnswers.computeIfAbsent(kind, key -> new HashMap<>()).computeIfAbsent(stack.getItem(), item -> index.scan(kind, stack));
            }
        }
        return index.scan(kind, stack);
    }
    private boolean scan(MachineKind kind, ItemStack stack) {
        for (MachineRecipe recipe : byMachine.getOrDefault(kind, List.of())) if (recipe.usesItem(stack)) return true;
        return false;
    }

    /** True when some two-ingredient recipe of that machine uses both stacks together. */
    public static boolean isPair(Level level, MachineKind kind, ItemStack first, ItemStack second) {
        for (MachineRecipe recipe : of(level).byMachine.getOrDefault(kind, List.of())) if (recipe.usesPair(first, second)) return true;
        return false;
    }
}
