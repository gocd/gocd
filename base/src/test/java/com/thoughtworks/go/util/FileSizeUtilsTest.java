/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class FileSizeUtilsTest {

    @Test
    public void shouldConvertBytes() {
        assertThat(FileSizeUtils.byteCountToDisplaySize((long) 1023), is("1023 bytes"));
    }

    @Test
    public void shouldConvertBytesToKilo() {
        assertThat(FileSizeUtils.byteCountToDisplaySize((long) (1024 + 512)), is("1.5 KB"));
    }

    @Test
    public void shouldOnlyKeep() {
        assertThat(FileSizeUtils.byteCountToDisplaySize((long) (1024 + 512 + 256)), is("1.8 KB"));
    }

    @Test
    public void shouldConvertBytesToMega() {
        assertThat(FileSizeUtils.byteCountToDisplaySize((long) (1024 * 1024)), is("1.0 MB"));
    }

    @Test
    public void shouldConvertBytesToMegaForFloat() {
        assertThat(FileSizeUtils.byteCountToDisplaySize((long) (1 * 1024 * 1024 + 512 * 1024)), is("1.5 MB"));
    }

    @Test
    public void shouldConvertBytesToGiga() {
        long twoGiga = 2L * 1024 * 1024 * 1024 + 512 * 1024 * 1024;
        assertThat(FileSizeUtils.byteCountToDisplaySize(twoGiga), is("2.5 GB"));
    }

    @Test
    public void shouldConvertBytesToTB() {
        long twoGiga = 2L * 1024 * 1024 * 1024 * 1024 + 512L * 1024 * 1024 * 1024;
        assertThat(FileSizeUtils.byteCountToDisplaySize(twoGiga), is("2.5 TB"));
    }

    @Test
    public void shouldConvertBytesToPB() {
        long twoGiga = 2L * 1024 * 1024 * 1024 * 1024 * 1024 + 512L * 1024 * 1024 * 1024 * 1024;
        assertThat(FileSizeUtils.byteCountToDisplaySize(twoGiga), is("2.5 PB"));
    }
}
