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

package com.thoughtworks.go.helpers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public final class FileSystemUtils {

    private static final String ROOT = "target" + File.separator + "testfiles";

    static {
        new File("target").mkdir();
    }

    private FileSystemUtils() {
    }

    public static File createDirectory(String directoryName) {
        File directory = new File(getTestRootDir(), directoryName);
        deleteDirectory(directory);
        directory.mkdir();
        directory.deleteOnExit();
        return directory;
    }

    private static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
        }
    }

    public static File createDirectory(String directoryName, String parent) {
        File directory = new File(getTestRootDir(), parent + File.separator + directoryName);
        deleteDirectory(directory);
        directory.mkdir();
        directory.deleteOnExit();
        return directory;
    }

    public static File getTestRootDir() {
        File root = new File(ROOT);
        if (!root.exists() && !root.mkdir()) {
            throw new RuntimeException("Failed to create directory for test data [" + root.getAbsolutePath() + "]");
        }
        return root;
    }

    public static File createFile(String filename, File directory) throws IOException {
        final File file = new File(directory, filename);

        // attempt workaround for intermitent w2k error:
        // java.io.IOException: Access is denied
        // at java.io.WinNTFileSystem.createFileExclusively(Native Method)
        // at java.io.File.createNewFile(File.java:883)
        // at net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils.createFile(FilesystemUtils.java:94)
        // ...
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6198547
        // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6325169
        int count = 0;
        IOException lastIOException = null;
        do {
            try {
                file.createNewFile();
            } catch (IOException e) {
                lastIOException = e;
                Thread.yield();
            }
            count++;
        } while (!file.exists() && count < 3);

        // another attempt to workaround the above bugs, using approach similar to that of Ant MkDirs taskdef
        // see Util.doMkDirs()
        if (!file.exists()) {
            try {
                Thread.sleep(10);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    lastIOException = e;
                }
            } catch (InterruptedException ex) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    lastIOException = e;
                }
            }
        }

        if (!file.exists()) {
            if (lastIOException != null) {
                throw new RuntimeException("Error creating file: " + file.getAbsolutePath(), lastIOException);
            } else {
                throw new RuntimeException("Error creating file: " + file.getAbsolutePath());
            }
        }

        file.deleteOnExit();
        return file;
    }

    public static File createFile(String filename) throws IOException {
        File file = new File(ROOT + File.separator + filename);
        file.createNewFile();
        file.deleteOnExit();
        return file;
    }

}
