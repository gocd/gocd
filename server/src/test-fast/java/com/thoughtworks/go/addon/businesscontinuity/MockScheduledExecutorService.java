package com.thoughtworks.go.addon.businesscontinuity;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MockScheduledExecutorService extends ScheduledThreadPoolExecutor {
    private int times;

    public MockScheduledExecutorService() {
        super(0);
        times = 1;
    }

    public MockScheduledExecutorService(int times) {
        super(0);
        this.times = times;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        for (int index = 0; index < times; index++) {
            command.run();
        }
        return null;
    }
}
