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
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.secrets.SecretsExtension;
import com.thoughtworks.go.plugin.domain.secrets.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class SecretParamResolverTest {
    @Mock
    private SecretsExtension secretsExtension;
    @Mock
    private GoConfigService goConfigService;
    private SecretParamResolver secretParamResolver;

    @BeforeEach
    void setUp() {
        initMocks(this);

        secretParamResolver = new SecretParamResolver(secretsExtension, goConfigService);
    }

    @Test
    void shouldDoNothingWhenGivenListIsNull() {
        secretParamResolver.resolve(null);

        verifyZeroInteractions(secretsExtension);
        verifyZeroInteractions(goConfigService);
    }

    @Test
    void shouldDoNothingWhenGivenListIsEmpty() {
        secretParamResolver.resolve(new SecretParams());

        verifyZeroInteractions(secretsExtension);
        verifyZeroInteractions(goConfigService);
    }

    @Test
    void shouldResolveSecretParamsOfVariousSecretConfig() {
        final SecretParams allSecretParams = new SecretParams(
                new SecretParam("secret_config_id_1", "username"),
                new SecretParam("secret_config_id_1", "password"),
                new SecretParam("secret_config_id_2", "access_key"),
                new SecretParam("secret_config_id_2", "secret_key")
        );
        final SecretConfig fileBasedSecretConfig = new SecretConfig("secret_config_id_1", "cd.go.file");
        final SecretConfig awsBasedSecretConfig = new SecretConfig("secret_config_id_2", "cd.go.aws");
        when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(fileBasedSecretConfig, awsBasedSecretConfig));
        when(secretsExtension.lookupSecrets(fileBasedSecretConfig.getPluginId(), fileBasedSecretConfig, new HashSet<>(asList("username", "password"))))
                .thenReturn(asList(new Secret("username", "some-username"), new Secret("password", "some-password")));
        when(secretsExtension.lookupSecrets(awsBasedSecretConfig.getPluginId(), awsBasedSecretConfig, new HashSet<>(asList("access_key", "secret_key"))))
                .thenReturn(asList(new Secret("access_key", "ABCDEFGHIJ1D"), new Secret("secret_key", "xyzdfjsdlwdoasd;q")));


        assertThat(allSecretParams).hasSize(4);
        assertThat(allSecretParams.get(0).getValue()).isNull();
        assertThat(allSecretParams.get(1).getValue()).isNull();
        assertThat(allSecretParams.get(2).getValue()).isNull();
        assertThat(allSecretParams.get(3).getValue()).isNull();

        secretParamResolver.resolve(allSecretParams);

        assertThat(allSecretParams).hasSize(4);
        assertThat(allSecretParams.get(0).getValue()).isEqualTo("some-username");
        assertThat(allSecretParams.get(1).getValue()).isEqualTo("some-password");
        assertThat(allSecretParams.get(2).getValue()).isEqualTo("ABCDEFGHIJ1D");
        assertThat(allSecretParams.get(3).getValue()).isEqualTo("xyzdfjsdlwdoasd;q");
    }

    @Test
    void shouldResolveValueInAllParamsEvenIfListContainsDuplicatedSecretParams() {
        final SecretParams allSecretParams = new SecretParams(
                new SecretParam("secret_config_id_1", "username"),
                new SecretParam("secret_config_id_1", "username")
        );

        final SecretConfig fileBasedSecretConfig = new SecretConfig("secret_config_id_1", "cd.go.file");
        when(goConfigService.cruiseConfig()).thenReturn(GoConfigMother.configWithSecretConfig(fileBasedSecretConfig));
        when(secretsExtension.lookupSecrets(fileBasedSecretConfig.getPluginId(), fileBasedSecretConfig, singleton("username")))
                .thenReturn(singletonList(new Secret("username", "some-username")));

        secretParamResolver.resolve(allSecretParams);

        assertThat(allSecretParams).hasSize(2);
        assertThat(allSecretParams.get(0).getValue()).isEqualTo("some-username");
        assertThat(allSecretParams.get(1).getValue()).isEqualTo("some-username");
    }
}