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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

/**
 * @understands persistent material
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
        this.workspace = workspace;
        this.projectPath = projectPath;
        this.domain = domain;
        this.configuration = configuration;
        this.fingerprint = toOldMaterial(null, null, null).getFingerprint();
    }

    protected boolean getUseTickets() {
        return useTickets == null ? false : useTickets;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MaterialInstance)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MaterialInstance that = (MaterialInstance) o;

        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        if (checkExternals != null ? !checkExternals.equals(that.checkExternals) : that.checkExternals != null) {
            return false;
        }
        if (domain != null ? !domain.equals(that.domain) : that.domain != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (projectPath != null ? !projectPath.equals(that.projectPath) : that.projectPath != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }
        if (submoduleFolder != null ? !submoduleFolder.equals(that.submoduleFolder) : that.submoduleFolder != null) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (useTickets != null ? !useTickets.equals(that.useTickets) : that.useTickets != null) {
            return false;
        }
        if (username != null ? !username.equals(that.username) : that.username != null) {
            return false;
        }
        if (view != null ? !view.equals(that.view) : that.view != null) {
            return false;
        }
        if (workspace != null ? !workspace.equals(that.workspace) : that.workspace != null) {
            return false;
        }

        return true;
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
        this.additionalDataMap = JsonHelper.safeFromJson(this.additionalData, HashMap.class);
    }

    public Map<String, String> getAdditionalDataMap() {
        return additionalDataMap == null ? new HashMap<String, String>() : additionalDataMap;
    }

    public boolean requiresUpdate(Map<String, String> additionalDataMap) {
        if (additionalDataMap == null) {
            additionalDataMap = new HashMap<>();
        }
        return !this.getAdditionalDataMap().equals(additionalDataMap);
    }
}
