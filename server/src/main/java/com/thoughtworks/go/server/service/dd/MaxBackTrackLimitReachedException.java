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


import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.util.SystemEnvironment;

import static java.lang.String.format;

/*
* Understands: Throws this exception when FainIn backtracking reaches the set limit
*/
public class MaxBackTrackLimitReachedException extends RuntimeException {
    MaxBackTrackLimitReachedException(DependencyMaterialConfig material, int maxBackTrackLimit) {
        super(format("Could not find valid fan-in revisions for dependent pipeline stage %s due to hitting the maximum backtrack " +
            "limit of %d revisions. This may mean this pipeline (or one of its upstream dependencies) runs & passes on matching " +
            "revisions far less often than some of its upstream dependencies. Use its VSM to review the upstream " +
            "dependencies, their run & pass history, include/exclude rules and the revisions involved to understand why " +
            "this may be the case, or consider carefully increasing the system property %s.",
            material.getPipelineStageName(), maxBackTrackLimit, SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT.propertyName()));
    }
}
