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

package com.thoughtworks.go.util;

/**
 * @understands operating system type
*/
public enum OperatingSystem {
    LINUX("Linux"), OSX("Mac OS X"), SUN_OS("SunOS"), WINDOWS("Windows"), UNKNOWN("Unknown");
    private String name;

    OperatingSystem(String name) {
        this.name = name;
    }

    public static OperatingSystem fromProperty() {
        String osName = new SystemEnvironment().getPropertyImpl("os.name");
        return parseOperatingSystem(osName);
    }

    public static OperatingSystem parseOperatingSystem(String osName) {
        if (osName.equals("Linux")) {
            return LINUX;
        } else if (osName.equals("SunOS")) {
            return SUN_OS;
        } else if (osName.equals("Mac OS X")) {
            return OSX;
        } else if (osName.startsWith("Windows")) {
            return WINDOWS;
        }
        return UNKNOWN;
    }

    @Override public String toString() {
        return name;
    }

    public String osName() {
        return name;
    }
}
