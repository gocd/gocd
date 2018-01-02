/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.materials.dependency;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.DependencyFilter;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

@ConfigTag(value = "pipeline", label = "Pipeline")
public class DependencyMaterialConfig extends AbstractMaterialConfig implements ParamsAttributeAware {
    public static final String PIPELINE_NAME = "pipelineName";
    public static final String STAGE_NAME = "stageName";
    public static final String PIPELINE_STAGE_NAME = "pipelineStageName";
    public static final String TYPE = "DependencyMaterial";
    private static final Pattern PIPELINE_STAGE_COMBINATION_PATTERN = Pattern.compile("^(.+) (\\[.+\\])$");
    public static final String ORIGIN = "origin";

    @ConfigAttribute(value = "pipelineName")
    private com.thoughtworks.go.config.CaseInsensitiveString pipelineName = new CaseInsensitiveString("Unknown");

    @ConfigAttribute(value = "stageName")
    private CaseInsensitiveString stageName = new CaseInsensitiveString("Unknown");

    private String pipelineStageName;


    public DependencyMaterialConfig() {
        super(TYPE);
    }

    public DependencyMaterialConfig(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        this(null, pipelineName, stageName);
    }

    public DependencyMaterialConfig(final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName, final String serverAlias) {
        this(null, pipelineName, stageName);
    }

    public DependencyMaterialConfig(final CaseInsensitiveString name, final CaseInsensitiveString pipelineName, final CaseInsensitiveString stageName) {
        super(TYPE, name, new ConfigErrors());
        bombIfNull(pipelineName, "null pipelineName");
        bombIfNull(stageName, "null stageName");
        this.pipelineName = pipelineName;
        this.stageName = stageName;
    }

    @Override
    public CaseInsensitiveString getName() {
        return super.getName() == null ? pipelineName : super.getName();
    }

    public String getUserName() {
        return "cruise";
    }

    @Override
    public String getLongDescription() {
        return getDescription();
    }

    @Override
    public Filter filter() {
        return new DependencyFilter();
    }

    @Override
    public boolean isInvertFilter() {
        return false;
    }

    @Override
    public boolean matches(String name, String regex) {
        return false;
    }

    @Override
    public String getDescription() {
        return CaseInsensitiveString.str(pipelineName);
    }

    @Override
    public String getTypeForDisplay() {
        return "Pipeline";
    }

    @Override
    public boolean isAutoUpdate() {
        return true;
    }

    @Override
    public void setAutoUpdate(boolean autoUpdate) {
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put("pipelineName", CaseInsensitiveString.str(pipelineName));
        parameters.put("stageName", CaseInsensitiveString.str(stageName));
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        appendCriteria(parameters);
    }

    public CaseInsensitiveString getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(CaseInsensitiveString pipelineName) {
        this.pipelineName = pipelineName;
    }

    public CaseInsensitiveString getStageName() {
        return stageName;
    }

    public void setStageName(CaseInsensitiveString stageName) {
        this.stageName = stageName;
    }

    @Override
    public String getFolder() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DependencyMaterialConfig that = (DependencyMaterialConfig) o;
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "DependencyMaterialConfig{" +
                "pipelineName='" + pipelineName + '\'' +
                ", stageName='" + stageName + '\'' +
                '}';
    }

    @Override
    public String getDisplayName() {
        return CaseInsensitiveString.str(getName());
    }

    @Override
    protected void validateConcreteMaterial(ValidationContext validationContext) {
        CaseInsensitiveString upstreamPipelineName = this.getPipelineName();
        CaseInsensitiveString upstreamStageName = this.getStageName();

        PipelineConfig upstreamPipeline = validationContext.getPipelineConfigByName(upstreamPipelineName);
        PipelineConfig pipeline = validationContext.getPipeline();
        if (upstreamPipeline==null) {
            errors.add(DependencyMaterialConfig.PIPELINE_STAGE_NAME, String.format("Pipeline with name '%s' does not exist, it is defined as a dependency for pipeline '%s' (%s)", upstreamPipelineName, pipeline.name(), pipeline.getOriginDisplayName()));
        }
        else if (upstreamPipeline.findBy(upstreamStageName) == null) {
            errors.add(DependencyMaterialConfig.PIPELINE_STAGE_NAME, String.format("Stage with name '%s' does not exist on pipeline '%s', it is being referred to from pipeline '%s' (%s)", upstreamStageName, upstreamPipelineName, pipeline.name(), pipeline.getOriginDisplayName()));
        }
    }

    @Override
    public String getUriForDisplay() {
        return String.format("%s / %s", pipelineName, stageName);
    }

    public void validateUniqueness(Set<CaseInsensitiveString> dependencies) {
        CaseInsensitiveString upstreamPipelineName = pipelineName;
        if (dependencies.contains(upstreamPipelineName)) {
            String message = (String.format("A pipeline can depend on each upstream pipeline only once. Remove one of the occurrences of '%s' from the current pipeline dependencies.", upstreamPipelineName));
            errors.add(PIPELINE_STAGE_NAME, message);
        }
        dependencies.add(pipelineName);
    }

    @Override
    public void setConfigAttributes(Object attributes) {
        resetCachedIdentityAttributes();
        if (attributes == null) {
            return;
        }
        Map attributesMap = (Map) attributes;
        if (attributesMap.containsKey(MATERIAL_NAME)) {
            name = new CaseInsensitiveString((String) attributesMap.get(MATERIAL_NAME));
            if (CaseInsensitiveString.isBlank(name)) {
                name = null;
            }
        }
        if (attributesMap.containsKey(PIPELINE_STAGE_NAME)) {
            pipelineStageName = (String) attributesMap.get(PIPELINE_STAGE_NAME);
            Matcher matcher = PIPELINE_STAGE_COMBINATION_PATTERN.matcher(pipelineStageName);
            if(matcher.matches()){
                pipelineName = new CaseInsensitiveString(matcher.group(1));
                String stageNameWithBrackets = matcher.group(2);
                stageName = new CaseInsensitiveString(stageNameWithBrackets.replace("[","").replace("]",""));
            }
            else {
               errors.add(PIPELINE_STAGE_NAME, String.format("'%s' should conform to the pattern 'pipeline [stage]'",pipelineStageName));
            }
        }
    }

    public String getPipelineStageName() {
        if (pipelineStageName != null) {
            return pipelineStageName;
        }
        if (CaseInsensitiveString.isBlank(pipelineName) || CaseInsensitiveString.isBlank(stageName)) {
            return null;
        }
        return String.format("%s [%s]", pipelineName, stageName);
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig){
        List<FetchTask> fetchTasks = pipelineConfig.getFetchTasks();
        for (FetchTask fetchTask : fetchTasks) {
            if(pipelineName.equals(fetchTask.getDirectParentInAncestorPath()))
                return true;
        }
        return false;
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        // Dependency materials are already unique within a pipeline
    }
}
