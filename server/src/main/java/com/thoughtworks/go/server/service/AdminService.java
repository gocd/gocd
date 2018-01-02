/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminService {
    private final GoConfigService goConfigService;

    @Autowired
    public AdminService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public Map populateModel(Map model) {
        goConfigService.populateAdminModel(model);
        return model;
    }

    public Map<String, Object> configurationJsonForSourceXml() {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Object> configJson = new LinkedHashMap<>();
        GoConfigService.XmlPartialSaver saver = goConfigService.fileSaver(false);
        configJson.put("location", goConfigService.fileLocation());
        configJson.put("content", saver.asXml());
        configJson.put("md5", saver.getMd5());
        json.put("config", configJson);
        return json;
    }

    public Map configurationMapForSourceXml() {
        HashMap map = new HashMap();
        return populateModel(map);
    }

    public GoConfigValidity updateConfig(Map attributes, HttpLocalizedOperationResult result) {
        GoConfigValidity validity;
        String configXml = (String) attributes.get("content");
        String configMd5 = (String) attributes.get("md5");
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(false);
        validity = fileSaver.saveXml(configXml, configMd5);
        if (!validity.isValid()) {
            result.badRequest(LocalizedMessage.string("SAVE_FAILED"));
        }
        return validity;
    }
}
