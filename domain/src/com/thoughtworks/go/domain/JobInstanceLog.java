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

package com.thoughtworks.go.domain;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.config.TestArtifactPlan.TEST_OUTPUT_FOLDER;

import com.thoughtworks.go.server.domain.BuildTestSuite;
import com.thoughtworks.go.server.domain.LogFile;
import com.thoughtworks.go.util.ArtifactLogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;

public class JobInstanceLog implements Serializable {
    private LogFile logFile;
    private Map props;

    public JobInstanceLog(LogFile file, Map properties) {
        this.logFile = file;
        this.props = properties;
    }

    public LogFile getLogFile() {
        return logFile;
    }

    public File getLogFolder() {
        return logFile.getFile().getParentFile();
    }

    public List getTestSuites() {
        List list = (List) props.get("testsuites");
        return list == null ? new ArrayList() : list;
    }

    public int getNumberOfTests() {
        int totalNumberOfTests = 0;

        List testSuites = getTestSuites();
        for (Object testSuite : testSuites) {
            BuildTestSuite suite = (BuildTestSuite) testSuite;
            totalNumberOfTests += suite.getNumberOfTests();
        }

        return totalNumberOfTests;
    }

    public int getNumberOfFailures() {
        int numberOfFailures = 0;

        List testSuites = getTestSuites();
        for (Object testSuite : testSuites) {
            BuildTestSuite suite = (BuildTestSuite) testSuite;
            numberOfFailures += suite.getNumberOfFailures();
        }

        return numberOfFailures;
    }

    public int getNumberOfErrors() {
        int numberOfErrors = 0;

        List testSuites = getTestSuites();
        for (Object testSuite : testSuites) {
            BuildTestSuite suite = (BuildTestSuite) testSuite;
            numberOfErrors += suite.getNumberOfErrors();
        }

        return numberOfErrors;
    }

    public File getArtifactFolder() {
        return (File) props.get("artifactfolder");
    }

    public List<File> getTestOutputs() {
        List files = new ArrayList();
        File testOutput = new File(getArtifactFolder(), TEST_OUTPUT_FOLDER);
        if (testOutput.exists()) {
            files.add(testOutput);
        }
        return files;
    }

    public String getBuildError() {
        Object obj = props.get("stacktrace.error");
        if (obj == null) {
            return "";
        }
        return StringUtils.defaultString(obj.toString());
    }

    public String stacktrace() {
        String stacktrace = (String) props.get("stacktrace.stacktrace");
        return StringUtils.defaultString(stacktrace);
    }



    public File getServerFailurePage() {
        File folder = new File(getArtifactFolder(), ArtifactLogUtil.CRUISE_OUTPUT_FOLDER);
        if (folder.exists()) {
            File serverFailurePage = new File(folder, ArtifactLogUtil.SERVER_FAILURE_PAGE);
            if (serverFailurePage.exists()) {
                return serverFailurePage;
            }
        }
        return null;
    }

    
    public File getTestIndexPage() {
        File folder = new File(getArtifactFolder(), TEST_OUTPUT_FOLDER);
        if (folder.exists()) {
            Collection collection = FileUtils.listFiles(folder, new IndexHtmlFilter(), new DirFilter());
            if (!collection.isEmpty()) {
                return (File) collection.iterator().next();
            }
        }
        return null;
    }

    class IndexHtmlFilter implements IOFileFilter {
        public boolean accept(File file) {
            if (file == null || !file.exists()) {
                return false;
            }
            return accept(file.getParentFile(), file.getName());

        }

        public boolean accept(File dir, String name) {
            return TestReportGenerator.TEST_RESULTS_FILE.equals(name);
        }
    }

    class DirFilter implements IOFileFilter {
        public boolean accept(File file) {
            return true;
        }

        public boolean accept(File dir, String name) {
            return true;
        }
    }
}
