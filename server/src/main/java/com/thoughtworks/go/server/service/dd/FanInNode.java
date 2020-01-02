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
package com.thoughtworks.go.server.service.dd;

import java.util.HashSet;
import java.util.Set;

import com.thoughtworks.go.domain.materials.MaterialConfig;

public abstract class FanInNode {
    MaterialConfig materialConfig;
    public Set<DependencyFanInNode> parents = new HashSet<>();

    protected FanInNode(MaterialConfig materialConfig) {
        this.materialConfig = materialConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FanInNode fanInNode = (FanInNode) o;
        if (materialConfig != null ? !materialConfig.getFingerprint().equals(fanInNode.materialConfig.getFingerprint()) : fanInNode.materialConfig != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return materialConfig != null ? materialConfig.getFingerprint().hashCode() : 0;
    }
}
