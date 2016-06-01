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

package com.thoughtworks.go.util.command;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.ExceptionUtils;

import static com.thoughtworks.go.util.ListUtil.join;

public class ConsoleResult {
    private int returnValue;
    private List<String> output;
    private List<String> error;
    private final List<CommandArgument> arguments;
    private final List<SecretString> secrets;
    private boolean failOnNonZeroReturn;

    public ConsoleResult(int returnValue, List<String> output, List<String> error, List<CommandArgument> arguments, List<SecretString> secrets) {
        this(returnValue, output, error, arguments, secrets, true);
    }

    public ConsoleResult(int returnValue, List<String> output, List<String> error, List<CommandArgument> arguments, List<SecretString> secrets, boolean failOnNonZeroReturn) {
        this.returnValue = returnValue;
        this.output = output;
        this.error = error;
        this.arguments = arguments;
        this.secrets = secrets;
        this.failOnNonZeroReturn = failOnNonZeroReturn;
    }

    public List<String> output() {
        return output;
    }

    public List<String> error() {
        return error;
    }

    public String replaceSecretInfo(String line) {
        if (line == null) {
            return null;
        }
        for (CommandArgument argument : arguments) {
            line = argument.replaceSecretInfo(line);
        }
        for (SecretString secret : secrets) {
            line = secret.replaceSecretInfo(line);
        }
        return line;
    }

    public List<String> outputForDisplay() {
        return forDisplay(output);
    }

    public int returnValue() {
        return returnValue;
    }

    public String outputAsString() {
        return join(output(), "\n");
    }

    public String outputForDisplayAsString() {
        return join(outputForDisplay(), "\n");
    }

    public String errorAsString() {
        return join(error(), "\n");
    }

    public String errorForDisplayAsString() {
        return join(forDisplay(error), "\n");
    }

    private ArrayList<String> forDisplay(List<String> from) {
        ArrayList<String> forDisplay = new ArrayList<>();
        for (String line : from) {
            forDisplay.add(replaceSecretInfo(line));
        }
        return forDisplay;
    }

    public boolean failed() {
        // Some git commands return non-zero return value for a "successfull" command (e.g. git config --get-regexp)
        // In such a scenario, we can't simply rely on return value to tell whether a command is successful or not
        return failOnNonZeroReturn ? returnValue() != 0 : false;
    }

    public String describe() {
        return "--OUTPUT ---\n" + outputForDisplayAsString() + "\n"
                + "--- ERROR ---\n" + errorForDisplayAsString() + "\n"
                + "---\n";
    }

    public static ConsoleResult unknownResult() {
        ArrayList<String> list = new ArrayList<>();
        list.add("Unknown result.");
        return new ConsoleResult(-1, new ArrayList<String>(), list, new ArrayList<CommandArgument>(), new ArrayList<SecretString>());
    }

    public Exception smudgedException(Exception rawException) {
        try {
            Throwable cause = rawException.getCause();
            if (cause != null) {
                smudgeException(cause);
            }
            smudgeException(rawException);
        } catch (Exception e) {
            ExceptionUtils.bomb(e);
        }
        return rawException;
    }

    private void smudgeException(Throwable rawException) throws NoSuchFieldException, IllegalAccessException {
        Field messageField = Throwable.class.getDeclaredField("detailMessage");
        messageField.setAccessible(true);
        messageField.set(rawException,replaceSecretInfo(rawException.getMessage()));
    }
}
