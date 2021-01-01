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
package com.thoughtworks.go.plugin.api.response;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

public class ResultTest {
    @Test
    public void shouldCreateResponseWithErrorMessages() throws Exception {
        Result result = new Result().withErrorMessages("Error 1", "Error 2");
        MatcherAssert.assertThat(result.isSuccessful(), is(false));
        MatcherAssert.assertThat(result.getMessages(), contains("Error 1", "Error 2"));
    }

    @Test
    public void shouldDefaultResponseAsSuccess() throws Exception {
        Result result = new Result();
        MatcherAssert.assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldResponseWithSuccessMessages() throws Exception {
        Result result = new Result().withSuccessMessages("Success", "Pass");
        MatcherAssert.assertThat(result.isSuccessful(), is(true));
        MatcherAssert.assertThat(result.getMessages(), contains("Success","Pass"));
    }

    @Test
    public void shouldReturnMessagesForDisplay() throws Exception {
        Result result = new Result().withSuccessMessages("Success", "Pass", "Firstpass");
        String messagesForDisplay = result.getMessagesForDisplay();
        MatcherAssert.assertThat(messagesForDisplay, is("Success\nPass\nFirstpass"));
    }

    @Test
    public void shouldReturnMessagesForDisplayWithEmptyMessages() throws Exception {
        Result result = new Result().withSuccessMessages();
        String messagesForDisplay = result.getMessagesForDisplay();
        MatcherAssert.assertThat(messagesForDisplay, is(""));
    }
}
