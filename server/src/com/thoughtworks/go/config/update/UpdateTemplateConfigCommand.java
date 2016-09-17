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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class UpdateTemplateConfigCommand extends TemplateConfigCommand{
    private String md5;
    private EntityHashingService entityHashingService;

    public UpdateTemplateConfigCommand(PipelineTemplateConfig templateConfig, Username currentUser, GoConfigService goConfigService, LocalizedOperationResult result, String md5, EntityHashingService entityHashingService) {
        super(templateConfig, result, currentUser, goConfigService);
        this.md5 = md5;
        this.entityHashingService = entityHashingService;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        PipelineTemplateConfig existingTemplateConfig = findAddedTemplate(modifiedConfig);
        TemplatesConfig templatesConfig = modifiedConfig.getTemplates();
        templatesConfig.removeTemplateNamed(existingTemplateConfig.name());
        templatesConfig.add(templateConfig);
        modifiedConfig.setTemplates(templatesConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        PipelineTemplateConfig pipelineTemplateConfig = findAddedTemplate(cruiseConfig);
        boolean freshRequest =  entityHashingService.md5ForEntity(pipelineTemplateConfig).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Template", templateConfig.name()));
        }

        return freshRequest;
    }
}

