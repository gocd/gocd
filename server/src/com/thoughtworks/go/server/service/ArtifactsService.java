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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.legacywrapper.LogParser;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.server.view.artifacts.ArtifactDirectoryChooser;
import com.thoughtworks.go.server.view.artifacts.BuildIdArtifactLocator;
import com.thoughtworks.go.server.view.artifacts.PathBasedArtifactsLocator;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.ArtifactLogUtil.getConsoleOutputFolderAndFileName;
import static java.lang.String.format;

@Service
public class ArtifactsService implements ArtifactUrlReader {
    public static final int DEFAULT_CONSOLE_LOG_LINE_BUFFER_SIZE = 1024;
    private final ArtifactsDirHolder artifactsDirHolder;
    private final ZipUtil zipUtil;
    private final JobResolverService jobResolverService;
    private final StageDao stageDao;
    private SystemService systemService;
    @Autowired
    private LogParser logParser;
    public static final Logger LOGGER = Logger.getLogger(ArtifactsService.class);
    public static final String LOG_XML_NAME = "log.xml";
    private ArtifactDirectoryChooser chooser;

    @Autowired
    public ArtifactsService(JobResolverService jobResolverService, StageDao stageDao,
                            ArtifactsDirHolder artifactsDirHolder, ZipUtil zipUtil, SystemService systemService) {
        this.artifactsDirHolder = artifactsDirHolder;
        this.zipUtil = zipUtil;
        this.jobResolverService = jobResolverService;
        this.stageDao = stageDao;
        this.systemService = systemService;

        //This is a Chain of Responsibility to decide which view should be shown for a particular artifact URL
        this.chooser = new ArtifactDirectoryChooser();

    }

    public void initialize() {
        chooser.add(new PathBasedArtifactsLocator(artifactsDirHolder.getArtifactsDir()));
        chooser.add(new BuildIdArtifactLocator(artifactsDirHolder.getArtifactsDir()));
    }

