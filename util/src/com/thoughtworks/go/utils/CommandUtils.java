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

package com.thoughtworks.go.utils;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class CommandUtils {
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public static String exec(String... commands) {
        return exec(null, commands);
    }

    public static String exec(File workingDirectory, String... commands) {
        try {
            Process process = Runtime.getRuntime().exec(commands, null, workingDirectory);
            return captureOutput(process).toString();
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    private static StringBuilder captureOutput(Process process) throws IOException, InterruptedException {
        BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder result = new StringBuilder();
        result.append("output:\n");
        dump(output, result);
        result.append("error:\n");
        dump(error, result);
        process.waitFor();
        return result;
    }

    private static StringBuilder dump(BufferedReader reader, StringBuilder builder) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line + "\n");
        }
        reader.close();
        return builder;
    }

    /**
     * Cheap, good-enough implementation of shell escape. i.e. only deals with quotes. poorly.
     * <p>
     * Put quotes around the given String if necessary.
     * <p/>
     * <p>
     * If the argument doesn't include spaces or quotes, return it as is. If it contains double quotes, use single
     * quotes - else surround the argument by double quotes.
     * </p>
     *
     * @throws ParseException if the argument contains both, single and double quotes.
     */
    public static String shellEscape(String argument) {
        if (argument.contains("\"")) {
            if (argument.contains("\'")) {
                throw new ParseException("Can't handle single and double quotes in same argument: " + argument);
            } else {
                return '\'' + argument + '\'';
            }
        } else if (argument.contains("\'") || argument.contains(" ")) {
            return '\"' + argument + '\"';
        } else {
            return argument;
        }
    }

    public static String shellJoin(String... args) {
        ArrayList<String> strings = new ArrayList<>();
        for (String arg : args) {
            strings.add(shellEscape(arg));
        }
        return StringUtils.join(strings, " ");
    }

}
