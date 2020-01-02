/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunMultipleInstanceJobTypeConfig implements JobTypeConfig {
    public static final String MARKER = "runInstance"; // This should ideally be owned by JobNameGenerator - Rajesh & JJ
    public static final Pattern MARKER_PATTERN = Pattern.compile("-" + MARKER + "-\\d+$");

    @Override
    public boolean isInstanceOf(String jobInstanceName, boolean ignoreCase, String jobConfigName) {
        String jobConfigNameWithMarker = translatedJobName(jobInstanceName, jobConfigName);
        return ignoreCase ? jobInstanceName.equalsIgnoreCase(jobConfigNameWithMarker) : jobInstanceName.equals(jobConfigNameWithMarker);
    }

    @Override
    public String translatedJobName(String jobInstanceName, String jobConfigName) {
        Matcher matcher = MARKER_PATTERN.matcher(jobInstanceName);
        if (matcher.find()) {
            String uuid = matcher.group();
            return jobConfigName + uuid;
        } else {
            return jobConfigName;
        }
    }

    public static boolean hasMarker(String s) {
        return RunMultipleInstanceJobTypeConfig.MARKER_PATTERN.matcher(s).find();
    }
}
