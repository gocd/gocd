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
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.Objects;

/**
 * @understands that configuration is defined in versioned source code repository at particular revision.
 */
public class RepoConfigOrigin implements ConfigOrigin {

    private ConfigRepoConfig configRepo;
    private String revision;

    public RepoConfigOrigin()
    {
    }
    public RepoConfigOrigin(ConfigRepoConfig configRepo,String revision)
    {
        this.configRepo = configRepo;
        this.revision = revision;
    }

    @Override
    public String toString() {
        return displayName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepoConfigOrigin that = (RepoConfigOrigin) o;
        return Objects.equals(configRepo, that.configRepo) &&
                Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configRepo, revision);
    }

    @Override
    public boolean canEdit() {
        // if there will be implementation to edit repository then this
        // class should participate in providing info on how to do that
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String displayName() {
        MaterialConfig materialConfig = configRepo != null ?
                configRepo.getRepo() : null;
        String materialName = materialConfig != null ?
                    materialConfig.getDisplayName() : "NULL material";
        return String.format("%s at %s", materialName, revision);
    }

    public MaterialConfig getMaterial() {
        if(configRepo == null)
            return  null;
        return configRepo.getRepo();
    }

    public boolean isFromRevision(String revision) {
        return this.revision.equals(revision);
    }

    public void setConfigRepo(ConfigRepoConfig configRepo) {
        this.configRepo = configRepo;
    }

    public ConfigRepoConfig getConfigRepo() {
        return configRepo;
    }

    public String getRevision() {
        return revision;
    }
}
