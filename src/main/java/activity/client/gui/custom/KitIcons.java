package activity.client.gui.custom;

import activity.client.gui.custom.api.modules.Category;
import activity.client.gui.custom.utils.render.fonts.Fonts;
import activity.client.gui.custom.utils.render.fonts.NvIcons;
import activity.client.gui.custom.utils.render.render2d.Render2D;

public final class KitIcons {
    private static final String ATLAS="activity:textures/gui/kit_icons.png";
    private KitIcons() {}
    public static void draw(Category category,float x,float y,float size,int color){
        int slot=switch(category){
            case VISUALS -> 0;case NPOT -> 1;case CRYSTAL -> 2;case UHC -> 3;case SMP -> 4;
            case MACE -> 5;case BEAST -> 6;case SWORD -> 7;case AXE -> 8;case DPOT -> 9;
            default -> -1;
        };
        if (slot < 0) {
            Fonts.NV.msdf(NvIcons.forCategory(category), x, y, size, color);
            return;
        }
        Render2D.imageUv(ATLAS, x, y, size, size, 0.0f, 0.0f, (float) slot / 10.0f, 0.0f, (float) (slot + 1) / 10.0f, 1.0f, color);
    }
}
