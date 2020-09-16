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
package com.thoughtworks.gocd;


import java.util.Arrays;
import java.util.TreeSet;

public class AssertJava {
    private static TreeSet<JavaVersion> SUPPORTED_VERSIONS = new TreeSet<>(Arrays.asList(
            JavaVersion.VERSION_11,
            JavaVersion.VERSION_12,
            JavaVersion.VERSION_13,
            JavaVersion.VERSION_14
    ));

    public static void assertVMVersion() {
        checkSupported(JavaVersion.current());
    }

    private static void checkSupported(JavaVersion currentJavaVersion) {
        if (!SUPPORTED_VERSIONS.contains(currentJavaVersion)) {
            System.err.println("Running GoCD requires Java version >= " + SUPPORTED_VERSIONS.first().name() +
                    " and <= " + SUPPORTED_VERSIONS.last() +
                    ". You are currently running with Java version " + currentJavaVersion + ". GoCD will now exit!");
            System.exit(1);
        }
    }
}
