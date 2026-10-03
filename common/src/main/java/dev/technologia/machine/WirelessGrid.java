package dev.technologia.machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Wireless power. Receivers announce themselves on a channel while they run; a sender shares its
 * throughput between the loaded receivers on its channel, in any dimension. Nothing is saved: the
 * list rebuilds itself as receivers tick. Channels are public numbers, not protected by owner.
 */
final class WirelessGrid {
    private static final Map<MinecraftServer, Map<Integer, Set<GlobalPos>>> RECEIVERS = new WeakHashMap<>();
    /** Reused on the server thread so a sender allocates nothing per tick. */
    private static final List<MachineBlockEntity> LIVE = new ArrayList<>();

    private static Set<GlobalPos> channel(ServerLevel level, int channel) {
        return RECEIVERS.computeIfAbsent(level.getServer(), server -> new HashMap<>()).computeIfAbsent(channel, key -> new LinkedHashSet<>());
    }

    static void join(MachineBlockEntity receiver, ServerLevel level) {
        channel(level, receiver.channel()).add(GlobalPos.of(level.dimension(), receiver.getBlockPos()));
        receiver.joinedChannel = receiver.channel();
    }

    static void send(MachineBlockEntity sender, ServerLevel level) {
        int budget = Math.min(sender.energy.stored(), sender.wirelessLimit());
        Set<GlobalPos> targets = channel(level, sender.channel());
        LIVE.clear();
        for (Iterator<GlobalPos> iterator = targets.iterator(); iterator.hasNext();) {
            GlobalPos target = iterator.next();
            ServerLevel world = level.getServer().getLevel(target.dimension());
            // An unloaded receiver stays listed: it works again as soon as its chunk loads.
            if (world == null || !world.isLoaded(target.pos())) continue;
            if (!(world.getBlockEntity(target.pos()) instanceof MachineBlockEntity receiver) || receiver.isRemoved()
                    || receiver.kind != MachineKind.WIRELESS_RECEIVER || receiver.channel() != sender.channel()) {
                iterator.remove();
                continue;
            }
            if (receiver.isRunning() && receiver.energy.stored() < receiver.energy.capacity()) LIVE.add(receiver);
        }
        if (targets.isEmpty()) { sender.status(14); return; }
        if (budget <= 0) { LIVE.clear(); sender.status(2); return; }
        int moved = 0;
        // Equal shares first, then whatever is left to anyone who still has room.
        for (int pass = 0; pass < 2 && moved < budget && !LIVE.isEmpty(); pass++) {
            int share = pass == 0 ? Math.max(1, budget / LIVE.size()) : budget;
            for (MachineBlockEntity receiver : LIVE) {
                int accepted = receiver.energy.receive(Math.min(share, budget - moved), false);
                if (accepted <= 0) continue;
                moved += accepted;
                receiver.wirelessIn += accepted;
                receiver.lastWireless = receiver.getLevel().getGameTime();
                receiver.setChanged();
                if (moved >= budget) break;
            }
        }
        LIVE.clear();
        if (moved > 0) { sender.energy.extract(moved, false); sender.setChanged(); }
        sender.rate = moved;
        sender.status(moved > 0 ? 1 : 0);
    }
    private WirelessGrid() {}
}
