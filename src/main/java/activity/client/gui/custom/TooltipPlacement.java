package activity.client.gui.custom;

public record TooltipPlacement(float x, float y, float width, float height) {
    public static TooltipPlacement place(float mouseX, float mouseY, float width, float height,
                                         float screenWidth, float screenHeight) {
        float margin = 8f;
        float gap = 14f;
        width = Math.min(width, Math.max(1f, screenWidth - margin * 2));
        height = Math.min(height, Math.max(1f, screenHeight - margin * 2));
        float x = mouseX + gap;
        float y = mouseY + gap;
        if (x + width > screenWidth - margin) x = mouseX - gap - width;
        if (y + height > screenHeight - margin) y = mouseY - gap - height;
        x = Math.clamp(x, margin, Math.max(margin, screenWidth - width - margin));
        y = Math.clamp(y, margin, Math.max(margin, screenHeight - height - margin));
        return new TooltipPlacement(x, y, width, height);
    }
}
