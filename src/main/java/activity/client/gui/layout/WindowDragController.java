package activity.client.gui.layout;

import activity.client.config.ActivityConfig;
import activity.client.config.ActivityConfigManager;

public class WindowDragController {

    private boolean dragging = false;
    private double dragStartX = 0;
    private double dragStartY = 0;
    private int initialWindowX = 0;
    private int initialWindowY = 0;
    private int currentWindowX = -1;
    private int currentWindowY = -1;
    private int currentWindowWidth = -1;
    private int currentWindowHeight = -1;
    private boolean maximized = false;
    private int unmaximizedX = -1;
    private int unmaximizedY = -1;
    private int unmaximizedWidth = -1;
    private int unmaximizedHeight = -1;

    public void init() {
        ActivityConfig config = ActivityConfigManager.getConfig();
        if (config != null) {
            this.currentWindowX = config.windowPosX;
            this.currentWindowY = config.windowPosY;
            this.currentWindowWidth = config.windowWidth;
            this.currentWindowHeight = config.windowHeight;
            this.maximized = config.windowMaximized;
            this.unmaximizedX = config.unmaximizedX;
            this.unmaximizedY = config.unmaximizedY;
            this.unmaximizedWidth = config.unmaximizedWidth;
            this.unmaximizedHeight = config.unmaximizedHeight;
        } else {
            this.currentWindowX = -1;
            this.currentWindowY = -1;
            this.currentWindowWidth = -1;
            this.currentWindowHeight = -1;
            this.maximized = false;
            this.unmaximizedX = -1;
            this.unmaximizedY = -1;
            this.unmaximizedWidth = -1;
            this.unmaximizedHeight = -1;
        }
    }

    public int getWindowX() {
        return currentWindowX;
    }

    public int getWindowY() {
        return currentWindowY;
    }

    public int getWindowWidth() {
        return currentWindowWidth;
    }

