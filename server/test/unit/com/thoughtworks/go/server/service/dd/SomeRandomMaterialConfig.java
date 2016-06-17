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

package com.thoughtworks.go.server.service.dd;

import java.util.Map;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.ValidationContext;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;

public class SomeRandomMaterialConfig extends AbstractMaterialConfig {
    public SomeRandomMaterialConfig() {
        super("SomeRandomMaterial");
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    protected void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    protected void validateConcreteMaterial(ValidationContext validationContext) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getFolder() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public Filter filter() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public boolean isInvertFilter() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public boolean matches(String name, String regex) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getDescription() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getTypeForDisplay() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getDisplayName() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public boolean isAutoUpdate() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public void setAutoUpdate(boolean autoUpdate)  {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getUriForDisplay() {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig) {
        throw new RuntimeException("Ouch!");
    }

    @Override
    public String getLongDescription() {
        throw new RuntimeException("Ouch!");
    }
}
