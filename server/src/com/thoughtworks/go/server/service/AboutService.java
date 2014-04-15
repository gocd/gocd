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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.Map;
import java.io.File;

import com.thoughtworks.go.i18n.Localizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AboutService {

    @Autowired private GoConfigService goConfigService;
    @Autowired private SystemService systemService;
    @Autowired private ArtifactsDirHolder artifactsDirChangeListener;
    @Autowired private Localizer localizer;

    public AboutService() {
    }

    public Map<String, Object> populateModel(String activeTab) {
        Map<String, Object> model = new HashMap<String, Object>();

        systemService.populateServerDetailsModel(model);

        populateServerDetailsModel(model);

        model.put("active", activeTab);
        model.put("current_tab", "");
        model.put("l", localizer);

        return model;
    }

    public void populateServerDetailsModel(Map<String, Object> model) {
        model.put("available_space", getAvailableSpace());

        goConfigService.populateLicenseAndConfigValidityInfo(model);
    }

    private Long getAvailableSpace() {
        File artifactsDir = artifactsDirChangeListener.getArtifactsDir();
        return artifactsDir.getUsableSpace() / (1024 * 1024);
    }

}
