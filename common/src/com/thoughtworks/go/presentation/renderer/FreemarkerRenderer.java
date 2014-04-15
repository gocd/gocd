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

package com.thoughtworks.go.presentation.renderer;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.thoughtworks.go.plugins.presentation.PluggableViewModel;
import com.thoughtworks.go.plugins.presentation.Renderer;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

/**
* @understands rendering a freemarker template
*/
public class FreemarkerRenderer implements Renderer {

    private final Configuration configuration;

    public FreemarkerRenderer() {
        configuration = new Configuration();
        configuration.setObjectWrapper(new DefaultObjectWrapper());
        configuration.setLocalizedLookup(false);//TODO: Once we start supporting plugin localization, turn this on
        configuration.setTemplateLoader(getTemplateLoader());
    }

    private TemplateLoader getTemplateLoader() {
        return new URLTemplateLoader() {
                @Override protected URL getURL(String s) {
                    try {
                        return new URL(s);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(String.format("Could not create url from %s", s));
                    }
                }
            };
    }

    public String render(PluggableViewModel viewModel, Map<String, Object> context) throws Exception {
        StringWriter writer = new StringWriter();
        configuration.getTemplate(viewModel.getTemplatePath()).process(context, writer);
        return writer.toString();
    }
}
