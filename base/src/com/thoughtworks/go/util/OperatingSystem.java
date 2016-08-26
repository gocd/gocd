/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.util;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.Scanner;

import static com.thoughtworks.go.util.StringUtil.isBlank;
import static com.thoughtworks.go.util.StringUtil.unQuote;

public class OperatingSystem {
    private static final String OS_FAMILY_NAME = System.getProperty("os.name");
    public static final String WINDOWS = "Windows";
    private static String OS_COMPLETE_NAME = detectCompleteName();

    private static String detectCompleteName() {
        String[] command = {"python", "-c", "import platform;print(platform.linux_distribution())"};
        try {
            Process process = Runtime.getRuntime().exec(command);
            Scanner scanner = new Scanner(process.getInputStream());
            String line = scanner.nextLine();
            OS_COMPLETE_NAME = cleanUpPythonOutput(line);
        } catch (Exception e) {
            try {
                OS_COMPLETE_NAME = readFromOsRelease();
            } catch (Exception ignored) {
                OS_COMPLETE_NAME = OS_FAMILY_NAME;
            }
        }
        return OS_COMPLETE_NAME;
    }

    private static String readFromOsRelease() throws Exception {
        try (FileReader fileReader = new FileReader(new File("/etc/os-release"))) {
            Properties properties = new Properties();
            properties.load(fileReader);
            return unQuote(properties.getProperty("PRETTY_NAME"));
        }
    }

    private static String cleanUpPythonOutput(String str) {
        String output = str.replaceAll("[()',]+", "");
        if (isBlank(output)) {
            throw new RuntimeException("The linux distribution string is empty");
        }
        return output;
    }

    public static String getCompleteName() {
        return OS_COMPLETE_NAME;
    }

    public static String getFamilyName() {
        if (OS_FAMILY_NAME.startsWith(WINDOWS))
            return WINDOWS;
        return OS_FAMILY_NAME;
    }

    public static boolean isFamily(String familyName) {
        return getFamilyName().equals(familyName);
    }
}
