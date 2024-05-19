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

import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.ExecScript;
import com.thoughtworks.go.util.command.InMemoryConsumer;
import org.apache.commons.lang3.ArrayUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractCommandBuilderScriptRunnerTest {

    static final Path ATTRIB_WINDOWS = Path.of("C:\\Windows\\system32\\attrib.exe");
    static final Path TAR_WINDOWS = Path.of("C:\\Windows\\system32\\tar.exe");


    static final String DIR_WINDOWS = "C:\\Windows";
    static final String DIR_WINDOWS_QUOTED = "\"C:\\Windows\"";
    static final String DIR_PROGRAM_FILES = "C:\\Program Files (x86)";
    static final String DIR_PROGRAM_FILES_QUOTED = "\"C:\\Program Files (x86)\"";


    @TempDir
    Path tempWorkDir;

    public List<String> createTestFoldersIn() throws IOException  {
        List<String> paths = List.of(
            "dir1", // Regular dir without spaces
            "dir 2", // Regular dir with space
            "\"dir3\""); // Regular dir without spaces pre-quoted
        for (Path dir : paths.stream().map(p -> tempWorkDir.resolve(StringUtil.unQuote(p))).toList()) {
            Files.createDirectory(dir);
        }
        return paths;
    }

    protected void assertThatExecutableOutputIncludesArgs(String executableLocation, String... executableArgs) {
        assertThatExecutableOutputIncludesArgs(executableLocation, new String[0], executableArgs);
    }

    /**
     * Executes the passed executable with the passed args, validating that the executable can be found invoked on the
     * filesystem, and that the args were interpreted correctly, by assuming the executable will print the raw args
     * somewhere in its output.
     * <p/>
     * Will likely only work correctly with certain executables or shell built-ins, such as `attrib`, `ls`, `dir`, `tar`
     * etc that print their args in the output and can validate that the args were interpreted successfully.
     */
    protected void assertThatExecutableOutputIncludesArgs(String executableLocation, String[] executableFlags, String... executableArgs) {
        ExecScript script = new ExecScript("");
        InMemoryConsumer output = new InMemoryConsumer();
        commandFor(executableLocation, ArrayUtils.addAll(executableFlags, executableArgs))
            .runScript(script, output, new EnvironmentVariableContext(), null);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(output.toString())
                .contains(Arrays.stream(executableArgs)
                    .map(CommandBuilderWithArgsListScriptRunnerTest::trimWrappingQuotesAndWhiteSpace)
                    .collect(Collectors.toList()))
                .doesNotContainIgnoringCase("not found")
                .doesNotContainIgnoringCase("no such file");
            softly.assertThat(script.foundError()).isFalse();
            softly.assertThat(script.getExitCode()).withFailMessage(() -> "Non-zero exit code indicates failure: " + output).isZero();
        });
    }

    abstract CommandLine commandFor(String executableLocation, String... executableArgs);


    Path executableWithPathSpaces(Path sourceExecutablePath) throws IOException {
        Path executable = Files.createDirectory(tempWorkDir.resolve("Directory With Spaces")).resolve(sourceExecutablePath.getFileName());
        Files.copy(sourceExecutablePath, executable);
        return executable;
    }

    static String trimWrappingQuotesAndWhiteSpace(String arg) {
        return arg.trim().replaceAll("(^\"*)|(\"*$)", "");
    }
}
