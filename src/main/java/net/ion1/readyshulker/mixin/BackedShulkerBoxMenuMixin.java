package net.ion1.readyshulker.mixin;

import net.ion1.readyshulker.BackedShulkerBoxMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class BackedShulkerBoxMenuMixin {
    @Inject(method = "broadcastChanges", at = @At("TAIL"))
    private void onChangesBroadcast(CallbackInfo ci) {
        readyShulker$syncIfBackedShulkerBox();
    }

    @Unique
    private void readyShulker$syncIfBackedShulkerBox() {
        if (!((Object) this instanceof BackedShulkerBoxMenu backedShulkerMenu)) {
            return;
        }

        backedShulkerMenu.syncBackingFromContainer();
    }
}
