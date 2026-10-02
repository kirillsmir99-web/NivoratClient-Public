package activity.client.mixin.pipeline;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface AsyncInputMoveAccessor {
    @Accessor("movementVector")
    void async$setMove(Vec2f vector);
}
