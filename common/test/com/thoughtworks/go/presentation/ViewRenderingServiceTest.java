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

package com.thoughtworks.go.presentation;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.Renderer;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ViewRenderingServiceTest {

    private ViewRenderingService viewRenderingService;

    @Before
    public void setUp() {
        viewRenderingService = new ViewRenderingService();
    }

    @Test
    public void shouldRegisterAFreemarkerRendererByDefault() throws Exception {
        AntTask task = new AntTask();
        Map<String, Object> localContext = new HashMap<String, Object>();
        localContext.put("task", task);

        String view = viewRenderingService.render(new TaskViewModel(task, this.getClass().getResource("test.ftl").toString(), Renderer.FREEMARKER), localContext);

        assertThat(view, is("hello world mates. This is my ant"));
    }

    @Test
    public void shouldNotRegisterOverAnExistingRenderer() throws Exception {
        Renderer orig = mock(Renderer.class);
        Renderer newRen = mock(Renderer.class);
        viewRenderingService.registerRenderer(Renderer.ERB, orig);
        try {
            viewRenderingService.registerRenderer(Renderer.ERB, newRen);
            fail("Should not be able to register a new erb renderer since there is already one registered");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is(String.format("Renderer '%s' for type '%s' is already registered. Cannot register '%s' as the renderer", orig, Renderer.ERB, newRen)));
        }
    }

    @Test
    public void shouldMergeTheParametersFromViewModelWithThePassedInContext() throws Exception {
        Renderer renderer = mock(Renderer.class);

        viewRenderingService.registerRenderer(Renderer.ERB, renderer);

        PluggableViewModel viewModel = mock(PluggableViewModel.class);
        when(viewModel.getRenderingFramework()).thenReturn(Renderer.ERB);
        Map factoryCtx = m("clashing_name", "inside_value", "task", "my-task");
        when(viewModel.getParameters()).thenReturn(factoryCtx);

        Map<String, Object> localCtx = new HashMap<String, Object>();
        localCtx.put("clashing_name", "outside_value");
        localCtx.put("param", "one");
        viewRenderingService.render(viewModel, localCtx);

        Map<String, Object> expected = new HashMap<String, Object>();
        expected.putAll(localCtx);
        expected.put("task", "my-task");
        expected.put(PluggableViewModel.FACTORY_CONTEXT_KEY, factoryCtx);
        expected.put(PluggableViewModel.LOCAL_CONTEXT_KEY, localCtx);

        verify(renderer).render(viewModel, expected);
    }

    @Test
    public void shouldRenderUsingTheRightRenderer() throws Exception {
        Map<String,Object> ctx = new HashMap<String, Object>();
        PluggableViewModel viewModel = mock(PluggableViewModel.class);
        when(viewModel.getRenderingFramework()).thenReturn("foo");

        Renderer erbRenderer = mock(Renderer.class);
        when(erbRenderer.render(eq(viewModel), any(Map.class))).thenReturn("hello world mates");

        viewRenderingService.registerRenderer("foo", erbRenderer);

        String view = viewRenderingService.render(viewModel, ctx);
        assertThat(view, is("hello world mates"));
    }
}
