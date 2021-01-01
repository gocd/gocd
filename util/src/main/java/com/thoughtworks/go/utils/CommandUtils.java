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
package com.thoughtworks.go.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.UnixLineEndingInputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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
            return captureOutput(process);
        } catch (Exception e) {
            throw bomb(e);
        }
    }

    private static String captureOutput(Process process) throws IOException, InterruptedException {
        StringWriter result = new StringWriter();
        result.append("output:\n");
        dump(result, process.getInputStream());
        result.append("error:\n");
        dump(result, process.getErrorStream());
        process.waitFor();
        return result.toString();
    }

    private static void dump(StringWriter result, InputStream inputStream) throws IOException {
        try (UnixLineEndingInputStream unixLineEndingInputStream = new UnixLineEndingInputStream(inputStream, true)) {
            IOUtils.copy(unixLineEndingInputStream, result, StandardCharsets.UTF_8);
        }
    }

    /**
     * Surrounds a string with double quotes if it is not already surrounded by single or double quotes, or if it contains
     * unescaped spaces, single quotes, or double quotes. When surrounding with double quotes, this method will only escape
     * double quotes in the String.
     * <p>
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
