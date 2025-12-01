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
package com.thoughtworks.go.server.service.dd;

import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;

import java.util.*;
import java.util.stream.Collectors;

abstract class FanInNode<T extends MaterialConfig> {
    final T materialConfig;
    final Set<DependencyFanInNode> parents = new HashSet<>();

    protected FanInNode(T materialConfig) {
        this.materialConfig = materialConfig;
    }

    static FanInNode<?> create(MaterialConfig material) {
        if (material instanceof ScmMaterialConfig || material instanceof PackageMaterialConfig || material instanceof PluggableSCMMaterialConfig) {
            return new RootFanInNode(material);
        } else if (material instanceof DependencyMaterialConfig depMaterial) {
            return new DependencyFanInNode(depMaterial);
        }
        throw new RuntimeException("Not a valid material type");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FanInNode<?> fanInNode = (FanInNode<?>) o;
        return (materialConfig != null) ? materialConfig.getFingerprint().equals(fanInNode.materialConfig.getFingerprint()) : (fanInNode.materialConfig == null);
    }

    @Override
    public int hashCode() {
        return materialConfig != null ? materialConfig.getFingerprint().hashCode() : 0;
    }

    record ByType(List<RootFanInNode> scm, List<DependencyFanInNode> dep) {
        @SuppressWarnings("unchecked")
        static ByType from(Collection<? extends FanInNode<?>> nodes) {
            Map<Boolean, ? extends List<? extends FanInNode<?>>> result = nodes
                .stream()
                .collect(Collectors.partitioningBy(child -> child instanceof RootFanInNode, Collectors.toUnmodifiableList()));
            return new ByType((List<RootFanInNode>) result.get(Boolean.TRUE), (List<DependencyFanInNode>) result.get(Boolean.FALSE));
        }

        boolean isAllScm() {
            return dep.isEmpty();
        }
    }
}
