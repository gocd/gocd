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

package com.thoughtworks.go.domain.materials.dependency;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import org.apache.commons.lang.StringUtils;

public class DependencyMaterialRevision implements Revision {
    private String pipelineName;
    private final String pipelineCounter;
    private String pipelineLabel;
    private String stageName;
    private int stageCounter;

    public static DependencyMaterialRevision create(String stringRevision, String pipelineLabel) {
        String[] strings = stringRevision.split("/");
        return DependencyMaterialRevision.create(strings[0], Integer.parseInt(strings[1]), pipelineLabel, strings[2], Integer.valueOf(strings[3]));
    }

    private DependencyMaterialRevision(String pipelineName, String pipelineCounter, String pipelineLabel, String stageName, int stageCounter) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
    }

    public static DependencyMaterialRevision create(String pipelineName, Integer pipelineCounter, String pipelineLabel, String stageName, int stageCounter) {
        if (pipelineCounter == null) {
            throw new IllegalArgumentException("Dependency material revision can not be created without pipeline counter.");
        }
        return new DependencyMaterialRevision(pipelineName, pipelineCounter.toString(), pipelineLabel, stageName, stageCounter);
    }

    public String getRevision() {
        return String.format("%s/%s/%s/%s", pipelineName, pipelineCounter, stageName, stageCounter);
    }

    public void putRevision(Map<String, String> map) {
        map.put(pipelineName, pipelineLabel);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DependencyMaterialRevision that = (DependencyMaterialRevision) o;

        if (stageCounter != that.stageCounter) {
            return false;
        }
        if (pipelineCounter != null ? !pipelineCounter.equals(that.pipelineCounter) : that.pipelineCounter != null) {
            return false;
        }
        if (pipelineLabel != null ? !pipelineLabel.equals(that.pipelineLabel) : that.pipelineLabel != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (pipelineCounter != null ? pipelineCounter.hashCode() : 0);
        result = 31 * result + (pipelineLabel != null ? pipelineLabel.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + stageCounter;
        return result;
    }

    public String toString() {
        return String.format("DependencyMaterialRevision[%s] [pipelineLabel = %s]", getRevision(), getPipelineLabel());
    }

    public String getRevisionUrl() {
        return "pipelines/" + this.getRevision();
    }

    public boolean isRealRevision() {
        return true;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public int getStageCounter() {
        return stageCounter;
    }

    /**
     * @deprecated used only in tests
     */
    public MaterialRevision convert(Material material, Date modifiedTime) {
        ArrayList<Modification> modifications = new ArrayList<>();
        modifications.add(new Modification(modifiedTime, getRevision(), getPipelineLabel(), null
        ));
        return new MaterialRevision(material, modifications);
    }

    public Integer getPipelineCounter() {
        return StringUtils.isEmpty(pipelineCounter) ? null : Integer.parseInt(pipelineCounter);
    }

    public String getStageName() {
        return stageName;
    }

}
