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
package com.thoughtworks.go.server.dao;

import java.util.List;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.util.Pagination;

public interface DependencyMaterialSourceDao {
    List<Modification> getPassedStagesByName(DependencyMaterial dependencyMaterial, Pagination pagination);

    List<Modification> getPassedStagesAfter(final String lastRevision, DependencyMaterial limit, Pagination offset);
}
