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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.domain.BaseCollection;

/**
 * @understands configuration has multiple origins
 */
public class MergeConfigOrigin extends BaseCollection<ConfigOrigin> implements ConfigOrigin {

    public MergeConfigOrigin(ConfigOrigin... origins)
    {
        for(ConfigOrigin part : origins)
        {
            this.add(part);
        }
    }

    @Override
    public boolean canEdit() {
        for(ConfigOrigin part : this)
        {
            if(part.canEdit())
                return true;
        }
        return false;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public String displayName() {
        return "TODO merge names";
    }
}
