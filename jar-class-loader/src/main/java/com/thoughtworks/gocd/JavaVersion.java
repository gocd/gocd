/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Copied from gradle's source and unused/misleading bits removed.
// https://github.com/gradle/gradle/blob/master/platforms/core-runtime/stdlib-java-extensions/src/main/java/org/gradle/api/JavaVersion.java
package com.thoughtworks.gocd;

import java.util.ArrayList;
import java.util.List;

/**
 * An enumeration of Java versions.
 * Before 9: https://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html
 * 9+: https://openjdk.java.net/jeps/223
 */
public enum JavaVersion {
    VERSION_1_1, VERSION_1_2, VERSION_1_3, VERSION_1_4,
    VERSION_1_5, VERSION_1_6, VERSION_1_7, VERSION_1_8,
    VERSION_1_9, VERSION_1_10,
    VERSION_11,
    VERSION_12,
    VERSION_13,
    VERSION_14,
    VERSION_15,
    VERSION_16,
    VERSION_17,
    VERSION_18,
    VERSION_19,
    VERSION_20,
    VERSION_21,
    VERSION_22,
    VERSION_23,
    VERSION_24,
    VERSION_25,
    VERSION_26,
    VERSION_27,
    VERSION_28,
    VERSION_29,
    VERSION_HIGHER;
    // Since Java 9, version should be X instead of 1.X
    private static final int FIRST_MAJOR_VERSION_ORDINAL = 9 - 1;
    private static JavaVersion currentJavaVersion;
    private final String versionName;

    JavaVersion() {
        this.versionName = ordinal() >= FIRST_MAJOR_VERSION_ORDINAL ? getMajorVersion() : "1." + getMajorVersion();
    }

    /**
     * Converts the given object into a {@code JavaVersion}.
     *
     * @param value An object whose toString() value is to be converted. May be null.
     * @return The version, or null if the provided value is null.
     * @throws IllegalArgumentException when the provided value cannot be converted.
     */
    public static JavaVersion toVersion(Object value) throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        if (value instanceof JavaVersion) {
            return (JavaVersion) value;
        }
        if (value instanceof Integer) {
            return getVersionForMajor((Integer) value);
        }

        String name = value.toString();
        return getVersionForMajor(JavaVersionParser.parseMajorVersion(name));
    }

    /**
     * Returns the version of the current JVM.
     *
     * @return The version of the current JVM.
     */
    public static JavaVersion current() {
        JavaVersion version = currentJavaVersion;
        if (version == null) {
            currentJavaVersion = version = toVersion(System.getProperty("java.version"));
        }
        return version;
    }

    /**
     * Returns if this version is compatible with the given version
     */
    public boolean isCompatibleWith(JavaVersion otherVersion) {
        return this.compareTo(otherVersion) >= 0;
    }

    @Override
    public String toString() {
        return versionName;
    }

    public String getMajorVersion() {
        return String.valueOf(ordinal() + 1);
    }

    private static JavaVersion getVersionForMajor(int major) {
        return major >= values().length ? JavaVersion.VERSION_HIGHER : values()[major - 1];
    }

    /*
     * Copyright 2024 the original author or authors.
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    /**
     * Parses the major version, as an integer, from the string returned by the {@code java.version} system property.
     */
    // Copied  from gradle's source and unused bits removed.
    // https://github.com/gradle/gradle/blob/master/platforms/core-runtime/stdlib-java-extensions/src/main/java/org/gradle/api/internal/jvm/JavaVersionParser.java
    private static class JavaVersionParser {

        public static int parseMajorVersion(String fullVersion) {
            int firstNonVersionCharIndex = findFirstNonVersionCharIndex(fullVersion);

            String[] versionStrings = fullVersion.substring(0, firstNonVersionCharIndex).split("\\.");
            List<Integer> versions = convertToNumber(fullVersion, versionStrings);

            if (isLegacyVersion(versions)) {
                assertTrue(fullVersion, versions.get(1) > 0);
                return versions.get(1);
            } else {
                return versions.get(0);
            }
        }

        private static void assertTrue(String value, boolean condition) {
            if (!condition) {
                throw new IllegalArgumentException("Could not determine Java version from '" + value + "'.");
            }
        }

        private static boolean isLegacyVersion(List<Integer> versions) {
            return 1 == versions.get(0) && versions.size() > 1;
        }

        private static List<Integer> convertToNumber(String value, String[] versionStrs) {
            List<Integer> result = new ArrayList<>();
            for (String s : versionStrs) {
                assertTrue(value, !isNumberStartingWithZero(s));
                try {
                    result.add(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    assertTrue(value, false);
                }
            }
            assertTrue(value, !result.isEmpty() && result.get(0) > 0);
            return result;
        }

        private static boolean isNumberStartingWithZero(String number) {
            return number.length() > 1 && number.startsWith("0");
        }

        private static int findFirstNonVersionCharIndex(String s) {
            assertTrue(s, !s.isEmpty());

            for (int i = 0; i < s.length(); ++i) {
                if (!isDigitOrPeriod(s.charAt(i))) {
                    assertTrue(s, i != 0);
                    return i;
                }
            }

            return s.length();
        }

        private static boolean isDigitOrPeriod(char c) {
            return (c >= '0' && c <= '9') || c == '.';
        }
    }
}
