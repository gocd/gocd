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
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class BasicPipelineConfigsTest {
    private static final String PIPELINES_WITH_PERMISSION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server artifactsdir=\"other-artifacts\">\n"
            + "    <security>\n"
            + "      <roles>\n"
            + "        <role name=\"admin\" />\n"
            + "        <role name=\"mingle\" />\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + " <authorization>\n"
            + " %s "
            + "  </authorization>"
            + "    <pipeline name=\"pipeline1\" labeltemplate=\"alpha.${COUNT}\">\n"
            + "       <materials>\n"
            + "         <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    private static final String VIEW_PERMISSION = "    <view>\n"
            + "      <user>jez</user>\n"
            + "      <user>lqiao</user>\n"
            + "      <role>mingle</role>\n"
            + "    </view>\n";

    private static final String OPERATION_PERMISSION = "    <operate>\n"
            + "      <user>jez</user>\n"
            + "      <user>lqiao</user>\n"
            + "      <role>mingle</role>\n"
            + "    </operate>\n";

    @Test
    public void shouldWriteOperatePermissionForGroupCorrectly() {
        OperationConfig operationConfig = new OperationConfig(new AdminUser(new CaseInsensitiveString("jez")), new AdminUser(new CaseInsensitiveString("lqiao")), new AdminRole(
                new CaseInsensitiveString("mingle")));
        Authorization authorization = new Authorization(operationConfig);
        PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1");
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs(authorization, pipelineConfig);
        MagicalGoConfigXmlWriter xmlWriter = new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()
        );
        String xml = xmlWriter.toXmlPartial(pipelineConfigs);
        assertThat(xml, is("<pipelines>\n"
                + "  <authorization>\n"
                + "    <operate>\n"
                + "      <user>jez</user>\n"
                + "      <user>lqiao</user>\n"
                + "      <role>mingle</role>\n"
                + "    </operate>\n"
                + "  </authorization>\n"
                + "  <pipeline name=\"pipeline1\">\n"
                + "    <materials>\n"
                + "      <svn url=\"http://some/svn/url\" dest=\"svnDir\" materialName=\"http___some_svn_url\" />\n"
                + "    </materials>\n"
                + "    <stage name=\"mingle\">\n"
                + "      <jobs />\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines>"));

    }

    @Test
    public void shouldLoadOperationPermissionForPipelines() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(OPERATION_PERMISSION));
        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization(), instanceOf(Authorization.class));

        AdminsConfig actual = group.getAuthorization().getOperationConfig();

        assertion(actual);
    }

    @Test
    public void shouldLoadOperationAndViewPermissionForPipelinesNoMatterTheConfigOrder() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(OPERATION_PERMISSION + VIEW_PERMISSION));

        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization(), instanceOf(Authorization.class));

        AdminsConfig actualView = group.getAuthorization().getViewConfig();
        AdminsConfig actualOperation = group.getAuthorization().getOperationConfig();

        assertion(actualView);
        assertion(actualOperation);
    }

    @Test
    public void shouldLoadViewAndOperationPermissionForPipelinesNoMatterTheConfigOrder() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(configureAuthorization(VIEW_PERMISSION + OPERATION_PERMISSION));

        PipelineConfigs group = cruiseConfig.getGroups().first();

        assertThat(group.getAuthorization(), instanceOf(Authorization.class));

        AdminsConfig actualView = group.getAuthorization().getViewConfig();
        AdminsConfig actualOperation = group.getAuthorization().getOperationConfig();

        assertion(actualView);
        assertion(actualOperation);
    }

    private void assertion(AdminsConfig actualView) {
        assertThat(actualView, hasItem((Admin) new AdminUser(new CaseInsensitiveString("jez"))));
        assertThat(actualView, hasItem((Admin) new AdminUser(new CaseInsensitiveString("lqiao"))));
        assertThat(actualView, hasItem((Admin) new AdminRole(new CaseInsensitiveString("mingle"))));
    }

    private String configureAuthorization(String permission) {
        return String.format(PIPELINES_WITH_PERMISSION, permission);
    }
}
