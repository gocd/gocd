/**
 * **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END**********************************
 */

package com.thoughtworks.go.server.view.velocity;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.velocity.VelocityView;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestVelocityView extends VelocityView {
    private final String templatePath;
    private final Map<String, Object> modelData;

    public TestVelocityView(String templatePath, Map<String, Object> modelData) {
        this.templatePath = templatePath;
        this.modelData = modelData;
    }

    public String render() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            setExposeSpringMacroHelpers(false);
            renderMergedOutputModel(modelData, request, response);
            return response.getContentAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Template getTemplate() throws Exception {
        Template template = new Template();

        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.getResourceStream("template1")).thenReturn(getClass().getResourceAsStream(templatePath));

        template.setRuntimeServices(new RuntimeInstance());
        template.setName("template1");
        template.setResourceLoader(loader);
        template.process();

        return template;
    }
}
