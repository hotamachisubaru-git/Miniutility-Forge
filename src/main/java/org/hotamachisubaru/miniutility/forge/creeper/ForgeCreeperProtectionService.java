package org.hotamachisubaru.miniutility.forge.creeper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ForgeCreeperProtectionService {
    private volatile boolean enabled;

    public ForgeCreeperProtectionService(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean toggleProtection() {
        this.enabled = !this.enabled;
        return this.enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!this.enabled) {
            return;
        }

        Entity source = event.getExplosion().getDirectSourceEntity();
        if (source == null) {
            source = event.getExplosion().getExploder();
        }

        if (source instanceof Creeper) {
            event.getAffectedBlocks().clear();
        }
    }
}
