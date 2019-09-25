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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.label.PipelineLabel;

import java.util.Arrays;

import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;

public final class ConfigFileFixture {

    public static final String BASIC_CONFIG =
            "<cruise schemaVersion=\""
                    + CONFIG_SCHEMA_VERSION + "\">\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url =\"svnurl\"/>"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <artifact type=\"build\" src='from' dest='to'/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static String configWithEnvironments(String environmentsBlock, int configSchemaVersion) {
        return "<cruise schemaVersion=\"" + configSchemaVersion + "\">\n"
                + "<server artifactsdir='artifactsDir' >"
                + "</server>"
                + "<pipelines group='group1'>\n"
                + "<pipeline name='pipeline1'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='mingle'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + environmentsBlock
                + "</cruise>";
    }


    public static String configWithTemplates(String template) {
        return "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='mingle'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + template
                + "</cruise>";
    }

    public static String configWithConfigRepos(String configReposBlock) {
        return "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                + CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + configReposBlock
                + "<pipelines>\n"
                + "<pipeline name='pipeline1'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='mingle'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"

                + "</cruise>";
    }

    public static String configWithPipeline(String pipelineBlock) {
        return configWithPipeline(pipelineBlock, CONFIG_SCHEMA_VERSION);
    }

    public static String configWithPipeline(String pipelineBlock, int schemaVersion) {
        return configWithPipelines("<pipelines>\n"
                + pipelineBlock
                + "</pipelines>\n", schemaVersion);
    }


    public static String configWithPipelines(String pipelinesBlock) {
        return configWithPipelines(pipelinesBlock, CONFIG_SCHEMA_VERSION);
    }

    public static String configWithPipelines(String pipelinesBlock, int schemaVersion) {
        return "<cruise schemaVersion='" + schemaVersion + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + pipelinesBlock
                + "</cruise>";
    }

    public static String configWithPluggableScm(String scmBlock, int schemaVersion) {
        return "<cruise schemaVersion='" + schemaVersion + "'>\n"
                + "<server artifactsdir='artifactsDir' >"
                + "</server>"
                + "<scms>"
                + scmBlock
                + "</scms>"
                + "</cruise>";
    }

    public static String config(String block, int schemaVersion) {
        return "<cruise schemaVersion='" + schemaVersion + "'>\n"
                + block
                + "</cruise>";
    }

    public static final String CONFIG_WITH_ANT_BUILDER =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' timeout='5'>\n "
                    + "        <artifacts>\n"
                    + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                    + "        </artifacts>\n"
                    + "        <tasks>"
                    + "          <ant buildfile='src/evolve.build' target='all'/>"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String CONFIG_WITH_NANT_AND_EXEC_BUILDER =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url ='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <nant nantpath='lib/apache-nant' buildfile='src/evolve.build' target='all'/>"
                    + "          <exec command='ls' workingdir='workdir' args='-la' />"
                    + "          <exec command='ls' />"
                    + "          <rake buildfile='myrake.rb' target='test' workingdir='somewhere' />"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "<pipeline name='pipeline2'>\n"
                    + "  <dependencies>"
                    + "     <depends pipeline=\"pipeline1\" stage=\"mingle\"/>"
                    + "  </dependencies>"
                    + "    <materials>\n"
                    + "      <svn url ='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <nant nantpath='lib/apache-nant' buildfile='src/evolve.build' target='all'/>"
                    + "          <exec command='ls' workingdir='workdir' args='-la' />"
                    + "          <exec command='ls' />"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String WITH_DUPLICATE_ENVIRONMENTS =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url ='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <exec command='ls' workingdir='workdir' args='-la' />"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "<environments>\n"
                    + "<environment name='foo' />\n"
                    + "<environment name='FOO' />\n"
                    + "</environments>\n"
                    + "</cruise>";

    public static final String TASKS_WITH_CONDITION =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <ant buildfile='src/evolve.build' target='all'>"
                    + "            <runif status='failed' />"
                    + "          </ant>"
                    + "          <nant buildfile='src/evolve.build' target='all'>"
                    + "            <runif status='failed' />"
                    + "            <runif status='any' />"
                    + "            <runif status='passed' />"
                    + "          </nant>"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String CONFIG_WITH_ARTIFACT_SRC =
            "<cruise schemaVersion='28'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline'>\n"
                    + "    <materials>\n"
                    + "      <svn url='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='stage'>\n"
                    + "    <jobs>\n"
                    + "      <job name='job' >\n "
                    + "         <tasks>"
                    + "             <ant buildfile='src/evolve.build' target='all' />"
                    + "         </tasks>"
                    + "         <artifacts>\n"
                    + "             <artifact src='%s'/>\n"
                    + "         </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String TASKS_WITH_ON_CANCEL =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <ant buildfile='src/evolve.build' target='all'>"
                    + "            <oncancel>"
                    + "              <exec command='kill.rb' workingdir='utils' />"
                    + "            </oncancel>"
                    + "          </ant>"
                    + "          <exec command='ls'>"
                    + "            <oncancel/>"
                    + "          </exec>"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "       <job name=\"downstream-job\">\n"
                    + "          <tasks>\n"
                    + "              <exec command=\"echo\" args=\"hello world!!\"><oncancel /></exec>\n"
                    + "          </tasks>\n"
                    + "       </job>"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String MATERIAL_WITH_NAME =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline'>\n"
                    + "    <materials>\n"
                    + "      <svn url='http://blahblah' materialName='svn' dest='svn' />\n"
                    + "      <hg url='http://blahblah' materialName='hg' dest='hg' />\n"
                    + "    </materials>\n"
                    + "  <stage name='dev'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String TASKS_WITH_ON_CANCEL_NESTED =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url='svnurl' />\n"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' >\n "
                    + "        <tasks>"
                    + "          <ant buildfile='src/evolve.build' target='all'>"
                    + "            <oncancel>"
                    + "              <exec command='kill.rb' workingdir='utils'>"
                    + "                 <oncancel>"
                    + "                     <exec command='kill.rb' workingdir='utils'/>"
                    + "                 </oncancel>"
                    + "              </exec>"
                    + "            </oncancel>"
                    + "          </ant>"
                    + "        </tasks>"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String CONTAINS_MULTI_SAME_STATUS_RUN_IF = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server artifactsdir=\"artifacts\">\n"
            + "  </server>\n"
            + "  <pipelines group=\"12345\">\n"
            + "    <pipeline name=\"test\">\n"
            + "      <materials>\n"
            + "        <hg url=\"http://hg-server/hg/connectfour\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"defaultStage\">\n"
            + "        <jobs>\n"
            + "          <job name=\"defaultJob\">\n"
            + "            <tasks>\n"
            + "              <exec command=\"echo\">\n"
            + "                <runif status=\"passed\" />\n"
            + "                <runif status=\"passed\" />\n"
            + "              </exec>\n"
            + "            </tasks>\n"
            + "          </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "  </cruise>";

    public static final String SAME_STATUS_RUN_IF_PARTIAL =
            "      <stage name=\"defaultStage\">\n"
                    + "        <jobs>\n"
                    + "          <job name=\"defaultJob\">\n"
                    + "            <tasks>\n"
                    + "              <exec command=\"echo\">\n"
                    + "                <runif status=\"passed\" />\n"
                    + "                <runif status=\"passed\" />\n"
                    + "              </exec>\n"
                    + "            </tasks>\n"
                    + "          </job>\n"
                    + "        </jobs>\n"
                    + "      </stage>\n";


    public static final String CONTAINS_MULTI_DIFFERENT_STATUS_RUN_IF = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines group=\"12345\">\n"
            + "    <pipeline name=\"test\">\n"
            + "      <materials>\n"
            + "        <hg url=\"http://hg-server/hg/connectfour\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"defaultStage\">\n"
            + "        <jobs>\n"
            + "          <job name=\"defaultJob\">\n"
            + "            <tasks>\n"
            + "              <exec command=\"echo\">\n"
            + "                <runif status=\"passed\" />\n"
            + "                <runif status=\"failed\" />\n"
            + "              </exec>\n"
            + "            </tasks>\n"
            + "          </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "  </cruise>";


    public static String LABEL_TEMPLATE_WITH_LABEL_TEMPLATE(String template) {
        return LABEL_TEMPLATE_WITH_LABEL_TEMPLATE(template, CONFIG_SCHEMA_VERSION);
    }

    public static String LABEL_TEMPLATE_WITH_LABEL_TEMPLATE(String template, int schemaVersion) {
        return "<cruise schemaVersion='" + schemaVersion + "'>\n"
                + "<server artifactsdir='artifactsDir' />"
                + "<pipelines>\n"
                + "<pipeline name='cruise' labeltemplate='" + template + "'>\n"
                + "    <materials>\n"
                + "      <git url='giturl' materialName='git' />\n"
                + "    </materials>\n"
                + "  <stage name='mingle'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' >\n "
                + "      </job>\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
    }

    public static final String MINIMAL =
            "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server></server></cruise>";

    public static final String OLD = "<cruise><server></server></cruise>";

    public static final String SERVER_WITH_ARTIFACTS_DIR = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "<server agentAutoRegisterKey=\"default\" webhookSecret=\"some-webhook-secret\" commandRepositoryLocation=\"default\" serverId=\"foo\" tokenGenerationKey=\"bar\">\n" +
            "<artifacts><artifactsDir>artifacts</artifactsDir></artifacts>"
            + "</server>"
            + "</cruise>\n";

    public static final String STAGE_WITH_EMPTY_AUTH = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "<artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + "    <pipeline name=\"pipeline1\">\n"
            + "      <materials>\n"
            + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <approval type=\"manual\">\n"
            + "        <authorization />\n"
            + "       </approval>\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" type=\"build\"/>\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String CONFIG_WITH_EMPTY_ROLES = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "    <security>\n"
            + "      <roles>\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + "    <pipeline name=\"pipeline2\">\n"
            + "       <materials>\n"
            + "         <hg url=\"hg\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"unit\" />\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";


    public static final String CONFIG_WITH_ADMIN_AND_SECURITY_AUTH_CONFIG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "    <security>\n"
            + "      <authConfigs>\n"
            + "        <authConfig id=\"tw-ldap\" pluginId=\"cd.go.authentication.ldap\">\n"
            + "         </authConfig>\n"
            + "       </authConfigs>\n"
            + "       <roles>\n"
            + "        <pluginRole name=\"go_admins\" authConfigId=\"tw-ldap\">\n"
            + "          <property>\n"
            + "            <key>AttributeName</key>\n"
            + "            <value>memberOf</value>\n"
            + "          </property>\n"
            + "          <property>\n"
            + "            <key>AttributeValue</key>\n"
            + "            <value>CN=SomeGroup</value>\n"
            + "          </property>"
            + "        </pluginRole>\n"
            + "         <role name=\"committer\">\n"
            + "         </role>\n"
            + "       </roles>\n"
            + "      <admins>\n"
            + "        <user>loser</user>\n"
            + "      </admins>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "</cruise>\n\n";

    public static final String CONFIG_WITH_EMPTY_USER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "    <security>\n"
            + "      <roles>\n"
            + "        <role name=\"admin\" >\n"
            + "         <users>"
            + "           <user></user>\n"
            + "         </users>"
            + "        </role>\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + "    <pipeline name=\"pipeline2\">\n"
            + "       <materials>\n"
            + "         <hg url=\"hg\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"unit\" />\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String CONFIG_WITH_DUPLICATE_ROLE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "    <security>\n"
            + "      <roles>\n"
            + "        <role name=\"admin\" />\n"
            + "        <role name=\"admin\" />\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + "    <pipeline name=\"pipeline2\">\n"
            + "       <materials>\n"
            + "         <hg url=\"hg\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"unit\" />\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String CONFIG_WITH_DUPLICATE_USER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "    <security>\n"
            + "      <roles>\n"
            + "        <role name=\"admin\" >\n"
            + "         <users>"
            + "           <user>ps</user>\n"
            + "           <user>ps</user>\n"
            + "         </users>"
            + "        </role>\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + "    <pipeline name=\"pipeline2\">\n"
            + "       <materials>\n"
            + "         <hg url=\"hg\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"unit\" />\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String TWO_PIPELINES = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server agentAutoRegisterKey=\"default\" webhookSecret=\"some-webhook-secret\" commandRepositoryLocation=\"default\" serverId=\"bar\" tokenGenerationKey=\"foo\">\n"
            + "    <security>\n"
            + "      <roles>\n"
            + "        <role name=\"admin\" />\n"
            + "        <role name=\"qa_lead\" />\n"
            + "      </roles>\n"
            + "    </security>\n"
            + "    <artifacts>\n"
            + "      <artifactsDir>other-artifacts</artifactsDir>\n"
            + "    </artifacts>\n"
            + "  </server>\n"
            + "  <pipelines group=\"defaultGroup\">\n"
            + "    <pipeline name=\"pipeline1\" labeltemplate=\"alpha.${COUNT}\">\n"
            + "       <timer>0 15 10 ? * MON-FRI</timer>\n"
            + "       <materials>\n"
            + "         <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <approval type=\"manual\">\n"
            + "          <authorization>"
            + "            <role>admin</role>"
            + "            <role>qa_lead</role>"
            + "            <user>jez</user>"
            + "          </authorization>"
            + "       </approval>\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact type=\"build\" src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "         <job name=\"unit\">\n"
            + "           <tasks>\n"
            + "             <exec command=\"ruby\" args=\"args\" workingdir=\"tmp\" />\n"
            + "           </tasks>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "    <pipeline name=\"pipeline2\">\n"
            + "       <materials>\n"
            + "         <hg url=\"hg\" />\n"
            + "       </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"unit\" />\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n";

    public static final String EMPTY_DEPENDENCIES = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
            + "  <server >\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + "    <pipeline name=\"pipeline1\">\n"
            + "      <dependencies />\n"
            + "      <materials>\n"
            + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"mingle\">\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" type=\"build\"/>\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n";

    public static final String PIPELINE_WITH_TRACKINGTOOL = "<pipeline name=\"pipeline1\">\n"
            + "  <trackingtool link=\"http://mingle05/projects/cce/cards/${ID}\" regex=\"(evo-\\d+)\" />\n"
            + "  <materials>\n"
            + "    <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "  </materials>\n"
            + "  <stage name=\"stage\">\n"
            + "    <jobs>\n"
            + "      <job name=\"functional\">\n"
            + "        <artifacts>\n"
            + "          <artifact type=\"build\" src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
            + "        </artifacts>\n"
            + "      </job>\n"
            + "    </jobs>\n"
            + "  </stage>\n"
            + "</pipeline>";

    public static final String CONFIG_WITH_TRACKINGTOOL = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server>\n"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + PIPELINE_WITH_TRACKINGTOOL
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String CRUISE = "<cruise schemaVersion=\"" + CONFIG_SCHEMA_VERSION + "\">\n"
            + "<server >"
            + "     <artifacts>" +
            "           <artifactsDir>artifactsDir</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "<pipelines>\n"
            + "  <pipeline name='cruise'>\n"
            + "    <materials>\n"
            + "      <svn url='svnurl' />\n"
            + "    </materials>\n"
            + "    <stage name='dev'>\n"
            + "      <jobs>\n"
            + "        <job name='linux' >\n "
            + "          <tasks>"
            + "            <ant />"
            + "          </tasks>"
            + "        </job>\n"
            + "        <job name='windows' >\n "
            + "          <tasks>"
            + "            <ant />"
            + "          </tasks>"
            + "        </job>\n"
            + "      </jobs>\n"
            + "    </stage>\n"
            + "  </pipeline>\n"
            + "</pipelines>\n"
            + "</cruise>";

    public static String withCommand(String jobWithCommand) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>logs</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "  <pipelines>\n"
                + "    <pipeline name=\"pipeline1\">\n"
                + "      <dependencies />\n"
                + "      <materials>\n"
                + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
                + "      </materials>\n"
                + "      <stage name=\"mingle\">\n"
                + "       <jobs>\n"
                + jobWithCommand
                + "        </jobs>\n"
                + "      </stage>\n"
                + "    </pipeline>\n"
                + "  </pipelines>\n"
                + "</cruise>";
    }

    public static String withServerConfig(String xml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                + CONFIG_SCHEMA_VERSION
                + "\">\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>logs</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + xml
                + "</server>"
                + "  <pipelines>\n"
                + "    <pipeline name=\"pipeline1\">\n"
                + "      <dependencies />\n"
                + "      <materials>\n"
                + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
                + "      </materials>\n"
                + "      <stage name=\"mingle\">\n"
                + "       <jobs>\n"
                + "        <job name='linux' >\n "
                + "          <tasks>"
                + "            <ant />"
                + "          </tasks>"
                + "        </job>\n"
                + "        </jobs>\n"
                + "      </stage>\n"
                + "    </pipeline>\n"
                + "  </pipelines>\n"
                + "</cruise>";
    }

    public static final String CONFIG =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                    + CONFIG_SCHEMA_VERSION + "\">\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>logs</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "  <security>"
                    + "    <roles>"
                    + "      <role name='admin'/>"
                    + "      <role name='qa_lead'/>"
                    + "    </roles>"
                    + "  </security>"
                    + "</server>"
                    + "  <pipelines>"
                    + "    <pipeline name='pipeline1' labeltemplate='" + PipelineLabel.COUNT_TEMPLATE + "'>"
                    + "      <materials>"
                    + "        <svn url ='svnUrl' checkexternals='true' username='username' password='password'/>"
                    + "      </materials>"
                    + "      <stage name='stage1'>"
                    + "        <approval type='manual'>"
                    + "          <authorization>"
                    + "            <role>admin</role>"
                    + "            <role>qa_lead</role>"
                    + "            <user>jez</user>"
                    + "          </authorization>"
                    + "        </approval>"
                    + "        <jobs>"
                    + "          <job name='plan1'>"
                    + "            <resources>"
                    + "              <resource>tiger</resource>"
                    + "              <resource>tiger  </resource>"
                    + "              <resource>lion</resource>"
                    + "            </resources>"
                    + "            <tabs>"
                    + "              <tab name=\"Emma\" path=\"logs/emma/index.html\" />"
                    + "              <tab name=\"EvolveClientLog\" path=\"logs/evolveClient.log\" />"
                    + "            </tabs>"
                    + "          </job>"
                    + "        </jobs>"
                    + "      </stage>"
                    + "      <stage name='stage2'>"
                    + "        <jobs>"
                    + "          <job name='plan2'>"
                    + "            <resources>"
                    + "              <resource>tiger</resource>"
                    + "              <resource>tiger  </resource>"
                    + "              <resource>lion</resource>"
                    + "            </resources>"
                    + "          </job>"
                    + "        </jobs>"
                    + "      </stage>"
                    + "    </pipeline>"
                    + "    <pipeline name='pipeline2'>"
                    + "      <materials>"
                    + "        <hg url='http://hgUrl.com' username='username' password='password' />"
                    + "      </materials>"
                    + "      <stage name='stage1'>"
                    + "        <jobs>"
                    + "          <job name='plan1'>"
                    + "            <resources>"
                    + "              <resource>mandrill</resource>"
                    + "            </resources>"
                    + "          </job>"
                    + "        </jobs>"
                    + "      </stage>"
                    + "    </pipeline>"
                    + "    <pipeline name='pipeline3'>"
                    + "      <materials>"
                    + "        <p4 port='localhost:1666' username='cruise' password='password' useTickets='true'>"
                    + "          <view><![CDATA["
                    + "//depot/dir1/... //lumberjack/..."
                    + "]]></view>"
                    + "        </p4>"
                    + "      </materials>"
                    + "      <stage name='stage1'>"
                    + "        <jobs>"
                    + "          <job name='plan1' />"
                    + "        </jobs>"
                    + "      </stage>"
                    + "    </pipeline>"
                    + "    <pipeline name='pipeline4'>"
                    + "      <materials>"
                    + "        <git url='git://username:password@gitUrl' />"
                    + "      </materials>"
                    + "      <stage name='stage1'>"
                    + "        <jobs>"
                    + "          <job name='plan1'>"
                    + "            <resources>"
                    + "              <resource>mandrill</resource>"
                    + "            </resources>"
                    + "          </job>"
                    + "        </jobs>"
                    + "      </stage>"
                    + "    </pipeline>"
                    + "  </pipelines>"
                    + "    <pipelines group=\"foo\">"
                    + "        <authorization>"
                    + "            <operate>"
                    + "                <role>qa_lead</role>"
                    + "            </operate>"
                    + "        </authorization>"
                    + "      <pipeline name=\"non-operatable-pipeline\">"
                    + "        <materials>"
                    + "          <git url=\"/tmp/git-stuff\" autoUpdate=\"false\" materialName=\"junit-failures-material\" />"
                    + "        </materials>"
                    + "        <stage name=\"one\">"
                    + "          <approval type=\"manual\" />"
                    + "          <jobs>"
                    + "            <job name=\"defaultJob\">"
                    + "              <tasks>"
                    + "                <exec command=\"sleep\" args=\"30\" />"
                    + "              </tasks>"
                    + "            </job>"
                    + "          </jobs>"
                    + "        </stage>"
                    + "      </pipeline>"
                    + "    </pipelines>"
                    + "</cruise>";

    public static final String ONE_CONFIG_REPO = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <config-repos>\n"
            + "    <config-repo id=\"id1\" pluginId=\"gocd-xml\">\n"
            + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
            + "    </config-repo >\n"
            + "  </config-repos>\n"
            + "</cruise>\n\n";

    public static final String ONE_PIPELINE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "  <server>"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + "    <pipeline name=\"pipeline1\">\n"
            + "      <materials>\n"
            + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"stage\">\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" type=\"build\"/>\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";


    public static final String TWO_DUPLICATED_FILTER = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
            + CONFIG_SCHEMA_VERSION + "\">\n"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + "    <pipeline name=\"pipeline1\">\n"
            + "      <materials>\n"
            + "        <svn url=\"foobar\" checkexternals=\"true\" >\n"
            + "             <filter>"
            + "                 <ignore pattern='*.doc'/>"
            + "<ignore pattern='*.doc'/>"
            + "</filter>"
            + "        </svn>"
            + "      </materials>\n"
            + "      <stage name=\"stage\">\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" type=\"build\"/>\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS
            = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>other-artifacts</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>\n"
            + "    <pipeline name=\"pipeline1\">\n"
            + "      <trackingtool link=\"http://mingle05/projects/cce/cards/${ID}\" regex=\"regex\" />\n"
            + "      <trackingtool link=\"http://mingle05/projects/cce/cards/${ID}\" regex=\"regex\" />\n"
            + "      <dependencies />\n"
            + "      <materials>\n"
            + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"stage\">\n"
            + "       <jobs>\n"
            + "         <job name=\"functional\">\n"
            + "           <artifacts>\n"
            + "             <artifact src=\"artifact1.xml\" dest=\"cruise-output\" type=\"build\"/>\n"
            + "           </artifacts>\n"
            + "         </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>\n\n";

    public static final String SERVER_TAG_WITH_DEFAULTS_PLUS_LICENSE_TAG =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                    + CONFIG_SCHEMA_VERSION + "\">\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifacts</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "</cruise>";

    public static final String DEFAULT_XML_WITH_2_AGENTS = xml();

    public static final String XML_WITH_SINGLE_ENVIRONMENT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>artifactsDir</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "<environments>"
            + "<environment name='dummy'/>"
            + "</environments>"
            + "</cruise>";

    public static final String XML_WITH_ENTERPRISE_LICENSE_FOR_TWO_USERS = xml();

    private static String xml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='20'>\n"
                + "<server artifactsdir=\"artifactsDir\">\n"
                + "</server>"
                + "</cruise>";
    }

    public static final String DEFAULT_XML_WITH_UNLIMITED_AGENTS =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + CONFIG_SCHEMA_VERSION + "\">\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>../server.logs</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "</cruise>";

    public static final String PIPELINES_WITH_DUPLICATE_STAGE_NAME
            = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>"
            + "    <pipeline name='studios'>"
            + "        <materials>"
            + "            <svn url='ape'/>"
            + "        </materials>"
            + "        <stage name='mingle'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "        <stage name='mingle'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "    </pipeline>"
            + "  </pipelines>"
            + "</cruise>";

    public static final String JOBS_WITH_SAME_NAME
            = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>"
            + "    <pipeline name='studios'>"
            + "        <materials>"
            + "            <svn url='ape'/>"
            + "        </materials>"
            + "        <stage name='mingle'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "    </pipeline>"
            + "  </pipelines>"
            + "</cruise>";


    public static final String SIMPLE_PIPELINE = "<pipeline name='mingle_pipeline'>"
            + "    <materials>"
            + "      <svn url ='svnurl'/>"
            + "    </materials>"
            + "  <stage name='mingle'>"
            + "    <jobs>"
            + "      <job name='cardlist' />"
            + "    </jobs>"
            + "  </stage>"
            + "</pipeline>";


    public static final String STAGE_WITH_NO_JOBS
            = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>"
            + "<server>"
            + "     <artifacts>" +
            "           <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir> " +
            "       </artifacts>"
            + "</server>"
            + "  <pipelines>"
            + "    <pipeline name='studios'>"
            + "        <materials>"
            + "            <svn url='ape'/>"
            + "        </materials>"
            + "        <stage name='mingle'>"
            + "            <jobs>"
            + "            </jobs>"
            + "        </stage>"
            + "    </pipeline>"
            + "  </pipelines>"
            + "</cruise>";

    public static final String VERSION_0 = "<cruise>"
            + "<server artifactsdir=\"target/testfiles/tmpCCRoot/data/logs\"></server>"
            + "  <pipelines>"
            + "    <pipeline name='pipeline'>"
            + "        <materials>"
            + "            <svn url='ape'/>"
            + "        </materials>"
            + "        <stage name='auto'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "            <approval type='manual'/>"
            + "        </stage>"
            + "        <stage name='manual'>"
            + "            <jobs>"
            + "                <job name='unit'/>"
            + "            </jobs>"
            + "        </stage>"
            + "    </pipeline>"
            + "  </pipelines>"
            + "</cruise>";


    public static final String VERSION_2
            = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"2\">\n"
            + "  <server>\n"
            + "  </server>\n"
            + "  <pipelines>\n"
            + "    <pipeline name=\"multiple\">\n"
            + "      <materials>\n"
            + "        <svn url=\"file:///home/cceuser/projects/cruise/manual-testing/multiple/repo/trunk/part1\"\n"
            + "         folder=\"part1\"\n"
            + "        />\n"
            + "      </materials>\n"
            + "      <stage name=\"helloworld-part2\">\n"
            + "        <jobs>\n"
            + "          <job name=\"run1\">\n"
            + "            <tasks>\n"
            + "              <exec command=\"/bin/bash\" args=\"helloworld.sh\" workingdir=\"part1\" />\n"
            + "            </tasks>\n"
            + "          </job>\n"
            + "       </jobs>\n"
            + "      </stage>\n"
            + "   </pipeline>\n"
            + "  </pipelines>\n"
            + "</cruise>";

    public static final String VERSION_5 =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"5\">\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='framework'>\n"
                    + "    <materials>\n"
                    + "      <svn url =\"svnurl\"/>"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <test src='from' dest='to'/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String VERSION_7 =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"7\">\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='framework'>\n"
                    + "    <materials>\n"
                    + "      <hg url =\"svnurl\" dest=\"something\">"
                    + "          <filter>"
                    + "             <ignore pattern=\"abc\"/>"
                    + "          </filter>"
                    + "      </hg>"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <test src='from' dest='to'/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String VERSION_16 =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise schemaVersion=\"16\">\n"
                    + "    <server artifactsdir='artifactsDir' />"
                    + "    <agents>\n"
                    + "        <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' />\n"
                    + "        <agent uuid='2' hostname='test2.com' ipaddress='192.168.0.2' isDenied='true' />\n"
                    + "        <agent uuid='3' hostname='test3.com' ipaddress='192.168.0.3' />\n"
                    + "    </agents>\n"
                    + "</cruise>";

    public static final String JOBS_WITH_DIFFERNT_CASE =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + " <pipelines>\n"
                    + "     <pipeline name='framework'>\n"
                    + "    <materials>\n"
                    + "      <hg url =\"svnurl\" dest=\"something\">"
                    + "      </hg>"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='test' />\n"
                    + "      <job name='Test' />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String PIPELINE_WITH_TIMER =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>logs</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + " <pipelines>\n"
                    + "     <pipeline name='pipeline'>\n"
                    + "         <timer>0 15 10 ? * MON-FRI</timer>"
                    + "         <materials>\n"
                    + "             <hg url =\"svnurl\" dest=\"something\"/>"
                    + "         </materials>\n"
                    + "         <stage name='dist'>\n"
                    + "             <jobs>\n"
                    + "                 <job name='test' />\n"
                    + "             </jobs>\n"
                    + "         </stage>\n"
                    + "     </pipeline>\n"
                    + " </pipelines>\n"
                    + "</cruise>";

    public static String multipleMaterial(String... materials) {
        StringBuilder sb = new StringBuilder();
        for (String material : materials) {
            sb.append(material).append('\n');
        }
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='ecl'>\n"
                + "    <materials>\n"
                + "         <svn url ='svnurl' dest='a'/>\n"
                + "    </materials>\n"
                + "  <stage name='firstStage'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "      <job name='bluemonkeybutt'>\n"
                + "        <artifacts>\n"
                + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                + "        </artifacts>\n"
                + "      </job>\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "  <stage name='secondStage'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "<pipeline name='ec2'>\n"
                + "    <materials>\n"
                + "         <svn url ='svnurl' dest='a'/>\n"
                + "    </materials>\n"
                + "  <stage name='firstStage'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "      <job name='bluemonkeybutt'>\n"
                + "        <artifacts>\n"
                + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                + "        </artifacts>\n"
                + "      </job>\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "  <stage name='secondStage'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "<pipeline name='framework'>\n"
                + "    <materials>\n"
                + sb.toString()
                + "    </materials>\n"
                + "  <stage name='dist'>\n"
                + "    <jobs>\n"
                + "      <job name='cardlist' />\n"
                + "      <job name='bluemonkeybutt'>\n"
                + "        <artifacts>\n"
                + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                + "        </artifacts>\n"
                + "      </job>\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
    }

    public static String withJob(String jobXml) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                + CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>logs</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "  <pipelines>\n"
                + "    <pipeline name=\"pipeline1\">\n"
                + "      <materials>\n"
                + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
                + "      </materials>\n"
                + "      <stage name=\"pre-mingle\">\n"
                + "       <jobs>\n"
                + "          <job name=\"run-ant\">\n"
                + "            <tasks>\n"
                + "              <exec command=\"/bin/bash\" args=\"helloworld.sh\" workingdir=\"part1\" />\n"
                + "            </tasks>\n"
                + "          </job>\n"
                + "        </jobs>\n"
                + "      </stage>\n"
                + "      <stage name=\"mingle\">\n"
                + "       <jobs>\n"
                + jobXml
                + "        </jobs>\n"
                + "      </stage>\n"
                + "    </pipeline>\n"
                + "  </pipelines>\n"
                + "</cruise>";
    }

    public static String withJob(String jobXml, String pipelineName) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                + CONFIG_SCHEMA_VERSION + "\">\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>logs</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<artifactStores>\n"
                + "    <artifactStore pluginId=\"cd.go.s3\" id=\"s3\">\n"
                + "        <property>\n"
                + "            <key>ACCESS_KEY</key>\n"
                + "            <value>some-secret-key</value>\n"
                + "        </property>\n"
                + "    </artifactStore>\n"
                + "</artifactStores>"
                + "  <pipelines>\n"
                + "    <pipeline name=\"" + pipelineName + "\">\n"
                + "      <materials>\n"
                + "        <svn url=\"foobar\" checkexternals=\"true\" />\n"
                + "      </materials>\n"
                + "      <stage name=\"pre-mingle\">\n"
                + "       <jobs>\n"
                + "          <job name=\"run-ant\">\n"
                + "            <tasks>\n"
                + "              <exec command=\"/bin/bash\" args=\"helloworld.sh\" workingdir=\"part1\" />\n"
                + "            </tasks>\n"
                + "          </job>\n"
                + "        </jobs>\n"
                + "      </stage>\n"
                + "      <stage name=\"mingle\">\n"
                + "       <jobs>\n"
                + jobXml
                + "        </jobs>\n"
                + "      </stage>\n"
                + "    </pipeline>\n"
                + "  </pipelines>\n"
                + "</cruise>";
    }

    public static final String PIPELINE_GROUPS =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\""
                    + CONFIG_SCHEMA_VERSION + "\">\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "</server>"
                    + "<pipelines group=\"studios\">\n"
                    + "<pipeline name='framework'>\n"
                    + "    <materials>\n"
                    + "      <hg url =\"svnurl\" dest=\"something\">"
                    + "          <filter>"
                    + "             <ignore pattern=\"abc\"/>"
                    + "          </filter>"
                    + "      </hg>"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "<pipelines group=\"perfessionalservice\">\n"
                    + "<pipeline name='framework1'>\n"
                    + "    <materials>\n"
                    + "      <hg url =\"svnurl\" dest=\"something\">"
                    + "          <filter>"
                    + "             <ignore pattern=\"abc\"/>"
                    + "          </filter>"
                    + "      </hg>"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <artifact src='from' dest='to' type=\"test\"/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String STAGE_AUTH_WITH_ADMIN_AND_AUTH =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                    + "<server>"
                    + "     <artifacts>" +
                    "           <artifactsDir>artifactsDir</artifactsDir> " +
                    "       </artifacts>"
                    + "     <security>"
                    + "      <authConfigs>\n"
                    + "        <authConfig id=\"tw-ldap\" pluginId=\"cd.go.authentication.ldap\">\n"
                    + "         </authConfig>\n"
                    + "       </authConfigs>\n"
                    + "     <admins>"
                    + "         <user>admin</user>"
                    + "     </admins>"
                    + "     </security>"
                    + "</server>"
                    + "<pipelines group=\"studios\">\n"
                    + "     <authorization>\n"
                    + "         <operate>\n"
                    + "             <user>operator</user>\n"
                    + "         </operate>\n"
                    + "     </authorization>\n"
                    + "<pipeline name='framework'>\n"
                    + "    <materials>\n"
                    + "      <hg url =\"svnurl\" dest=\"something\" />"
                    + "    </materials>\n"
                    + "  <stage name='dist'>\n"
                    + "     <approval type='manual'>\n"
                    + "         <authorization>\n"
                    + "             <user>admin</user>\n"
                    + "         </authorization>\n"
                    + "     </approval>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static final String VALID_XML_3169 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"14\">\n"
            + "  <server artifactsdir=\"artifacts\">\n"
            + "    <license user=\"Go UAT ThoughtWorks\">dc7Q7ii7wQA7O8UxZAnud3ZFDi09MDaXYCwNZjjVyyhnZXK1kAQIZ4U+k/Tt\n"
            + "wCbfAmLCizhqvNvC3ZYCsa2zKfA26f+tUnc0WjRBK6ttfTVl9M9t08t+ZcAI\n"
            + "JhtONBURkA3YumffkxyAaPdPJq5tMaZYWjaX1pBpGlG0LjR+HwAkZnteTYeI\n"
            + "XMd7w0z741K8irGi3fLY+pyc8VB0jnv0J8tSWamL2sjy6irkuSw9q70PAtxb\n"
            + "q7MZBEkIaT3VFpehkyMvutKFUC6igyET3kd5WJoxeXj0W5ZucFGgKgFYlLNa\n"
            + "UQwnhuako+UAXhDhvMa2ud+fARbyZJasjGQQ77w6NQ==</license>\n"
            + "  </server>\n"
            + "  <pipelines group=\"12345\">\n"
            + "    <pipeline name=\"test\">\n"
            + "      <materials>\n"
            + "        <hg url=\"http://hg-server/hg/connectfour\" />\n"
            + "      </materials>\n"
            + "      <stage name=\"defaultStage\">\n"
            + "        <jobs>\n"
            + "          <job name=\"defaultJob\">\n"
            + "            <tasks>\n"
            + "              <exec command=\"echo\">\n"
            + "                <runif status=\"passed\" />\n"
            + "                <arg value=\"test\" />\n"
            + "              </exec>\n"
            + "            </tasks>\n"
            + "          </job>\n"
            + "        </jobs>\n"
            + "      </stage>\n"
            + "    </pipeline>\n"
            + "  </pipelines>\n"
            + "  </cruise>";

    public static final String WITH_VMMS_CONFIG =
            "<cruise schemaVersion='50'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url =\"svnurl\"/>"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <log src='from' dest='to'/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "<vmms>\n"
                    + "<ec2 accessKey='test' secretAccessKey='test'>\n"
                    + "<ami imageId='test' />\n"
                    + "</ec2>\n"
                    + "</vmms>\n"
                    + "</cruise>";

    public static final String WITH_LOG_ARTIFACT_CONFIG =
            "<cruise schemaVersion='50'>\n"
                    + "<server artifactsdir='artifactsDir' />"
                    + "<pipelines>\n"
                    + "<pipeline name='pipeline1'>\n"
                    + "    <materials>\n"
                    + "      <svn url =\"svnurl\"/>"
                    + "    </materials>\n"
                    + "  <stage name='mingle'>\n"
                    + "    <jobs>\n"
                    + "      <job name='cardlist' />\n"
                    + "      <job name='bluemonkeybutt'>\n"
                    + "        <artifacts>\n"
                    + "          <log src='from1' />\n"
                    + "          <log src='from2' dest='to2'/>\n"
                    + "          <artifact src='from3'/>\n"
                    + "          <artifact src='from4' dest='to4'/>\n"
                    + "        </artifacts>\n"
                    + "      </job>\n"
                    + "    </jobs>\n"
                    + "  </stage>\n"
                    + "</pipeline>\n"
                    + "</pipelines>\n"
                    + "</cruise>";

    public static CruiseConfig configWith(PipelineConfigs... pipelineConfigses) {
        return new BasicCruiseConfig(pipelineConfigses);
    }

    public static CruiseConfig configWith(PipelineConfig... pipelineConfigs) {
        BasicPipelineConfigs configs = new BasicPipelineConfigs();
        configs.setGroup("defaultGroup");
        configs.addAll(Arrays.asList(pipelineConfigs));
        return new BasicCruiseConfig(configs);
    }

    public static String configWithSecurity(String security) {
        String defaultArtifact = "<artifacts><artifactsDir>logs</artifactsDir></artifacts>";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "  <server>\n"
                + security
                + defaultArtifact
                + "  </server>"
                + "  </cruise>";
    }

    public static String configWithArtifactSourceAs(String artifactSource) {
        return String.format(CONFIG_WITH_ARTIFACT_SRC, artifactSource);
    }

    public static String pipelineWithAttributes(String pipelineTagAttributes, int schemaVersion) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><cruise schemaVersion=\"" + schemaVersion + "\">\n" +
                "<pipelines>\n" +
                "  <pipeline " + pipelineTagAttributes + ">\n" +
                "    <materials>\n" +
                "      <git url=\"git1\"/>\n" +
                "    </materials>\n" +
                "    <stage name=\"stage1\">\n" +
                "      <jobs>\n" +
                "        <job name=\"job1\">\n" +
                "          <tasks>\n" +
                "            <ant/>\n" +
                "          </tasks>\n" +
                "        </job>\n" +
                "      </jobs>\n" +
                "    </stage>\n" +
                "  </pipeline>\n" +
                "</pipelines>\n" +
                "</cruise>";
    }
}
