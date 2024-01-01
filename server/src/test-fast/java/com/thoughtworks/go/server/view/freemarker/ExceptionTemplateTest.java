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
package com.thoughtworks.go.server.view.freemarker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionTemplateTest extends AbstractFreemarkerTemplateTest {
    public static final String TEMPLATE_PATH = "exceptions_page.ftlh";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp(TEMPLATE_PATH);
    }

    @Test
    public void shouldHaveErrorMessageAlongWithCustomMessageInOutputOfTemplate() {
        Map<String, Object> data = new HashMap<>();
        data.put("errorMessage", "ERROR MESSAGE");

        String output = view.render(data);
        assertThat(output).contains("$('#trans_content').html(\"Sorry, an unexpected error occurred [ERROR MESSAGE]. :( Please check the server logs for more information.\");");
    }

    @Test
    public void shouldHaveErrorMessageAlongWithCustomMessageEscapedCorrectly() {
        Map<String, Object> data = new HashMap<>();
        data.put("errorMessage", "<Error> you shouldn't \"expect\"");

        String output = view.render(data);
        assertThat(output).contains("$('#trans_content').html(\"Sorry, an unexpected error occurred [<Error> you shouldn\\'t \\\"expect\\\"]. :( Please check the server logs for more information.\");");
    }

    @Test
    public void shouldHaveTheGenericMessageInOutputOfTemplateWhenCustomErrorMessageIsNotProvided() {
        String output = view.render(new HashMap<>());
        assertThat(output).contains("$('#trans_content').html(\"Sorry, an unexpected error occurred. :( Please check the server logs for more information.\");");
    }
}
