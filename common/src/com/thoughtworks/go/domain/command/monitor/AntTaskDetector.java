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

package com.thoughtworks.go.domain.command.monitor;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.thoughtworks.go.util.command.StreamConsumer;

public class AntTaskDetector implements StreamConsumer {
    Pattern taskPattern = Pattern.compile("(\\w+):");

    private Reporter reporter;

    public AntTaskDetector(Reporter reporter) {
        this.reporter = reporter;
    }

    public void consumeLine(String line) {
        Matcher matcher = taskPattern.matcher(line);
        if (matcher.matches()) {
            reporter.reportStatusDetail("building [" + matcher.group(1) + "]");
        }
    }
}
