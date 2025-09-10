/*
 * Copyright Thoughtworks, Inc.
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

package com.thoughtworks.go.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class TestRepoTest {
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFile() {
        assertThat(TestRepo.toFileURI(new File("/var/lib/foo/"))).isEqualTo("file:///var/lib/foo");
        assertThat(TestRepo.toFileURI(new File("/var/a dir with spaces/foo"))).isEqualTo("file:///var/a%20dir%20with%20spaces/foo");
        assertThat(TestRepo.toFileURI(new File("/var/司徒空在此/foo"))).isEqualTo("file:///var/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4/foo");
    }


    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFileOnWindows() {
        assertThat(TestRepo.toFileURI(new File("c:\\foo")).startsWith("file:///c:/foo")).isTrue();
        assertThat(TestRepo.toFileURI(new File("c:\\a dir with spaces\\foo")).startsWith("file:///c:/a%20dir%20with%20spaces/foo")).isTrue();
    }
}