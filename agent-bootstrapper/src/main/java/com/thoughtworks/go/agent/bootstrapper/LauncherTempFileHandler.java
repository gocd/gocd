/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.*;

class LauncherTempFileHandler implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LauncherTempFileHandler.class);
    private static final Path LAUNCHER_TMP_FILE_LIST = Path.of(".tmp.file.list");
    private static volatile Thread reaperThread;

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            reapFiles();
            sleepForAMoment();
        }
    }

    private void reapFiles() {
        try {
            List<String> fileList = Files.readAllLines(LAUNCHER_TMP_FILE_LIST);
            Set<String> fileSet = new HashSet<>(fileList);
            for (String fileName : fileSet) {
                File file = new File(fileName);
                FileUtils.deleteQuietly(file);
                File depsDir = new File(FileUtil.TMP_PARENT_DIR, fileName);
                FileUtils.deleteQuietly(depsDir);
                if (!file.exists() && !depsDir.exists()) {
                    fileList.remove(fileName);
                }
            }
            writeToFile(fileList, false);
        } catch (Exception ignore) {
        }
    }

    public synchronized static void startTempFileReaper() {
        if (reaperThread == null) {
            reaperThread = new Thread(new LauncherTempFileHandler());
            reaperThread.setName("TempFileReaper" + reaperThread.getName());
            reaperThread.setDaemon(true);
            reaperThread.start();
        }
    }

    synchronized static void writeToFile(final List<String> rows, final boolean append) {
        try {
            Files.writeString(LAUNCHER_TMP_FILE_LIST, join(lineSeparator(), rows), UTF_8, CREATE, WRITE, append ? APPEND : TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOG.error("Could not update temp files list", e);
        }
    }

    private static void sleepForAMoment() {
        try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
