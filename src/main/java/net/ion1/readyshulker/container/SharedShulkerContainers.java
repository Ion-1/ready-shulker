package net.ion1.readyshulker.container;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Registry of open shulker containers, for multiplayer sync.
 */
public final class SharedShulkerContainers {
    private static final Map<ItemStack, Entry> OPEN = new IdentityHashMap<>();

    private SharedShulkerContainers() {
    }

    public static ItemStackBackedContainer acquire(ItemStack backing, Container parent, int parentSlot) {
        Entry entry = OPEN.get(backing);
        if (entry == null) {
            entry = new Entry(new ItemStackBackedContainer(backing, parent, parentSlot));
            OPEN.put(backing, entry);
        } else if (parentSlot < 0 || parentSlot >= parent.getContainerSize() || parent.getItem(parentSlot) != backing) {
            throw new IllegalStateException("Parent slot no longer contains the shared shulker");
        }
        entry.references++;
        return entry.container;
    }

    public static void release(ItemStack backing, ItemStackBackedContainer container) {
        Entry entry = OPEN.get(backing);
        if (entry == null || entry.container != container) {
            throw new IllegalStateException("Released an unregistered shared shulker container");
        }
        if (--entry.references == 0) {
            OPEN.remove(backing);
        }
    }

    private static final class Entry {
        private final ItemStackBackedContainer container;
        private int references;

        private Entry(ItemStackBackedContainer container) {
            this.container = container;
        }
    }
}
