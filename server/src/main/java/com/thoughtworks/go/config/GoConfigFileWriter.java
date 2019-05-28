/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class GoConfigFileWriter {
    private final Charset UTF_8 = StandardCharsets.UTF_8;
    private SystemEnvironment systemEnvironment;
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

    public GoConfigFileWriter(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public synchronized void writeToConfigXmlFile(String content) {
        FileChannel channel = null;
        FileOutputStream outputStream = null;
        FileLock lock = null;

        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(fileLocation(), "rw");
            channel = randomAccessFile.getChannel();
            lock = channel.lock();
            randomAccessFile.seek(0);
            randomAccessFile.setLength(0);
            outputStream = new FileOutputStream(randomAccessFile.getFD());

            IOUtils.write(content, outputStream, UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null && lock != null) {
                try {
                    lock.release();
                    channel.close();
                    IOUtils.closeQuietly(outputStream);
                } catch (IOException e) {
                    LOGGER.error("Error occured when releasing file lock and closing file.", e);
                }
            }
        }
    }

    public File fileLocation() {
        return new File(systemEnvironment.getCruiseConfigFile());
    }
}
