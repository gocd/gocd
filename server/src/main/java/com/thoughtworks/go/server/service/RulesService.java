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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

@Service
public class RulesService {
    private GoConfigService goConfigService;

    @Autowired
    public RulesService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean validateSecretConfigReferences(ScmMaterial scmMaterial) {
        SecretParams secretParams = scmMaterial.getSecretParams();
        secretParams.forEach(secretParam -> {
            SecretConfig secretConfig = goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId());
            List<CaseInsensitiveString> pipelines = goConfigService.pipelinesWithMaterial(scmMaterial.getFingerprint());
            pipelines.forEach(pipeline -> {
                PipelineConfigs group = goConfigService.findGroupByPipeline(pipeline);
                if (!secretConfig.canRefer(PipelineConfigs.class, group.getGroup())) {
                    throw new RulesViolationException(format("Material with url: '%s' in Pipeline: '%s' and Pipeline Group: '%s'" +
                                    " does not have permission to refer to Secrets using SecretConfig: '%s'",
                            scmMaterial.getUrl(), pipeline, group.getGroup(), secretConfig.getId()));
                }
            });
        });

        return true;
    }
}
