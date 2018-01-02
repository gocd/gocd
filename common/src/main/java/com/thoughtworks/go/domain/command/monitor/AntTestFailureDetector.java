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

public class AntTestFailureDetector {
    Pattern suitePattern = Pattern.compile(".*Testsuite: com.thoughtworks.go.agent.service.SslInfrastructureServiceTest");
    Pattern testResultPattern = Pattern.compile("\\[junit\\] Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Time elapsed: (\\d+.\\d+) sec");

    private boolean inSuite;

    private int count;
    private int failures;
    private int errors;
    private long totalTime;

    public AntTestFailureDetector(Reporter reporter) {
    }

    public void consumeLine(String line) {
        Matcher suite = suitePattern.matcher(line);
        if (suite.matches()) {
            inSuite = true;
        }

        if (!inSuite) return;
        
        Matcher results = testResultPattern.matcher(line);
        if (results.matches()) {
            count += Integer.parseInt(results.group(1));
            failures += Integer.parseInt(results.group(2));
            errors += Integer.parseInt(results.group(3));
            totalTime += Float.parseFloat(results.group(4)) * 1000;
            inSuite = false;
        }
    }

    public int getCount() {
        return count;
    }

    public int getFailures() {
        return failures;
    }

    public int getErrors() {
        return errors;
    }

    public long getTotalTime() {
        return totalTime;
    }
}
