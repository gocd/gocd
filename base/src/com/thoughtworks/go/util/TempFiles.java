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

package com.thoughtworks.go.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TempFiles {

    private List<File> createdFiles = new ArrayList<>();
    private Clock clock;

    private Runnable cleanupHook = new Runnable() {
        public void run() {
            cleanUp();
        }
    };

    public TempFiles() {
        this.clock = new SystemTimeClock();
        Runtime.getRuntime().addShutdownHook(new Thread(cleanupHook));
    }

    public File createFile(String fileName) throws IOException {
        File file = new File(tempFolder(), fileName);
        file.getParentFile().mkdirs();
        file.createNewFile();
        createdFiles.add(file);
        return file;
    }

    public void cleanUp() {
        for (File file : createdFiles) {
            if (file.isDirectory()) {
                FileUtil.deleteFolder(file);
            } else {
                file.delete();
            }
        }
    }

    public File mkdir(String dirName) {
        File file = new File(tempFolder(), dirName);
        file.mkdirs();
        createdFiles.add(file);
        return file;
    }

    private static File tempFolder() {
        File cruiseTmpDir = new File(System.getProperty("java.io.tmpdir"), "cruise");
        cruiseTmpDir.mkdirs();
        return cruiseTmpDir;
    }

    public static File createUniqueFile(String prefix) {
        return new File(tempFolder(), prefix + "-" + UUID.randomUUID());
    }

    public File createUniqueFolder(String folder) {
        return mkdir(folder + clock.currentTimeMillis());
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
