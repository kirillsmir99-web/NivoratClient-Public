package activity.client.gui;

import activity.client.gui.custom.HoverDelay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HoverDelayTest {
    @Test void slowFramesDoNotRestartContinuousHover() {
        var delay=new HoverDelay<Object>(450_000_000L);
        var item=new Object();
        delay.update(item,0);
        for(long time:new long[]{200_000_000L,400_000_000L,600_000_000L})delay.update(item,time);
        assertSame(item,delay.ready(600_000_000L));
    }
    @Test void leavingOrChangingTargetRestartsDelay() {
        var delay=new HoverDelay<Object>(450_000_000L);
        var first=new Object();var second=new Object();
        delay.update(first,0);assertNull(delay.ready(449_000_000L));
        assertSame(first,delay.ready(450_000_000L));
        delay.update(null,500_000_000L);assertNull(delay.ready(1_000_000_000L));
        delay.update(second,1_000_000_000L);assertNull(delay.ready(1_100_000_000L));
        assertSame(second,delay.ready(1_450_000_000L));
    }
}