    public boolean saveFile(File dest, InputStream stream, boolean shouldUnzip, int attempt) {
        String destPath = dest.getAbsolutePath();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Saving file [" + destPath + "]");
            }
            if (shouldUnzip) {
                zipUtil.unzip(new ZipInputStream(stream), dest);
            } else {
                systemService.streamToFile(stream, dest);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File [" + destPath + "] saved.");
            }
            return true;
        } catch (IOException e) {
            final String message = format("Failed to save the file to: [%s]", destPath);
            if (attempt < GoConstants.PUBLISH_MAX_RETRIES) {
                LOGGER.warn(message, e);
            } else {
                LOGGER.error(message, e);
            }
            return false;
        }
    }

    public boolean saveOrAppendFile(File dest, InputStream stream) {
        String destPath = dest.getAbsolutePath();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Appending file [" + destPath + "]");
            }
            systemService.streamToFile(stream, dest);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("File [" + destPath + "] appended.");
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save the file to : [" + destPath + "]", e);
            return false;
        }
    }

    public LogFile getInstanceLogFile(JobIdentifier jobIdentifier) throws IllegalArtifactLocationException {
        File outputFolder = findArtifact(jobIdentifier, ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        return new LogFile(new File(outputFolder, LOG_XML_NAME));
    }

    public Map parseLogFile(LogFile logFile, boolean buildPassed) throws ArtifactsParseException {
        try {
            Map properties;
            File cacheFile = serializedPropertiesFile(logFile.getFile());
            if (cacheFile.exists()) {
                properties = (Map) ObjectUtil.readObject(cacheFile);
            } else if (logFile.getFile().exists()) {
                properties = logParser.parseLogFile(logFile, buildPassed);
                ObjectUtil.writeObject(properties, cacheFile);
            } else {
                properties = new HashMap();
            }
            return properties;
        } catch (Exception e) {
            LOGGER.error("Error parsing log file: ", e);
            String filePath = logFile == null ? "null log file" : logFile.getFile().getPath();
            throw new ArtifactsParseException("Error parsing log file: " + filePath, e);
        }

    }

    public File serializedPropertiesFile(File logFile) {
        return new File(logFile.getParent(), "." + logFile.getName() + ".ser");
    }

    public void moveConsoleArtifacts(LocatableEntity locatableEntity) {
        try {
            File from = chooser.temporaryConsoleFile(locatableEntity);
            File to = chooser.findArtifact(locatableEntity, getConsoleOutputFolderAndFileName());
            FileUtils.moveFile(from, to);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalArtifactLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public File temporaryConsoleFile(JobIdentifier jobIdentifier) {
        return chooser.temporaryConsoleFile(jobIdentifier);
    }

    public static interface LineListener {
        LineListener NO_OP_LINE_LISTENER = new LineListener() {
            public void copyLine(CharSequence line) {

            }
        };

        void copyLine(CharSequence line);
    }

    private char[] realloc(char[] old) {
        char[] newAlloc = new char[old.length * 2];
        for (int i = 0; i < old.length; i++) {
            newAlloc[i] = old[i];
        }
        return newAlloc;
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

    public File findArtifact(JobIdentifier identifier, String path) throws IllegalArtifactLocationException {
        return chooser.findArtifact(identifier, path);
    }

    public String findArtifactRoot(JobIdentifier identifier) throws IllegalArtifactLocationException {
        JobIdentifier id = jobResolverService.actualJobIdentifier(identifier);
        try {
            String fullArtifactPath = chooser.findArtifact(id, "").getCanonicalPath();
            String artifactRoot = artifactsDirHolder.getArtifactsDir().getCanonicalPath();
            String relativePath = fullArtifactPath.replace(artifactRoot, "");
            if (relativePath.startsWith(File.separator)) {
                relativePath = relativePath.replaceFirst("\\" + File.separator, "");
            }
            return relativePath;
        } catch (IOException e) {
            throw new IllegalArtifactLocationException("No artifact found.", e);
        }
    }

    public String findArtifactUrl(JobIdentifier jobIdentifier) {
        JobIdentifier actualId = jobResolverService.actualJobIdentifier(jobIdentifier);
        return format("/files/%s", actualId.buildLocator());
    }

    public String findArtifactUrl(JobIdentifier jobIdentifier, String path) {
        return format("%s/%s", findArtifactUrl(jobIdentifier), path);
    }

    public File getArtifactLocation(String path) throws IllegalArtifactLocationException {
        try {
            File file = new File(artifactsDirHolder.getArtifactsDir(), path);
            if (!FileUtil.isSubdirectoryOf(artifactsDirHolder.getArtifactsDir(), file)) {
                throw new IllegalArtifactLocationException("Illegal artifact path " + path);
            }
            return file;
        } catch (Exception e) {
            throw new IllegalArtifactLocationException("Illegal artifact path " + path);
        }
    }

    public void purgeArtifactsForStage(Stage stage) {
        StageIdentifier stageIdentifier = stage.getIdentifier();
        try {
            File stageRoot = chooser.findArtifact(stageIdentifier, "");
            File cachedStageRoot = chooser.findCachedArtifact(stageIdentifier);
            deleteFile(cachedStageRoot);
            boolean didDelete = deleteArtifactsExceptCruiseOutput(stageRoot);

            if (!didDelete) {
                LOGGER.error(format("Artifacts for stage '%s' at path '%s' was not deleted", stageIdentifier.entityLocator(), stageRoot.getAbsolutePath()));
            }
        } catch (Exception e) {
            LOGGER.error(format("Error occurred while clearing artifacts for '%s'. Error: '%s'", stageIdentifier.entityLocator(), e.getMessage()), e);
        }
        stageDao.markArtifactsDeletedFor(stage);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("Marked stage '%s' as artifacts deleted.", stageIdentifier.entityLocator()));
        }
    }

    private boolean deleteArtifactsExceptCruiseOutput(File stageRoot) throws IOException {
        File[] jobs = stageRoot.listFiles();
        if (jobs == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + stageRoot);
        }

        boolean didDelete = true;

        for (File jobRoot : jobs) {
            File[] artifacts = jobRoot.listFiles();
            if (artifacts == null) {  // null if security restricted
                throw new IOException("Failed to list contents of " + stageRoot);
            }
            for (File artifact : artifacts) {
                if (artifact.isDirectory() && artifact.getName().equals(ArtifactLogUtil.CRUISE_OUTPUT_FOLDER)) {
                    continue;
                }
                didDelete &= deleteFile(artifact);
            }
        }
        return didDelete;
    }

    private boolean deleteFile(File file) {
        return FileUtils.deleteQuietly(file);
    }

    public void appendToConsoleLog(JobIdentifier jobIdentifier, String text) throws IllegalArtifactLocationException, IOException {
        File file = findArtifact(jobIdentifier, ArtifactLogUtil.getConsoleLogOutputFolderAndFileName());
        updateConsoleLog(file, new ByteArrayInputStream(text.getBytes()), LineListener.NO_OP_LINE_LISTENER);
    }

}
