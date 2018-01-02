/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.NestedJarClassLoader;
import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class TfsSDKCommandBuilderTest {
    private TfsSDKCommandBuilder builder;
    private final String DOMAIN = "CORPORATE";
    private final String USERNAME = "userName";
    private final String PASSWORD = "password";
    private final String computedWorkspaceName = "boo-yaa-goo-moo-foo";
    private NestedJarClassLoader mockSdkLoader;

    @Before
    public void setup() throws IOException, URISyntaxException {
        mockSdkLoader = mock(NestedJarClassLoader.class);
        builder = new TfsSDKCommandBuilder(mockSdkLoader);
    }

    @Test
    public void shouldLoadTheCorrectImplementationOfSDKCommandViaTheNestedClassLoader() throws Exception {
        when(mockSdkLoader.loadClass("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter")).thenThrow(new RuntimeException());
        try {
            builder.buildTFSSDKCommand(null,new UrlArgument("url"), DOMAIN, USERNAME, PASSWORD, computedWorkspaceName, "$/project");
            fail("should have failed to load class as we are not wiring any dependencies");
        } catch (Exception e) {
            //Do not worry about load class failing. We're only asserting that load class is invoked with the right FQN for TFSSDKCommand
        }
        verify(mockSdkLoader, times(1)).loadClass("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter");
    }
}
