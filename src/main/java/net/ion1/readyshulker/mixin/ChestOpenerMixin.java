package net.ion1.readyshulker.mixin;

import net.ion1.readyshulker.menu.ShulkerStackBackedContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.block.entity.ChestBlockEntity$1")
public abstract class ChestOpenerMixin {
    @Shadow
    @Final
    private ChestBlockEntity this$0;

    @Inject(method = "isOwnContainer", at = @At("HEAD"), cancellable = true)
    private void readyShulker$isNestedViewer(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.containerMenu instanceof ShulkerStackBackedContainerMenu menu && menu.ownsChestOpener(this.this$0)) {
            cir.setReturnValue(true);
        }
    }
}
