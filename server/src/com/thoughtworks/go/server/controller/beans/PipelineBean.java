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

package com.thoughtworks.go.server.controller.beans;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.Tasks;
import com.thoughtworks.go.config.TestArtifactPlan;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.apache.commons.lang.StringUtils;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isBlank;

public class PipelineBean {
    private String pipelineName;
    private MaterialConfig materialConfig;
    private String arguments;
    private String[] type;
    private String builder;
    private String buildfile;
    private String target;
    private String[] src;
    private String[] dest;
    private String command;

    public PipelineBean(String pipelineName, MaterialConfig materialConfig, String builder,
                        String buildfile, String target, String[] src, String[] dest, String[] type, String command,
                        String arguments) {
        this.pipelineName = pipelineName;
        this.materialConfig = materialConfig;
        this.builder = StringUtils.isBlank(builder) ? "ant" : builder;
        this.target = target;
        this.buildfile = buildfile;
        this.src = src == null ? new String[0] : src;
        this.dest = dest == null ? new String[0] : dest;
        this.type = type == null ? new String[0] : type;
        this.command = command;
        this.arguments = arguments;
    }

    public PipelineConfig getPipelineConfig() {
        return new PipelineConfig(new CaseInsensitiveString(pipelineName), new MaterialConfigs(materialConfig), getStage());
    }

    private StageConfig getStage() {
        JobConfig plan = new JobConfig(new CaseInsensitiveString("defaultJob"), new Resources(), getArtifactPlans(), getTasks());
        return new StageConfig(new CaseInsensitiveString("defaultStage"), new JobConfigs(plan));
    }

    public ArtifactPlans getArtifactPlans() {
        ArtifactPlans artifactPlans = new ArtifactPlans();
        for (int i = 0; i < src.length; i++) {
            if (isBlank(src[i])) {
                continue;
            }
            ArtifactPlan plan;
            if (StringUtils.equals(type[i], "test")) {
                plan = new TestArtifactPlan();
            } else {
                plan = new ArtifactPlan();
            }
            plan.setSrc(defaultString(src[i]));
            plan.setDest(defaultString(dest[i]));
            artifactPlans.add(plan);
        }
        return artifactPlans;
    }

    public Tasks getTasks() {
        Tasks tasks = new Tasks();
        if ("ant".equals(builder)) {
            AntTask antTask = new AntTask();
            antTask.setTarget(this.target);
            antTask.setBuildFile(
                    defaultString(StringUtils.isBlank(this.buildfile) ? "build.xml" : this.buildfile));
            tasks.add(antTask);
        } else if ("nant".equals(builder)) {
            NantTask nantTask = new NantTask();
            nantTask.setTarget(this.target);
            nantTask.setBuildFile(
                    defaultString(StringUtils.isBlank(this.buildfile) ? "default.build" : this.buildfile));
            tasks.add(nantTask);
        } else if ("rake".equals(builder)) {
            RakeTask rakeTask = new RakeTask();
            rakeTask.setTarget(this.target);
            rakeTask.setBuildFile(StringUtils.isBlank(this.buildfile) ? null : this.buildfile);
            tasks.add(rakeTask);
        } else if ("exec".equals(builder)) {
            String trimmedCommand = StringUtils.defaultString(this.command).trim();
            String trimmedArguments = StringUtils.defaultString(this.arguments).trim();
            ExecTask execTask = new ExecTask(trimmedCommand, trimmedArguments, (String) null);
            tasks.add(execTask);
        }
        return tasks;
    }
}
