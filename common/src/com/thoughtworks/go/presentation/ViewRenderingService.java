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

import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.Renderer;
import com.thoughtworks.go.presentation.renderer.FreemarkerRenderer;
import org.springframework.stereotype.Component;

/**
 * @understands rendering a freemarker template
 */
@Component
public class ViewRenderingService implements Renderer {
    final Map<String, Renderer> renderers = new HashMap<>();

    public ViewRenderingService() {
        renderers.put(Renderer.FREEMARKER, new FreemarkerRenderer());
    }

    public void registerRenderer(final String framework, Renderer renderer) {
        if (renderers.containsKey(framework)) {
            throw new RuntimeException(String.format("Renderer '%s' for type '%s' is already registered. Cannot register '%s' as the renderer", renderers.get(framework), framework, renderer));
        }
        renderers.put(framework, renderer);
    }

    public String render(PluggableViewModel viewModel, Map<String, Object> localContext) throws Exception {
        if (!renderers.containsKey(viewModel.getRenderingFramework())) {
            throw new RuntimeException("No appropriate render found");
        }
        return renderers.get(viewModel.getRenderingFramework()).render(viewModel, mixedContext(localContext, viewModel.getParameters()));
    }

    private Map<String, Object> mixedContext(Map<String, Object> localContext, Map<String, Object> factoryContext) {
        Map<String, Object> mixedContext = new HashMap<>();
        mixedContext.putAll(factoryContext);
        mixedContext.putAll(localContext);
        mixedContext.put(PluggableViewModel.FACTORY_CONTEXT_KEY, factoryContext);
        mixedContext.put(PluggableViewModel.LOCAL_CONTEXT_KEY, localContext);
        return mixedContext;
    }
}
