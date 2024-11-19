/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ResultTest {
    @Test
    public void shouldCreateResponseWithErrorMessages() {
        Result result = new Result().withErrorMessages("Error 1", "Error 2");
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.getMessages()).contains("Error 1", "Error 2");
    }

    @Test
    public void shouldDefaultResponseAsSuccess() {
        Result result = new Result();
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldResponseWithSuccessMessages() {
        Result result = new Result().withSuccessMessages("Success", "Pass");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getMessages()).contains("Success","Pass");
    }

    @Test
    public void shouldReturnMessagesForDisplay() {
        Result result = new Result().withSuccessMessages("Success", "Pass", "Firstpass");
        String messagesForDisplay = result.getMessagesForDisplay();
        assertThat(messagesForDisplay).isEqualTo("Success\nPass\nFirstpass");
    }

    @Test
    public void shouldReturnMessagesForDisplayWithEmptyMessages() {
        Result result = new Result().withSuccessMessages();
        String messagesForDisplay = result.getMessagesForDisplay();
        assertThat(messagesForDisplay).isEqualTo("");
    }
}
