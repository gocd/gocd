/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.agent.bootstrapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.go.agent.common.util.JarUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class LauncherTempFileHandler implements Runnable {

    private static volatile Thread reaperThread;

    static final String LAUNCHER_TMP_FILE_LIST = ".tmp.file.list";

    private static final Log LOG = LogFactory.getLog(DefaultAgentLauncherCreatorImpl.class);

    private static final int TEN_MINS = 1000 * 60 * 10;

    @Override public void run() {
        while (true) {
            reapFiles();
            sleepForAMoment();
        }
    }

    private void reapFiles() {
        FileReader tmpFileReader = null;
        try {
            tmpFileReader = new FileReader(LAUNCHER_TMP_FILE_LIST);
            List fileList = IOUtils.readLines(tmpFileReader);
            Set<String> fileSet = new HashSet<>(fileList);
            for (String fileName : fileSet) {
                File file = new File(fileName);
                FileUtils.deleteQuietly(file);
                JarUtil.cleanup(fileName);
                if (!file.exists() && !JarUtil.tempFileExist(fileName)) {
                    fileList.remove(fileName);
                }
            }
            writeToFile(fileList, false);
        } catch (Exception e) {
        } finally {
            IOUtils.closeQuietly(tmpFileReader);
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
        FileWriter tmpFileListWriter = null;
        try {
            tmpFileListWriter = new FileWriter(LAUNCHER_TMP_FILE_LIST, append);
            IOUtils.writeLines(rows, IOUtils.LINE_SEPARATOR, tmpFileListWriter);
        } catch (IOException e) {
            LOG.error("Could not update temp files list", e);
        } finally {
            IOUtils.closeQuietly(tmpFileListWriter);
        }
    }

    private static void sleepForAMoment() {
        try {
            Thread.sleep(TEN_MINS);
        } catch (InterruptedException e) {
        }
    }
}
