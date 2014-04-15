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

package com.thoughtworks.go.agent.launcher;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UrlConstructorTest {
    @Test
    public void shouldGenerateCorrectUrlForGivenPath() {
        UrlConstructor urlConstructor = new UrlConstructor("localhost", 8080);
        assertThat(urlConstructor.serverUrlFor("foo/bar"), is("http://localhost:8080/go/foo/bar"));
        assertThat(urlConstructor.serverUrlFor("admin/agent"), is("http://localhost:8080/go/admin/agent"));
    }

    @Test
    public void shouldGenerateCorrectServerBaseUrl() {
        UrlConstructor urlConstructor = new UrlConstructor("localhost", 8080);
        assertThat(urlConstructor.serverSslBaseUrl(8443), is("https://localhost:8443/go/"));
    }
}
