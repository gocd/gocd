/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.*;

import java.util.ArrayList;
import java.util.List;

public class PipelineConfigTreeValidator {
    private final PipelineConfig pipelineConfig;

    public PipelineConfigTreeValidator(PipelineConfig pipelineConfig) {
        this.pipelineConfig = pipelineConfig;
    }

    public boolean validate(PipelineConfigSaveValidationContext validationContext) {
        pipelineConfig.validate(validationContext);
        pipelineCreationSpecificValidations(validationContext);

        validateDependencies(validationContext);
        boolean isValid = pipelineConfig.errors().isEmpty();
        PipelineConfigSaveValidationContext contextForChildren = validationContext.withParent(pipelineConfig);

        for (StageConfig stageConfig : pipelineConfig.getStages()) {
            isValid = stageConfig.validateTree(contextForChildren) && isValid;
            if (pipelineConfig.hasTemplateApplied()) {
                final List<ConfigErrors> allErrors = new ArrayList<>();
                new GoConfigGraphWalker(stageConfig).walk(new ErrorCollectingHandler(allErrors) {
                    @Override
                    public void handleValidation(Validatable validatable, ValidationContext context) {
                    }
                });
                for (ConfigErrors error : allErrors) {
                    pipelineConfig.errors().add("template", ListUtil.join(error.getAll()));
                }
            }
        }
        validateCyclicDependencies(validationContext);
        isValid = pipelineConfig.materialConfigs().validateTree(contextForChildren) && isValid;
        isValid = pipelineConfig.getParams().validateTree(contextForChildren) && isValid;
        isValid = pipelineConfig.getVariables().validateTree(contextForChildren) && isValid;
        if (pipelineConfig.getTrackingTool() != null)
            isValid = pipelineConfig.getTrackingTool().validateTree(contextForChildren) && isValid;
        if (pipelineConfig.getMingleConfig() != null)
            isValid = pipelineConfig.getMingleConfig().validateTree(contextForChildren) && isValid;
        if (pipelineConfig.getTimer() != null)
            isValid = pipelineConfig.getTimer().validateTree(contextForChildren) && isValid;
        return isValid;
    }

    private void pipelineCreationSpecificValidations(PipelineConfigSaveValidationContext validationContext) {
        if (validationContext.isPipelineBeingCreated()) {
            validationContext.getGroups().validatePipelineNameUniqueness();
            PipelineConfigs group = validationContext.getPipelineGroup();
            group.validateGroupNameAndAddErrorsTo(pipelineConfig.errors());
        }
    }

    private void validateCyclicDependencies(PipelineConfigSaveValidationContext validationContext) {
        final DFSCycleDetector dfsCycleDetector = new DFSCycleDetector();
        try {
            dfsCycleDetector.topoSort(pipelineConfig.name(), new PipelineConfigValidationContextDependencyState(pipelineConfig, validationContext));
        } catch (Exception e) {
            pipelineConfig.materialConfigs().addError("base", e.getMessage());
        }
    }

    private void validateDependencies(PipelineConfigSaveValidationContext validationContext) {
        if (validationContext.isPipelineBeingCreated()) return;
        for (CaseInsensitiveString selected : validationContext.getPipelinesWithDependencyMaterials()) {
            if (selected.equals(pipelineConfig.name())) continue;
            PipelineConfig selectedPipeline = validationContext.getPipelineConfigByName(selected);
            validateDependencyMaterialsForDownstreams(validationContext, selected, selectedPipeline);
            validateFetchTasksForOtherPipelines(validationContext, selectedPipeline);
        }
    }

    private void validateDependencyMaterialsForDownstreams(PipelineConfigSaveValidationContext validationContext, CaseInsensitiveString selected, PipelineConfig downstreamPipeline) {
        Node dependenciesOfSelectedPipeline = validationContext.getDependencyMaterialsFor(selected);
        for (Node.DependencyNode dependencyNode : dependenciesOfSelectedPipeline.getDependencies()) {
            if (dependencyNode.getPipelineName().equals(pipelineConfig.name())) {
                for (MaterialConfig materialConfig : downstreamPipeline.materialConfigs()) {
                    if (materialConfig instanceof DependencyMaterialConfig) {
                        DependencyMaterialConfig dependencyMaterialConfig = new Cloner().deepClone((DependencyMaterialConfig) materialConfig);
                        dependencyMaterialConfig.validate(validationContext.withParent(downstreamPipeline));
                        List<String> allErrors = dependencyMaterialConfig.errors().getAll();
                        for (String error : allErrors) {
                            pipelineConfig.errors().add("base", error);
                        }
                    }
                }
            }
        }
    }

    private void validateFetchTasksForOtherPipelines(PipelineConfigSaveValidationContext validationContext, PipelineConfig downstreamPipeline) {
        for (StageConfig stageConfig : downstreamPipeline.getStages()) {
            for (JobConfig jobConfig : stageConfig.getJobs()) {
                for (Task task : jobConfig.getTasks()) {
                    if (task instanceof FetchTask) {
                        FetchTask fetchTask = (FetchTask) task;
                        if (fetchTask.getPipelineNamePathFromAncestor() != null && !StringUtil.isBlank(CaseInsensitiveString.str(fetchTask.getPipelineNamePathFromAncestor().getPath())) && fetchTask.getPipelineNamePathFromAncestor().pathIncludingAncestor().contains(pipelineConfig.name())) {
                            fetchTask = new Cloner().deepClone(fetchTask);
                            fetchTask.validateTask(validationContext.withParent(downstreamPipeline).withParent(stageConfig).withParent(jobConfig));
                            List<String> allErrors = fetchTask.errors().getAll();
                            for (String error : allErrors) {
                                pipelineConfig.errors().add("base", error);
                            }
                        }
                    }
                }
            }
        }
    }

    private class PipelineConfigValidationContextDependencyState implements PipelineDependencyState {
        private PipelineConfig pipelineConfig;
        private PipelineConfigSaveValidationContext validationContext;

        public PipelineConfigValidationContextDependencyState(PipelineConfig pipelineConfig, PipelineConfigSaveValidationContext validationContext) {
            this.pipelineConfig = pipelineConfig;
            this.validationContext = validationContext;
        }

        @Override
        public boolean hasPipeline(CaseInsensitiveString key) {
            return validationContext.getPipelineConfigByName(key) != null;
        }

        @Override
        public Node getDependencyMaterials(CaseInsensitiveString pipelineName) {
            if (pipelineConfig.name().equals(pipelineName))
                return pipelineConfig.getDependenciesAsNode();
            return validationContext.getDependencyMaterialsFor(pipelineName);
        }
    }

}
