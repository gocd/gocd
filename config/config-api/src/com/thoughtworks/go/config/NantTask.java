/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.TaskProperty;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.File;
import java.util.List;
import java.util.Map;

@ConfigTag("nant")
public class NantTask extends BuildTask {
    @ConfigAttribute(value = "nantpath", optional = true, allowNull = true)
    private String nantPath;
    private static final String NANT_EXECUTABLE = "nant";
    public static final String NANT_PATH = "nantPath";
    private final String NANT = "NAnt";
    public static final String TYPE="nant";

    public NantTask() { }

    public String arguments() {
        StringBuffer buffer = new StringBuffer();
        if (buildFile != null) {
            buffer.append("-buildfile:\"").append(FileUtil.normalizePath(buildFile)).append('\"');
        }
        if (target != null) {
            buffer.append(" ").append(target);
        }
        return buffer.toString();
    }

    @Override
    public String getTaskType() {
        return "nant";
    }

    public String getTypeForDisplay() {
        return NANT;
    }

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

    protected void setBuildTaskConfigAttributes(Map attributeMap) {
        nantPath = inferValueFromMap(attributeMap, NANT_PATH);
    }

    @Override
    public List<TaskProperty> getPropertiesForDisplay() {
        List<TaskProperty> list = super.getPropertiesForDisplay();
        if (nantPath != null) {
            list.add(new TaskProperty(NANT_PATH, nantPath));
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

        if (nantPath != null ? !nantPath.equals(nantTask.nantPath) : nantTask.nantPath != null) {
            return false;
        }

        return true;
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
