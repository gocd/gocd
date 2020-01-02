/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import org.apache.commons.lang3.StringUtils;

@ConfigTag("depends")
public class DependsConfig {
    @ConfigAttribute(value = "pipeline", optional = false) private String pipeline;
    @ConfigAttribute(value = "stage", optional = false) private String stage;

    public DependsConfig() {

    }

    public DependsConfig(String pipeline, String stage) {
        this.pipeline = pipeline;
        this.stage = stage;
    }

    public String getPipeline() {
        return pipeline;
    }

    public String getStage() {
        return stage;
    }

    public boolean depends(String pipelineName, String stageName) {
        return StringUtils.equals(this.pipeline, pipelineName) && StringUtils.equals(this.stage, stageName);
    }
}
