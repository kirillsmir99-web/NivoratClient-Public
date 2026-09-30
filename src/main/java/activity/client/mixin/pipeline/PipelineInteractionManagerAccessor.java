package activity.client.mixin.pipeline;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface PipelineInteractionManagerAccessor {
    @Accessor("lastSelectedSlot")
    int activity$getLastSelectedSlot();

    @Accessor("lastSelectedSlot")
    void activity$setLastSelectedSlot(int slot);

    @Invoker("syncSelectedSlot")
    void invokeSyncSelectedSlot();
}
