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

package com.thoughtworks.go.agent.service;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import oshi.util.GlobalConfig;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static oshi.util.GlobalConfig.OSHI_OS_LINUX_ALLOWUDEV;

@ExtendWith(SystemStubsExtension.class)
class SystemInfoTest {

    @SystemStub
    SystemProperties props;

    @Test
    public void shouldDisableUdevUsage() {
        SystemInfo.determineOperatingSystemCompleteName();
        assertThat(GlobalConfig.get(OSHI_OS_LINUX_ALLOWUDEV, true)).isFalse();
    }

    @Test
    public void shouldDefaultJnaTmpDirIfUnset() {
        SystemInfo.determineOperatingSystemCompleteName();
        assertThat(System.getProperty("jna.tmpdir")).isEqualTo(FileUtil.TMP_PARENT_DIR);
    }

    @Test
    public void shouldPreserveJnaTmpDirIfSet() {
        String defaultTempDir = System.getProperty("java.io.tmpdir");
        props.set("jna.tmpdir", defaultTempDir);
        SystemInfo.determineOperatingSystemCompleteName();
        assertThat(System.getProperty("jna.tmpdir")).isEqualTo(defaultTempDir);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    public void shouldGetCompleteNameOnMac() {
        assertThat(SystemInfo.getOperatingSystemCompleteName()).matches("macOS [0-9.]+ \\(.*\\)");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void shouldGetCompleteNameOnWindows() {
        assertThat(SystemInfo.getOperatingSystemCompleteName()).matches("Windows( \\w+)? [0-9.]+( \\(.*\\))?");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void shouldGetCompleteNameOnLinux() {
        assertThat(SystemInfo.getOperatingSystemCompleteName()).matches("[ \\w]+ [0-9.]+( \\w+)?( \\(.*\\))?");
    }

    @Test
    public void shouldFallbackToJvmFamilyOnLibraryError() {
        try (MockedStatic<SystemInfo> systemInfo = mockStatic(SystemInfo.class, Answers.CALLS_REAL_METHODS)) {
            systemInfo.when(SystemInfo::newSystemInfo).thenThrow(new IllegalArgumentException("Cannot determine OS"));

            assertThat(SystemInfo.determineOperatingSystemCompleteName()).isEqualTo(new SystemEnvironment().getOperatingSystemFamilyJvmName());
        }
    }
}
