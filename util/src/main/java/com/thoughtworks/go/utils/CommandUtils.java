/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class CommandUtils {

    private static final Pattern QUOTED_STRING = Pattern.compile("^(['\"]).+(\\1)$");
    private static final Pattern UNESCAPED_SPACE_OR_QUOTES = Pattern.compile("(?<!\\\\)(?:\\\\{2})*[ '\"]");
    private static final Pattern DOUBLE_QUOTE = Pattern.compile("(\")");

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
     * Surrounds a string with double quotes if it is not already surrounded by single or double quotes, or if it contains
     * unescaped spaces, single quotes, or double quotes. When surrounding with double quotes, this method will only escape
     * double quotes in the String.
     *
     * This method assumes the argument is well-formed if it was already surrounded by either single or double quotes.
     *
     * @param argument String to quote
     * @return the quoted String, if not already quoted
     */
    public static String quoteArgument(String argument) {
        if (QUOTED_STRING.matcher(argument).matches() || !UNESCAPED_SPACE_OR_QUOTES.matcher(argument).find()) {
            // assume the argument is well-formed if it's already quoted or if there are no unescaped spaces or quotes
            return argument;
        }

        return String.format("\"%s\"", DOUBLE_QUOTE.matcher(argument).replaceAll(Matcher.quoteReplacement("\\") + "$1"));
    }

    public static String shellJoin(String... args) {
        ArrayList<String> strings = new ArrayList<>();
        for (String arg : args) {
            strings.add(quoteArgument(arg));
        }
        return StringUtils.join(strings, " ");
    }

}
