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
package com.thoughtworks.go.server;

import java.io.IOException;
import java.net.ServerSocket;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import com.thoughtworks.go.util.validators.Validation;
import com.thoughtworks.go.util.validators.ServerPortValidator;

public class GoServerValidatorTest {
    private ServerPortValidator serverPortValidator;
    private ServerSocket socket;
    private static final int PORT_TO_TRY = 8991;

    @Test
    public void shouldValidatorServerBeforeStart() throws IOException, InterruptedException {
        try {
            startServerOnPort(PORT_TO_TRY);
            serverPortValidator = new ServerPortValidator(PORT_TO_TRY);
            Validation validation = serverPortValidator.validate(new Validation());
            assertThat(validation.isSuccessful(), is(false));

        } finally {
            shutDownServer();
        }
    }

    @Test
    public void shouldNotFailIfPortIsAvailable() throws IOException, InterruptedException {
        serverPortValidator = new ServerPortValidator(PORT_TO_TRY);
        Validation validation = serverPortValidator.validate(new Validation());
        assertThat(validation.isSuccessful(), is(true));
    }

    private void shutDownServer() throws IOException {
        socket.close();
    }

    private void startServerOnPort(int port) throws IOException {
        socket = new ServerSocket(port);
    }

}
