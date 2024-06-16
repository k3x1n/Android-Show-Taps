package show.taps;

import show.taps.DevInfo;

interface KernelInterface {
    boolean start(@nullable String dev, in int[] colors,
                int touchPointSize, int dismissTime,
                int circleStroke, int lineStroke, int colorAlpha,
                int viewMode);

    boolean isRunning();

    void continueUse();

    void stop();

    oneway void exit();

    oneway void updateInfo(int touchPointSize, int dismissTime, int circleStroke, int lineStroke, int colorAlpha);

    oneway void updateColors(in int[] colors);

}
