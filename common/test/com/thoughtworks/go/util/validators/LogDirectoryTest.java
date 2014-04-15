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

package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.IOException;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.OperatingSystem;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.util.FileUtil.writeContentToFile;
import static com.thoughtworks.go.util.OperatingSystem.LINUX;
import static com.thoughtworks.go.util.OperatingSystem.OSX;
import static com.thoughtworks.go.util.OperatingSystem.SUN_OS;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

@RunWith(JunitExtRunner.class)
public class LogDirectoryTest {
    private File configFolder;
    private File existingLogFolder;
    private File log4jPropertiesFile;

    @Before
    public void setUp() throws Exception {
        configFolder = TestFileUtil.createTempFolder("config-" + System.currentTimeMillis());
        existingLogFolder = TestFileUtil.createTempFolder("log-" + System.currentTimeMillis());
        log4jPropertiesFile = new File(configFolder, "log4j.properties");
        FileUtils.writeStringToFile(log4jPropertiesFile, LOG4J_CONFIG_FILE_CONTENTS);
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.deleteFolder(configFolder);
        FileUtil.deleteFolder(existingLogFolder);
    }

    @Test
    public void shouldHaveCorrectPaths() throws Exception {
        assertThat(LogDirectory.fromEnvironment(LINUX), is(new LogDirectory("/var/log/go-server")));
        assertThat(LogDirectory.fromEnvironment(SUN_OS), is(new LogDirectory("/var/log/cruise-server")));
        assertThat(LogDirectory.fromEnvironment(OSX), is(new LogDirectory("/Library/Logs/GoServer")));
        assertThat(LogDirectory.fromEnvironment(OperatingSystem.WINDOWS), is(LogDirectory.CURRENT));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldAppendShineLog4jPropertiesToExistingFile() throws IOException {
        String dir = existingLogFolder.getAbsolutePath();
        LogDirectory logDirectory = new LogDirectory(dir);
        logDirectory.update(log4jPropertiesFile, new Validation());
        String upgradedFile = readFileToString(log4jPropertiesFile);
        assertThat(upgradedFile, containsString("log4j.logger.com.thoughtworks.studios.shine=WARN,ShineFileAppender\n"
                + "# Rolling log file output for shine...\n"
                + "log4j.appender.ShineFileAppender=org.apache.log4j.RollingFileAppender\n"
                + "log4j.appender.ShineFileAppender.File=" + dir + "/go-shine.log\n"
                + "log4j.appender.ShineFileAppender.MaxFileSize=10240KB\n"
                + "log4j.appender.ShineFileAppender.MaxBackupIndex=50\n"
                + "log4j.appender.ShineFileAppender.layout=org.apache.log4j.PatternLayout\n"
                + "log4j.appender.ShineFileAppender.layout.conversionPattern=%d{ISO8601} %5p [%t] %c{1}:%L - %m%n"));
        assertThat(upgradedFile, not(containsString("#upgrade_check")));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldAppendShineLog4jPropertiesToExistingFile_InWindows() throws IOException {//this exists just because of the case of \r\n line ending problem
        String dir = existingLogFolder.getAbsolutePath();
        LogDirectory logDirectory = new LogDirectory(dir);
        logDirectory.update(log4jPropertiesFile, new Validation());
        String upgradedFile = readFileToString(log4jPropertiesFile);
        assertThat(upgradedFile, containsString("log4j.logger.com.thoughtworks.studios.shine=WARN,ShineFileAppender\r\n"
                + "# Rolling log file output for shine...\r\n"
                + "log4j.appender.ShineFileAppender=org.apache.log4j.RollingFileAppender\r\n"
                + "log4j.appender.ShineFileAppender.File=" + dir + "/go-shine.log\r\n"
                + "log4j.appender.ShineFileAppender.MaxFileSize=10240KB\r\n"
                + "log4j.appender.ShineFileAppender.MaxBackupIndex=50\r\n"
                + "log4j.appender.ShineFileAppender.layout=org.apache.log4j.PatternLayout\r\n"
                + "log4j.appender.ShineFileAppender.layout.conversionPattern=%d{ISO8601} %5p [%t] %c{1}:%L - %m%n"));
        assertThat(upgradedFile, not(containsString("#upgrade_check")));
    }

    @Test
    public void shouldNotAppendShineLog4jPropertiesToExistingFileIfOneAlreadyExists() throws IOException {
        writeContentToFile(readFileToString(log4jPropertiesFile) + "\nlog4j.logger.com.thoughtworks.studios.shine=INFO,FooFileAppender\n", log4jPropertiesFile);
        LogDirectory logDirectory = new LogDirectory(existingLogFolder.getAbsolutePath());
        logDirectory.update(log4jPropertiesFile, new Validation());
        String upgradedFile = readFileToString(log4jPropertiesFile);
        assertThat(upgradedFile, containsString("log4j.logger.com.thoughtworks.studios.shine=INFO,FooFileAppender\n"));
        assertThat(upgradedFile, not(containsString("log4j.logger.com.thoughtworks.studios.shine=WARN,ShineFileAppender\n")));
        assertThat(upgradedFile, not(containsString("#upgrade_check")));
    }

    @Test
    public void shouldAppendRailsWarnToExistingFile() throws IOException {
        LogDirectory logDirectory = new LogDirectory(existingLogFolder.getAbsolutePath());
        logDirectory.update(log4jPropertiesFile, new Validation());
        String upgradedFile = readFileToString(log4jPropertiesFile);
        assertThat(upgradedFile, containsString("log4j.logger.com.thoughtworks.go.server.Rails=WARN"));
        assertThat(upgradedFile, not(containsString("#upgrade_check")));
    }

    @Test
    public void shouldUpdateLog4jPropertiesFileWhenNewFolderExists() throws Exception {
        LogDirectory logDirectory = new LogDirectory(existingLogFolder.getAbsolutePath());
        logDirectory.update(log4jPropertiesFile, new Validation());

        assertThat(readFileToString(log4jPropertiesFile), containsString(
                "log4j.appender.FileAppender.File=" + existingLogFolder.getAbsolutePath() + "/go-server.log")
        );
    }

    @Test
    public void shouldLeaveLog4jPropertiesFileAloneWhenFolderDoesNotExist() throws Exception {
        File notExists = new File(existingLogFolder.getAbsolutePath() + "-not-exists");
        LogDirectory logDirectory = new LogDirectory(notExists.getAbsolutePath());
        logDirectory.update(log4jPropertiesFile, new Validation());

        assertThat(readFileToString(log4jPropertiesFile), containsString(
                "log4j.appender.FileAppender.File=go-server.log")
        );
    }

    private static final String LOG4J_CONFIG_FILE_CONTENTS =
              "log4j.rootLogger=INFO, FileAppender\n"
            + "log4j.logger.org.apache.velocity=ERROR, FileAppender\n"
            + "\n"
            + "log4j.appender.FileAppender=org.apache.log4j.RollingFileAppender\n"
            + "log4j.appender.FileAppender.File=go-server.log\n"
            + "log4j.appender.FileAppender.MaxFileSize=10240KB\n"
            + "log4j.appender.FileAppender.MaxBackupIndex=50\n"
            + "log4j.appender.FileAppender.layout=org.apache.log4j.PatternLayout\n"
            + "log4j.appender.FileAppender.layout.conversionPattern=%d{ISO8601} %5p [%t] %c{1}:%L - %m%n\n"
            + "\n"
            + "log4j.appender.PerfAppender=org.apache.log4j.RollingFileAppender\n"
            + "log4j.appender.PerfAppender.File=go-perf.log\n"
            + "log4j.appender.PerfAppender.MaxFileSize=10240KB\n"
            + "log4j.appender.PerfAppender.MaxBackupIndex=20\n"
            + "log4j.appender.PerfAppender.layout=org.apache.log4j.PatternLayout\n"
            + "log4j.appender.PerfAppender.layout.conversionPattern=%d{ISO8601} %5p [%t] %c{1}:%L - %m%n\n"
            + "\n"
            + "log4j.appender.AgentServAppender=org.apache.log4j.RollingFileAppender\n"
            + "log4j.appender.AgentServAppender.File=cruise-agentserv.log\n"
            + "log4j.appender.AgentServAppender.MaxFileSize=10240KB\n"
            + "log4j.appender.AgentServAppender.MaxBackupIndex=20\n"
            + "log4j.appender.AgentServAppender.layout=org.apache.log4j.PatternLayout\n"
            + "log4j.appender.AgentServAppender.layout.conversionPattern=%d{ISO8601} %5p [%t] %c{1}:%L - %m%n";
}