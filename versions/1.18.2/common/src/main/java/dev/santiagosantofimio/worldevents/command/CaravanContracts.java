package dev.santiagosantofimio.worldevents.command;

import dev.santiagosantofimio.worldevents.data.WorldEventsData;
import dev.santiagosantofimio.worldevents.event.ActiveEvent;
import dev.santiagosantofimio.worldevents.event.EventManager;
import dev.santiagosantofimio.worldevents.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldevents.i18n.Lang;
import dev.santiagosantofimio.worldevents.item.EventItems;
import dev.santiagosantofimio.worldevents.mob.NomadMerchant;
import dev.santiagosantofimio.worldevents.util.Drops;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CaravanContracts {
    public static final class Contract {
        private final String id;
        private final Item item;
        private final int count;

        public Contract(String id, Item item, int count) {
            this.id = id;
            this.item = item;
            this.count = count;
        }

        public String id() { return id; }
        public Item item() { return item; }
        public int count() { return count; }
    }

    public enum Result {
        SUCCESS, NO_EVENT, TOO_FAR, LIMIT, NOT_ENOUGH
    }

    private static final int MAX_PER_EVENT = 2;
    private static final List<Contract> CONTRACTS = java.util.Arrays.asList(
            new Contract("wheat", Items.WHEAT, 48),
            new Contract("iron", Items.IRON_INGOT, 12),
            new Contract("rotten_flesh", Items.ROTTEN_FLESH, 64));

    private CaravanContracts() {
    }

    public static List<Contract> all() {
        return CONTRACTS;
    }

    public static Contract get(String id) {
        for (Contract contract : CONTRACTS) {
            if (contract.id().equals(id)) {
                return contract;
            }
        }
        return null;
    }

    public static int remaining(ServerPlayer player) {
        ActiveEvent active = EventManager.active(player.getLevel());
        if (active == null || !MerchantCaravanEvent.ID.equals(active.event().id())) {
            return -1;
        }
        CompoundTag progress = active.data().getCompound("contracts");
        String key = player.getUUID().toString();
        return Math.max(0, MAX_PER_EVENT - progress.getInt(key));
    }

    public static Result submit(ServerPlayer player, Contract contract) {
        if (player.isSpectator() || player.isCreative() || remaining(player) < 0) {
            return Result.NO_EVENT;
        }
        if (remaining(player) == 0) {
            return Result.LIMIT;
        }
        boolean nearby = !player.getLevel().getEntitiesOfClass(WanderingTrader.class, player.getBoundingBox().inflate(8.0),
                merchant -> NomadMerchant.is(merchant) && merchant.isAlive()).isEmpty();
        if (!nearby) {
            return Result.TOO_FAR;
        }
        int available = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(contract.item())) {
                available += stack.getCount();
            }
        }
        if (available < contract.count()) {
            return Result.NOT_ENOUGH;
        }
        int needed = contract.count();
        for (int slot = 0; slot < player.getInventory().getContainerSize() && needed > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(contract.item())) {
                int taken = Math.min(stack.getCount(), needed);
                stack.shrink(taken);
                needed -= taken;
            }
        }
        ActiveEvent active = EventManager.active(player.getLevel());
        CompoundTag progress = active.data().getCompound("contracts");
        String key = player.getUUID().toString();
        progress.putInt(key, progress.getInt(key) + 1);
        active.data().put("contracts", progress);
        WorldEventsData.get(player.getLevel()).setDirty();
        ItemStack reward = EventItems.EVENT_SEAL.create(Lang.language(player), 1);
        if (!player.getInventory().add(reward)) {
            Drops.spawn(player.getLevel(), player.position(), reward);
        }
        return Result.SUCCESS;
    }
}
