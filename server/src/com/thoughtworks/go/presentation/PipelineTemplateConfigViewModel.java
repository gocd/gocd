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

package com.thoughtworks.go.presentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ParamsAttributeAware;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;

/**
 * @understands: How to render the PipelineTemplateConfig object for new & create
 */
public class PipelineTemplateConfigViewModel implements ParamsAttributeAware {

    public static final String TEMPLATE = "template";
    public static final String USE_EXISTING_PIPELINE = "useExistingPipeline";
    public static final String PIPELINE_NAMES = "pipelineNames";
    public static final String SELECTED_PIPELINE_NAME = "selectedPipelineName";

    private final PipelineTemplateConfig pipelineTemplateConfig;
    private final List<PipelineConfig> pipelineConfigs;
    private boolean useExistingPipeline = false;
    private String selectedPipelineName;

    //Used in specs
    public PipelineTemplateConfigViewModel() {
        this(new PipelineTemplateConfig(), "", new ArrayList<PipelineConfig>());
    }

    public PipelineTemplateConfigViewModel(PipelineTemplateConfig pipelineTemplateConfig, final String selectedPipelineName, List<PipelineConfig> pipelineConfigs) {
        this.pipelineTemplateConfig = pipelineTemplateConfig;
        this.pipelineConfigs = pipelineConfigs;
        this.selectedPipelineName = selectedPipelineName;
    }

    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        pipelineTemplateConfig.setConfigAttributes(attributeMap.get(TEMPLATE));
        useExistingPipeline = "1".equals(attributeMap.get(USE_EXISTING_PIPELINE));
        if (useExistingPipeline) {
            selectedPipelineName = attributeMap.get(SELECTED_PIPELINE_NAME).toString();
            pipelineTemplateConfig.copyStages(getPipeline(selectedPipelineName));
        }
        else {
            pipelineTemplateConfig.addDefaultStage();
        }
    }

    private PipelineConfig getPipeline(String pipelineName) {
        for (PipelineConfig config : pipelineConfigs) {
            if (config.name().equals(new CaseInsensitiveString(pipelineName))) {
                return config;
            }
        }
        return null;
    }

    public PipelineTemplateConfig templateConfig() {
        return pipelineTemplateConfig;
    }

    public boolean useExistingPipeline() {
        return useExistingPipeline;
    }

    public String selectedPipelineName() {
        return selectedPipelineName;
    }

    public List<String> pipelineNames() {
        List<String> names = new ArrayList<>();
        for (PipelineConfig config : pipelineConfigs) {
            names.add(config.name().toString());
        }
        return names;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineTemplateConfigViewModel that = (PipelineTemplateConfigViewModel) o;

        if (pipelineConfigs != null ? !pipelineConfigs.equals(that.pipelineConfigs) : that.pipelineConfigs != null) {
            return false;
        }
        if (pipelineTemplateConfig != null ? !pipelineTemplateConfig.equals(that.pipelineTemplateConfig) : that.pipelineTemplateConfig != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pipelineTemplateConfig != null ? pipelineTemplateConfig.hashCode() : 0;
        result = 31 * result + (pipelineConfigs != null ? pipelineConfigs.hashCode() : 0);
        return result;
    }
}
