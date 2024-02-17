package show.taps;

import show.taps.DevInfo;

interface KernelInterface {
    DevInfo[] getInputPath();

    boolean start(String dev, in int[] colors,
            int touchPointSize, int dismissTime, int circleStroke, int lineStroke, int colorAlpha);

    boolean isRunning();

    void stop();

    oneway void updateInfo(int touchPointSize, int dismissTime, int circleStroke, int lineStroke, int colorAlpha);

    oneway void updateColors(in int[] colors);

}
