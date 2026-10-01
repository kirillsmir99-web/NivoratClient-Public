package activity.client.gui;

import activity.client.gui.custom.utils.render.render2d.ClientPalette;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteShaderContractTest {
    @Test void glassUniformArrayAndJavaSlotsHaveTheSameStd140Layout() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/kimiko/shaders/ui/glass/glass.fsh")) {
            assertNotNull(stream);
            String shader = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            var array = Pattern.compile("vec4 pal\\[(\\d+)\\]").matcher(shader);
            assertTrue(array.find());
            assertEquals(Integer.parseInt(array.group(1)) * 16, ClientPalette.SLOT_STRIDE_BYTES * ClientPalette.SLOTS);
            var stride = Pattern.compile("zFlag - 10\\) \\* (\\d+)").matcher(shader);
            assertTrue(stride.find());
            assertEquals(Integer.parseInt(stride.group(1)) * 16, ClientPalette.SLOT_STRIDE_BYTES);
        }
    }
}
