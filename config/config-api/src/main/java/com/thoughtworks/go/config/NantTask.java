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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.TaskProperty;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigTag("nant")
public class NantTask extends BuildTask {
    @ConfigAttribute(value = "nantpath", allowNull = true)
    private String nantPath;
    private static final String NANT_EXECUTABLE = "nant";
    public static final String NANT_PATH = "nantPath";
    public static final String TYPE = "nant";

    public NantTask() { }

    @Override
    public String arguments() {
        ArrayList<String> args = new ArrayList<>();
        if (buildFile != null) {
            args.add("-buildfile:\"" + FilenameUtils.separatorsToUnix(buildFile) + "\"");
        }

        if (target != null) {
            args.add(target);
        }

        return StringUtils.join(args, " ");
    }

    @Override
    public String getTaskType() {
        return "nant";
    }

    @Override
    public String getTypeForDisplay() {
        return "NAnt";
    }

    @Override
    public String command() {
        String command = NANT_EXECUTABLE;
        if (nantPath != null) {
            command = new File(nantPath, NANT_EXECUTABLE).getPath();
        }
        return command;
    }

    public void setNantPath(String path) {
        this.nantPath = path;
    }

    public String getNantPath() {
        return nantPath;
    }

    @Override
    protected void setBuildTaskConfigAttributes(Map attributeMap) {
        nantPath = inferValueFromMap(attributeMap, NANT_PATH);
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        List<TaskProperty> list = super.getPropertiesForDisplay();
        if (nantPath != null) {
            list.add(new TaskProperty("Nant Path", nantPath));
        }
        return list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NantTask)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        NantTask nantTask = (NantTask) o;

        return nantPath != null ? nantPath.equals(nantTask.nantPath) : nantTask.nantPath == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nantPath != null ? nantPath.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
