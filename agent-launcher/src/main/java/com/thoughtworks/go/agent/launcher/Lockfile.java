/*
 * Copyright 2024 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.agent.launcher;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Lockfile implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Lockfile.class);

    static final String TOUCH_FREQUENCY_PROPERTY = "lockfile.touch.frequency.millis";
    static final String SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_PROPERTY = "lockfile.sleep.before.lastmodified.check.millis";

    private static final long DEFAULT_TOUCH_FREQUENCY_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final Duration DEFAULT_LOCK_FILE_TIMEOUT = Duration.ofMinutes(10);
    private static final long DEFAULT_SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_MILLIS = TimeUnit.SECONDS.toMillis(10);

    private final File lockFile;
    private final long touchFrequencyMillis;
    private final long sleepTimeBeforeLastModifiedCheckMillis;

    private Thread touchLoop;
    private volatile boolean keepRunning;

    public Lockfile(File filename) {
        this.lockFile = filename;
        this.touchFrequencyMillis = getLongProperty(TOUCH_FREQUENCY_PROPERTY, DEFAULT_TOUCH_FREQUENCY_MILLIS);
        this.sleepTimeBeforeLastModifiedCheckMillis = getLongProperty(SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_PROPERTY,
            DEFAULT_SLEEP_TIME_FOR_LAST_MODIFIED_CHECK_MILLIS);
    }

    private long getLongProperty(String propertyName, long defaultValue) {
        long propValue = defaultValue;
        String propValueSet = System.getProperty(propertyName);
        if (propValueSet != null) {
            try {
                propValue = Long.parseLong(propValueSet);
            } catch (Exception ignored) {
            }
        }
        return propValue;
    }

    boolean exists() {
        return lockFile.exists() && lockFileChangedWithinMinutes(DEFAULT_LOCK_FILE_TIMEOUT);
    }

    boolean lockFileChangedWithinMinutes(Duration duration) {
        sleepForSomeTime(); // prevents race condition where the file was just created and not modified.
        long changedAgo = System.currentTimeMillis() - lockFile.lastModified();
        return changedAgo < duration.toMillis();
    }

    private void sleepForSomeTime() {
        try {
            LOG.info("Sleeping for {} ms before 'last modified check'...", sleepTimeBeforeLastModifiedCheckMillis);
            Thread.sleep(sleepTimeBeforeLastModifiedCheckMillis);
        } catch (InterruptedException ignore) {
        }
    }

    void setHooks() throws IOException {
        touch();
        spawnTouchLoop();
    }

    void touch() throws IOException {
        FileUtils.touch(lockFile);
    }

    synchronized void spawnTouchLoop() {
        if (touchLoop == null) {
            keepRunning = true;
            touchLoop = new Thread(this);
            touchLoop.setName("TouchLoop" + touchLoop.getName());
            touchLoop.setDaemon(true);
            touchLoop.start();
        }
    }

    @Override
    public String toString() {
        return "<Lockfile: " + lockFile.getAbsolutePath() + ">";
    }

    @Override
    public void run() {
        LOG.info("Using lock file: {}", lockFile.getAbsolutePath());
        do {
            try {
                touch();
                Thread.sleep(touchFrequencyMillis);
            } catch (Exception e) {
                return;
            }
        } while (keepRunning);
    }

    public synchronized void delete() {
        requestTouchLoopToStop();
        waitForTouchLoopThread();
        try {
            deleteLockFile();
        } catch (Exception e) {
            LOG.error("Error deleting lock file at {}", lockFile.getAbsolutePath(), e);
        }
    }


    private void requestTouchLoopToStop() {
        keepRunning = false;
        if (touchLoop != null) {
            touchLoop.interrupt();
        }
    }

    private void deleteLockFile() {
        if (touchLoop == null) {
            //I haven't got the lock
            return;
        }
        if (!lockFile.exists()) {
            LOG.warn("No lock file found at {}", lockFile.getAbsolutePath());
            return;
        }
        if (!lockFile.delete()) {
            LOG.error("Unable to delete lock file at {}", lockFile.getAbsolutePath());
        }
    }

    private void waitForTouchLoopThread() {
        try {
            if (touchLoop != null) {
                touchLoop.join(TimeUnit.SECONDS.toMillis(10));
            }
        } catch (InterruptedException ignore) {
        }
    }

    public boolean tryLock() {
        if (exists()) {
            String alreadyRunningMsg = "Already running agent launcher in this folder.";
            System.out.println(alreadyRunningMsg);
            LOG.error(alreadyRunningMsg);
            return false;
        }

        try {
            setHooks();
            return true;
        } catch (IOException e) {
            String lockFileFailMsg = "Unable to lock file (" + lockFile.getAbsolutePath() + ")";
            System.err.println(lockFileFailMsg);
            LOG.error(lockFileFailMsg, e);
            return false;
        }
    }

}
