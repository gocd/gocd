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

import com.google.gson.Gson;
import com.thoughtworks.go.spark.SparkController;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import spark.ModelAndView;
import spark.TemplateEngine;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

class FreeMarkerEngine extends TemplateEngine {
    private static final Gson GSON = new Gson();
    private final InitialContextProvider initialContextProvider;
    private final Class<? extends SparkController> controller;
    private final Configuration configuration;
    private LayoutTemplateProvider provider;

    FreeMarkerEngine(Configuration configuration, LayoutTemplateProvider provider, Class<? extends SparkController> controller, InitialContextProvider initialContextProvider) {
        this.controller = controller;
        this.initialContextProvider = initialContextProvider;
        this.configuration = configuration;
        this.provider = provider;
    }

    @Override
    public String render(ModelAndView modelAndView) {
        try {
            Template template = configuration.getTemplate(provider.layout(), "utf-8");
            Object model = modelAndView.getModel();
            if (model == null) {
                model = Collections.emptyMap();
            }
            if (model instanceof Map) {
                Map<String, Object> context = initialContextProvider.getContext((Map) model, controller, modelAndView.getViewName());
                StringWriter writer = new StringWriter();
                Object meta = context.get("meta");
                context.put("meta", GSON.toJson(meta));
                template.process(context, writer);
                return writer.toString();
            } else {
                throw new IllegalArgumentException("modelAndView must be of type java.util.Map");
            }
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
