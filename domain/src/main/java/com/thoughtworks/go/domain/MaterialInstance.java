/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

/**
 * persistent material
 */
public abstract class MaterialInstance extends PersistentObject {
    protected String url;
    protected String username;
    protected String pipelineName;
    protected String stageName;
    protected String view;
    protected Boolean useTickets;
    protected String branch;
    protected String submoduleFolder;
    protected String flyweightName;
    protected String fingerprint;
    protected Boolean checkExternals;
    protected String workspace;
    protected String projectPath;
    protected String domain;
    protected String configuration;
    private String additionalData;
    private Map<String, String> additionalDataMap;

    protected MaterialInstance() {
    }

    public MaterialInstance(String url, String username, String pipelineName, String stageName, String view, Boolean useTickets, String branch, String submoduleFolder, String flyweightName,
                            final Boolean checkExternals, String projectPath, String domain, String configuration) {
        bombIfNull(flyweightName, "Flyweight name cannot be null.");

        this.url = url;
        this.username = username;
        this.submoduleFolder = submoduleFolder;
        this.checkExternals = checkExternals;
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.view = view;
        this.useTickets = useTickets;
        this.branch = branch;
        this.flyweightName = flyweightName;
        this.projectPath = projectPath;
        this.domain = domain;
        this.configuration = configuration;
        this.fingerprint = toOldMaterial(null, null, null).getFingerprint();
    }

    public boolean getUseTickets() {
        return useTickets != null && useTickets;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MaterialInstance)) return false;
        if (!super.equals(o)) return false;
        MaterialInstance that = (MaterialInstance) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(username, that.username) &&
                Objects.equals(pipelineName, that.pipelineName) &&
                Objects.equals(stageName, that.stageName) &&
                Objects.equals(view, that.view) &&
                Objects.equals(useTickets, that.useTickets) &&
                Objects.equals(branch, that.branch) &&
                Objects.equals(submoduleFolder, that.submoduleFolder) &&
                Objects.equals(checkExternals, that.checkExternals) &&
                Objects.equals(workspace, that.workspace) &&
                Objects.equals(projectPath, that.projectPath) &&
                Objects.equals(domain, that.domain);
    }

    public abstract Material toOldMaterial(String name, String folder, String password);

    protected void setName(String name, AbstractMaterial material) {
        material.setName(name == null ? null : new CaseInsensitiveString(name));
    }

    public String getFlyweightName() {
        return flyweightName;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getConfiguration() {
        return configuration;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(String additionalData) {
        this.additionalData = additionalData;
        this.additionalDataMap = JsonHelper.safeFromJson(this.additionalData);
    }

    public Map<String, String> getAdditionalDataMap() {
        return additionalDataMap == null ? new HashMap<>() : additionalDataMap;
    }

    public boolean requiresUpdate(Map<String, String> additionalDataMap) {
        if (additionalDataMap == null) {
            additionalDataMap = new HashMap<>();
        }
        return !this.getAdditionalDataMap().equals(additionalDataMap);
    }

    public String getBranch() {
        return branch;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getView() {
        return view;
    }

    public Boolean getCheckExternals() {
        return checkExternals;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getDomain() {
        return domain;
    }
}
