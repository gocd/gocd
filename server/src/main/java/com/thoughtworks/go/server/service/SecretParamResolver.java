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
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
public class SecretParamResolver {
    private SecretsExtension secretsExtension;
    private GoConfigService goConfigService;

    @Autowired
    public SecretParamResolver(SecretsExtension secretsExtension, GoConfigService goConfigService) {
        this.secretsExtension = secretsExtension;
        this.goConfigService = goConfigService;
    }

    public void resolve(List<SecretParam> secretParams) {
        Map<SecretConfig, List<SecretParam>> collect = secretParams.stream()
                .collect(groupingBy(secretParam -> goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId())));

        collect.forEach((sc, l) -> {
            List<Secret> secrets = secretsExtension.lookupSecrets(sc.getPluginId(), sc, keys(l));
            assignValue(secrets, l);
        });
    }

    private List<String> keys(List<SecretParam> secretParams) {
        return secretParams.stream().map(SecretParam::getKey).collect(toList());
    }

    private void assignValue(List<Secret> secrets, List<SecretParam> secretParams) {
        secrets.stream().forEach(secret -> {
            List<SecretParam> params = secretParams.stream().filter(secretParam -> secret.getKey().equalsIgnoreCase(secretParam.getKey())).collect(toList());
            params.stream().forEach(p -> p.setValue(secret.getValue()));
        });
    }
}
