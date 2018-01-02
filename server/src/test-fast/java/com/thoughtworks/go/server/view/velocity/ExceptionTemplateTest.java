/* **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.view.velocity;

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class ExceptionTemplateTest {
    public static final String TEMPLATE_PATH = "/WEB-INF/vm/exceptions_page.vm";

    @Test
    public void shouldHaveErrorMessageAlongWithCustomMessageInOutputOfTemplate() throws Exception {
        HashMap<String, Object> data = new HashMap<>();
        data.put("errorMessage", "ERROR MESSAGE");

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);
        view.setupAdditionalFakeTemplate("shared/_header.vm", "header_template_content");
        view.setupAdditionalFakeTemplate("shared/_flash_message.vm", "flash_message_template_content");

        String output = view.render();
        assertThat(output, containsString("$('trans_content').update(\"Sorry, an unexpected error occurred [ERROR MESSAGE]. :( Please check the server logs for more information.\");"));
    }

    @Test
    public void shouldHaveTheGenericMessageInOutputOfTemplateWhenCustomErrorMessageIsNotProvided() throws Exception {
        HashMap<String, Object> data = new HashMap<>();

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);
        view.setupAdditionalFakeTemplate("shared/_header.vm", "header_template_content");
        view.setupAdditionalFakeTemplate("shared/_flash_message.vm", "flash_message_template_content");

        String output = view.render();
        assertThat(output, containsString("$('trans_content').update(\"Sorry, an unexpected error occurred. :( Please check the server logs for more information.\");"));
    }
}