/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import java.io.IOException;

import com.thoughtworks.go.domain.XmlRepresentable;
import com.thoughtworks.go.domain.XmlWriterContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class XmlApiService {

    private final PropertiesService propertiesService;
    private final ArtifactsService artifactsService;
    private final JobInstanceService jobInstanceService;
    private final StageService stageService;

    @Autowired
    public XmlApiService(PropertiesService propertiesService, ArtifactsService artifactsService, JobInstanceService jobInstanceService, StageService stageService) {
        this.propertiesService = propertiesService;
        this.artifactsService = artifactsService;
        this.jobInstanceService = jobInstanceService;
        this.stageService = stageService;
    }

    private XmlWriterContext ctxFor(String baseUrl) {
        return new XmlWriterContext(baseUrl, propertiesService, artifactsService, jobInstanceService, stageService);
    }

    public Document write(XmlRepresentable representable, String baseUrl) throws IOException, DocumentException {
        return representable.toXml(ctxFor(baseUrl));
    }
}
