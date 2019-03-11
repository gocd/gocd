/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.SecretConfig;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.SecretParams;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toMap;

@Component
public class SecretParamResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecretParamResolver.class);
    private SecretsExtension secretsExtension;
    private GoConfigService goConfigService;

    @Autowired
    public SecretParamResolver(SecretsExtension secretsExtension, GoConfigService goConfigService) {
        this.secretsExtension = secretsExtension;
        this.goConfigService = goConfigService;
    }

    public void resolve(SecretParams secretParams) {
        if (secretParams == null || secretParams.isEmpty()) {
            LOGGER.debug("No secret params to resolve.");
            return;
        }

        secretParams.groupBySecretConfigId().forEach(lookupAndUpdateSecretParamsValue());
    }

    private BiConsumer<String, SecretParams> lookupAndUpdateSecretParamsValue() {
        return (secretConfigId, secretParamsToResolve) -> {
            final Map<String, SecretParam> secretParamMap = secretParamsToResolve.stream().collect(toMap(SecretParam::getKey, secretParam -> secretParam));
            final SecretConfig secretConfig = goConfigService.cruiseConfig().getSecretConfigs().find(secretConfigId);

            secretsExtension.lookupSecrets(secretConfig.getPluginId(), secretConfig, secretParamsToResolve.keys())
                    .forEach(assignValue(secretParamMap));
        };
    }

    private Consumer<Secret> assignValue(Map<String, SecretParam> secretParamMap) {
        return secret -> secretParamMap.get(secret.getKey()).setValue(secret.getValue());
    }
}
