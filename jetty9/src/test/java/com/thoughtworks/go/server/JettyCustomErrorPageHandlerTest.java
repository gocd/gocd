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
package com.thoughtworks.go.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JettyCustomErrorPageHandlerTest {

    JettyCustomErrorPageHandler errorHandler;
    HttpServletRequest request;
    PrintWriter writer;
    private ArgumentCaptor<String> captor;

    @BeforeEach
    public void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        writer = mock(PrintWriter.class);
        errorHandler = new JettyCustomErrorPageHandler();
        captor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    public void shouldWriteErrorPageFor404WithMessage() throws Exception {
        errorHandler.writeErrorPage(request, writer, 404, null, false);

        verify(writer).write(captor.capture());
        String fileContents = captor.getValue();

        assertThat(fileContents, containsString("<h1>404</h1>"));
        assertThat(fileContents, containsString("<h2>Not Found</h2>"));
    }

    @Test
    public void shouldNotUseErrorMessageFromResponse() throws Exception {
        errorHandler.writeErrorPage(request, writer, 500, "this message should not be rendered", false);

        verify(writer).write(captor.capture());
        String fileContents = captor.getValue();

        assertThat(fileContents, not(containsString("this message should not be rendered")));
    }
}
