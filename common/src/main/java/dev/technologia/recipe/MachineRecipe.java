package dev.technologia.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.technologia.machine.MachineKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** A shared, reloadable processing recipe. Alloy ingredients may occupy either input slot. */
public final class MachineRecipe implements Recipe<MachineRecipeInput> {
    public static final RecipeType<MachineRecipe> TYPE = new RecipeType<>() {
        @Override public String toString() { return "technologia:processing"; }
    };
    public static final RecipeSerializer<MachineRecipe> SERIALIZER = new Serializer();
    public record CountedIngredient(Ingredient ingredient, int count) {
        static final Codec<CountedIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CountedIngredient::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(CountedIngredient::count)
        ).apply(instance, CountedIngredient::new));
        boolean matches(ItemStack stack) { return stack.getCount() >= count && ingredient.test(stack); }
    }
    private final MachineKind machine;
    private final List<CountedIngredient> ingredients;
    private final ItemStack result, byproduct;
    private final int time, energy;

    public MachineRecipe(MachineKind machine, List<CountedIngredient> ingredients, ItemStack result, ItemStack byproduct, int time, int energy) {
        this.machine = machine; this.ingredients = List.copyOf(ingredients);
        this.result = result.copy(); this.byproduct = byproduct.copy(); this.time = time; this.energy = energy;
    }
    public MachineKind machine() { return machine; }
    public int time() { return time; }
    public int energy() { return energy; }
    public List<ItemStack> outputs() { return byproduct.isEmpty() ? List.of(result.copy()) : List.of(result.copy(), byproduct.copy()); }
    /** Counts consumed from each physical slot, or an empty array when no match exists. */
    public int[] consumption(MachineRecipeInput input) {
        if (input.machine() != machine || input.size() != ingredients.size()) return new int[0];
        if (ingredients.size() == 1) return ingredients.getFirst().matches(input.getItem(0)) ? new int[]{ingredients.getFirst().count()} : new int[0];
        var a = ingredients.get(0); var b = ingredients.get(1);
        if (a.matches(input.getItem(0)) && b.matches(input.getItem(1))) return new int[]{a.count(), b.count()};
        if (b.matches(input.getItem(0)) && a.matches(input.getItem(1))) return new int[]{b.count(), a.count()};
        return new int[0];
    }
    @Override public boolean matches(MachineRecipeInput input, Level level) { return consumption(input).length != 0; }
    @Override public ItemStack assemble(MachineRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= ingredients.size(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }
    @Override public RecipeType<?> getType() { return TYPE; }
    @Override public NonNullList<Ingredient> getIngredients() {
        var values = NonNullList.<Ingredient>create(); ingredients.forEach(value -> values.add(value.ingredient())); return values;
    }
    private static DataResult<MachineKind> machine(String id) {
        for (MachineKind kind : MachineKind.values()) if (kind.id.equals(id) && kind.isProcessor() && kind != MachineKind.FURNACE) return DataResult.success(kind);
        return DataResult.error(() -> "Unknown processing machine: " + id);
    }
    private static DataResult<MachineRecipe> validate(MachineRecipe recipe) {
        return recipe.ingredients.size() == recipe.machine.inputCount() ? DataResult.success(recipe)
                : DataResult.error(() -> recipe.machine.id + " requires exactly " + recipe.machine.inputCount() + " ingredients");
    }
    private static final class Serializer implements RecipeSerializer<MachineRecipe> {
        private static final MapCodec<MachineRecipe> CODEC = RecordCodecBuilder.<MachineRecipe>mapCodec(instance -> instance.group(
                Codec.STRING.comapFlatMap(MachineRecipe::machine, kind -> kind.id).fieldOf("machine").forGetter(recipe -> recipe.machine),
                CountedIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                ItemStack.STRICT_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(recipe -> recipe.byproduct),
                Codec.intRange(1, 12000).fieldOf("time").forGetter(recipe -> recipe.time),
                Codec.intRange(1, 100000).fieldOf("energy").forGetter(recipe -> recipe.energy)
        ).apply(instance, MachineRecipe::new)).validate(MachineRecipe::validate);
        private static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override public MachineRecipe decode(RegistryFriendlyByteBuf buffer) {
                MachineKind kind = machine(buffer.readUtf()).getOrThrow();
                int size = buffer.readVarInt();
                if (size != kind.inputCount()) throw new IllegalArgumentException("Invalid processing ingredient count");
                List<CountedIngredient> ingredients = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
                    int count = buffer.readVarInt();
                    if (count < 1 || count > 64) throw new IllegalArgumentException("Invalid processing ingredient quantity");
                    ingredients.add(new CountedIngredient(ingredient, count));
                }
                ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
                ItemStack byproduct = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
                int time = buffer.readVarInt(), energy = buffer.readVarInt();
                if (time < 1 || time > 12000 || energy < 1 || energy > 100000) throw new IllegalArgumentException("Invalid processing cost");
                return new MachineRecipe(kind, ingredients, result, byproduct, time, energy);
            }
            @Override public void encode(RegistryFriendlyByteBuf buffer, MachineRecipe recipe) {
                buffer.writeUtf(recipe.machine.id); buffer.writeVarInt(recipe.ingredients.size());
                for (var ingredient : recipe.ingredients) { Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient.ingredient()); buffer.writeVarInt(ingredient.count()); }
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result); ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.byproduct);
                buffer.writeVarInt(recipe.time); buffer.writeVarInt(recipe.energy);
            }
        };
        @Override public MapCodec<MachineRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
