/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class AgentBuildingInfo implements Serializable {
    @Expose
    private final String buildingInfo;
    @Expose
    private final String buildLocator;
    public static final AgentBuildingInfo NOT_BUILDING = new AgentBuildingInfo("", "");

    public AgentBuildingInfo(String buildingInfo, String buildLocator) {
        bombIfNull(buildLocator, "Build locator cannot be null");
        bombIfNull(buildingInfo, "Building info cannot be null");
        this.buildingInfo = buildingInfo;
        this.buildLocator = buildLocator;
    }

    public String getBuildingInfo() {
        return buildingInfo;
    }

    public String toString() {
        return String.format("AgentBuildingInfo[%s, %s]", buildingInfo, buildLocator);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AgentBuildingInfo that = (AgentBuildingInfo) o;

        if (buildLocator != null ? !buildLocator.equals(that.buildLocator) : that.buildLocator != null) {
            return false;
        }
        if (buildingInfo != null ? !buildingInfo.equals(that.buildingInfo) : that.buildingInfo != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (buildingInfo != null ? buildingInfo.hashCode() : 0);
        result = 31 * result + (buildLocator != null ? buildLocator.hashCode() : 0);
        return result;
    }

    public String getPipelineName() {
        if(isBuilding()) {
            return buildLocator.split("/")[0];
        }
        return null;
    }

    public String getJobName() {
        if (isBuilding()) {
            try {
                return buildLocator.split("/")[4];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    }

    public String getStageName() {
        if(isBuilding()) {
            try {
                return buildLocator.split("/")[2];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
        return null;
    }

    public boolean isBuilding() {
        return !buildingInfo.equals("");
    }

    public String getBuildLocator() {
        return buildLocator;
    }
}
