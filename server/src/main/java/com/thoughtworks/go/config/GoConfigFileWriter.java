/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

public class GoConfigFileWriter {
    private SystemEnvironment systemEnvironment;
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass().getName());

    public GoConfigFileWriter(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public synchronized void writeToConfigXmlFile(String content) {
        try (
                RandomAccessFile randomAccessFile = new RandomAccessFile(systemEnvironment.getCruiseConfigFile(), "rw");
                FileChannel channel = randomAccessFile.getChannel();
                FileLock lock = channel.lock();
        ) {
            randomAccessFile.seek(0);
            randomAccessFile.setLength(0);
            final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            channel.transferFrom(Channels.newChannel(new ByteArrayInputStream(bytes)), 0, bytes.length);
        } catch (IOException e) {
            LOGGER.error("Error occured when writing config XML to file", e);
            throw new UncheckedIOException(e);
        }
    }
}
