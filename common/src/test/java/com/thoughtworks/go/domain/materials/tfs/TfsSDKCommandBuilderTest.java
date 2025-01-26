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
package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.command.UrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TfsSDKCommandBuilderTest {
    private TfsSDKCommandBuilder builder;
    private final String DOMAIN = "CORPORATE";
    private final String USERNAME = "userName";
    private final String PASSWORD = "password";
    private final String computedWorkspaceName = "boo-yaa-goo-moo-foo";
    private ClassLoader mockSdkLoader;
    private boolean invoked;
    private String className;

    @BeforeEach
    public void setup() {
        // we intentionally don't use mockito to mock this
        // see https://bugs.openjdk.java.net/browse/JDK-8202758
        mockSdkLoader = new ClassLoader() {
            @Override
            public Class<?> loadClass(String name) {
                invoked = true;
                className = name;
                throw new RuntimeException();
            }
        };
        builder = new TfsSDKCommandBuilder(mockSdkLoader);
    }

    @Test
    public void shouldLoadTheCorrectImplementationOfSDKCommandViaTheNestedClassLoader() {
        try {
            builder.buildTFSSDKCommand(null,new UrlArgument("url"), DOMAIN, USERNAME, PASSWORD, computedWorkspaceName, "$/project");
            fail("should have failed to load class as we are not wiring any dependencies");
        } catch (Exception e) {
            //Do not worry about load class failing. We're only asserting that load class is invoked with the right FQN for TFSSDKCommand
        }
        assertThat(invoked).isTrue();
        assertThat(className).isEqualTo("com.thoughtworks.go.tfssdk.TfsSDKCommandTCLAdapter");
    }
}
