package activity.client.module.stub;

import net.minecraft.text.Text;

/**
 * Backward compatibility alias stub for AutoStunSlime -> AutoStunSlam.
 * @deprecated Use {@link AutoStunSlamStub} instead.
 */
@Deprecated
public class AutoStunSlimeStub extends AutoStunSlamStub {

    public static final String ID = "auto_stun_slime";

    public AutoStunSlimeStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_stun_slam.name"),
            Text.translatable("activity.module.auto_stun_slam.desc")
        );
    }
}
