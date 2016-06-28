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

public interface TestReportGenerator {
    public static final String TEST_RESULTS_FILE = "index.html";
    public static final String TOTAL_TEST_COUNT = "tests_total_count";
    public static final String FAILED_TEST_COUNT = "tests_failed_count";
    public static final String IGNORED_TEST_COUNT = "tests_ignored_count";
    public static final String TEST_TIME = "tests_total_duration";

    Properties generate(File[] allTestFiles, String uploadDestPath);
}
