package net.ion1.readyshulker.mixin;

import net.ion1.readyshulker.menu.ChestMenuTransfer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChestMenu.class)
public abstract class ChestMenuMixin implements ChestMenuTransfer {
    @Unique
    private boolean readyShulker$transferOnRemoval;
    @Unique
    private boolean readyShulker$transferred;

    @Override
    public void readyShulker$transferOnRemoval() {
        this.readyShulker$transferOnRemoval = true;
    }

    @Override
    public boolean readyShulker$takeTransferred() {
        boolean transferred = this.readyShulker$transferred;
        this.readyShulker$transferred = false;
        return transferred;
    }

    @Redirect(method = "removed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;stopOpen(Lnet/minecraft/world/entity/ContainerUser;)V"))
    private void readyShulker$transferOpener(Container container, ContainerUser user) {
        if (this.readyShulker$transferOnRemoval) {
            this.readyShulker$transferOnRemoval = false;
            this.readyShulker$transferred = true;
        } else {
            container.stopOpen(user);
        }
    }
}
