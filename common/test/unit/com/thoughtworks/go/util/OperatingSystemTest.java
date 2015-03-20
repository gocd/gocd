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

package com.thoughtworks.go.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.util.OperatingSystem;
import org.junit.Test;

public class OperatingSystemTest {

    @Test
    public void shouldProvideCorrentStringRepresentation() {
        assertThat(OperatingSystem.WINDOWS.toString(),is("Windows"));
        assertThat(OperatingSystem.LINUX.toString(),is("Linux"));
        assertThat(OperatingSystem.OSX.toString(),is("Mac OS X"));
        assertThat(OperatingSystem.SUN_OS.toString(),is("SunOS"));
        assertThat(OperatingSystem.UNKNOWN.toString(),is("Unknown"));
    }

    @Test
    public void shouldSortByAlphabeticalOrderOfNames() {
        List<OperatingSystem> operatingSystemArrayList = Arrays.asList(OperatingSystem.WINDOWS, OperatingSystem.UNKNOWN, OperatingSystem.LINUX, OperatingSystem.OSX, OperatingSystem.SUN_OS);
        Collections.sort(operatingSystemArrayList);
        assertThat(operatingSystemArrayList.get(0), is(OperatingSystem.LINUX));
        assertThat(operatingSystemArrayList.get(1), is(OperatingSystem.OSX));
        assertThat(operatingSystemArrayList.get(2), is(OperatingSystem.SUN_OS));
        assertThat(operatingSystemArrayList.get(3), is(OperatingSystem.WINDOWS));
        assertThat(operatingSystemArrayList.get(4), is(OperatingSystem.UNKNOWN));

    }
}
