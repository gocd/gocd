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
package com.thoughtworks.go.util.command;


import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class HgUrlArgumentTest {
    @Test
    void shouldMaskThePasswordInDisplayName() {
        HgUrlArgument hgUrlArgument = new HgUrlArgument("http://user:pwd@url##branch");
        assertThat(hgUrlArgument.forDisplay(), is("http://user:******@url##branch"));
    }

    @Test
    void shouldReturnAURLWithoutPassword() {
        assertThat(new HgUrlArgument("http://user:pwd@url##branch").defaultRemoteUrl(), is("http://user@url#branch"));
    }

    @Test
    void shouldReturnAURLWhenPasswordIsNotSpecified() {
        assertThat(new HgUrlArgument("http://user@url##branch").defaultRemoteUrl(), is("http://user@url#branch"));
    }

    @Test
    void shouldReturnTheURLWhenNoCredentialsAreSpecified() {
        assertThat(new HgUrlArgument("http://url##branch").defaultRemoteUrl(), is("http://url#branch"));
    }

    @Test
    void shouldReturnUrlWithoutPasswordWhenUrlIncludesPort() {
        assertThat(new HgUrlArgument("http://user:pwd@domain:9887/path").defaultRemoteUrl(), is("http://user@domain:9887/path"));
    }

    @Test
    void shouldNotModifyAbsoluteFilePaths() {
        assertThat(new HgUrlArgument("/tmp/foo").defaultRemoteUrl(), is("/tmp/foo"));
    }

    @Test
    void shouldNotModifyFileURIS() {
        assertThat(new HgUrlArgument("file://junk").defaultRemoteUrl(), is("file://junk"));
    }

    @Test
    void shouldNotModifyWindowsFileSystemPath() {
        assertThat(new HgUrlArgument("c:\\foobar").defaultRemoteUrl(), is("c:\\foobar"));
    }

}
