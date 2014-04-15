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

package com.thoughtworks.go.server.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * @deprecated ChrisS : This class has no good reason to exist. Kill it.
 */
public class LogFile  {
    private static final String LOG_COMPRESSED_SUFFIX = ".xml.gz";
    private File logFile;

    public LogFile(File logFile) {
        this.logFile = logFile;
    }

    public LogFile(File folder, String fileName) {
        this(new File(folder, fileName));
    }

    public InputStream getInputStream() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(logFile);
        if (isZippedLogFile()) {
            return new GZIPInputStream(fileInputStream);
        } else {
            return fileInputStream;
        }
    }

    private boolean isZippedLogFile() {
        return logFile.getName().endsWith(LOG_COMPRESSED_SUFFIX);
    }

    public File getFile() {
        return logFile;
    }

    public String getPath() {
        return getFile().getPath();
    }
}
