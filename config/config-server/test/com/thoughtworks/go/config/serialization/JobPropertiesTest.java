/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.BuildPlanMother;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static com.thoughtworks.go.helper.ConfigFileFixture.ONE_PIPELINE;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JobPropertiesTest {
    private MagicalGoConfigXmlLoader loader;
    private MagicalGoConfigXmlWriter writer;
    private ConfigCache configCache = new ConfigCache();

    @Before
    public void setUp() throws Exception {
        loader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        writer = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
    }

    @Test
    public void shouldLoadJobProperties() throws Exception {
        String jobXml = "<job name=\"dev\">\n"
                + "  <properties>\n"
                + "    <property name=\"coverage\" src=\"reports/emma.html\" xpath=\"//coverage/class\" />\n"
                + "    <property name=\"prop2\" src=\"test.xml\" xpath=\"//value\" />\n"
                + "  </properties>\n"
                + "</job>";
        CruiseConfig config = loader.loadConfigHolder(ConfigFileFixture.withJob(jobXml)).configForEdit;
        JobConfig jobConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).get(1).allBuildPlans().first();
        assertThat(jobConfig.getProperties().first(),
                is(new ArtifactPropertiesGenerator("coverage", "reports/emma.html", "//coverage/class")));
        assertThat(jobConfig.getProperties().get(1),
                is(new ArtifactPropertiesGenerator("prop2", "test.xml", "//value")));
    }

    @Test
    public void shouldWriteJobProperties() throws Exception {
        String jobXml = "<job name=\"dev\">\n"
                + "  <properties>\n"
                + "    <property name=\"coverage\" src=\"reports/emma.html\" xpath=\"//coverage/class\" />\n"
                + "    <property name=\"prop2\" src=\"test.xml\" xpath=\"//value\" />\n"
                + "  </properties>\n"
                + "</job>";
        CruiseConfig config = loader.loadConfigHolder(ConfigFileFixture.withJob(jobXml)).configForEdit;
        JobConfig jobConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).get(1).allBuildPlans().first();
        assertThat(writer.toXmlPartial(jobConfig), is(jobXml));
    }

    @Test
    public void shouldNotLoadDuplicateJobProperties() throws Exception {
        String jobXml = "<job name=\"dev\">\n"
                + "<properties>\n"
                + "<property name=\"coverage\" src=\"reports/emma.html\" xpath=\"//coverage/class\" />\n"
                + "<property name=\"coverage\" src=\"test.xml\" xpath=\"//value\" />\n"
                + "</properties>\n"
                + "</job>";
        try {
            loadJobConfig(jobXml);
            fail("should not define two job properties with same name");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Duplicate unique value"));
        }
    }

    @Test
    public void shouldNotWriteDuplicateJobProperties() throws Exception {
        ArtifactPropertiesGenerator artifactProperties = new ArtifactPropertiesGenerator("coverage",
                "reports/emma.html", "//coverage/class");
        CruiseConfig cruiseConfig = cruiseConfigWithProperties(artifactProperties, artifactProperties);
        try {
            writer.write(cruiseConfig, new ByteArrayOutputStream(), false);
            fail("should not write two job properties with same name");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Duplicate unique value"));
        }
    }

    @Test
    public void shouldNotAllowEmptyJobProperties() throws Exception {
        String jobXml = "<job name=\"dev\">\n"
                + "<properties>\n"
                + "</properties>\n"
                + "</job>";
        try {
            loadJobConfig(jobXml);
            fail("should not allow empty job properties");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("One of '{property}' is expected"));
        }
    }

    @Test
    public void shouldNotWriteEmptyJobProperties() throws Exception {
        CruiseConfig cruiseConfig = cruiseConfigWithProperties();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.write(cruiseConfig, buffer, false);
        assertThat(new String(buffer.toByteArray()), not(containsString("properties")));
    }

    private CruiseConfig cruiseConfigWithProperties(ArtifactPropertiesGenerator... artifactPropertieses)
            throws Exception {
        CruiseConfig cruiseConfig = loader.loadConfigHolder(ONE_PIPELINE).configForEdit;
        JobConfig jobConfig = BuildPlanMother.withArtifactPropertiesGenerator(artifactPropertieses);
        cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).first().allBuildPlans().add(jobConfig);
        return cruiseConfig;
    }

    private JobConfig loadJobConfig(String jobXml) throws Exception {
               CruiseConfig config = loader.loadConfigHolder(ConfigFileFixture.withJob(jobXml)).configForEdit;
        return config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).first().allBuildPlans().first();
    }

}



