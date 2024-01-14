/*
 * Copyright 2024 Thoughtworks, Inc.
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

import java.util.List;

import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;

public final class ConfigFileFixture {

    public static final String BASIC_CONFIG =
            ("""
                    <cruise schemaVersion="%d">
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                    </server>
                    <pipelines>
                        <pipeline name='pipeline1'>
                            <materials>
                              <svn url ="svnurl"/>
                            </materials>
                          <stage name='mingle'>
                            <jobs>
                              <job name='cardlist'><tasks><ant /></tasks></job>
                              <job name='bluemonkeybutt'>
                                <tasks><ant /></tasks>
                                <artifacts>
                                  <artifact type="build" src='from' dest='to'/>
                                </artifacts>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static String configWithEnvironments(String environmentsBlock, int configSchemaVersion) {
        return ("""
                <cruise schemaVersion="%d">
                <pipelines group='group1'>
                <pipeline name='pipeline1'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                  <stage name='mingle'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                %s</cruise>""").formatted(configSchemaVersion, environmentsBlock);
    }


    public static String configWithTemplates(String template) {
        return ("""
                <cruise schemaVersion='%d'>
                <server>
                    <artifacts>
                        <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                </server>
                <pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url ="svnurl"/>
                          </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                        </jobs>
                      </stage>
                    </pipeline>
                </pipelines>
                %s</cruise>""").formatted(CONFIG_SCHEMA_VERSION, template);
    }

    public static String configWithConfigRepos(String configReposBlock) {
        return ("""
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                <server>
                    <artifacts>
                        <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                </server>
                %s
                <pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url ="svnurl"/>
                          </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                        </jobs>
                      </stage>
                    </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, configReposBlock);
    }

    public static String configWithPipeline(String pipelineBlock) {
        return configWithPipeline(pipelineBlock, CONFIG_SCHEMA_VERSION);
    }

    public static String configWithPipeline(String pipelineBlock, int schemaVersion) {
        return configWithPipelines(("""
                <pipelines>
                  %s
                </pipelines>
                """).formatted(pipelineBlock), schemaVersion);
    }

    public static String configWithPipelines(String pipelinesBlock, int schemaVersion) {
        return ("""
                <cruise schemaVersion='%d'>
                  %s
                </cruise>""").formatted(schemaVersion, pipelinesBlock);
    }

    public static String configWithPluggableScm(String scmBlock, int schemaVersion) {
        return ("""
                <cruise schemaVersion='%d'>
                    <server artifactsdir='artifactsDir' >
                    </server>
                    <scms>
                      %s
                    </scms>
                </cruise>""").formatted(schemaVersion, scmBlock);
    }

    public static String config(String block, int schemaVersion) {
        return ("""
                <cruise schemaVersion='%d'>
                  %s
                </cruise>""").formatted(schemaVersion, block);
    }

    public static final String CONFIG_WITH_ANT_BUILDER =
            ("""
                    <cruise schemaVersion='%d'>
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                    </server>
                    <pipelines>
                        <pipeline name='pipeline1'>
                            <materials>
                              <svn url='svnurl' />
                            </materials>
                          <stage name='mingle'>
                            <jobs>
                              <job name='cardlist' timeout='5'>
                                 <artifacts>
                                  <artifact src='from' dest='to' type="test"/>
                                </artifacts>
                                <tasks>
                                  <ant buildfile='src/evolve.build' target='all'/>
                                </tasks>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_NANT_AND_EXEC_BUILDER =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                    <artifacts>
                        <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                    </server>
                    <pipelines>
                        <pipeline name='pipeline1'>
                            <materials>
                              <svn url ='svnurl' />
                            </materials>
                          <stage name='mingle'>
                            <jobs>
                              <job name='cardlist' >
                                 <tasks>
                                     <nant nantpath='lib/apache-nant' buildfile='src/evolve.build' target='all'/>
                                     <exec command='ls' workingdir='workdir' args='-la' />
                                     <exec command='ls' />
                                     <rake buildfile='myrake.rb' target='test' workingdir='somewhere' />
                                </tasks>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                        <pipeline name='pipeline2'>
                          <dependencies>
                            <depends pipeline="pipeline1" stage="mingle"/>
                          </dependencies>
                          <materials>
                              <svn url ='svnurl' />
                          </materials>
                          <stage name='dist'>
                            <jobs>
                              <job name='cardlist' >
                                 <tasks>
                                    <nant nantpath='lib/apache-nant' buildfile='src/evolve.build' target='all'/>
                                    <exec command='ls' workingdir='workdir' args='-la' />
                                    <exec command='ls' />
                                </tasks>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String WITH_DUPLICATE_ENVIRONMENTS =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                    </server>
                    <pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url ='svnurl' />
                        </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist' >
                             <tasks>
                                <exec command='ls' workingdir='workdir' args='-la' />
                            </tasks>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    <environments>
                        <environment name='foo' />
                        <environment name='FOO' />
                    </environments>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String TASKS_WITH_CONDITION =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                    <artifacts>
                        <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                    </server>
                    <pipelines>
                        <pipeline name='pipeline1'>
                            <materials>
                              <svn url='svnurl' />
                            </materials>
                          <stage name='mingle'>
                            <jobs>
                              <job name='cardlist' >
                                 <tasks>
                                    <ant buildfile='src/evolve.build' target='all'>
                                        <runif status='failed' />
                                    </ant>
                                    <nant buildfile='src/evolve.build' target='all'>
                                        <runif status='failed' />
                                        <runif status='any' />
                                        <runif status='passed' />
                                    </nant>
                                </tasks>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_ARTIFACT_SRC =
            """
                    <cruise schemaVersion='28'>
                    <server artifactsdir='artifactsDir' /><pipelines>
                    <pipeline name='pipeline'>
                        <materials>
                          <svn url='svnurl' />
                        </materials>
                      <stage name='stage'>
                        <jobs>
                          <job name='job' >
                              <tasks>
                                <ant buildfile='src/evolve.build' target='all' />
                              </tasks>
                              <artifacts>
                                 <artifact src='%s'/>
                             </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""";

    public static final String TASKS_WITH_ON_CANCEL =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                    </server>
                    <pipelines>
                        <pipeline name='pipeline1'>
                            <materials>
                              <svn url='svnurl' />
                            </materials>
                          <stage name='mingle'>
                            <jobs>
                              <job name='cardlist' >
                                 <tasks>
                                    <ant buildfile='src/evolve.build' target='all'>
                                        <oncancel>
                                            <exec command='kill.rb' workingdir='utils' />
                                        </oncancel>
                                    </ant>
                                    <exec command='ls'>
                                        <oncancel/>
                                    </exec>
                                  </tasks>
                               </job>
                               <job name="downstream-job">
                                  <tasks>
                                      <exec command="echo" args="hello world!!"><oncancel /></exec>
                                  </tasks>
                               </job>
                             </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String MATERIAL_WITH_NAME =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                      </server>
                    <pipelines>
                        <pipeline name='pipeline'>
                            <materials>
                              <svn url='http://blahblah' materialName='svn' dest='svn' />
                              <hg url='http://blahblah' materialName='hg' dest='hg' />
                            </materials>
                          <stage name='dev'>
                            <jobs>
                              <job name='cardlist' >
                               </job>
                            </jobs>
                          </stage>
                        </pipeline>
                    </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String TASKS_WITH_ON_CANCEL_NESTED =
            ("""
                <cruise schemaVersion='%d'>
                    <server>
                        <artifacts>
                            <artifactsDir>artifactsDir</artifactsDir>
                        </artifacts>
                      </server>
                    <pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url='svnurl' />
                        </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist' >
                             <tasks>
                                <ant buildfile='src/evolve.build' target='all'>
                                    <oncancel>
                                        <exec command='kill.rb' workingdir='utils'>
                                            <oncancel>
                                                <exec command='kill.rb' workingdir='utils'/>
                                            </oncancel>
                                        </exec>
                                    </oncancel>
                                </ant>
                             </tasks>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONTAINS_MULTI_SAME_STATUS_RUN_IF = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
            <server>
                <artifacts>
                  <artifactsDir>artifacts</artifactsDir>
                </artifacts>
            </server>
              <pipelines group="12345">
                <pipeline name="test">
                  <materials>
                    <hg url="http://hg-server/hg/connectfour" />
                  </materials>
                  <stage name="defaultStage">
                    <jobs>
                      <job name="defaultJob">
                        <tasks>
                          <exec command="echo">
                            <runif status="passed" />
                            <runif status="passed" />
                          </exec>
                        </tasks>
                      </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
              </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String SAME_STATUS_RUN_IF_PARTIAL =
            """
                          <stage name="defaultStage">
                            <jobs>
                              <job name="defaultJob">
                                <tasks>
                                  <exec command="echo">
                                    <runif status="passed" />
                                    <runif status="passed" />
                                  </exec>
                                </tasks>
                              </job>
                            </jobs>
                          </stage>
                    """;


    public static final String CONTAINS_MULTI_DIFFERENT_STATUS_RUN_IF = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifacts</artifactsDir>
                     </artifacts>
                </server>
              <pipelines group="12345">
                <pipeline name="test">
                  <materials>
                    <hg url="http://hg-server/hg/connectfour" />
                  </materials>
                  <stage name="defaultStage">
                    <jobs>
                      <job name="defaultJob">
                        <tasks>
                          <exec command="echo">
                            <runif status="passed" />
                            <runif status="failed" />
                          </exec>
                        </tasks>
                      </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);


    public static String LABEL_TEMPLATE_WITH_LABEL_TEMPLATE(String template) {
        return ("""
                <cruise schemaVersion='%d'>
                <server>
                    <artifacts>
                      <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                </server>
                <pipelines>
                <pipeline name='cruise' labeltemplate='%s'>
                    <materials>
                      <git url='giturl' materialName='git' />
                    </materials>
                  <stage name='mingle'>
                    <jobs>
                      <job name='cardlist' >
                         <tasks><exec command='echo'><runif status='passed' /></exec></tasks>
                      </job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, template);
    }

    public static final String MINIMAL =
            ("<cruise schemaVersion='%d'>\n" +
                    "<server></server></cruise>").formatted(CONFIG_SCHEMA_VERSION);

    public static final String OLD = "<cruise><server></server></cruise>";

    public static final String SERVER_WITH_ARTIFACTS_DIR = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server agentAutoRegisterKey="default" webhookSecret="some-webhook-secret" serverId="foo" tokenGenerationKey="bar">
                <artifacts>
                  <artifactsDir>artifacts</artifactsDir>
                </artifacts>
              </server>
            </cruise>
            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String SERVER_WITH_ARTIFACTS_DIR_AND_PURGE_SETTINGS = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
            <server agentAutoRegisterKey="default" webhookSecret="some-webhook-secret" serverId="foo" tokenGenerationKey="bar">
            <artifacts>
               <artifactsDir>artifacts</artifactsDir>
               <purgeSettings>
                   <purgeStartDiskSpace>50.0</purgeStartDiskSpace>
                   <purgeUptoDiskSpace>100.0</purgeUptoDiskSpace>
               </purgeSettings></artifacts>
            </server>
            </cruise>
            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String STAGE_WITH_EMPTY_AUTH = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server>
                  <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                   </artifacts>
              </server>
              <pipelines>
                <pipeline name="pipeline1">
                  <materials>
                    <svn url="foobar" checkexternals="true" />
                  </materials>
                  <stage name="mingle">
                   <approval type="manual">
                    <authorization />
                   </approval>
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact src="artifact1.xml" dest="cruise-output" type="build"/>
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_EMPTY_ROLES = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server >
                <artifacts>
                  <artifactsDir>other-artifacts</artifactsDir>
                </artifacts>
                <security>
                  <roles>
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline2">
                   <materials>
                     <hg url="hg" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="unit" />
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);


    public static final String CONFIG_WITH_ADMIN_AND_SECURITY_AUTH_CONFIG = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server >
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                   </artifacts>
                <security>
                  <authConfigs>
                    <authConfig id="tw-ldap" pluginId="cd.go.authentication.ldap">
                     </authConfig>
                   </authConfigs>
                   <roles>
                    <pluginRole name="go_admins" authConfigId="tw-ldap">
                      <property>
                        <key>AttributeName</key>
                        <value>memberOf</value>
                      </property>
                      <property>
                        <key>AttributeValue</key>
                        <value>CN=SomeGroup</value>
                      </property>
                    </pluginRole>
                     <role name="committer">
                     </role>
                   </roles>
                  <admins>
                    <user>loser</user>
                  </admins>
                </security>
              </server>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_EMPTY_USER = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server >
                <artifacts>
                  <artifactsDir>other-artifacts</artifactsDir>
                </artifacts>
                <security>
                  <roles>
                    <role name="admin" >
                     <users>
                       <user></user>
                     </users>
                    </role>
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline2">
                   <materials>
                     <hg url="hg" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="unit" />
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_DUPLICATE_ROLE = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server >
                <artifacts>
                    <artifactsDir>other-artifacts</artifactsDir>
                </artifacts>
                <security>
                  <roles>
                    <role name="admin" />
                    <role name="admin" />
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline2">
                   <materials>
                     <hg url="hg" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="unit" />
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String CONFIG_WITH_DUPLICATE_USER = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server >
                <artifacts>
                  <artifactsDir>other-artifacts</artifactsDir>
                </artifacts>
                <security>
                  <roles>
                    <role name="admin" >
                     <users>
                       <user>ps</user>
                       <user>ps</user>
                     </users>
                    </role>
                  </roles>
                </security>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline2">
                   <materials>
                     <hg url="hg" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="unit" />
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String TWO_PIPELINES = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server agentAutoRegisterKey="default" webhookSecret="some-webhook-secret" serverId="bar" tokenGenerationKey="foo">
                <security>
                  <roles>
                    <role name="admin" />
                    <role name="qa_lead" />
                  </roles>
                </security>
                <artifacts>
                  <artifactsDir>other-artifacts</artifactsDir>
                </artifacts>
              </server>
              <pipelines group="defaultGroup">
                <pipeline name="pipeline1" labeltemplate="alpha.${COUNT}">
                   <timer>0 15 10 ? * MON-FRI</timer>
                   <materials>
                     <svn url="foobar" checkexternals="true" />
                   </materials>
                  <stage name="mingle">
                   <approval type="manual">
                      <authorization>
                        <role>admin</role>
                        <role>qa_lead</role>
                        <user>jez</user>
                      </authorization>
                   </approval>
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                       </artifacts>
                     </job>
                     <job name="unit">
                       <tasks>
                         <exec command="ruby" args="args" workingdir="tmp" />
                       </tasks>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
                <pipeline name="pipeline2">
                   <materials>
                     <hg url="hg" />
                   </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="unit">
                       <tasks>
                         <ant />
                       </tasks>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>
            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String EMPTY_DEPENDENCIES = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
              <server >
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name="pipeline1">
                  <dependencies />
                  <materials>
                    <svn url="foobar" checkexternals="true" />
                  </materials>
                  <stage name="mingle">
                   <jobs>
                     <job name="functional">
                       <tasks>
                         <ant />
                       </tasks>
                       <artifacts>
                         <artifact src="artifact1.xml" dest="cruise-output" type="build"/>
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>
            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String PIPELINE_WITH_TRACKINGTOOL = """
            <pipeline name="pipeline1">
              <trackingtool link="http://mingle05/projects/cce/cards/${ID}" regex="(evo-\\d+)" />
              <materials>
                <svn url="foobar" checkexternals="true" />
              </materials>
              <stage name="stage">
                <jobs>
                  <job name="functional">
                    <tasks>
                      <ant />
                    </tasks>
                    <artifacts>
                      <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                    </artifacts>
                  </job>
                </jobs>
              </stage>
            </pipeline>""";

    public static final String CONFIG_WITH_TRACKINGTOOL = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server>
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
            %s  </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION, PIPELINE_WITH_TRACKINGTOOL);

    public static final String CRUISE = ("""
            <cruise schemaVersion="%d">
            <server >
                 <artifacts>
                       <artifactsDir>artifactsDir</artifactsDir>
                 </artifacts>
            </server>
            <pipelines>
              <pipeline name='cruise'>
                <materials>
                  <svn url='svnurl' />
                </materials>
                <stage name='dev'>
                  <jobs>
                    <job name='linux' >
                       <tasks>
                        <ant />
                      </tasks>
                    </job>
                    <job name='windows' >
                       <tasks>
                        <ant />
                      </tasks>
                    </job>
                  </jobs>
                </stage>
              </pipeline>
            </pipelines>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static String withCommand(String jobWithCommand) {
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>logs</artifactsDir>
                     </artifacts>
                </server>
                  <pipelines>
                    <pipeline name="pipeline1">
                      <dependencies />
                      <materials>
                        <svn url="foobar" checkexternals="true" />
                      </materials>
                      <stage name="mingle">
                       <jobs>
                %s        </jobs>
                      </stage>
                    </pipeline>
                  </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, jobWithCommand);
    }

    public static String withServerConfig(String xml) {
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                  <server>
                     <artifacts>
                           <artifactsDir>logs</artifactsDir>
                     </artifacts>
                     %s
                  </server>
                  <pipelines>
                    <pipeline name="pipeline1">
                      <dependencies />
                      <materials>
                        <svn url="foobar" checkexternals="true" />
                      </materials>
                      <stage name="mingle">
                       <jobs>
                        <job name='linux' >
                           <tasks>
                            <ant />
                          </tasks>
                        </job>
                        </jobs>
                      </stage>
                    </pipeline>
                  </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, xml);
    }

    public static final String CONFIG =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                    <server>
                      <artifacts>
                        <artifactsDir>logs</artifactsDir>
                      </artifacts>
                      <security>
                        <roles>
                          <role name='admin'/>
                          <role name='qa_lead'/>
                        </roles>
                      </security>
                    </server>
                      <pipelines>
                        <pipeline name='pipeline1' labeltemplate='%s'>
                          <materials>
                            <svn url ='svnUrl' checkexternals='true' username='username' password='password'/>
                          </materials>
                          <stage name='stage1'>
                            <approval type='manual'>
                              <authorization>
                                <role>admin</role>
                                <role>qa_lead</role>
                                <user>jez</user>
                              </authorization>
                            </approval>
                            <jobs>
                              <job name='plan1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks>
                                <resources>
                                  <resource>tiger</resource>
                                  <resource>tiger  </resource>
                                  <resource>lion</resource>
                                </resources>
                                <tabs>
                                  <tab name="Emma" path="logs/emma/index.html" />
                                  <tab name="EvolveClientLog" path="logs/evolveClient.log" />
                                </tabs>
                              </job>
                            </jobs>
                          </stage>
                          <stage name='stage2'>
                            <jobs>
                              <job name='plan2'><tasks><exec command='echo'><runif status='passed' /></exec></tasks>
                                <resources>
                                  <resource>tiger</resource>
                                  <resource>tiger  </resource>
                                  <resource>lion</resource>
                                </resources>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                        <pipeline name='pipeline2'>
                          <materials>
                            <hg url='https://hgUrl.com' username='username' password='password' />
                          </materials>
                          <stage name='stage1'>
                            <jobs>
                              <job name='plan1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks>
                                <resources>
                                  <resource>mandrill</resource>
                                </resources>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                        <pipeline name='pipeline3'>
                          <materials>
                            <p4 port='localhost:1666' username='cruise' password='password' useTickets='true'>
                              <view><![CDATA[//depot/dir1/... //lumberjack/...]]></view>
                            </p4>
                          </materials>
                          <stage name='stage1'>
                            <jobs>
                              <job name='plan1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                            </jobs>
                          </stage>
                        </pipeline>
                        <pipeline name='pipeline4'>
                          <materials>
                            <git url='git://username:password@gitUrl' />
                          </materials>
                          <stage name='stage1'>
                            <jobs>
                              <job name='plan1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks>
                                <resources>
                                  <resource>mandrill</resource>
                                </resources>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>
                      </pipelines>
                        <pipelines group="foo">
                            <authorization>
                                <operate>
                                    <role>qa_lead</role>
                                </operate>
                            </authorization>
                          <pipeline name="non-operatable-pipeline">
                            <materials>
                              <git url="/tmp/git-stuff" autoUpdate="false" materialName="junit-failures-material" />
                            </materials>
                            <stage name="one">
                              <approval type="manual" />
                              <jobs>
                                <job name="defaultJob">
                                  <tasks>
                                    <exec command="sleep" args="30" />
                                  </tasks>
                                </job>
                              </jobs>
                            </stage>
                          </pipeline>
                        </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION, PipelineLabel.COUNT_TEMPLATE);

    public static final String ONE_CONFIG_REPO = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
            <server>
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
            </server>
              <config-repos>
                <config-repo id="id1" pluginId="gocd-xml">
                  <git url="https://github.com/tomzo/gocd-indep-config-part.git" />
                </config-repo >
              </config-repos>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String ONE_PIPELINE = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
              <server>
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
              </server>
              <pipelines>
                <pipeline name="pipeline1">
                  <materials>
                    <svn url="foobar" checkexternals="true" />
                  </materials>
                  <stage name="stage">
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact src="artifact1.xml" dest="cruise-output" type="build"/>
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);


    public static final String TWO_DUPLICATED_FILTER = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
            <server>
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name="pipeline1">
                  <materials>
                    <svn url="foobar" checkexternals="true" >
                         <filter>
                             <ignore pattern='*.doc'/>
            <ignore pattern='*.doc'/>
            </filter>
                    </svn>
                  </materials>
                  <stage name="stage">
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact src="artifact1.xml" dest="cruise-output" type="build"/>
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String INVALID_CONFIG_WITH_MULTIPLE_TRACKINGTOOLS
            = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
            <server>
                 <artifacts>
                       <artifactsDir>other-artifacts</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name="pipeline1">
                  <trackingtool link="http://mingle05/projects/cce/cards/${ID}" regex="regex" />
                  <trackingtool link="http://mingle05/projects/cce/cards/${ID}" regex="regex" />
                  <dependencies />
                  <materials>
                    <svn url="foobar" checkexternals="true" />
                  </materials>
                  <stage name="stage">
                   <jobs>
                     <job name="functional">
                       <tasks><ant /></tasks>
                       <artifacts>
                         <artifact src="artifact1.xml" dest="cruise-output" type="build"/>
                       </artifacts>
                     </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
            </cruise>

            """).formatted(CONFIG_SCHEMA_VERSION);

    public static final String SERVER_TAG_WITH_DEFAULTS_PLUS_LICENSE_TAG =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                    <server>
                         <artifacts>
                               <artifactsDir>artifacts</artifactsDir>
                         </artifacts>
                    </server>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String DEFAULT_XML_WITH_2_AGENTS = xml();

    public static final String XML_WITH_SINGLE_ENVIRONMENT = ("""
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
            <server>
                 <artifacts>
                       <artifactsDir>artifactsDir</artifactsDir>
                 </artifacts>
            </server>
            <environments>
            <environment name='dummy'/>
            </environments>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String XML_WITH_ENTERPRISE_LICENSE_FOR_TWO_USERS = xml();

    private static String xml() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='20'>
                <server artifactsdir="artifactsDir">
                </server></cruise>""";
    }

    public static final String DEFAULT_XML_WITH_UNLIMITED_AGENTS =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                    <server>
                         <artifacts>
                               <artifactsDir>./../server.logs</artifactsDir>
                         </artifacts>
                    </server>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String PIPELINES_WITH_DUPLICATE_STAGE_NAME
            = ("""
            <cruise schemaVersion='%d'>
            <server>
                 <artifacts>
                       <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name='studios'>
                    <materials>
                        <svn url='ape'/>
                    </materials>
                    <stage name='mingle'>
                        <jobs>
                            <job name='unit'/>
                        </jobs>
                    </stage>
                    <stage name='mingle'>
                        <jobs>
                            <job name='unit'/>
                        </jobs>
                    </stage>
                </pipeline>
              </pipelines>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String JOBS_WITH_SAME_NAME
            = ("""
            <cruise schemaVersion='%d'>
            <server>
                 <artifacts>
                       <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name='studios'>
                    <materials>
                        <svn url='ape'/>
                    </materials>
                    <stage name='mingle'>
                        <jobs>
                            <job name='unit'/>
                            <job name='unit'/>
                        </jobs>
                    </stage>
                </pipeline>
              </pipelines>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);


    public static final String SIMPLE_PIPELINE = """
            <pipeline name='mingle_pipeline'>
                <materials>
                  <svn url ='svnurl'/>
                </materials>
              <stage name='mingle'>
                <jobs>
                  <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                </jobs>
              </stage>
            </pipeline>""";


    public static final String STAGE_WITH_NO_JOBS
            = ("""
            <cruise schemaVersion='%d'>
            <server>
                 <artifacts>
                       <artifactsDir>target/testfiles/tmpCCRoot/data/logs</artifactsDir>
                 </artifacts>
            </server>
              <pipelines>
                <pipeline name='studios'>
                    <materials>
                        <svn url='ape'/>
                    </materials>
                    <stage name='mingle'>
                        <jobs>
                        </jobs>
                    </stage>
                </pipeline>
              </pipelines>
            </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String VERSION_0 = """
            <cruise>
            <server artifactsdir="target/testfiles/tmpCCRoot/data/logs"></server>
              <pipelines>
                <pipeline name='pipeline'>
                    <materials>
                        <svn url='ape'/>
                    </materials>
                    <stage name='auto'>
                        <jobs>
                            <job name='unit'/>
                        </jobs>
                        <approval type='manual'/>
                    </stage>
                    <stage name='manual'>
                        <jobs>
                            <job name='unit'/>
                        </jobs>
                    </stage>
                </pipeline>
              </pipelines>
            </cruise>""";


    public static final String VERSION_2
            = """
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="2">
              <server>
              </server>
              <pipelines>
                <pipeline name="multiple">
                  <materials>
                    <svn url="file:///home/cceuser/projects/cruise/manual-testing/multiple/repo/trunk/part1"
                     folder="part1"
                    />
                  </materials>
                  <stage name="helloworld-part2">
                    <jobs>
                      <job name="run1">
                        <tasks>
                          <exec command="/bin/bash" args="helloworld.sh" workingdir="part1" />
                        </tasks>
                      </job>
                   </jobs>
                  </stage>
               </pipeline>
              </pipelines>
            </cruise>""";

    public static final String VERSION_5 =
            """
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="5">
                    <server artifactsdir='artifactsDir' /><pipelines>
                    <pipeline name='framework'>
                        <materials>
                          <svn url ="svnurl"/>
                        </materials>
                      <stage name='dist'>
                        <jobs>
                          <job name='cardlist' />
                          <job name='bluemonkeybutt'>
                            <artifacts>
                              <test src='from' dest='to'/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""";

    public static final String VERSION_7 =
            """
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="7">
                    <server artifactsdir='artifactsDir' /><pipelines>
                    <pipeline name='framework'>
                        <materials>
                          <hg url ="svnurl" dest="something">
                            <filter>
                              <ignore pattern="abc"/>
                            </filter>
                          </hg>
                        </materials>
                      <stage name='dist'>
                        <jobs>
                          <job name='cardlist' />
                          <job name='bluemonkeybutt'>
                            <artifacts>
                              <test src='from' dest='to'/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""";

    public static final String JOBS_WITH_DIFFERENT_CASE =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                    <server>
                         <artifacts>
                               <artifactsDir>artifactsDir</artifactsDir>
                         </artifacts>
                    </server>
                     <pipelines>
                         <pipeline name='framework'>
                        <materials>
                          <hg url ="svnurl" dest="something">
                          </hg>
                        </materials>
                      <stage name='dist'>
                        <jobs>
                          <job name='test'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                          <job name='Test'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String PIPELINE_WITH_TIMER =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                    <server>
                         <artifacts>
                               <artifactsDir>logs</artifactsDir>
                         </artifacts>
                    </server>
                     <pipelines>
                         <pipeline name='pipeline'>
                             <timer>0 15 10 ? * MON-FRI</timer>
                             <materials>
                                 <hg url ="svnurl" dest="something"/>
                             </materials>
                             <stage name='dist'>
                                 <jobs>
                                     <job name='test'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                                 </jobs>
                             </stage>
                         </pipeline>
                     </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static String multipleMaterial(String... materials) {
        StringBuilder sb = new StringBuilder();
        for (String material : materials) {
            sb.append(material).append('\n');
        }
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifactsDir</artifactsDir>
                     </artifacts>
                </server>
                <pipelines>
                <pipeline name='ecl'>
                    <materials>
                         <svn url ='svnurl' dest='a'/>
                    </materials>
                  <stage name='firstStage'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      <job name='bluemonkeybutt'>
                        <tasks><ant /></tasks>
                        <artifacts>
                          <artifact src='from' dest='to' type="test"/>
                        </artifacts>
                      </job>
                    </jobs>
                  </stage>
                  <stage name='secondStage'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                <pipeline name='ec2'>
                    <materials>
                         <svn url ='svnurl' dest='a'/>
                    </materials>
                  <stage name='firstStage'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      <job name='bluemonkeybutt'>
                        <tasks><ant /></tasks>
                        <artifacts>
                          <artifact src='from' dest='to' type="test"/>
                        </artifacts>
                      </job>
                    </jobs>
                  </stage>
                  <stage name='secondStage'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                <pipeline name='framework'>
                    <materials>
                %s    </materials>
                  <stage name='dist'>
                    <jobs>
                      <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      <job name='bluemonkeybutt'>
                        <tasks><ant /></tasks>
                        <artifacts>
                          <artifact src='from' dest='to' type="test"/>
                        </artifacts>
                      </job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, sb);
    }

    public static String withJob(String jobXml) {
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                <server>
                     <artifacts>
                           <artifactsDir>logs</artifactsDir>
                     </artifacts>
                </server>
                  <pipelines>
                    <pipeline name="pipeline1">
                      <materials>
                        <svn url="foobar" checkexternals="true" />
                      </materials>
                      <stage name="pre-mingle">
                       <jobs>
                          <job name="run-ant">
                            <tasks>
                              <exec command="/bin/bash" args="helloworld.sh" workingdir="part1" />
                            </tasks>
                          </job>
                        </jobs>
                      </stage>
                      <stage name="mingle">
                       <jobs>
                %s        </jobs>
                      </stage>
                    </pipeline>
                  </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, jobXml);
    }

    public static String withJob(String jobXml, String pipelineName) {
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                <server>
                     <artifacts>
                           <artifactsDir>logs</artifactsDir>
                     </artifacts>
                </server>
                <artifactStores>
                    <artifactStore pluginId="cd.go.s3" id="s3">
                        <property>
                            <key>ACCESS_KEY</key>
                            <value>some-secret-key</value>
                        </property>
                    </artifactStore>
                </artifactStores>
                  <pipelines>
                    <pipeline name="%s">
                      <materials>
                        <svn url="foobar" checkexternals="true" />
                      </materials>
                      <stage name="pre-mingle">
                       <jobs>
                          <job name="run-ant">
                            <tasks>
                              <exec command="/bin/bash" args="helloworld.sh" workingdir="part1" />
                            </tasks>
                          </job>
                        </jobs>
                      </stage>
                      <stage name="mingle">
                       <jobs>
                %s        </jobs>
                      </stage>
                    </pipeline>
                  </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION, pipelineName, jobXml);
    }

    public static final String PIPELINE_GROUPS =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="%d">
                    <server>
                         <artifacts>
                               <artifactsDir>artifactsDir</artifactsDir>
                         </artifacts>
                    </server>
                    <pipelines group="studios">
                    <pipeline name='framework'>
                        <materials>
                          <hg url ="svnurl" dest="something">
                              <filter>
                                 <ignore pattern="abc"/>
                              </filter>
                          </hg>
                        </materials>
                      <stage name='dist'>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                          <job name='bluemonkeybutt'>
                            <tasks><ant /></tasks>
                            <artifacts>
                              <artifact src='from' dest='to' type="test"/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    <pipelines group="perfessionalservice">
                    <pipeline name='framework1'>
                        <materials>
                          <hg url ="svnurl" dest="something">
                              <filter>
                                 <ignore pattern="abc"/>
                              </filter>
                          </hg>
                        </materials>
                      <stage name='dist'>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                          <job name='bluemonkeybutt'>
                            <tasks>
                              <ant />
                            </tasks>
                            <artifacts>
                              <artifact src='from' dest='to' type="test"/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String STAGE_AUTH_WITH_ADMIN_AND_AUTH =
            ("""
                    <?xml version="1.0" encoding="utf-8"?>
                    <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                    <server>
                         <artifacts>
                               <artifactsDir>artifactsDir</artifactsDir>
                         </artifacts>
                         <security>
                          <authConfigs>
                            <authConfig id="tw-ldap" pluginId="cd.go.authentication.ldap">
                             </authConfig>
                           </authConfigs>
                         <admins>
                             <user>admin</user>
                         </admins>
                         </security>
                    </server>
                    <pipelines group="studios">
                         <authorization>
                             <operate>
                                 <user>operator</user>
                             </operate>
                         </authorization>
                    <pipeline name='framework'>
                        <materials>
                          <hg url ="svnurl" dest="something" />
                        </materials>
                      <stage name='dist'>
                         <approval type='manual'>
                             <authorization>
                                 <user>admin</user>
                             </authorization>
                         </approval>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""").formatted(CONFIG_SCHEMA_VERSION);

    public static final String VALID_XML_3169 = """
            <?xml version="1.0" encoding="utf-8"?>
            <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion="14">
              <server artifactsdir="artifacts">
                <license user="Go UAT Thoughtworks">dc7Q7ii7wQA7O8UxZAnud3ZFDi09MDaXYCwNZjjVyyhnZXK1kAQIZ4U+k/Tt
            wCbfAmLCizhqvNvC3ZYCsa2zKfA26f+tUnc0WjRBK6ttfTVl9M9t08t+ZcAI
            JhtONBURkA3YumffkxyAaPdPJq5tMaZYWjaX1pBpGlG0LjR+HwAkZnteTYeI
            XMd7w0z741K8irGi3fLY+pyc8VB0jnv0J8tSWamL2sjy6irkuSw9q70PAtxb
            q7MZBEkIaT3VFpehkyMvutKFUC6igyET3kd5WJoxeXj0W5ZucFGgKgFYlLNa
            UQwnhuako+UAXhDhvMa2ud+fARbyZJasjGQQ77w6NQ==</license>
              </server>
              <pipelines group="12345">
                <pipeline name="test">
                  <materials>
                    <hg url="http://hg-server/hg/connectfour" />
                  </materials>
                  <stage name="defaultStage">
                    <jobs>
                      <job name="defaultJob">
                        <tasks>
                          <exec command="echo">
                            <runif status="passed" />
                            <arg value="test" />
                          </exec>
                        </tasks>
                      </job>
                    </jobs>
                  </stage>
                </pipeline>
              </pipelines>
              </cruise>""";

    public static final String WITH_VMMS_CONFIG =
            """
                    <cruise schemaVersion='50'>
                    <server artifactsdir='artifactsDir' /><pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url ="svnurl"/>
                        </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                          <job name='bluemonkeybutt'>
                            <artifacts>
                              <log src='from' dest='to'/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    <vmms>
                    <ec2 accessKey='test' secretAccessKey='test'>
                    <ami imageId='test' />
                    </ec2>
                    </vmms>
                    </cruise>""";

    public static final String WITH_LOG_ARTIFACT_CONFIG =
            """
                    <cruise schemaVersion='50'>
                    <server artifactsdir='artifactsDir' /><pipelines>
                    <pipeline name='pipeline1'>
                        <materials>
                          <svn url ="svnurl"/>
                        </materials>
                      <stage name='mingle'>
                        <jobs>
                          <job name='cardlist'>
                            <tasks>
                              <exec command='echo'>
                                <runif status='passed' />
                              </exec>
                            </tasks>
                          </job>
                          <job name='bluemonkeybutt'>
                            <artifacts>
                              <log src='from1' />
                              <log src='from2' dest='to2'/>
                              <artifact src='from3'/>
                              <artifact src='from4' dest='to4'/>
                            </artifacts>
                          </job>
                        </jobs>
                      </stage>
                    </pipeline>
                    </pipelines>
                    </cruise>""";

    public static CruiseConfig configWith(PipelineConfigs... pipelineConfigses) {
        return new BasicCruiseConfig(pipelineConfigses);
    }

    public static CruiseConfig configWith(PipelineConfig... pipelineConfigs) {
        BasicPipelineConfigs configs = new BasicPipelineConfigs();
        configs.setGroup("defaultGroup");
        configs.addAll(List.of(pipelineConfigs));
        return new BasicCruiseConfig(configs);
    }

    public static String configWithSecurity(String security) {
        String defaultArtifact = "<artifacts><artifactsDir>logs</artifactsDir></artifacts>";
        return ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                  <server>
                %s%s  </server>
                  </cruise>""").formatted(CONFIG_SCHEMA_VERSION, security, defaultArtifact);
    }

    public static String configWithArtifactSourceAs(String artifactSource) {
        return String.format(CONFIG_WITH_ARTIFACT_SRC, artifactSource);
    }

    public static String pipelineWithAttributes(String pipelineTagAttributes, int schemaVersion) {
        return ("""
                <?xml version="1.0" encoding="UTF-8"?><cruise schemaVersion="%d">
                <pipelines>
                  <pipeline %s>
                    <materials>
                      <git url="git1"/>
                    </materials>
                    <stage name="stage1">
                      <jobs>
                        <job name="job1">
                          <tasks>
                            <ant/>
                          </tasks>
                        </job>
                      </jobs>
                    </stage>
                  </pipeline>
                </pipelines>
                </cruise>""").formatted(schemaVersion, pipelineTagAttributes);
    }
}
