/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.builder;

import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.util.command.CommandLine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommandBuilderScriptRunnerTest extends AbstractCommandBuilderScriptRunnerTest {

    @Nested
    @EnabledOnOs(OS.WINDOWS)
    class Windows {
        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES_QUOTED})
        void commandInSpacePathCanRunArgsWithSpacesAndQuotes(String attribArg) throws Exception {
            assertThatExecutableOutputIncludesArgs(executableWithPathSpaces(ATTRIB_WINDOWS).toString(), attribArg);
        }

        @Test
        void commandInSpacePathCanRunMultipleArgsInclSpaces() throws Exception {
            List<String> paths = createTestFoldersIn();
            String[] pathsAsStrings = paths.stream().map(s -> '"' + s + '"').toArray(String[]::new);
            assertThatExecutableOutputIncludesArgs(executableWithPathSpaces(TAR_WINDOWS).toString(), new String[] { "-cvf", "test.tar"}, pathsAsStrings);
        }

        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES_QUOTED})
        void commandInNormalPathCanRunArgsWithSpacesAndQuotes(String attribArg) throws Exception {
            assertThatExecutableOutputIncludesArgs(ATTRIB_WINDOWS.toString(), attribArg);
        }

        @Test
        void commandInNormalPathCanRunMultipleArgsInclSpaces() throws Exception {
            List<String> paths = createTestFoldersIn();
            String[] pathsAsStrings = paths.stream().map(s -> '"' + s + '"').toArray(String[]::new);
            assertThatExecutableOutputIncludesArgs(TAR_WINDOWS.toString(), new String[] { "-cvf", "test.tar"}, pathsAsStrings);
        }
    }

    @Override
    CommandLine commandFor(String executableLocation, String... executableArgs) {
        return new CommandBuilder(executableLocation, String.join(" ", executableArgs), tempWorkDir.toFile(), RunIfConfigs.CONFIGS, null, "go")
            .buildCommandLine()
            .withEncoding(StandardCharsets.UTF_8);
    }

}
