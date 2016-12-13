package org.gradle.profiler.hp;

import java.io.File;

public class HonestProfilerArgs {
    private final File hpHomeDir;
    private final File fgHomeDir;
    private final File logPath;
    private final int port;
    private final int interval;

    public HonestProfilerArgs(final File hpHomeDir, final File fgHomeDir, final File logPath, final int port, final int interval) {
        this.hpHomeDir = hpHomeDir;
        this.fgHomeDir = fgHomeDir;
        this.logPath = logPath;
        this.port = port;
        this.interval = interval;
    }

    public File getHpHomeDir() {
        return hpHomeDir;
    }

    public File getFgHomeDir() {
        return fgHomeDir;
    }

    public File getLogPath() {
        return logPath;
    }

    public int getPort() {
        return port;
    }

    public int getInterval() {
        return interval;
    }
}
