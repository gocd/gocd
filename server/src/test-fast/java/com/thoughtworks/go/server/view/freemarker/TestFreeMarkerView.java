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

import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.web.GoCDFreeMarkerView;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

class TestFreeMarkerView extends GoCDFreeMarkerView {

    private static final String REQUEST_CONTEXT_ATTRIBUTE = "req";

    public TestFreeMarkerView(String template) throws Exception {
        FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
        configurer.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configurer.setTemplateLoaderPath("file:src/main/webapp/WEB-INF/vm");

        Properties settings = new Properties();
        settings.put("recognizeStandardFileExtensions", "true");
        configurer.setFreemarkerSettings(settings);

        setConfiguration(configurer.createConfiguration());
        setServletContext(new MockServletContext());
        setRequestContextAttribute(REQUEST_CONTEXT_ATTRIBUTE);
        setUrl(template);
    }

    public String render(Map<String, Object> modelData) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setExposeSpringMacroHelpers(false);
            SessionUtilsHelper.loginAsAnonymous(request);
            modelData.put(REQUEST_CONTEXT_ATTRIBUTE, request);
            renderMergedOutputModel(modelData, request, response);
            return response.getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
