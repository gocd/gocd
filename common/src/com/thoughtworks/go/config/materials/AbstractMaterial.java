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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.StringUtil;

import java.util.*;

/**
 * @understands material configuration
 */
public abstract class AbstractMaterial extends PersistentObject implements Material {
    /**
     * CAREFUL!, this should be the same as the one used in migration 47_create_new_materials.sql
     */
    public static final String FINGERPRINT_DELIMITER = "<|>";
    public static final String SQL_CRITERIA_TYPE = "type";
    private static final int TRUNCATED_NAME_MAX_LENGTH = 20;

    protected CaseInsensitiveString name;
    protected String type;
    private Map<String, Object> sqlCriteria;
    private Map<String, Object> attributesForXml;
    private String pipelineUniqueFingerprint;
    protected String fingerprint;

    public AbstractMaterial(String typeName) {
        type = typeName;
    }

    public CaseInsensitiveString getName() {
        return name;
    }

    public final Map<String, Object> getSqlCriteria() {
        if (sqlCriteria == null) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            appendCriteria(map);
            sqlCriteria = Collections.unmodifiableMap(map);
        }
        return sqlCriteria;
    }

    public final Map<String, Object> getAttributesForXml() {
        if (attributesForXml == null) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            appendAttributes(map);
            attributesForXml = Collections.unmodifiableMap(map);
        }
        return attributesForXml;
    }

    public String getFingerprint() {
        if (fingerprint == null) {
            fingerprint = generateFingerprintFromCriteria(getSqlCriteria());
        }
        return fingerprint;
    }

    public String getPipelineUniqueFingerprint() {
        if (pipelineUniqueFingerprint == null) {
            Map<String, Object> basicCriteria = new LinkedHashMap<>(getSqlCriteria());
            appendPipelineUniqueCriteria(basicCriteria);
            pipelineUniqueFingerprint = generateFingerprintFromCriteria(basicCriteria);
        }
        return pipelineUniqueFingerprint;
    }

    private String generateFingerprintFromCriteria(Map<String, Object> sqlCriteria) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Object> criteria : sqlCriteria.entrySet()) {
            list.add(new StringBuilder().append(criteria.getKey()).append("=").append(criteria.getValue()).toString());
        }
        String fingerprint = ListUtil.join(list, FINGERPRINT_DELIMITER);
        // CAREFUL! the hash algorithm has to be same as the one used in 47_create_new_materials.sql
        return CachedDigestUtils.sha256Hex(fingerprint);
    }

    public String getTruncatedDisplayName() {
        String displayName = getDisplayName();
        if (displayName.length() > TRUNCATED_NAME_MAX_LENGTH) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(displayName.substring(0, TRUNCATED_NAME_MAX_LENGTH / 2));
            buffer.append("...");
            buffer.append(displayName.substring(displayName.length() - TRUNCATED_NAME_MAX_LENGTH / 2));
            displayName = buffer.toString();
        }
        return displayName;
    }

    protected abstract void appendCriteria(Map<String, Object> parameters);

    protected abstract void appendAttributes(Map<String,Object> parameters);

    protected abstract void appendPipelineUniqueCriteria(Map<String, Object> basicCriteria);

    public void setName(final CaseInsensitiveString name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getShortRevision(String revision) {
        return revision;
    }

    public boolean isSameFlyweight(Material other) {
        return getFingerprint().equals(other.getFingerprint());
    }

    @Override
    public boolean hasSameFingerprint(MaterialConfig materialConfig) {
        return getFingerprint().equals(materialConfig.getFingerprint());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractMaterial that = (AbstractMaterial) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return String.format("AbstractMaterial{name=%s, type=%s}", name, type);
    }

    protected void resetCachedIdentityAttributes() {
        sqlCriteria = null;
        attributesForXml = null;
        pipelineUniqueFingerprint = null;
    }

    @Override
    public MaterialConfig config() {
        throw new RuntimeException("You need to implement this");
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        throw new RuntimeException("You need to implement this");
    }

    protected boolean hasDestinationFolder() {
        return !StringUtil.isBlank(getFolder());
    }

    public boolean supportsDestinationFolder() {
        return false;
    }

    @Override
    public void updateFromConfig(MaterialConfig materialConfig) {
        if(materialConfig instanceof PasswordAwareMaterial) {
            PasswordAwareMaterial passwordConfig = (PasswordAwareMaterial) materialConfig;
            ((PasswordAwareMaterial) this).setPassword(passwordConfig.getPassword());
        }
    }
}
