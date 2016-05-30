/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.view.velocity;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.runtime.RuntimeInstance;
import org.apache.velocity.runtime.resource.ContentResource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.mockito.Matchers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.view.velocity.VelocityView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TestVelocityView extends VelocityView {
    private final String templatePath;
    private final Map<String, Object> modelData;

    private List<Template> additionalTemplates;
    private ResourceLoader loader;
    private RuntimeInstance runtimeServices;

    public TestVelocityView(String templatePath, Map<String, Object> modelData) {
        this.templatePath = templatePath;
        this.modelData = modelData;

        this.additionalTemplates = new ArrayList<Template>();
        loader = mock(ResourceLoader.class);
        runtimeServices = spy(new RuntimeInstance());

        setupToolAttributes();
    }

    public void setupAdditionalRealTemplate(String templateName) {
        setupAdditionalRealTemplate(templateName, "/WEB-INF/vm/" + templateName);
    }

    public void setupAdditionalRealTemplate(String templateName, String realPathToTemplate) {
        additionalTemplates.add(setupTemplate(loader, runtimeServices, templateName, getClass().getResourceAsStream(realPathToTemplate)));
    }

    public void setupAdditionalFakeTemplate(String templateName, String fakeContent) {
        setupContentResource(loader, runtimeServices, templateName, fakeContent);
        additionalTemplates.add(setupTemplate(loader, runtimeServices, templateName, IOUtils.toInputStream(fakeContent)));
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
        Template realTemplateForTest = setupTemplate(loader, runtimeServices, "template1", getClass().getResourceAsStream(templatePath));

        List<Template> templates = new ArrayList<Template>();
        templates.add(realTemplateForTest);
        templates.addAll(additionalTemplates);

        for (Template template : templates) {
            template.process();
        }

        return templates.get(0);
    }

    private Template setupTemplate(ResourceLoader loader, RuntimeInstance runtimeServices, String templateName, InputStream templateContents) {
        try {
            Template template = new Template();
            template.setRuntimeServices(runtimeServices);
            template.setResourceLoader(loader);
            template.setName(templateName);

            when(loader.getResourceStream(templateName)).thenReturn(templateContents);
            doReturn(template).when(runtimeServices).getTemplate(templateName);
            doReturn(template).when(runtimeServices).getTemplate(eq(templateName), Matchers.<String>any());

            return template;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupContentResource(ResourceLoader loader, RuntimeInstance runtimeServices, String templateName, String fakeContent) {
        try {
            ContentResource resource = new ContentResource();
            resource.setRuntimeServices(runtimeServices);
            resource.setResourceLoader(loader);
            resource.setName(templateName);
            resource.setData(fakeContent);

            doReturn(resource).when(runtimeServices).getContent(templateName);
            doReturn(resource).when(runtimeServices).getContent(eq(templateName), Matchers.<String>any());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* Represents "tools" setup in toolbox.vm. Part of setup of velocity. */
    private void setupToolAttributes() {
        HashMap<String, Class> toolAttributes = new HashMap<String, Class>();
        toolAttributes.put("esc", org.apache.velocity.tools.generic.EscapeTool.class);
        toolAttributes.put("util", com.thoughtworks.go.server.util.WebUtils.class);
        setToolAttributes(toolAttributes);
    }
}
