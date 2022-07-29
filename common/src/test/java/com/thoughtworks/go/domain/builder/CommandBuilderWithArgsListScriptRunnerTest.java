/*
 * Copyright 2022 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommandBuilderWithArgsListScriptRunnerTest extends AbstractCommandBuilderScriptRunnerTest {

    @Nested
    @EnabledOnOs(OS.WINDOWS)
    class WindowsCorrectedQuotingStrategy {
        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES, DIR_PROGRAM_FILES_QUOTED, "", " "})
        void commandInSpacePathCanRunArgsWithSpacesAndQuotes(String attribArg) throws Exception {
            doPossiblyQuotedArgsTest(executableWithPathSpaces(ATTRIB).toString(), attribArg);
        }

        @Test
        void commandInSpacePathCanRunMultipleArgsInclSpaces() throws Exception {
            List<String> paths = createTestFoldersIn();
            doPossiblyQuotedArgsTest(executableWithPathSpaces(TAR).toString(), new String[]{"-cvf", "test.tar"}, paths.toArray(String[]::new));
        }

        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES, DIR_PROGRAM_FILES_QUOTED})
        void commandInNormalPathCanRunArgsWithSpacesAndQuotes(String attribArg) throws Exception {
            doPossiblyQuotedArgsTest(ATTRIB.toString(), attribArg);
        }

        @Test
        void commandInNormalPathCanRunMultipleArgsInclSpaces() throws Exception {
            List<String> paths = createTestFoldersIn();
            doPossiblyQuotedArgsTest(TAR.toString(), new String[]{"-cvf", "test.tar"}, paths.toArray(String[]::new));
        }
    }

    @Nested
    @EnabledOnOs(OS.WINDOWS)
    @ExtendWith(SystemStubsExtension.class)
    class WindowsLegacyQuotingStrategy {
        @SystemStub
        SystemProperties props;

        @BeforeEach
        void enableLegacyWindowsProps() {
            props.set(BaseCommandBuilder.QUOTE_ALL_WINDOWS_ARGS, "N");
        }

        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS})
        void commandInSpacePathCanRunArgsWithoutSpacesOrQuotes(String attribArg) throws Exception {
            doPossiblyQuotedArgsTest(executableWithPathSpaces(ATTRIB).toString(), attribArg);
        }

        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES, DIR_PROGRAM_FILES_QUOTED})
        void commandInSpacePathCanRunArgsWithSpacesWithWorkaround(String attribArg) throws Exception {
            doPossiblyQuotedArgsTest('"' + executableWithPathSpaces(ATTRIB).toString(), attribArg + '"');
        }

        @ParameterizedTest
        @ValueSource(strings = {DIR_WINDOWS, DIR_WINDOWS_QUOTED, DIR_PROGRAM_FILES, DIR_PROGRAM_FILES_QUOTED})
        void commandInNormalPathCanRunArgsWithSpacesAndQuotes(String attribArg) throws Exception {
            doPossiblyQuotedArgsTest(ATTRIB.toString(), attribArg);
        }

        @Test
        void commandInNormalPathCanRunMultipleArgsInclSpaces() throws Exception {
            List<String> paths = createTestFoldersIn();
            doPossiblyQuotedArgsTest(TAR.toString(), new String[]{"-cvf", "test.tar"}, paths.toArray(String[]::new));
        }
    }


    @Override
    CommandLine commandFor(String executableLocation, String... executableArgs) {
        return new CommandBuilderWithArgList(executableLocation, executableArgs, tempWorkDir.toFile(), RunIfConfigs.CONFIGS, null, "go")
            .buildCommandLine()
            .withEncoding(StandardCharsets.UTF_8);
    }

}
