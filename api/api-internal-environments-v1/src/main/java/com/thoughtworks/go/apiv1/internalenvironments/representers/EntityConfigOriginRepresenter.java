/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.internalenvironments.representers;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.apiv1.internalenvironments.representers.configorigin.ConfigRepoOriginRepresenter;
import com.thoughtworks.go.apiv1.internalenvironments.representers.configorigin.ConfigXmlOriginRepresenter;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;

public class EntityConfigOriginRepresenter {
    public static void toJSON(OutputWriter outputWriter, ConfigOrigin origin) {
        if (origin instanceof FileConfigOrigin || origin == null) {
            jsoninzeConfigXmlOrigin((FileConfigOrigin) origin, outputWriter);
        } else if (origin instanceof RepoConfigOrigin) {
            jsonizeConfigRepoOrigin((RepoConfigOrigin) origin, outputWriter);
        }
    }

    private static void jsoninzeConfigXmlOrigin(FileConfigOrigin origin, OutputWriter originWriter) {
        ConfigXmlOriginRepresenter.toJSON(originWriter, origin);
    }

    private static void jsonizeConfigRepoOrigin(RepoConfigOrigin origin, OutputWriter originWriter) {
        ConfigRepoOriginRepresenter.toJSON(originWriter, origin);
    }
}
