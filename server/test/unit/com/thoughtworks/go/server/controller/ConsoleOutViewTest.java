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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.ConsoleConsumer;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class ConsoleOutViewTest {

    @Test
    public void setsUpContentEncoding() throws Exception {
        ConsoleOutView view = new ConsoleOutView(mock(ConsoleConsumer.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(null, null, response);
        assertThat(response.getCharacterEncoding(), is("UTF-8"));
    }

    @Test
    public void setsUpCharset() throws Exception {
        ConsoleOutView view = new ConsoleOutView(mock(ConsoleConsumer.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(null, null, response);
        assertThat(response.getContentType(), is(view.getContentType()));
        assertThat(view.getContentType(), is(GoConstants.RESPONSE_CHARSET));
    }
}