/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class GoConfigHolderTest {
    @Test
    public void shouldStoreMd5Checksum() throws Exception {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder = new GoConfigHolder(config, config, config, partials(config.getConfigRepos().first()));

        assertThat(holder.getChecksum().md5SumOfConfigForEdit, is("main-config-md5"));
        assertThat(holder.getChecksum().md5SumOfPartials, is(md5(partials(config.getConfigRepos().first()))));
    }

    @Test
    public void shouldStoreMd5ChecksumWhenThereIsNotConfigRepo() throws Exception {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        config.getConfigRepos().clear();
        GoConfigHolder holder = new GoConfigHolder(config, config, null, null);

        assertThat(holder.getChecksum().md5SumOfConfigForEdit, is("main-config-md5"));
        assertThat(holder.getChecksum().md5SumOfPartials, is(nullValue()));
    }

    @Test
    public void updatingMergedConfigShouldUpdateChecksum() throws Exception {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder = new GoConfigHolder(config, config, null, null);

        assertThat(holder.getChecksum().md5SumOfConfigForEdit, is("main-config-md5"));
        assertThat(holder.getChecksum().md5SumOfPartials, is(nullValue()));

        holder.setMergedConfigForEdit(new BasicCruiseConfig(), partials(config.getConfigRepos().first()));
        assertThat(holder.getChecksum().md5SumOfConfigForEdit, is("main-config-md5"));
        assertThat(holder.getChecksum().md5SumOfPartials, is(md5(partials(config.getConfigRepos().first()))));
    }

    @Test
    public void shouldGenerateSameChecksumIfTheObjectsHaventChanged() {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder1 = new GoConfigHolder(config, config, config, partials(config.getConfigRepos().first()));
        GoConfigHolder holder2 = new GoConfigHolder(config, config, config, partials(config.getConfigRepos().first()));

        assertThat(holder1.getChecksum().equals(holder2.getChecksum()), is(true));
    }

    @Test
    public void checksumsShouldDifferIfThePartialsHaveChanged() {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder1 = new GoConfigHolder(config, config, config,
                Arrays.asList(PartialConfigMother.withPipeline("p1"), PartialConfigMother.withEnvironment("e1")));
        GoConfigHolder holder2 = new GoConfigHolder(config, config, config,
                Arrays.asList(PartialConfigMother.withPipeline("p2"), PartialConfigMother.withEnvironment("e1")));

        assertThat(holder1.getChecksum().equals(holder2.getChecksum()), is(false));
    }

    @Test
    public void checksumsShouldDifferIfTheConfigRepoHasChanged() {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder1 = new GoConfigHolder(config, config, config, partials(new ConfigRepoConfig()));
        GoConfigHolder holder2 = new GoConfigHolder(config, config, config, partials(new ConfigRepoConfig()));

        assertThat(holder1.getChecksum().equals(holder2.getChecksum()), is(false));
    }

    @Test
    public void checksumsShouldDifferIfTheMainConfigHasChanged() {
        BasicCruiseConfig config = configWithMd5("main-config-md5");
        GoConfigHolder holder1 = new GoConfigHolder(config, config, config,
                Arrays.asList(PartialConfigMother.withPipeline("p1"), PartialConfigMother.withEnvironment("e1")));
        BasicCruiseConfig config2 = configWithMd5("main-config-md5");
        GoConfigHolder holder2 = new GoConfigHolder(config2, config2, config2,
                Arrays.asList(PartialConfigMother.withPipeline("p1"), PartialConfigMother.withEnvironment("e2")));

        assertThat(holder1.getChecksum().equals(holder2.getChecksum()), is(false));
    }

    private BasicCruiseConfig configWithMd5(String md5) {
        BasicCruiseConfig basicCruiseConfig = new BasicCruiseConfig();
        ReflectionUtil.setField(basicCruiseConfig, "md5", md5);
        basicCruiseConfig.getConfigRepos().add(new ConfigRepoConfig());
        return basicCruiseConfig;
    }

    private List<PartialConfig> partials(ConfigRepoConfig configRepo) {
        PartialConfig environmentWithEnvVars = PartialConfigMother.withEnvironment("e2");
        environmentWithEnvVars.getEnvironments().first().addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "plain", "value", false));
        environmentWithEnvVars.getEnvironments().first().addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "secure", "encrypted", true));
        return Arrays.asList(PartialConfigMother.withPipeline("p1"),
                PartialConfigMother.withEnvironment("e1"),
                PartialConfigMother.pipelineWithDependencyMaterial("downstream", PipelineConfigMother.pipelineConfig("upstream"), new RepoConfigOrigin(configRepo, "rev1")),
                environmentWithEnvVars,
                PartialConfigMother.withPipelineAssociatedWithTemplate("p2", "t1", new RepoConfigOrigin(configRepo, "rev1")),
                PartialConfigMother.withPipelineInGroup("p3", "g1"),
                PartialConfigMother.withFullBlownPipeline("p4")
        );
    }

    private String md5(Object obj) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
                objectOutputStream.writeObject(obj);
                return md5Hex(byteArrayOutputStream.toByteArray());
            }
        }
    }

}