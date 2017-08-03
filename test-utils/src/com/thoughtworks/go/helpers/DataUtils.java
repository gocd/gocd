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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;

public final class DataUtils {

    public static final String TEST_DATA_DIR = "test/data/";
    public static final String CONFIG_XML = "cruise-config.xml";
    public static final String FAILING_LOG = "cruisecontrollog_internalerror.log";
    public static final String FAILING_BUILD_XML = "log20051209122104.xml";
    public static final String PASSING_BUILD_LBUILD_0_XML = "log20051209122103Lbuild.489.xml";
    public static final String LOGFILE_OF_PROJECT2 = "log20060703155722.xml";

    private DataUtils() {
    }

    public static File getConfigXmlAsFile() {
        return getData(CONFIG_XML);
    }

    public static File getConfigXmlOfWebApp() {
        return getFileOfWebApp(CONFIG_XML);
    }

    public static File getDashboardConfigXmlOfWebApp() throws Exception {
        return getFileOfWebApp(CONFIG_XML);
    }

    private static File getFileOfWebApp(String file) {
        File ccroot = new File(FileSystemUtils.getTestRootDir(), "tmpCCRoot");
        File data = new File(ccroot, "data");
        return new File(data, file);
    }

    public static void cloneCCHome() throws Exception {
        File ccRoot = getConfigXmlAsFile().getParentFile();
        File tmpCCRoot = FileSystemUtils.createDirectory("tmpCCRoot");
        FileUtils.copyDirectoryToDirectory(ccRoot, tmpCCRoot);
    }


    public static File getLogRootOfWebapp() {
        return getSubFolderOfWebApp("logs");
    }

    private static File getSubFolderOfWebApp(String subFolder) {
        File ccroot = new File(FileSystemUtils.getTestRootDir(), "tmpCCRoot");
        File data = new File(ccroot, "data");
        return new File(data, subFolder);
    }

    public static LogFile getPassingBuildLbuildAsFile() throws Exception {
        return getLogFile("logs/project1/" + PASSING_BUILD_LBUILD_0_XML);
    }

    public static LogFile getFailedBuildLbuildAsFile() throws Exception {
        return getLogFile("logs/project1/" + FAILING_BUILD_XML);
    }

    public static File getPassingHtmlAsFile() {
        return getLogFile("logs/project1/" + "passed-index.html").getFile();
    }

    public static File getFailedHtmlAsFile() {
        return getLogFile("logs/project1/" + "failed-index.html").getFile();
    }

    public static LogFile getZippedBuildAsFile() throws Exception {
        return getLogFile("logs/project1/log20061209122103Lbuild.489.xml.gz");
    }

    public static File getLogDirAsFile() {
        return getData("logs");
    }

    public static File getProjectLogDirAsFile(String project) throws Exception {
        return new File(getLogDirAsFile(), project);
    }

    public static File getProject2BuildAsFile() throws Exception {
        return getLogFile("logs/project2/" + LOGFILE_OF_PROJECT2).getFile();
    }

    private static File getData(String filename) {
        return new File(TEST_DATA_DIR + filename);
    }

    public static LogFile getLogFile(String filename) {
        return new LogFile(new File(TEST_DATA_DIR + filename));
    }

    public static File createDefaultCCConfigFile() throws IOException {
        File configurationFile = TestFileUtil.createTempFile("config.xml");
        FileUtil.writeContentToFile("<cruisecontrol><project name=\"project1\"/></cruisecontrol>\n", configurationFile);
        return configurationFile;
    }

    public static String readFileContent(File file) throws Exception {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();
    }

    public static File getDashboardConfig() throws Exception {
        return getData(CONFIG_XML);
    }
}
