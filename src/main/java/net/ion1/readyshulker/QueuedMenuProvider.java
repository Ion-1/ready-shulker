package net.ion1.readyshulker;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import org.jspecify.annotations.NonNull;

/**
 * Defers menu open calls until the next server tick.
 */
public final class QueuedMenuProvider implements MenuProvider {
    private static final Queue<QueuedOpenRequest> OPEN_QUEUE = new ArrayDeque<>();

    private final SimpleMenuProvider delegate;

    public QueuedMenuProvider(MenuConstructor menuConstructor, Component title) {
        this.delegate = new SimpleMenuProvider(menuConstructor, title);
    }

    public static void enqueue(ServerPlayer player, MenuConstructor menuConstructor, Component title) {
        OPEN_QUEUE.add(new QueuedOpenRequest(player, new QueuedMenuProvider(menuConstructor, title)));
    }

    public static void tick() {
        QueuedOpenRequest queued;
        while ((queued = OPEN_QUEUE.poll()) != null) {
            // Avoid a client close-screen roundtrip when swapping between our shulker menus.
            // closeContainer() sends ClientboundContainerClosePacket, which makes the client
            // briefly leave GUI mode and recenter the mouse cursor before the next screen opens.
            if (queued.player().containerMenu instanceof BackedShulkerBoxMenu) {
                queued.player().doCloseContainer();
            }
            queued.player().openMenu(queued.provider());
        }
    }

    @Override
    public @NonNull Component getDisplayName() {
        return this.delegate.getDisplayName();
    }

    @Override
    public @NonNull AbstractContainerMenu createMenu(int syncId, @NonNull Inventory inventory, @NonNull Player player) {
        return this.delegate.createMenu(syncId, inventory, player);
    }

    private record QueuedOpenRequest(ServerPlayer player, MenuProvider provider) {
    }
}

