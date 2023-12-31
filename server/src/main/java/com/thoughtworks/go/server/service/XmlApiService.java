/*
 * Copyright 2024 Thoughtworks, Inc.
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

import com.thoughtworks.go.server.domain.xml.XmlRepresentable;
import com.thoughtworks.go.server.domain.xml.XmlWriterContext;
import com.thoughtworks.go.util.SystemEnvironment;
import org.dom4j.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.apache.commons.lang3.StringUtils.endsWithAny;

@Service
public class XmlApiService {

    private final ArtifactsService artifactsService;
    private final JobInstanceService jobInstanceService;
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public XmlApiService(ArtifactsService artifactsService,
                         JobInstanceService jobInstanceService,
                         SystemEnvironment systemEnvironment) {
        this.artifactsService = artifactsService;
        this.jobInstanceService = jobInstanceService;
        this.systemEnvironment = systemEnvironment;
    }

    private XmlWriterContext ctxFor(String baseUrl) {
        return new XmlWriterContext(baseUrl, artifactsService, jobInstanceService);
    }

    public Document write(XmlRepresentable representable, String baseUrl) {
        checkBaseUrl(baseUrl);
        return representable.toXml(ctxFor(baseUrl));
    }

    private void checkBaseUrl(String baseUrl) {
        String expectedContextPath = systemEnvironment.getWebappContextPath();
        if (!endsWithAny(baseUrl.toLowerCase(), expectedContextPath, expectedContextPath + "/")) {
            throw new IllegalArgumentException("The baseUrl must end with " + expectedContextPath);
        }
    }
}
