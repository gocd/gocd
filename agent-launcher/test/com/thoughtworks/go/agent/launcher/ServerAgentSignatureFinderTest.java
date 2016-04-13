/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.launcher;

import com.thoughtworks.go.agent.common.UrlConstructor;
import com.thoughtworks.go.util.SslVerificationMode;
import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.fail;

public class ServerAgentSignatureFinderTest {
    @Test
    public void shouldThrowExceptionIfUrlCannotBeContacted() throws MalformedURLException {
        final UrlConstructor urlConstructor = new UrlConstructor("https://www.uiookbkweruoyiuygfihkjhqkwehkq.com:8154/go");
        ServerBinaryDownloader signatureFinder = new ServerBinaryDownloader(urlConstructor, DownloadableFile.AGENT, null, SslVerificationMode.NONE);
        try {
            signatureFinder.headers();
            fail("Shouldn't work if server doesn't exist");
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
