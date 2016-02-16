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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.GoConstants;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MagicalGoConfigXmlLoaderFixture {
    public static void assertNotValid(String message, String xmlMaterials) {
        try {
            toMaterials(xmlMaterials);
            fail("Should not be valid");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString(message));
        }
    }

    public static void assertValid(String xmlMaterials) throws Exception {
        toMaterials(xmlMaterials);
    }

    public static MaterialConfigs toMaterials(String materials)
            throws Exception {

        ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

        MagicalGoConfigXmlLoader xmlLoader = new MagicalGoConfigXmlLoader(new ConfigCache(), registry
        );
        String pipelineXmlPartial =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<cruise "
                        + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "        xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" "
                        + "        schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
                        + "  <server artifactsdir=\"logs\">\n"
                        + "  </server>\n"
                        + "  <pipelines>"
                        + "<pipeline name=\"pipeline\">\n"
                        + materials
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <artifact src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n"
                        + "</pipelines>"
                        + "</cruise>\n";
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(FileUtil.readToEnd(toInputStream(pipelineXmlPartial))).config;
        return cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline")).materialConfigs();
    }

}
