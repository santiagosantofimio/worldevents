package dev.santiagosantofimio.worldomens.command;

import dev.santiagosantofimio.worldomens.data.WorldOmensData;
import dev.santiagosantofimio.worldomens.event.ActiveEvent;
import dev.santiagosantofimio.worldomens.event.EventManager;
import dev.santiagosantofimio.worldomens.event.impl.MerchantCaravanEvent;
import dev.santiagosantofimio.worldomens.i18n.Lang;
import dev.santiagosantofimio.worldomens.item.EventItems;
import dev.santiagosantofimio.worldomens.mob.NomadMerchant;
import dev.santiagosantofimio.worldomens.util.Drops;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CaravanContracts {
    public record Contract(String id, Item item, int count) {
    }

    public enum Result {
        SUCCESS, NO_EVENT, TOO_FAR, LIMIT, NOT_ENOUGH
    }

    private static final int MAX_PER_EVENT = 2;
    private static final List<Contract> CONTRACTS = List.of(
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
        ActiveEvent active = EventManager.active(player.level());
        if (active == null || !MerchantCaravanEvent.ID.equals(active.event().id())) {
            return -1;
        }
        CompoundTag progress = active.data().getCompoundOrEmpty("contracts");
        String key = player.getUUID().toString();
        return Math.max(0, MAX_PER_EVENT - progress.getIntOr(key, 0));
    }

    public static Result submit(ServerPlayer player, Contract contract) {
        if (player.isSpectator() || player.isCreative() || remaining(player) < 0) {
            return Result.NO_EVENT;
        }
        if (remaining(player) == 0) {
            return Result.LIMIT;
        }
        boolean nearby = !player.level().getEntitiesOfClass(WanderingTrader.class, player.getBoundingBox().inflate(8.0),
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
        ActiveEvent active = EventManager.active(player.level());
        CompoundTag progress = active.data().getCompoundOrEmpty("contracts");
        String key = player.getUUID().toString();
        progress.putInt(key, progress.getIntOr(key, 0) + 1);
        active.data().put("contracts", progress);
        WorldOmensData.get(player.level()).setDirty();
        ItemStack reward = EventItems.EVENT_SEAL.create(Lang.language(player), 1);
        if (!player.getInventory().add(reward)) {
            Drops.spawn(player.level(), player.position(), reward);
        }
        return Result.SUCCESS;
    }
}
