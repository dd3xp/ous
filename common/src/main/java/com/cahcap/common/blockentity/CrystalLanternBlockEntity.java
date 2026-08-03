package com.cahcap.common.blockentity;

import com.cahcap.common.recipe.CrystalLanternFuelRecipe;
import com.cahcap.common.registry.ModRegistries;
import com.cahcap.common.util.BlockEntityHelper;
import com.cahcap.common.util.PotionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Crystal Lantern - burns fuel to project its bound potion effect over an area.
 * <p>
 * The bound effect is baked in at craft time (from the pot used in the recipe) and never
 * changes afterwards; only its identity and colour are stored, not level or duration.
 * What counts as fuel and how long it lasts comes from {@link CrystalLanternFuelRecipe}, so a
 * datapack can add fuels without touching code.
 */
public class CrystalLanternBlockEntity extends BlockEntity {

    /** Capacity of the single fuel slot. Belongs to the lantern, not to any one fuel. */
    public static final int MAX_FUEL = 64;
    /** Used only when the loaded fuel has no definition, e.g. after a datapack removed it. */
    private static final int DEFAULT_BURN_TIME = 1200;
    /** Non-instant effects are refreshed every 1.5s with a 2s duration, i.e. permanent. */
    private static final int EFFECT_INTERVAL = 30;
    /** Instant effects (harm/heal) fire once per 2s instead. */
    private static final int INSTANT_INTERVAL = 40;
    private static final int EFFECT_DURATION = 40;
    /** Radius in blocks around the lantern. */
    private static final int RADIUS = 25;

    /** The fuel waiting to be burnt. Stored as a stack so any datapack-defined fuel works. */
    private ItemStack fuelStack = ItemStack.EMPTY;
    /** Ticks left on the item currently burning. Consumed up front, so it cannot be refunded. */
    private int burnTicksLeft;
    private int effectTimer;

    /** Effect ids bound at craft time. Empty means the lantern does nothing. */
    private List<String> boundEffects = new ArrayList<>();
    /** Colour inherited from the pot, used to tint the lamp body. */
    private int potionColor = 0xFFFFFF;

