/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;

import java.io.Serializable;

@ConfigTag("property")
public class ArtifactPropertyConfig implements Serializable, Validatable {
    public static final String NAME = "name";

    @ConfigAttribute(value = "name", optional = false)
    @SkipParameterResolution
    private String name;
    @ConfigAttribute(value = "src", optional = false)
    private String src;
    @ConfigAttribute(value = "xpath", optional = false)
    private String xpath;
    private ConfigErrors configErrors = new ConfigErrors();

    public ArtifactPropertyConfig() {
    }

    public ArtifactPropertyConfig(String name, String src, String xpath) {
        this.name = name;
        this.src = src;
        this.xpath = xpath;
    }

    public ArtifactPropertyConfig(ArtifactPropertyConfig other) {
        this(other.name, other.src, other.xpath);
        this.configErrors = other.configErrors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }


    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getXpath() {
        return xpath;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        return configErrors.isEmpty();
    }

    @Override
    public void validate(ValidationContext validationContext) {
        if (new NameTypeValidator().isNameInvalid(name)) {
            this.configErrors.add(NAME, NameTypeValidator.errorMessage("property", name));
        }
    }

    @Override
    public ConfigErrors errors() {
        return configErrors;
    }

    @Override
    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactPropertyConfig)) return false;

        ArtifactPropertyConfig that = (ArtifactPropertyConfig) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (src != null ? !src.equals(that.src) : that.src != null) return false;
        return xpath != null ? xpath.equals(that.xpath) : that.xpath == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (src != null ? src.hashCode() : 0);
        result = 31 * result + (xpath != null ? xpath.hashCode() : 0);
        return result;
    }
}
