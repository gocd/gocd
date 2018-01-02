/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.MaterialAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyMaterialAgent implements MaterialAgent {
    private static final Logger LOG = LoggerFactory.getLogger(DependencyMaterialAgent.class);
    private MaterialRevision revision;

    public DependencyMaterialAgent(MaterialRevision revision) {
        this.revision = revision;
    }

    public void prepare() {
        //currently this method do nothing
    }
}