    public int getWindowHeight() {
        return currentWindowHeight;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void setMaximized(boolean maximized) {
        this.maximized = maximized;
    }

    public int getUnmaximizedX() {
        return unmaximizedX;
    }

    public int getUnmaximizedY() {
        return unmaximizedY;
    }

    public int getUnmaximizedWidth() {
        return unmaximizedWidth;
    }

    public int getUnmaximizedHeight() {
        return unmaximizedHeight;
    }

    public void setWindowPosition(int x, int y) {
        this.currentWindowX = x;
        this.currentWindowY = y;
    }

    public void setWindowDimensions(int width, int height) {
        this.currentWindowWidth = width;
        this.currentWindowHeight = height;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void maximize(WindowLayout layout) {
        if (this.currentWindowX >= 0) {
            this.unmaximizedX = this.currentWindowX;
        } else if (layout != null) {
            this.unmaximizedX = layout.windowX;
        }
        if (this.currentWindowY >= 0) {
            this.unmaximizedY = this.currentWindowY;
        } else if (layout != null) {
            this.unmaximizedY = layout.windowY;
        }
        if (this.currentWindowWidth > 0) {
            this.unmaximizedWidth = this.currentWindowWidth;
        } else if (layout != null) {
            this.unmaximizedWidth = layout.windowWidth;
        }
        if (this.currentWindowHeight > 0) {
            this.unmaximizedHeight = this.currentWindowHeight;
        } else if (layout != null) {
            this.unmaximizedHeight = layout.windowHeight;
        }
        this.maximized = true;
        persistState();
    }

    public void restore() {
        this.maximized = false;
        if (this.unmaximizedX >= 0 && this.unmaximizedY >= 0) {
            this.currentWindowX = this.unmaximizedX;
            this.currentWindowY = this.unmaximizedY;
        }
        if (this.unmaximizedWidth > 0 && this.unmaximizedHeight > 0) {
            this.currentWindowWidth = this.unmaximizedWidth;
            this.currentWindowHeight = this.unmaximizedHeight;
        }
        persistState();
    }

    public boolean toggleMaximize(WindowLayout layout) {
        if (this.maximized) {
            restore();
            return false;
        } else {
            maximize(layout);
            return true;
        }
    }

    private void persistState() {
        try {
            ActivityConfig config = ActivityConfigManager.getConfig();
            if (config != null) {
                config.windowPosX = this.currentWindowX;
                config.windowPosY = this.currentWindowY;
                config.windowWidth = this.currentWindowWidth;
                config.windowHeight = this.currentWindowHeight;
                config.windowMaximized = this.maximized;
                config.unmaximizedX = this.unmaximizedX;
                config.unmaximizedY = this.unmaximizedY;
                config.unmaximizedWidth = this.unmaximizedWidth;
                config.unmaximizedHeight = this.unmaximizedHeight;
                ActivityConfigManager.save();
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean startDrag(double mouseX, double mouseY, WindowLayout layout, boolean overControls) {
        if (overControls) return false;

        if (mouseX >= layout.headerX && mouseX < layout.headerX + layout.headerWidth &&
            mouseY >= layout.headerY && mouseY < layout.headerY + layout.headerHeight) {
            if (this.maximized) {
                restore();
                int unmaxW = (this.currentWindowWidth > 0) ? this.currentWindowWidth : (layout.windowWidth * 3 / 4);
                this.currentWindowX = (int) Math.round(mouseX - unmaxW / 2.0);
                this.currentWindowY = (int) Math.round(mouseY - layout.headerHeight / 2.0);
                this.initialWindowX = this.currentWindowX;
                this.initialWindowY = this.currentWindowY;
            } else {
                this.initialWindowX = layout.windowX;
                this.initialWindowY = layout.windowY;
            }
            this.dragging = true;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
            return true;
        }
        return false;
    }

    public void clampWindowPosition(int screenWidth, int screenHeight, int windowWidth, int windowHeight) {
        if (this.maximized) {

            if (this.unmaximizedWidth > 0 && this.unmaximizedHeight > 0) {
                this.unmaximizedWidth = Math.min(this.unmaximizedWidth, screenWidth);
                this.unmaximizedHeight = Math.min(this.unmaximizedHeight, screenHeight);
            }
            if (this.unmaximizedX >= 0 && this.unmaximizedY >= 0) {
                int unmaxW = this.unmaximizedWidth > 0 ? this.unmaximizedWidth : (screenWidth * 3 / 4);
                int unmaxH = this.unmaximizedHeight > 0 ? this.unmaximizedHeight : (screenHeight * 3 / 4);
                int maxX = Math.max(0, screenWidth - unmaxW);
                int maxY = Math.max(0, screenHeight - unmaxH);
                this.unmaximizedX = Math.clamp(this.unmaximizedX, 0, maxX);
                this.unmaximizedY = Math.clamp(this.unmaximizedY, 0, maxY);
                this.currentWindowX = this.unmaximizedX;
                this.currentWindowY = this.unmaximizedY;
            }
            try {
                ActivityConfig config = ActivityConfigManager.getConfig();
                if (config != null) {
                    config.unmaximizedX = this.unmaximizedX;
                    config.unmaximizedY = this.unmaximizedY;
                    config.unmaximizedWidth = this.unmaximizedWidth;
                    config.unmaximizedHeight = this.unmaximizedHeight;
                    config.windowPosX = this.currentWindowX;
                    config.windowPosY = this.currentWindowY;
                }
            } catch (Throwable ignored) {
            }
            return;
        }

        if (this.currentWindowX >= 0 && this.currentWindowY >= 0) {
            int maxX = Math.max(0, screenWidth - windowWidth);
            int maxY = Math.max(0, screenHeight - windowHeight);
            this.currentWindowX = Math.clamp(this.currentWindowX, 0, maxX);
            this.currentWindowY = Math.clamp(this.currentWindowY, 0, maxY);
            this.currentWindowWidth = windowWidth;
            this.currentWindowHeight = windowHeight;
            try {
                ActivityConfig config = ActivityConfigManager.getConfig();
                if (config != null) {
                    config.windowPosX = this.currentWindowX;
                    config.windowPosY = this.currentWindowY;
                    config.windowWidth = this.currentWindowWidth;
                    config.windowHeight = this.currentWindowHeight;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean onDrag(double mouseX, double mouseY, int screenWidth, int screenHeight, int windowWidth, int windowHeight) {
        if (!this.dragging) return false;

        int deltaX = (int) (mouseX - this.dragStartX);
        int deltaY = (int) (mouseY - this.dragStartY);

        int newX = this.initialWindowX + deltaX;
        int newY = this.initialWindowY + deltaY;

        int maxX = Math.max(0, screenWidth - windowWidth);
        int maxY = Math.max(0, screenHeight - windowHeight);

        this.currentWindowX = Math.clamp(newX, 0, maxX);
        this.currentWindowY = Math.clamp(newY, 0, maxY);

        return true;
    }

    public boolean stopDrag() {
        if (this.dragging) {
            this.dragging = false;
            try {
                ActivityConfig config = ActivityConfigManager.getConfig();
                if (config != null) {
                    config.windowPosX = this.currentWindowX;
                    config.windowPosY = this.currentWindowY;
                    config.windowWidth = this.currentWindowWidth;
                    config.windowHeight = this.currentWindowHeight;
                    config.windowMaximized = this.maximized;
                    ActivityConfigManager.save();
                }
            } catch (Throwable ignored) {
            }
            return true;
        }
        return false;
    }

    public void recenter() {
        this.currentWindowX = -1;
        this.currentWindowY = -1;
        this.currentWindowWidth = -1;
        this.currentWindowHeight = -1;
        this.maximized = false;
        this.unmaximizedX = -1;
        this.unmaximizedY = -1;
        this.unmaximizedWidth = -1;
        this.unmaximizedHeight = -1;
        try {
            ActivityConfig config = ActivityConfigManager.getConfig();
            if (config != null) {
                config.windowPosX = -1;
                config.windowPosY = -1;
                config.windowWidth = -1;
                config.windowHeight = -1;
                config.windowMaximized = false;
                config.unmaximizedX = -1;
                config.unmaximizedY = -1;
                config.unmaximizedWidth = -1;
                config.unmaximizedHeight = -1;
                ActivityConfigManager.save();
            }
        } catch (Throwable ignored) {
        }
    }
}
