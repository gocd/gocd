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

package com.thoughtworks.go.server.service.materials;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.ValidationBean;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MaterialConnectivityService {
    private static final Map<Class, MaterialConnectivityChecker> connectivityCheckerMap = new HashMap<>();
    private MaterialConfigConverter materialConfigConverter;

    @Autowired
    public MaterialConnectivityService(MaterialConfigConverter materialConfigConverter) {
        this.materialConfigConverter = materialConfigConverter;
        populateCheckers();
    }


    public ValidationBean checkConnection(MaterialConfig materialConfig, final SubprocessExecutionContext execCtx) {
        Material material = materialConfigConverter.toMaterial(materialConfig);
        return getCheckerImplementation(material).checkConnection(material, execCtx);
    }

    private void populateCheckers() {
        connectivityCheckerMap.put(GitMaterial.class, new GitMaterialConnectivityChecker());
        connectivityCheckerMap.put(SvnMaterial.class, new SvnMaterialConnectivityChecker());
        connectivityCheckerMap.put(HgMaterial.class, new HgMaterialConnectivityChecker());
        connectivityCheckerMap.put(TfsMaterial.class, new TfsMaterialConnectivityChecker());
        connectivityCheckerMap.put(P4Material.class, new P4MaterialConnectivityChecker());
        connectivityCheckerMap.put(DependencyMaterial.class, new DependencyMaterialConnectivityChecker());
    }

    private MaterialConnectivityChecker getCheckerImplementation(Material material) {
        MaterialConnectivityChecker checker = connectivityCheckerMap.get(getMaterialClass(material));
        return checker == null ? new NoOpMaterialConnectivityChecker() : checker;
    }

    Class<? extends Material> getMaterialClass(Material material) {
        return material.getClass();
    }

    private final class GitMaterialConnectivityChecker implements MaterialConnectivityChecker<GitMaterial> {
        @Override
        public ValidationBean checkConnection(GitMaterial material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class HgMaterialConnectivityChecker implements MaterialConnectivityChecker<HgMaterial> {
        @Override
        public ValidationBean checkConnection(HgMaterial material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class TfsMaterialConnectivityChecker implements MaterialConnectivityChecker<TfsMaterial> {
        @Override
        public ValidationBean checkConnection(TfsMaterial material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class SvnMaterialConnectivityChecker implements MaterialConnectivityChecker<SvnMaterial> {
        @Override
        public ValidationBean checkConnection(SvnMaterial material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class P4MaterialConnectivityChecker implements MaterialConnectivityChecker<P4Material> {
        @Override
        public ValidationBean checkConnection(P4Material material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class DependencyMaterialConnectivityChecker implements MaterialConnectivityChecker<DependencyMaterial> {
        @Override
        public ValidationBean checkConnection(DependencyMaterial material, SubprocessExecutionContext executionContext) {
            return material.checkConnection(executionContext);
        }
    }

    private final class NoOpMaterialConnectivityChecker implements MaterialConnectivityChecker<GitMaterial> {
        @Override
        public ValidationBean checkConnection(GitMaterial material, SubprocessExecutionContext executionContext) {
            return ValidationBean.valid();
        }
    }
}
