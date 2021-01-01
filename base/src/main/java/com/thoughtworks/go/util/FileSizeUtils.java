/*
 * Copyright 2021 ThoughtWorks, Inc.
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

    public static String byteCountToDisplaySize(long size) {
        if (size < 1024L) {
            return String.valueOf(size) + (size > 1 ? " bytes" : " byte");
        }
        long exp = (long) (Math.log(size) / Math.log((long) 1024));
        double value = size / Math.pow((long) 1024, exp);
        char unit = "KMGTPEZY".charAt((int) exp - 1);
        return String.format("%.1f %s%s", value, unit, "B");
    }
}
