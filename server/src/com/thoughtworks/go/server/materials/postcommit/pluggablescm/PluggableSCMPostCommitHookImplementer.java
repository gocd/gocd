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

package com.thoughtworks.go.server.materials.postcommit.pluggablescm;

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookImplementer;
import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PluggableSCMPostCommitHookImplementer implements PostCommitHookImplementer {
    static final String SCM_NAME = "scm_name";

    @Override
    public Set<Material> prune(Set<Material> materials, Map params) {
        HashSet<Material> prunedCollection = new HashSet<>();
        String paramSCMName = (String) params.get(SCM_NAME);
        if (StringUtils.isNotBlank(paramSCMName)) {
            for (Material material : materials) {
                if (material instanceof PluggableSCMMaterial && paramSCMName.equalsIgnoreCase(((PluggableSCMMaterial) material).getScmConfig().getName())) {
                    prunedCollection.add(material);
                }
            }
        }
        return prunedCollection;
    }
}