    public CrystalLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.CRYSTAL_LANTERN_BE.get(), pos, state);
    }

    // ==================== fuel lookup ====================

    /**
     * Looks up the fuel definition for a stack. Queried on demand rather than cached: the project
     * does the same for kiln/incense/herb pot recipes, it keeps working across {@code /reload},
     * and the lantern only needs it once per burn rather than every tick.
     */
    public static Optional<CrystalLanternFuelRecipe> findFuel(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .getRecipeFor(ModRegistries.CRYSTAL_LANTERN_FUEL_RECIPE_TYPE.get(),
                        new SingleRecipeInput(stack), level)
                .map(RecipeHolder::value);
    }

    /** Whether the lantern accepts this item at all. */
    public static boolean isFuel(Level level, ItemStack stack) {
        return findFuel(level, stack).isPresent();
    }

    // ==================== state ====================

    public ItemStack getFuelStack() {
        return fuelStack;
    }

    public int getFuelCount() {
        return fuelStack.getCount();
    }

    public List<String> getBoundEffects() {
        return boundEffects;
    }

    public int getPotionColor() {
        return potionColor;
    }

    public boolean isWorking() {
        return !boundEffects.isEmpty() && (burnTicksLeft > 0 || !fuelStack.isEmpty());
    }

    /** Copies the binding baked in at craft time onto this block entity. */
    public void setBinding(List<String> effects, int color) {
        this.boundEffects = new ArrayList<>(effects);
        this.potionColor = color;
        setChanged();
        syncToClient();
    }

    /** Whether this stack can go in right now — valid fuel, and matching whatever is already held. */
    public boolean canAccept(ItemStack stack) {
        if (!isFuel(level, stack)) {
            return false;
        }
        return fuelStack.isEmpty() || ItemStack.isSameItemSameComponents(fuelStack, stack);
    }

    /**
     * Adds as much of the held fuel as fits.
     *
     * @return how many were taken from the stack
     */
    public int insertFuel(ItemStack stack) {
        if (!canAccept(stack)) {
            return 0;
        }
        int accepted = Math.min(stack.getCount(), MAX_FUEL - fuelStack.getCount());
        if (accepted <= 0) {
            return 0;
        }
        if (fuelStack.isEmpty()) {
            fuelStack = stack.copyWithCount(accepted);
        } else {
            fuelStack.grow(accepted);
        }
        setChanged();
        syncToClient();
        return accepted;
    }

    /** Removes the whole slot and returns it, or an empty stack when already empty. */
    public ItemStack extractFuel() {
        if (fuelStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = fuelStack;
        fuelStack = ItemStack.EMPTY;
        setChanged();
        syncToClient();
        return out;
    }

    // ==================== tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  CrystalLanternBlockEntity be) {
        if (be.boundEffects.isEmpty()) {
            return;
        }

        if (be.burnTicksLeft <= 0) {
            if (be.fuelStack.isEmpty()) {
                // Burnt out: drop the lit state so the light goes off and the tint reverts.
                be.effectTimer = 0;
                if (state.getValue(BlockStateProperties.LIT)) {
                    level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false),
                            Block.UPDATE_ALL);
                }
                return;
            }
            // Consume up front, not when the burn is up — otherwise the last item can be pulled
            // back out a second before it finishes and the effect comes for free.
            be.burnTicksLeft = findFuel(level, be.fuelStack)
                    .map(CrystalLanternFuelRecipe::getBurnTime)
                    .orElse(DEFAULT_BURN_TIME);
            be.fuelStack.shrink(1);
            be.setChanged();
            be.syncToClient();
            if (!state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true),
                        Block.UPDATE_ALL);
            }
        }

        be.burnTicksLeft--;
        be.effectTimer++;
        be.applyEffects(level, pos);
    }

    private void applyEffects(Level level, BlockPos pos) {
        List<Holder<MobEffect>> instant = new ArrayList<>();
        List<Holder<MobEffect>> lasting = new ArrayList<>();
        for (String id : boundEffects) {
            Holder<MobEffect> effect = PotionHelper.getEffectForType(id);
            if (effect == null) {
                continue;
            }
            (PotionHelper.isInstantEffect(effect) ? instant : lasting).add(effect);
        }

        boolean fireLasting = !lasting.isEmpty() && effectTimer % EFFECT_INTERVAL == 0;
        boolean fireInstant = !instant.isEmpty() && effectTimer % INSTANT_INTERVAL == 0;
        if (!fireLasting && !fireInstant) {
            return;
        }

        AABB area = new AABB(pos).inflate(RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (fireLasting) {
                for (Holder<MobEffect> effect : lasting) {
                    entity.addEffect(new MobEffectInstance(effect, EFFECT_DURATION, 0, true, false));
                }
            }
            if (fireInstant) {
                for (Holder<MobEffect> effect : instant) {
                    // Instant effects resolve immediately, so duration/visibility are irrelevant.
                    entity.addEffect(new MobEffectInstance(effect, 1, 0, true, false));
                }
            }
        }
    }

    // ==================== persistence ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fuelStack.isEmpty()) {
            tag.put("Fuel", fuelStack.save(registries));
        }
        tag.putInt("BurnTicksLeft", burnTicksLeft);
        tag.putInt("PotionColor", potionColor);
        ListTag list = new ListTag();
        for (String id : boundEffects) {
            list.add(StringTag.valueOf(id));
        }
        tag.put("BoundEffects", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuelStack = tag.contains("Fuel")
                ? ItemStack.parse(registries, tag.getCompound("Fuel")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        burnTicksLeft = tag.getInt("BurnTicksLeft");
        potionColor = tag.contains("PotionColor") ? tag.getInt("PotionColor") : 0xFFFFFF;
        boundEffects = new ArrayList<>();
        ListTag list = tag.getList("BoundEffects", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            boundEffects.add(list.getString(i));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    /**
     * Required for live updates: {@link #getUpdateTag} only covers the initial chunk send, so
     * without this the client keeps the block entity it got when the lantern was placed and the
     * look-at count never changes.
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void syncToClient() {
        BlockEntityHelper.syncToClient(this);
    }
}
