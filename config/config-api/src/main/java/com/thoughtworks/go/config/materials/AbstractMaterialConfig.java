/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.preprocessor.SkipParameterResolution;
import com.thoughtworks.go.config.validation.NameTypeValidator;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Understands material configuration
 */
public abstract class AbstractMaterialConfig implements MaterialConfig, ParamsAttributeAware {
    public static final String MATERIAL_NAME = "materialName";

    /**
     * CAREFUL!, this should be the same as the one used in migration 47_create_new_materials.sql
     */
    public static final String FINGERPRINT_DELIMITER = "<|>";

    public static final String MATERIAL_TYPE = "materialType";

    @SkipParameterResolution
    @ConfigAttribute(value = "materialName", allowNull = true)
    protected CaseInsensitiveString name;

    protected String type;
    protected ConfigErrors errors = new ConfigErrors();

    private Map<String, Object> sqlCriteria;
    private String pipelineUniqueFingerprint;
    private String fingerprint;

    public AbstractMaterialConfig(String typeName) {
        type = typeName;
    }

    public AbstractMaterialConfig(String typeName, CaseInsensitiveString name, ConfigErrors errors) {
        this(typeName);
        this.name = name;
        this.errors = errors;
    }

    @Override
    public CaseInsensitiveString getName() {
        return name;
    }

    public CaseInsensitiveString getMaterialName() {
        return name;
    }

    @Override
    public final Map<String, Object> getSqlCriteria() {
        if (sqlCriteria == null) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            appendCriteria(map);
            sqlCriteria = Collections.unmodifiableMap(map);
        }
        return sqlCriteria;
    }

    @Override
    public final Map<String, Object> getAttributesForScope() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("autoUpdate", isAutoUpdate());
        appendCriteria(map);
        return Collections.unmodifiableMap(map);
    }

    @Override
    public String getFingerprint() {
        if (fingerprint == null) {
            fingerprint = fingerprintFrom(getSqlCriteria());
        }
        return fingerprint;
    }

    @Override
    public String getPipelineUniqueFingerprint() {
        if (pipelineUniqueFingerprint == null) {
            Map<String, Object> basicCriteria = new LinkedHashMap<>(getSqlCriteria());
            appendPipelineUniqueCriteria(basicCriteria);
            pipelineUniqueFingerprint = fingerprintFrom(basicCriteria);
        }
        return pipelineUniqueFingerprint;
    }

    private String fingerprintFrom(Map<String, Object> map) {
        // CAREFUL! the hash algorithm has to be same as the one used in 47_create_new_materials.sql
        return DigestUtils.sha256Hex(map.entrySet().stream()
            .map(criteria -> criteria.getKey() + "=" + criteria.getValue())
            .collect(joining(FINGERPRINT_DELIMITER))
        );
    }

    protected abstract void appendCriteria(Map<String, Object> parameters);

    protected abstract void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria);

    @Override
    public void setName(final CaseInsensitiveString name) {
        this.name = name;
    }

    @Override
    public void setName(String name) {
        this.name = new CaseInsensitiveString(name);
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final void validate(ValidationContext validationContext) {
        if (name != null && !StringUtils.isBlank(CaseInsensitiveString.str(name)) && !new NameTypeValidator().isNameValid(name)) {
            errors().add(MATERIAL_NAME, NameTypeValidator.errorMessage("material", name));
        }
        validateConcreteMaterial(validationContext);
    }

    @Override
    public boolean validateTree(ValidationContext validationContext) {
        validate(validationContext);
        validateExtras(validationContext);

        return errors().isEmpty();
    }

    protected void validateExtras(ValidationContext validationContext) {
    }

    protected abstract void validateConcreteMaterial(ValidationContext validationContext);

    @Override
    public ConfigErrors errors() {
        return errors;
    }

    @Override
    public void addError(String fieldName, String message) {
        errors.add(fieldName, message);
    }

    @Override
    public void validateNameUniqueness(Map<CaseInsensitiveString, AbstractMaterialConfig> map) {
        if (CaseInsensitiveString.isBlank(getName())) {
            return;
        }
        CaseInsensitiveString currentMaterialName = getName();
        AbstractMaterialConfig materialWithSameName = map.get(currentMaterialName);
        if (materialWithSameName != null) {
            materialWithSameName.addNameConflictError();
            addNameConflictError();
            return;
        }
        map.put(currentMaterialName, this);
    }

    @Override
    public boolean isSameFlyweight(MaterialConfig other) {
        return getFingerprint().equals(other.getFingerprint());
    }

    private void addNameConflictError() {
        errors.add("materialName", String.format(
            "You have defined multiple materials called '%s'. "
                + "Material names are case-insensitive and must be unique. "
                + "Note that for dependency materials the default materialName is the name of the upstream pipeline. You can override this by setting the materialName explicitly for the upstream pipeline.",
            getDisplayName()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractMaterialConfig that = (AbstractMaterialConfig) o;

        return Objects.equals(name, that.name) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("AbstractMaterial{name=%s, type=%s}", name, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setConfigAttributes(Object attributes) {
        resetCachedIdentityAttributes();//TODO: BUG: update this after making changes to attributes, because this is thread unsafe if primed by another thread when initialization is half way through(and the returning API will use inconsistent and temprory value) --sara & jj
        Map<String, String> map = (Map<String, String>) attributes;
        if (map.containsKey(MATERIAL_NAME)) {
            String name = map.get(MATERIAL_NAME);
            this.name = StringUtils.isBlank(name) ? null : new CaseInsensitiveString(name);
        }
    }

    @Override
    public boolean isUsedInLabelTemplate(PipelineConfig pipelineConfig) {
        CaseInsensitiveString materialName = getName();
        return materialName != null && pipelineConfig.getLabelTemplate().toLowerCase().contains(String.format("${%s}", materialName.toLower()));
    }

    protected void resetCachedIdentityAttributes() {
        sqlCriteria = null;
        pipelineUniqueFingerprint = null;
    }
}
