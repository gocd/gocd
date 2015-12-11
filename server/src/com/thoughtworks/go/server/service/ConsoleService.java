/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.ConsoleOut;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.LocatableEntity;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;

@Component
public class ConsoleService {

    public static final Logger LOGGER = Logger.getLogger(ConsoleService.class);
    private ArtifactDirectoryChooser chooser;
    public static final int DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE = 1024;
    private ArtifactsDirHolder artifactsDirHolder;


    public ConsoleService(ArtifactDirectoryChooser chooser) {
        this.chooser = chooser;
    }

    @Autowired
    public ConsoleService(ArtifactsDirHolder artifactsDirHolder) {
        this(new ArtifactDirectoryChooser());
        this.artifactsDirHolder = artifactsDirHolder;
    }

    public void initialize() {
        chooser.add(new PathBasedArtifactsLocator(artifactsDirHolder.getArtifactsDir()));
        chooser.add(new BuildIdArtifactLocator(artifactsDirHolder.getArtifactsDir()));
    }

    ConsoleOut getConsoleOut(int startingLine, InputStream inputStream) throws IOException {
        int lineNumber = 0;

        StringBuffer buffer = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String consoleLine;
            while (null != (consoleLine = reader.readLine())) {
                if (lineNumber >= startingLine) {
                    buffer.append(consoleLine);
                    buffer.append(FileUtil.lineSeparator());
                }
                lineNumber++;
            }
        } catch (FileNotFoundException ex) {
            String message = "Could not read console out: " + ex.getMessage();
            LOGGER.error(message);
            LOGGER.trace(message, ex);
        } finally {
            inputStream.close();
        }
        return new ConsoleOut(buffer.toString(), startingLine, lineNumber);
    }

    public ConsoleOut getConsoleOut(JobIdentifier identifier, int startingLine) throws IOException, IllegalArtifactLocationException {
        return getConsoleOut(startingLine, new FileInputStream(findConsoleArtifact(identifier)));
    }

    public File findConsoleArtifact(JobIdentifier identifier) throws IllegalArtifactLocationException {
        File file = chooser.temporaryConsoleFile(identifier);
        if (!file.exists()) {
            file = chooser.findArtifact(identifier, getConsoleOutputFolderAndFileName());
        }
        return file;
    }


    public File consoleLogFile(JobIdentifier jobIdentifier) throws IllegalArtifactLocationException {
        File file = chooser.temporaryConsoleFile(jobIdentifier);
        if (file.exists()) {
            return file;
        }
        File finalConsole = chooser.findArtifact(jobIdentifier, getConsoleOutputFolderAndFileName());
        if (finalConsole.exists()) return finalConsole;
        return file;
    }

    public void appendToConsoleLog(JobIdentifier jobIdentifier, String text) throws IllegalArtifactLocationException, IOException {
        File file = findConsoleArtifact(jobIdentifier);
        updateConsoleLog(file, new ByteArrayInputStream(text.getBytes()), LineListener.NO_OP_LINE_LISTENER);
    }

    public boolean updateConsoleLog(File dest, InputStream in, LineListener lineListener) throws IOException {
        File parentFile = dest.getParentFile();
        parentFile.mkdirs();

        LOGGER.trace("Updating console log [" + dest.getAbsolutePath() + "]");

        char[] data = new char[DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE];
        char[] overflow = new char[DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE];

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(dest, dest.exists()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            int hasRead, overflowIndex, offset = 0, end;
            while ((hasRead = reader.read(data, offset, data.length - offset)) != -1) {
                end = offset + hasRead;
                overflowIndex = end;
                for (int i = end; i > 0; i--) {
                    int index = i - 1;
                    char c = data[index];
                    if ('\n' == c) {
                        break;
                    }
                    overflow[index] = data[index];
                    overflowIndex = index;
                }
                if (overflowIndex == 0) {
                    if (end == data.length) {//realloc if line is bigger than our buffer
                        data = realloc(data);
                        overflow = realloc(overflow);
                        offset = end;
                        continue;
                    } else {
                        overflowIndex = end;
                        offset = 0;
                    }
                }
                lineListener.copyLine(new CharArraySequence(data, 0, overflowIndex));
                writer.write(data, 0, overflowIndex);
                //place overflow back in data
                for (int i = overflowIndex; i < end; i++) {
                    data[i - overflowIndex] = overflow[i];
                }
                offset = end - overflowIndex;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to update console log at : [" + dest.getAbsolutePath() + "]", e);
            return false;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Console log [" + dest.getAbsolutePath() + "] saved.");
        }
        return true;
    }

    public void moveConsoleArtifacts(LocatableEntity locatableEntity) {
        try {
            File from = chooser.temporaryConsoleFile(locatableEntity);

            // Job cancellation skips temporary file creation. Force create one if it does not exist.
            FileUtils.touch(from);

            File to = chooser.findArtifact(locatableEntity, getConsoleOutputFolderAndFileName());
            FileUtils.moveFile(from, to);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface LineListener {
        LineListener NO_OP_LINE_LISTENER = new LineListener() {
            public void copyLine(CharSequence line) {

            }
        };

        void copyLine(CharSequence line);
    }

    static class CharArraySequence implements CharSequence {
        private final char[] chars;
        private final int start;
        private final int end;

        CharArraySequence(char[] chars, int start, int end) {
            this.chars = chars;
            this.start = start;
            this.end = end;
        }

        public int length() {
            return end;
        }

        public char charAt(int index) {
            return chars[start + index];
        }

        public CharSequence subSequence(int start, int end) {
            return new CharArraySequence(chars, start, end);
        }

        @Override
        public String toString() {
            return new String(chars, start, end - start);
        }
    }

    private char[] realloc(char[] old) {
        char[] newAlloc = new char[old.length * 2];
        for (int i = 0; i < old.length; i++) {
            newAlloc[i] = old[i];
        }
        return newAlloc;
    }

}
