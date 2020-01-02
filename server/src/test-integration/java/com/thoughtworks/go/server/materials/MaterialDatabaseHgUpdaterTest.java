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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.TestRepo;

import java.io.IOException;

public class MaterialDatabaseHgUpdaterTest extends TestBaseForDatabaseUpdater {
    @Override
    protected Material material() {
        return new HgMaterial(testRepo.projectRepositoryUrl(), null);
    }

    @Override
    protected TestRepo repo() throws IOException {
        return new HgTestRepo(temporaryFolder);
    }

}
