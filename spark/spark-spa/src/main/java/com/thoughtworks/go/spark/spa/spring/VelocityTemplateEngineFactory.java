/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.ui.velocity.SpringResourceLoader;
import org.springframework.web.context.ServletContextAware;
import spark.TemplateEngine;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import static org.apache.velocity.runtime.RuntimeConstants.*;

@Component
public class VelocityTemplateEngineFactory implements ServletContextAware, InitializingBean {
    private final InitialContextProvider initialContextProvider;
    private final ResourceLoader resourceLoader;
    private final String resourceBasePaths;

    private VelocityEngine velocityEngine;
    private int modificationCheckInterval = 0;

    @Autowired
    public VelocityTemplateEngineFactory(InitialContextProvider initialContextProvider, ResourceLoader resourceLoader, @Value("${go.spark.spa.velocity.base.path}") String resourceBasePaths) {
        this.initialContextProvider = initialContextProvider;
        this.resourceLoader = resourceLoader;
        this.resourceBasePaths = resourceBasePaths;
    }

    public TemplateEngine create(Class<? extends SparkController> controller, LayoutTemplateProvider provider) {
        return new VelocityTemplateEngine(velocityEngine, provider, controller, initialContextProvider);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (servletContext.getInitParameter("rails.env").equals("development")) {
            this.modificationCheckInterval = 1;
        }
    }

    @Override
    public void afterPropertiesSet() {
        Properties properties = new Properties();

        // this is setup so that file takes preference over classpath, so that we can reload stuff in dev environment
        // and fallback to spring classloader in prod environment
        properties.setProperty(RESOURCE_LOADER, "file," + SpringResourceLoader.NAME);
        properties.setProperty(FILE_RESOURCE_LOADER_CACHE, "true");
        properties.setProperty(EVENTHANDLER_REFERENCEINSERTION, org.apache.velocity.app.event.implement.EscapeHtmlReference.class.getName());
        properties.setProperty(FILE_RESOURCE_LOADER_PATH, resourceBasePaths);
        properties.setProperty("file.resource.loader.modificationCheckInterval", String.valueOf(modificationCheckInterval));

        properties.setProperty(SpringResourceLoader.SPRING_RESOURCE_LOADER_CLASS, SpringResourceLoader.class.getName());
        properties.setProperty(SpringResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");

        if (modificationCheckInterval != 0) {
            properties.setProperty(VM_LIBRARY_AUTORELOAD, "true");
        }
        properties.setProperty(VM_LIBRARY, "macros/urls.vm");
        this.velocityEngine = new VelocityEngine(properties);
        this.velocityEngine.setApplicationAttribute(SpringResourceLoader.SPRING_RESOURCE_LOADER, resourceLoader);
        this.velocityEngine.setApplicationAttribute(SpringResourceLoader.SPRING_RESOURCE_LOADER_PATH, resourceBasePaths);
    }

    private String toFiles(Resource[] velocityResource) throws IOException {
        ArrayList<String> files = new ArrayList<>();
        for (Resource resource : velocityResource) {
            String absolutePath = new File(resource.getURL().getFile()).getAbsolutePath();
            files.add(absolutePath);
        }
        return StringUtils.join(files, ",");
    }

}
