/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.configrepo.contract;

import java.util.ArrayList;
import java.util.Collection;

public class CRParseResult {
    private Collection<CREnvironment> environments = new ArrayList<>();
    private Collection<CRPipeline> pipelines = new ArrayList<>();
    private ErrorCollection errors;

    public CRParseResult(){}
    public CRParseResult(Collection<CREnvironment> environments,Collection<CRPipeline> pipelines,ErrorCollection errors){
        this.environments = environments;
        this.pipelines = pipelines;
        this.errors = errors;
    }

    public CRParseResult(ErrorCollection errors) {
        this.errors = errors;
    }

    public Collection<CREnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Collection<CREnvironment> environments) {
        this.environments = environments;
    }

    public Collection<CRPipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Collection<CRPipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public ErrorCollection getErrors() {
        return errors;
    }

    public void setErrors(ErrorCollection errors) {
        this.errors = errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
