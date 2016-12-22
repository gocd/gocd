/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.messaging.plugin;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.service.GoConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class StageStatusPluginNotifierTest {
    @Mock
    private NotificationPluginRegistry notificationPluginRegistry;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock
    private PluginNotificationQueue pluginNotificationQueue;
    @Mock
    private Stage stage;

    private ArgumentCaptor<PluginNotificationMessage> pluginNotificationMessage;
    private StageStatusPluginNotifier stageStatusPluginNotifier;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        pluginNotificationMessage = ArgumentCaptor.forClass(PluginNotificationMessage.class);

        stageStatusPluginNotifier = new StageStatusPluginNotifier(notificationPluginRegistry, goConfigService, pipelineSqlMapDao, pluginNotificationQueue);
    }

    @Test
    public void shouldNotifyInterestedPluginsCorrectly() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        doNothing().when(pluginNotificationQueue).post(pluginNotificationMessage.capture());
        Pipeline pipeline = createPipeline();
        when(goConfigService.findGroupNameByPipeline(new CaseInsensitiveString("pipeline-name"))).thenReturn("pipeline-group");
        when(pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter("pipeline-name", 1)).thenReturn(pipeline.getBuildCause());

        stageStatusPluginNotifier.stageStatusChanged(pipeline.getFirstStage());

        PluginNotificationMessage message = pluginNotificationMessage.getValue();
        assertThat(message.getRequestName(), is(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION));
        Map requestMap = message.getRequestData();

        Map pipelineMap = (Map) requestMap.get("pipeline");
        assertThat((String) pipelineMap.get("name"), is("pipeline-name"));
        assertThat((String) pipelineMap.get("counter"), is("1"));

        assertThat((String) pipelineMap.get("group"), is("pipeline-group"));
        assertThat((String) pipelineMap.get("label"), is ("LABEL-1"));

        List revisionsList = (List) pipelineMap.get("build-cause");
        Map gitRevisionMap = (Map) revisionsList.get(0);
        Map gitMaterialMap = (Map) gitRevisionMap.get("material");
        assertThat((String) gitMaterialMap.get("type"), is("git"));
        Map gitConfigurationMap = (Map) gitMaterialMap.get("git-configuration");
        assertThat((String) gitConfigurationMap.get("url"), is("http://user:******@gitrepo.com"));
        assertThat((String) gitConfigurationMap.get("branch"), is("branch"));
        assertModification(gitRevisionMap, "1");

        Map hgRevisionMap = (Map) revisionsList.get(1);
        Map hgMaterialMap = (Map) hgRevisionMap.get("material");
        assertThat((String) hgMaterialMap.get("type"), is("mercurial"));
        Map hgConfigurationMap = (Map) hgMaterialMap.get("mercurial-configuration");
        assertThat((String) hgConfigurationMap.get("url"), is("http://user:******@hgrepo.com"));
        assertModification(hgRevisionMap, "1");

        Map svnRevisionMap = (Map) revisionsList.get(2);
        Map svnMaterialMap = (Map) svnRevisionMap.get("material");
        assertThat((String) svnMaterialMap.get("type"), is("svn"));
        Map svnConfigurationMap = (Map) svnMaterialMap.get("svn-configuration");
        assertThat((String) svnConfigurationMap.get("url"), is("http://user:******@svnrepo.com"));
        assertThat((String) svnConfigurationMap.get("username"), is("username"));
        assertThat(svnConfigurationMap.get("password"), is(nullValue()));
        assertThat((Boolean) svnConfigurationMap.get("check-externals"), is(false));
        assertModification(svnRevisionMap, "1");

        Map tfsRevisionMap = (Map) revisionsList.get(3);
        Map tfsMaterialMap = (Map) tfsRevisionMap.get("material");
        assertThat((String) tfsMaterialMap.get("type"), is("tfs"));
        Map tfsConfigurationMap = (Map) tfsMaterialMap.get("tfs-configuration");
        assertThat((String) tfsConfigurationMap.get("url"), is("http://user:******@tfsrepo.com"));
        assertThat((String) tfsConfigurationMap.get("domain"), is("domain"));
        assertThat((String) tfsConfigurationMap.get("username"), is("username"));
        assertThat(tfsConfigurationMap.get("password"), is(nullValue()));
        assertThat((String) tfsConfigurationMap.get("project-path"), is("project-path"));
        assertModification(tfsRevisionMap, "1");

        Map p4RevisionMap = (Map) revisionsList.get(4);
        Map p4MaterialMap = (Map) p4RevisionMap.get("material");
        assertThat((String) p4MaterialMap.get("type"), is("perforce"));
        Map p4ConfigurationMap = (Map) p4MaterialMap.get("perforce-configuration");
        assertThat((String) p4ConfigurationMap.get("url"), is("127.0.0.1:1666"));
        assertThat((String) p4ConfigurationMap.get("username"), is("username"));
        assertThat(p4ConfigurationMap.get("password"), is(nullValue()));
        assertThat((String) p4ConfigurationMap.get("view"), is("view"));
        assertThat((Boolean) p4ConfigurationMap.get("use-tickets"), is(false));
        assertModification(p4RevisionMap, "1");

        Map pipelineRevisionMap = (Map) revisionsList.get(5);
        Map pipelineMaterialMap = (Map) pipelineRevisionMap.get("material");
        assertThat((String) pipelineMaterialMap.get("type"), is("pipeline"));
        Map pipelineConfigurationMap = (Map) pipelineMaterialMap.get("pipeline-configuration");
        assertThat((String) pipelineConfigurationMap.get("pipeline-name"), is("pipeline-name"));
        assertThat((String) pipelineConfigurationMap.get("stage-name"), is("stage-name"));
        assertModification(pipelineRevisionMap, "pipeline-name/1/stage-name/1");

        Map packageRevisionMap = (Map) revisionsList.get(6);
        Map packageMaterialMap = (Map) packageRevisionMap.get("material");
        assertThat((String) packageMaterialMap.get("type"), is("package"));
        assertThat((String) packageMaterialMap.get("plugin-id"), is("pluginid"));
        Map repositoryConfigurationMap = (Map) packageMaterialMap.get("repository-configuration");
        assertThat((String) repositoryConfigurationMap.get("k1"), is("repo-v1"));
        assertThat(repositoryConfigurationMap.get("k2"), is(nullValue()));
        Map packageConfigurationMap = (Map) packageMaterialMap.get("package-configuration");
        assertThat((String) packageConfigurationMap.get("k3"), is("package-v1"));
        assertThat(packageConfigurationMap.get("k4"), is(nullValue()));
        assertModification(packageRevisionMap, "1");

        Map scmRevisionMap = (Map) revisionsList.get(7);
        Map scmMaterialMap = (Map) scmRevisionMap.get("material");
        assertThat((String) scmMaterialMap.get("type"), is("scm"));
        assertThat((String) scmMaterialMap.get("plugin-id"), is("pluginid"));
        Map scmConfigurationMap = (Map) scmMaterialMap.get("scm-configuration");
        assertThat((String) scmConfigurationMap.get("k1"), is("v1"));
        assertThat(scmConfigurationMap.get("k2"), is(nullValue()));
        assertModification(scmRevisionMap, "1");

        Map stageMap = (Map) pipelineMap.get("stage");
        assertThat((String) stageMap.get("name"), is("stage-name"));
        assertThat((String) stageMap.get("counter"), is("1"));
        assertThat((String) stageMap.get("approval-type"), is("success"));
        assertThat((String) stageMap.get("approved-by"), is("changes"));
        assertThat((String) stageMap.get("state"), is("Passed"));
        assertThat((String) stageMap.get("result"), is("Passed"));
        assertThat((String) stageMap.get("create-time"), is("2011-07-13T19:43:37.100Z"));
        assertThat((String) stageMap.get("last-transition-time"), is("2011-07-13T19:43:37.100Z"));

        List jobsList = (List) stageMap.get("jobs");
        Map jobMap = (Map) jobsList.get(0);
        assertThat((String) jobMap.get("name"), is("job-name"));
        assertThat((String) jobMap.get("schedule-time"), is("2011-07-13T19:43:37.100Z"));
        assertThat((String) jobMap.get("complete-time"), is("2011-07-13T19:43:37.100Z"));
        assertThat((String) jobMap.get("state"), is("Completed"));
        assertThat((String) jobMap.get("result"), is("Passed"));
        assertThat((String) jobMap.get("agent-uuid"), is("uuid"));
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfNoPluginIsInterested() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(false);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue, never()).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotNotifyInterestedPluginsIfStageStateIsNotScheduledOrCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Unknown);

        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue, never()).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(true);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Unknown);

        stageStatusPluginNotifier = getMockedStageStatusPluginNotifier();
        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsReScheduled() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(true);
        when(stage.getState()).thenReturn(StageState.Unknown);

        stageStatusPluginNotifier = getMockedStageStatusPluginNotifier();
        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(any(PluginNotificationMessage.class));
    }

    @Test
    public void shouldNotifyInterestedPluginsIfStageStateIsCompleted() throws Exception {
        when(notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)).thenReturn(true);
        when(stage.isScheduled()).thenReturn(false);
        when(stage.isReRun()).thenReturn(false);
        when(stage.getState()).thenReturn(StageState.Passed);

        stageStatusPluginNotifier = getMockedStageStatusPluginNotifier();
        stageStatusPluginNotifier.stageStatusChanged(stage);

        verify(pluginNotificationQueue).post(any(PluginNotificationMessage.class));
    }

    private Pipeline createPipeline() throws Exception {
        Pipeline pipeline = PipelineMother.pipelineWithAllTypesOfMaterials("pipeline-name", "stage-name", "job-name");
        List<MaterialRevision> materialRevisions = pipeline.getMaterialRevisions().getRevisions();
        PackageDefinition packageDefinition = ((PackageMaterial) materialRevisions.get(6).getMaterial()).getPackageDefinition();
        packageDefinition.getRepository().getConfiguration().get(1).handleSecureValueConfiguration(true);
        packageDefinition.getConfiguration().addNewConfigurationWithValue("k4", "package-v2", false);
        packageDefinition.getConfiguration().get(1).handleSecureValueConfiguration(true);
        SCM scm = ((PluggableSCMMaterial) materialRevisions.get(7).getMaterial()).getScmConfig();
        scm.getConfiguration().get(1).handleSecureValueConfiguration(true);
        Stage stage = pipeline.getFirstStage();
        stage.setId(1L);
        stage.setPipelineId(1L);
        stage.setCreatedTime(new Timestamp(getFixedDate().getTime()));
        stage.setLastTransitionedTime(new Timestamp(getFixedDate().getTime()));
        JobInstance job = stage.getJobInstances().get(0);
        job.setScheduledDate(getFixedDate());
        job.getTransition(JobState.Completed).setStateChangeTime(getFixedDate());
        return pipeline;
    }

    private Date getFixedDate() throws Exception {
        return new SimpleDateFormat(StageStatusPluginNotifier.DATE_PATTERN).parse("2011-07-13T19:43:37.100Z");
    }

    private void assertModification(Map revisionMap, String revision) {
        assertThat((Boolean) revisionMap.get("changed"), is(true));

        List modificationList = (List) revisionMap.get("modifications");
        Map modificationMap = (Map) modificationList.get(0);
        assertThat((String) modificationMap.get("revision"), is(revision));
        assertThat(modificationMap.get("modified-time"), is(notNullValue()));
        assertThat(modificationMap.get("data"), is(notNullValue()));
    }

    private StageStatusPluginNotifier getMockedStageStatusPluginNotifier() {
        return new StageStatusPluginNotifier(notificationPluginRegistry, goConfigService, pipelineSqlMapDao, pluginNotificationQueue) {
            Map createRequestDataMap(Stage stage) {
                return null;
            }
        };
    }
}
