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
package com.thoughtworks.go.junitext;

import com.googlecode.junit.ext.checkers.Checker;
import com.googlecode.junit.ext.checkers.OSChecker;

public class EnhancedOSChecker implements Checker {
    public static final String MAC = "mac";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "win";
    public static final String DO_NOT_RUN_ON = "false";

    private String targetOS;
    private boolean shouldMatch;

    public EnhancedOSChecker(String[] conditions) {
        if (conditions.length != 2 || !(conditions[0].equals("true") || conditions[0].equals("false"))) {
            throw new RuntimeException("Expected 2 arguments: arg1 = OS type, arg2 = 'true' or 'false'. Can be used as: @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.DO_NOT_RUN_ON, EnhancedOSChecker.WINDOWS})");
        }
        this.shouldMatch = Boolean.parseBoolean(conditions[0]);
        this.targetOS = conditions[1];
    }

    public EnhancedOSChecker(String targetOS) {
        this.shouldMatch = true;
        this.targetOS = targetOS;
    }

    @Override
    public boolean satisfy() {
        return new OSChecker(targetOS).satisfy() == shouldMatch;
    }
}
