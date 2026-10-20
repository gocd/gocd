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
package com.thoughtworks.go.config.update;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.util.ClonerFactory;

public class PartialConfigUpdateCommand implements UpdateConfigCommand {
    private static final Cloner CLONER = ClonerFactory.instance();

    private final String fingerprint;
    private final PartialConfig partial;
    private final PartialConfigResolver resolver;

    public PartialConfigUpdateCommand(final String fingerprint, final PartialConfig partial, PartialConfigResolver resolver) {
        this.fingerprint = fingerprint;
        this.partial = partial;
        this.resolver = resolver;
    }

    @Override
    public CruiseConfig update(CruiseConfig cruiseConfig) {
        if (partial != null && fingerprint != null) {
            PartialConfig config = resolver.findPartialByFingerprint(cruiseConfig, fingerprint);

            if (config != null) {
                // TODO this seems dodgy. Other than races, what if something fails below? Aren't we missing a partial?
                cruiseConfig.getPartials().remove(config);
            }

            PartialConfig cloned = CLONER.deepClone(partial);
            cruiseConfig.getPartials().add(cloned);

            if (cloned.hasErrors()) {
                return cruiseConfig;
            }

            for (PartialConfig partial : cruiseConfig.getPartials()) {
                for (EnvironmentConfig environmentConfig : partial.getEnvironments()) {
                    if (!cruiseConfig.getEnvironments().hasEnvironmentNamed(environmentConfig.name())) {
                        cruiseConfig.addEnvironment(new BasicEnvironmentConfig(environmentConfig.name()));
                    }
                }
                for (PipelineConfigs pipelineConfigs : partial.getGroups()) {
                    if (!cruiseConfig.getGroups().hasGroup(pipelineConfigs.getGroup())) {
                        cruiseConfig.getGroups().add(new BasicPipelineConfigs(pipelineConfigs.getGroup(), new Authorization()));
                    }
                }
            }
        }
        return cruiseConfig;
    }
}
