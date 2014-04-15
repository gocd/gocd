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

package com.thoughtworks.go.domain.materials.tfs;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
* @understands
*/
@XStreamAlias("workspace-folder")
public class WorkingFolder {
    public static final String DEPTH_FULL = "full";

    public static final String TYPE_MAP = "map";

    @XStreamAsAttribute
    @XStreamAlias("server-item")
    private String serverItem;

    @XStreamAsAttribute
    @XStreamAlias("local-item")
    private String localItem;

    @XStreamAsAttribute
    private String type;

    @XStreamAsAttribute
    private String depth;

    public WorkingFolder(String serverItem, String localItem, String type, String depth) {
        this(serverItem, localItem);
        this.type = type;
        this.depth = depth;
    }

    public WorkingFolder(String projectPath, String absolutePath) {
        this.serverItem = projectPath;
        this.localItem = absolutePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkingFolder that = (WorkingFolder) o;

        if (hasDifferentAttributes(that)) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        return true;
    }

    private boolean hasDifferentAttributes(WorkingFolder that) {
        if (depth != null ? !depth.equals(that.depth) : that.depth != null) {
            return true;
        }
        if (localItem != null ? !localItem.equals(that.localItem) : that.localItem != null) {
            return true;
        }
        if (serverItem != null ? !serverItem.equals(that.serverItem) : that.serverItem != null) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = serverItem != null ? serverItem.hashCode() : 0;
        result = 31 * result + (localItem != null ? localItem.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (depth != null ? depth.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "WorkingFolder{" +
                "serverItem='" + serverItem + '\'' +
                ", localItem='" + localItem + '\'' +
                ", type='" + type + '\'' +
                ", depth='" + depth + '\'' +
                '}';
    }

    public boolean matchesLocalDir(File workingDir) throws IOException {
        return localItem.equals(workingDir.getCanonicalPath());
    }
}
