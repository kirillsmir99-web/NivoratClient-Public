package activity.client.module.stub;

import net.minecraft.text.Text;

@Deprecated
public class MatrixSlimeStub extends MatrixTransformStub {

    public static final String ID = "auto_stun_slime";

    public MatrixSlimeStub() {
        super(
            ID,
            Text.translatable("activity.module.auto_stun_slam.name"),
            Text.translatable("activity.module.auto_stun_slam.desc")
        );
    }
}
