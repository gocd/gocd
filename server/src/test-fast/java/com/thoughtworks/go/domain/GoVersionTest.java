/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.domain.exception.VersionFormatException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GoVersionTest {
    @Test
    public void shouldCreateAGOVersionInstanceFromStringVersion() {
        GoVersion goVersion = new GoVersion("15.1.0-2321");

        assertThat(goVersion.getMajor(), is(15));
        assertThat(goVersion.getMinor(), is(1));
        assertThat(goVersion.getPatches(), is(0));
        assertThat(goVersion.getModifications(), is(2321));
    }

    @Test
    public void shouldCreateAGOVersionFromServerVersionString(){
        GoVersion goVersion = new GoVersion("14.3.0(2689-aefdfc53acf7aa93cb31bc79a843b9ec0042358e)");

        assertThat(goVersion.getMajor(), is(14));
        assertThat(goVersion.getMinor(), is(3));
        assertThat(goVersion.getPatches(), is(0));
        assertThat(goVersion.getModifications(), is(2689));
    }

    @Test
    public void shouldCreateAGOVersionFromNewStyleServerVersionString(){
        GoVersion goVersion = new GoVersion("16.5.0 (2689-762ce7739db26e8dc7db45ae12c5acbe5c494a57)");

        assertThat(goVersion.getMajor(), is(16));
        assertThat(goVersion.getMinor(), is(5));
        assertThat(goVersion.getPatches(), is(0));
        assertThat(goVersion.getModifications(), is(2689));
    }

    @Test(expected = VersionFormatException.class)
    public void shouldErrorOutIfStringVersionIsNotInCorrectFormat(){
        new GoVersion("12.abc.1");
    }

    @Test
    public void shouldBeAbleToCompareVersions(){
        assertThat(new GoVersion("15.1.0-123").compareTo(new GoVersion("15.1.0-123")), is(0));
        assertThat(new GoVersion("15.1.0-123").compareTo(new GoVersion("15.2.0-100")), is(-1));
        assertThat(new GoVersion("15.2.0-1").compareTo(new GoVersion("15.1.0-123")), is(1));
    }

    @Test
    public void shouldBeAbleToCheckForEquality(){
        assertThat(new GoVersion("15.1.0-123").equals(new GoVersion("15.1.0-123")), is(true));
        assertThat(new GoVersion("15.4.1-123").equals(new GoVersion("15.1.0-123")), is(false));
    }

    @Test
    public void shouldHaveAStringRepresentation(){
        GoVersion goVersion = new GoVersion("15.1.0-2321");

        assertThat(goVersion.toString(), is("15.1.0-2321"));
    }

    @Test
    public void shouldCheckIfGreaterThan(){
        assertTrue(new GoVersion("15.2.0-2321").isGreaterThan(new GoVersion("15.1.0-2321")));
        assertFalse(new GoVersion("15.2.0-2321").isGreaterThan(new GoVersion("15.2.0-2321")));
        assertFalse(new GoVersion("15.1.0-2321").isGreaterThan(new GoVersion("15.2.0-2321")));
    }
}
