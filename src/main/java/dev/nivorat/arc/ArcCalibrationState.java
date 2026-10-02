package dev.nivorat.arc;

public enum ArcCalibrationState {
    WAITING("Ожидание", 0xff7e8b9f),
    RECORDING("Запись моторики", 0xff3ea4e8),
    PAUSED("Приостановлена", 0xffe89b3e),
    PROCESSING("Анализ и синтез", 0xff9b5de5),
    READY("Профиль готов", 0xff2ec4b6);

    private final String title;
    private final int colorRgba;

    ArcCalibrationState(String title, int colorRgba) {
        this.title = title;
        this.colorRgba = colorRgba;
    }

    public String getTitle() {
        return title;
    }

    public int getColorRgba() {
        return colorRgba;
    }

    public boolean isInteractive() {
        return this == RECORDING;
    }

    public boolean isPaused() {
        return this == PAUSED;
    }

    public boolean isSessionActive() {
        return this == RECORDING || this == PAUSED || this == PROCESSING;
    }
}
