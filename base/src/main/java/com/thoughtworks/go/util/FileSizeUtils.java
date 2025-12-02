/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util;

public class FileSizeUtils {

    public static String byteCountToDisplaySize(long bytes) {
        if (bytes < 1024) {
            return bytes + (bytes > 1 ? " bytes" : " byte");
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        double value = bytes / Math.pow(1024, exp);
        char unit = "KMGTPEZY".charAt(exp - 1);
        return String.format("%.1f %s%s", value, unit, "B");
    }
}
