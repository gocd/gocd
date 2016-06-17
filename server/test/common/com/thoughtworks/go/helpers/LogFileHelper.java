/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helpers;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.server.service.ArtifactsDirHolder;
import com.thoughtworks.go.server.service.ArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.helpers.LogFileHelper.FailedJunitPage.failedHtml;
import static com.thoughtworks.go.helpers.LogFileHelper.FailedJunitPage.failedLogTemplate;
import static com.thoughtworks.go.helpers.LogFileHelper.PassedJunitPage.passedHtml;
import static com.thoughtworks.go.helpers.LogFileHelper.PassedJunitPage.passedLogTemplate;
import static org.mockito.Mockito.mock;

/**
 * @understands How to set up log files for testing
 */
public final class LogFileHelper {
    private ArtifactsService artifactsService;
    private List<File> createdFiles = new ArrayList<File>();
    private LogFileHelper(File artifactsDir) throws IOException {
        this.artifactsService = artifactsDao(artifactsDir);
    }

    public static LogFileHelper createInstanceForRootOfWebapp() throws IOException {
        return new LogFileHelper(DataUtils.getLogRootOfWebapp());
    }

    public static LogFileHelper createInstanceForLocalhost() throws IOException {
        return new LogFileHelper(DataUtils.getLogRootOfWebapp());
    }

    private static ArtifactsService artifactsDao(File artifactsDir) throws IOException {
        return new ArtifactsService(null, null, new ArtifactsDirHolder(null, new FakeGoConfigService(artifactsDir)), new ZipUtil(), null);
    }

    public void onTearDown() {
        for (File createdFile : createdFiles) {
            createdFile.delete();
        }
    }

    public File createLogFileForBuildInstance(JobInstance instance) throws IllegalArtifactLocationException {
        return instance.isPassed() ?
                createLogFile(passedLogTemplate(), passedHtml(), instance.getIdentifier())
                : createLogFile(failedLogTemplate(), failedHtml(), instance.getIdentifier());
    }

    public File createConsoleLogFileForBuildInstance(JobIdentifier jobIdentifier)
            throws IllegalArtifactLocationException {
        File dir = artifactsService.findArtifact(jobIdentifier, "");
        dir.mkdirs();
        File file = new File(dir, ArtifactLogUtil.CONSOLE_LOG_FILE_NAME);
        try {
            new FileOutputStream(file).close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        createdFiles.add(file);
        file.deleteOnExit();
        return file;
    }

    private File createLogFile(InputStream logFileStream, InputStream indexhtml,
                               JobIdentifier jobIdentifier) throws IllegalArtifactLocationException {
        File logFile = artifactsService.findArtifact(jobIdentifier, "cruise-output/log.xml");
        File log = artifactsService.findArtifact(jobIdentifier, "cruise-output/" + logFile.getName());
        artifactsService.saveFile(log, logFileStream, false, 1);
        File test = artifactsService.findArtifact(jobIdentifier, "testoutput/result/index.html");
        artifactsService.saveFile(test, indexhtml, false, 1);
        createdFiles.add(logFile);
        createdFiles.add(artifactsService.serializedPropertiesFile(logFile));

        logFile.deleteOnExit();
        logFile.getParentFile().deleteOnExit();
        return logFile;
    }

    public static final class FailedJunitPage {
        private FailedJunitPage() {
        }

        static File logFile;
        static File htmlFile;

        static {
            try {
                logFile = DataUtils.getFailedBuildLbuildAsFile().getFile();
                htmlFile = DataUtils.getFailedHtmlAsFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static InputStream failedLogTemplate() {
            try {
                return new FileInputStream(logFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static String[] summaryBoxMarker(final String jobConfig) {
            return new String[]{
                    jobConfig + " - failed"
            };
        }

        public static String artifactsTabMarker() {
            return ArtifactLogUtil.CRUISE_OUTPUT_FOLDER;
        }

        public static String[] errorsAndWarningsTabMarker() {
            return new String[]{"This is my error message", "This is my stacktrace", "Failed tests"};
        }

        public static boolean isSameContent(String content) {
            return StringUtils.equals(content, printFile());
        }

        public static String printFile() {
            try {
                return IOUtils.toString(new FileInputStream(logFile));
            } catch (IOException e) {
                return null;
            }
        }

        public static InputStream failedHtml() {
            try {
                return new FileInputStream(htmlFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static final class PassedJunitPage {
        private PassedJunitPage() {
        }

        static File logFile;
        static File htmlFile;

        static {
            try {
                logFile = DataUtils.getPassingBuildLbuildAsFile().getFile();
                htmlFile = DataUtils.getPassingHtmlAsFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        static InputStream passedLogTemplate() {
            try {
                return new FileInputStream(logFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static String artifactsTabMarker() {
            return ArtifactLogUtil.CRUISE_OUTPUT_FOLDER;
        }

        public static String errorsAndWarningsTabMarker() {
            return "No failure";
        }

        public static String[] summaryBoxMarker(String builPlan) {
            return new String[]{
                    builPlan + " - passed"
            };
        }

        public static InputStream passedHtml() {
            try {
                return new FileInputStream(htmlFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class FakeGoConfigService extends GoConfigService {
        private File artifactsDir;

        public FakeGoConfigService(File artifactsDir) throws IOException {
            super(new GoConfigDao(new CachedGoConfig(new ServerHealthService(), mock(GoFileConfigDataSource.class), mock(CachedGoPartials.class))) {
                public CruiseConfig load() {
                    return null;
                }
            }, null, new SystemTimeClock(), new GoConfigMigration(mock(ConfigRepository.class), new TimeProvider(), new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()
                    ), null, null, ConfigElementImplementationRegistryMother.withNoPlugins(),
                    new InstanceFactory(), mock(CachedGoPartials.class));
            this.artifactsDir = artifactsDir;
        }

        public File artifactsDir() {
            return artifactsDir;
        }
    }
}
