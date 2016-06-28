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

package com.thoughtworks.go.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.server.domain.LogFile;

public class NullJobInstanceLog extends JobInstanceLog {
    public NullJobInstanceLog() {
        super(null, new HashMap());
    }

    public NullJobInstanceLog(LogFile file, Map properties) {
        super(file, properties);
    }

    public LogFile getLogFile() {
        return null;
    }

    public File getLogFolder() {
        return null;
    }

    public List getTestSuites() {
        return new ArrayList();
    }

    public int getNumberOfTests() {
        return 0;
    }

    public int getNumberOfFailures() {
        return 0;
    }

    public int getNumberOfErrors() {
        return 0;
    }

    public File getArtifactFolder() {
        return null;
    }

    public List<File> getTestOutputs() {
        return new ArrayList<>();
    }

    public String getBuildError() {
        return "";
    }

    public String stacktrace() {
        return "";
    }

    public File getTestIndexPage() {
        return null;
    }
}
