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
package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

public class DependencyMaterialRevision implements Revision {
    private final String pipelineName;
    private final int pipelineCounter;
    private final String pipelineLabel;
    private final String stageName;
    private final int stageCounter;

    public static DependencyMaterialRevision create(String stringRevision, String pipelineLabel) {
        String[] strings = stringRevision.split("/");
        return DependencyMaterialRevision.create(strings[0], Integer.parseInt(strings[1]), pipelineLabel, strings[2], Integer.parseInt(strings[3]));
    }

    @VisibleForTesting
    public static DependencyMaterialRevision create(String pipelineName, int pipelineCounter, String pipelineLabel, String stageName, int stageCounter) {
        return new DependencyMaterialRevision(pipelineName, pipelineCounter, pipelineLabel, stageName, stageCounter);
    }

    private DependencyMaterialRevision(String pipelineName, int pipelineCounter, String pipelineLabel, String stageName, int stageCounter) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
    }

    @Override
    public String getRevision() {
        return String.format("%s/%s/%s/%s", pipelineName, pipelineCounter, stageName, stageCounter);
    }

    public void putRevision(Map<String, String> map) {
        map.put(pipelineName, pipelineLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependencyMaterialRevision that = (DependencyMaterialRevision) o;

        return stageCounter == that.stageCounter &&
            pipelineCounter == that.pipelineCounter &&
            Objects.equals(pipelineLabel, that.pipelineLabel) &&
            Objects.equals(pipelineName, that.pipelineName) &&
            Objects.equals(stageName, that.stageName);
    }

    @Override
    public int hashCode() {
        int result;
        result = (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + pipelineCounter;
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + stageCounter;
        return result;
    }

    @Override
    public String toString() {
        return String.format("DependencyMaterialRevision[%s] [pipelineLabel = %s]", getRevision(), getPipelineLabel());
    }

    @Override
    public String getRevisionUrl() {
        return "pipelines/" + this.getRevision();
    }

    @Override
    public boolean isRealRevision() {
        return true;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public int getPipelineCounter() {
        return pipelineCounter;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public int getStageCounter() {
        return stageCounter;
    }

    @TestOnly
    public MaterialRevision convert(Material material, Date modifiedTime) {
        List<Modification> modifications = new ArrayList<>();
        modifications.add(new Modification(modifiedTime, getRevision(), getPipelineLabel(), null));
        return new MaterialRevision(material, modifications);
    }

    public String getStageName() {
        return stageName;
    }

}
