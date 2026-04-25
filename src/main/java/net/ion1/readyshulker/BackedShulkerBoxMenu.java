package net.ion1.readyshulker;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.NonNull;

// ShulkerBoxMenu backed by an item stack
// Closes when the player is no longer holding the stack, and saves the contents to the stack when closed
// Requires Mixin to inject private doClick
public class BackedShulkerBoxMenu extends ShulkerBoxMenu {
    private final ItemStack backing;
    private final SimpleContainer container;

    public BackedShulkerBoxMenu(final int containerId, final Inventory inventory, final SimpleContainer container, final ItemStack shulkerStack) {
        super(containerId, inventory, container);
        this.backing = shulkerStack;
        this.container = container;
    }

    public boolean isBackingStack(ItemStack stack) {
        return this.backing == stack;
    }

    public SimpleContainer getBackingContainer() {
        return this.container;
    }

    public void syncBackingFromContainer() {
        this.backing.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.container.getItems()));
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        syncBackingFromContainer();
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return super.stillValid(player) && isBackedByPlayerInventory(player, this.backing);
    }

    private static boolean isBackedByPlayerInventory(Player player, @NonNull ItemStack backingStack) {
        if (backingStack.isEmpty()) {
            return false;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i) == backingStack) {
                return true;
            }
        }

        return false;
    }
}
