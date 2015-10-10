/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web.i18n;

import java.util.Map;

import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import org.springframework.web.servlet.ModelAndView;

public class JavascriptMessagesInterceptorTest extends RequestContextTestCase {

    @Test
    public void shouldContainMessagesInModel() throws Exception {
        ModelAndView modelAndView = new ModelAndView();
        JavascriptMessagesInterceptor interceptor = new JavascriptMessagesInterceptor();
        interceptor.postHandle(request, null, null, modelAndView);
        Map<String, Object> real = modelAndView.getModel();
        assertThat(real, hasKey(JavascriptMessagesInterceptor.JAVASCRIPT_MESSAGES_KEY));
        String messages = (String) modelAndView.getModel().get(JavascriptMessagesInterceptor.JAVASCRIPT_MESSAGES_KEY);
        assertThat(messages, containsString(CurrentStatus.BUILDING.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentStatus.DISCONTINUED.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentStatus.QUEUED.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentStatus.WAITING.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentStatus.PAUSED.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentResult.PASSED.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentResult.FAILED.getStatus().toLowerCase()));
        assertThat(messages, containsString(CurrentResult.UNKNOWN.getStatus().toLowerCase()));
        assertThat(messages, containsString("last"));
        mockContext.assertIsSatisfied();
    }
}