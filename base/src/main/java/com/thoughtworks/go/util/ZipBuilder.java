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
package com.thoughtworks.go.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

public class ZipBuilder {
    private final int level;
    private final ZipUtil zipUtil;
    private final OutputStream destinationStream;
    private final boolean excludeRootDir;
    private final Map<String, File> toAdd = new HashMap<>();

    public ZipBuilder(ZipUtil zipUtil, int level, OutputStream destinationStream, boolean excludeRootDir) {
        this.zipUtil = zipUtil;
        this.destinationStream = destinationStream;
        this.excludeRootDir = excludeRootDir;
        this.level = level;
    }

    public ZipBuilder add(String directoryNameInsideZip, File sourceToZip) {
        toAdd.put(directoryNameInsideZip, sourceToZip);
        return this;
    }

    public void done() throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(destinationStream))) {
            zip.setLevel(level);
            for (Map.Entry<String, File> zipDirToSourceFileEntry : toAdd.entrySet()) {
                File sourceFileToZip = zipDirToSourceFileEntry.getValue();
                String destinationFolder = zipDirToSourceFileEntry.getKey();
                zipUtil.addToZip(new ZipPath(destinationFolder), sourceFileToZip, zip, excludeRootDir);
            }
            zip.flush();
        }
    }
}
