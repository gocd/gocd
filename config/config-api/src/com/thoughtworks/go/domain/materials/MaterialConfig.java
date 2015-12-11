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

package com.thoughtworks.go.domain.materials;

import java.io.Serializable;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigInterface;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.Filter;

@ConfigInterface
public interface MaterialConfig extends Serializable, Validatable {

    String getFolder();

    CaseInsensitiveString getName();

    Filter filter();

    boolean matches(String name, String regex);

    Map<String, Object> getSqlCriteria();

    String getDescription();

    String getFingerprint();

    String getPipelineUniqueFingerprint();

    String getTypeForDisplay();

    String getDisplayName();

    String getType();

    boolean isAutoUpdate();

    void setAutoUpdate(boolean autoUpdate);

    void validateNameUniqueness(Map<CaseInsensitiveString, AbstractMaterialConfig> map);

    String getUriForDisplay();

    boolean isSameFlyweight(MaterialConfig other);

    boolean isUsedInLabelTemplate(PipelineConfig pipelineConfig);

    Boolean isUsedInFetchArtifact(PipelineConfig pipelineConfig);

    String getLongDescription();

    void setConfigAttributes(Object attributes);

    String getShortRevision(String revision);

    String getTruncatedDisplayName();
}
