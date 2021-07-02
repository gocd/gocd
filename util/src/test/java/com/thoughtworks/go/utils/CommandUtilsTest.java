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

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandUtilsTest {
    @Test
    public void quoteArgument() throws Exception {
        String alreadyDoubleQuoted = "\"Hello there! I'm properly quoted\"";
        String alreadyDoubleQuotedWithUnclosedQuote = "\"Hello there! I've got an unmatched double quote \" inside\"";
        String noQuotes = "I have nothing";
        String apostrophe = "don't";
        String dontNeedQuoting = "ohrly?";
        String singleQuotesInside = "java is a 'fun' language";
        String doubleQuotesInside = "java is a \"fun\" language";
        String unmatchedDoubleQuotes = "java is a \"fun' language";
        String alreadyProperlyEscaped = "This\\ string\\ needs\\ \\'no\\'\\ further\\ \\\"escaping\\\"\\ at\\ all.";
        String bashScript = "if [ \"${SOME_VARIABLE}\" != 'SOME_STRING' ]; then echo \"SOME_VARIABLE doesn't look right.\"; exit 1; fi";
        String someQuotesEscapedSomeNot = "I was \\\"so\" lazy I forgot to escape the other quote";

        assertEquals(alreadyDoubleQuoted, CommandUtils.quoteArgument(alreadyDoubleQuoted));
        assertEquals(alreadyDoubleQuotedWithUnclosedQuote, CommandUtils.quoteArgument(alreadyDoubleQuotedWithUnclosedQuote));
        assertEquals(wrapQuotes(noQuotes), CommandUtils.quoteArgument(noQuotes));
        assertEquals(wrapQuotes(apostrophe), CommandUtils.quoteArgument(apostrophe));
        assertEquals(dontNeedQuoting, CommandUtils.quoteArgument(dontNeedQuoting));
        assertEquals(wrapQuotes(singleQuotesInside), CommandUtils.quoteArgument(singleQuotesInside));
        assertEquals(wrapQuotes(doubleQuotesInside.replaceAll("\"", Matcher.quoteReplacement("\\\""))), CommandUtils.quoteArgument(doubleQuotesInside));
        assertEquals(wrapQuotes(unmatchedDoubleQuotes.replaceAll("\"", Matcher.quoteReplacement("\\\""))), CommandUtils.quoteArgument(unmatchedDoubleQuotes));
        assertEquals(alreadyProperlyEscaped, CommandUtils.quoteArgument(alreadyProperlyEscaped));
        assertEquals(wrapQuotes(bashScript.replaceAll("\"", Matcher.quoteReplacement("\\\""))), CommandUtils.quoteArgument(bashScript));
        assertEquals(wrapQuotes("I was \\\\\"so\\\" lazy I forgot to escape the other quote"), CommandUtils.quoteArgument(someQuotesEscapedSomeNot),
                "Should blindly escape internal double quotes; don't try to be smart and fix " +
                        "only the unescaped quotes because that it would make it hard for users to determine why " +
                        "improperly quoted commands fail to execute");
    }

    @Test
    public void shellJoin() throws Exception {
        String[] tokens = new String[]{
                "bash",
                "-c",
                "if [ \"${SOME_VARIABLE}\" != 'SOME_STRING' ]; then echo \"SOME_VARIABLE doesn't look right.\"; exit 1; fi"
        };

        assertEquals("bash -c \"if [ \\\"${SOME_VARIABLE}\\\" != 'SOME_STRING' ]; then echo \\\"SOME_VARIABLE doesn't look right.\\\"; exit 1; fi\"", CommandUtils.shellJoin(tokens));
    }

    private String wrapQuotes(String s) {
        return String.format("\"%s\"", s);
    }
}