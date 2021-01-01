/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.spark.spa.spring;

import com.thoughtworks.go.spark.SparkController;
import freemarker.core.XHTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.SpringTemplateLoader;
import org.springframework.web.context.ServletContextAware;
import spark.TemplateEngine;

import javax.servlet.ServletContext;

@Component
public class FreemarkerTemplateEngineFactory implements ServletContextAware, InitializingBean {
    private final InitialContextProvider initialContextProvider;
    private final ResourceLoader resourceLoader;
    private final String resourceLoaderPath;

    private Configuration configuration;
    private boolean shouldCheckForTemplateModifications = false;

    @Autowired
    public FreemarkerTemplateEngineFactory(InitialContextProvider initialContextProvider, ResourceLoader resourceLoader, @Value("${go.spark.spa.freemarker.base.path}") String resourceLoaderPath) {
        this.initialContextProvider = initialContextProvider;
        this.resourceLoader = resourceLoader;
        this.resourceLoaderPath = resourceLoaderPath;
    }

    public TemplateEngine create(Class<? extends SparkController> controller, LayoutTemplateProvider provider) {
        return new FreeMarkerEngine(configuration, provider, controller, initialContextProvider);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.shouldCheckForTemplateModifications = servletContext.getInitParameter("rails.env").equals("development");
    }

    @Override
    public void afterPropertiesSet() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_28);
        configuration.setDefaultEncoding("utf-8");
        configuration.setLogTemplateExceptions(true);
        configuration.setNumberFormat("computer");
        configuration.setOutputFormat(XHTMLOutputFormat.INSTANCE);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setTemplateLoader(new SpringTemplateLoader(this.resourceLoader, this.resourceLoaderPath));

        if (this.shouldCheckForTemplateModifications) {
            configuration.setTemplateUpdateDelayMilliseconds(1000);
        } else {
            configuration.setTemplateUpdateDelayMilliseconds(Long.MAX_VALUE);
        }

        this.configuration = configuration;
    }
}
