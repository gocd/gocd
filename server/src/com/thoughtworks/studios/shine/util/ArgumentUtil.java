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

package com.thoughtworks.studios.shine.util;

import java.util.Collection;

public abstract class ArgumentUtil {

    public static void guaranteeFalse(String message, boolean toCheck) {
        if (toCheck) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void guaranteeNotNull(Object toCheck, String message) {
        if (toCheck == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void guaranteeTrue(boolean toCheck, String message) {
        if (!toCheck) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void guaranteeInList(Object toCheck, Collection shouldBeIn, String argumentName) {
        if (!shouldBeIn.contains(toCheck)) {
            throw new IllegalArgumentException(argumentName + " '" + toCheck + "' is not in the list of possible values (" + delimitList(shouldBeIn, ", ") + ")");
        }
    }

    private static String delimitList(Collection list, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Object obj : list) {
            if (!first) {
                sb.append(delimiter);
            } else {
                first = false;
            }

            sb.append("'");
            sb.append(obj != null ? obj.toString() : "null");
            sb.append("'");
        }

        return sb.toString();
    }
}
