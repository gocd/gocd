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

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HgUrlArgumentTest {
     @Test
     public void shouldMaskThePasswordInDisplayName(){
         HgUrlArgument hgUrlArgument = new HgUrlArgument("http://user:pwd@url##branch");
         assertThat(hgUrlArgument.forDisplay(), is("http://user:******@url##branch"));
     }

    @Test
    public void shouldReturnAURLWithoutPassword(){
        assertThat(new HgUrlArgument("http://user:pwd@url##branch").defaultRemoteUrl(), is("http://user@url#branch"));
    }

    @Test
    public void shouldReturnAURLWhenPasswordIsNotSpecified() throws Exception {
        assertThat(new HgUrlArgument("http://user@url##branch").defaultRemoteUrl(), is("http://user@url#branch"));
    }

    @Test
    public void shouldReturnTheURLWhenNoCredentialsAreSpecified() throws Exception {
        assertThat(new HgUrlArgument("http://url##branch").defaultRemoteUrl(), is("http://url#branch"));
    }

    @Test
    public void shouldReturnUrlWithoutPasswordWhenUrlIncludesPort() throws Exception {
        assertThat(new HgUrlArgument("http://user:pwd@domain:9887/path").defaultRemoteUrl(), is("http://user@domain:9887/path"));
    }

    @Test
    public void shouldNotModifyAbsoluteFilePaths() throws Exception {
        assertThat(new HgUrlArgument("/tmp/foo").defaultRemoteUrl(), is("/tmp/foo"));
    }

    @Test
    public void shouldNotModifyFileURIS() throws Exception {
        assertThat(new HgUrlArgument("file://junk").defaultRemoteUrl(), is("file://junk"));
    }
    @Test
    public void shouldNotModifyWindowsFileSystemPath() throws Exception {
        assertThat(new HgUrlArgument("c:\\foobar").defaultRemoteUrl(), is("c:\\foobar"));
    }

}
