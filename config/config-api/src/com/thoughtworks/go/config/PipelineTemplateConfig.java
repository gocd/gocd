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

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.preprocessor.ClassAttributeCache;
import com.thoughtworks.go.config.preprocessor.ParamReferenceCollectorFactory;
import com.thoughtworks.go.config.preprocessor.ParamResolver;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.BaseCollection;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @understands abstracting a pipeline definition
 */
@ConfigTag("pipeline")
@ConfigCollection(value = StageConfig.class)
public class PipelineTemplateConfig extends BaseCollection<StageConfig> implements Validatable, ParamsAttributeAware {
    private static final ClassAttributeCache.FieldCache FIELD_CACHE = new ClassAttributeCache.FieldCache();
    private static final Cloner CLONER = new Cloner();

    public static final String NAME = "name";

    @ConfigAttribute(optional = false, value = "name")
    private CaseInsensitiveString name;

    public static final String AUTHORIZATION = "authorization";

    @ConfigSubtag @SkipParameterResolution
    private Authorization authorization = new Authorization();

    private final ConfigErrors configErrors = new ConfigErrors();

    public PipelineTemplateConfig() {
    }

    public PipelineTemplateConfig(CaseInsensitiveString name, StageConfig... items) {
        super(items);
        this.name = name;
    }

    public PipelineTemplateConfig(CaseInsensitiveString name, Authorization authorization, StageConfig... items) {
        this(name, items);
        this.authorization = authorization;
    }

    public CaseInsensitiveString name() {
        return name;
    }

    public void validate(ValidationContext validationContext) {
        validateTemplateName();
        validateStageNameUniqueness();
        validateStageConfig(validationContext);
    }

    public void validateStageConfig(ValidationContext validationContext) {
        ValidationContext contextForChildren = validationContext.withParent(this);
        for(StageConfig stageConfig : this) {
            stageConfig.validateTree(contextForChildren);
        }
    }

    private void validateStageNameUniqueness() {
        Map<String, StageConfig> stageNameMap = new HashMap<>();
        for (StageConfig stageConfig : this) {
            stageConfig.validateNameUniqueness(stageNameMap);
        }
    }

    private void validateTemplateName() {
        if (!new NameTypeValidator().isNameValid(name)) {
            errors().add(NAME, NameTypeValidator.errorMessage("template", name));
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public StageConfig getStage(final CaseInsensitiveString stageName) {
        return findBy(stageName);
    }

    public List<StageConfig> getStages() {
        return this;
    }

    public void setName(String name) {
        setName(new CaseInsensitiveString(name));
    }

    public void setName(CaseInsensitiveString name) {
        this.name = name;
    }

    public StageConfig findBy(final CaseInsensitiveString stageName) {
        for (StageConfig stageConfig : this) {
            if (stageConfig.name().equals(stageName)) {
                return stageConfig;
            }
        }
        return null;
    }

    public boolean addStageWithoutValidityAssertion(StageConfig stageConfig) {
        return super.add(stageConfig);
    }

    public void incrementIndex(StageConfig stageToBeMoved) {
        moveStage(stageToBeMoved, 1);
    }

    public void decrementIndex(StageConfig stageToBeMoved) {
        moveStage(stageToBeMoved, -1);
    }

    private void moveStage(StageConfig moveMeStage, int moveBy) {
        int current = this.indexOf(moveMeStage);
        if (current == -1) {
            throw new RuntimeException(String.format("Cannot find the stage '%s' in pipeline '%s'", moveMeStage.name(), name()));
        }
        this.remove(moveMeStage);
        this.add(current + moveBy, moveMeStage);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PipelineTemplateConfig config = (PipelineTemplateConfig) o;

        if (name != null ? !name.equals(config.name) : config.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public boolean matches(CaseInsensitiveString templateName) {
        return this.name.equals(templateName);
    }


    public void setConfigAttributes(Object attributes) {
        Map attributeMap = (Map) attributes;
        if (attributeMap.containsKey(NAME)) {
            String strName = (String) attributeMap.get(NAME);
            name = new CaseInsensitiveString(strName);
        }
        if (attributeMap.containsKey(AUTHORIZATION)) {
            this.authorization = new Authorization();
            this.authorization.setConfigAttributes(attributeMap.get(AUTHORIZATION));
        }
        else {
            this.authorization = new Authorization();
        }
    }

    public void addDefaultStage() {
        add(new StageConfig(new CaseInsensitiveString(StageConfig.DEFAULT_NAME), new JobConfigs(new JobConfig(JobConfig.DEFAULT_NAME))));
    }

    public void validateNameUniquness(Map<String, PipelineTemplateConfig> templateMap) {
        String currentName = name.toLower();
        PipelineTemplateConfig templateWithSameName = templateMap.get(currentName);
        if (templateWithSameName == null) {
            templateMap.put(currentName, this);
        } else {
            templateWithSameName.addError(NAME, String.format("Template name '%s' is not unique", templateWithSameName.name()));
            this.addError(NAME, String.format("Template name '%s' is not unique", name));
        }
    }

    public ParamsConfig referredParams() {
        ParamReferenceCollectorFactory paramHandlerFactory = new ParamReferenceCollectorFactory();
        new ParamResolver(paramHandlerFactory, FIELD_CACHE).resolve(CLONER.deepClone(this));
        ParamsConfig paramsConfig = new ParamsConfig();
        for (String param : paramHandlerFactory.referredParams()) {
            paramsConfig.add(new ParamConfig(param, null));
        }
        return paramsConfig;
    }

    public void copyStages(PipelineConfig pipeline) {
        if (pipeline != null) {
            addAll(pipeline);
        }
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public List<ConfigErrors> getAllErrors() {
        return ErrorCollector.getAllErrors(this);
    }
}
