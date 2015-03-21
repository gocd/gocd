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
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.notification.NotificationExtension;
import com.thoughtworks.go.plugin.access.notification.NotificationPluginRegistry;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageStatusPluginNotifier implements StageStatusListener {
    public static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private NotificationPluginRegistry notificationPluginRegistry;
    private GoConfigService goConfigService;
    private PipelineSqlMapDao pipelineSqlMapDao;
    private PluginNotificationQueue pluginNotificationQueue;

    @Autowired
    public StageStatusPluginNotifier(NotificationPluginRegistry notificationPluginRegistry, GoConfigService goConfigService, PipelineSqlMapDao pipelineSqlMapDao, PluginNotificationQueue pluginNotificationQueue) {
        this.notificationPluginRegistry = notificationPluginRegistry;
        this.goConfigService = goConfigService;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.pluginNotificationQueue = pluginNotificationQueue;
    }

    @Override
    public void stageStatusChanged(final Stage stage) {
        if (notificationPluginRegistry.isAnyPluginInterestedIn(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION)) {
            Map data = createRequestDataMap(stage);

            pluginNotificationQueue.post(new PluginNotificationMessage(NotificationExtension.STAGE_STATUS_CHANGE_NOTIFICATION, data));
        }
    }

    Map createRequestDataMap(Stage stage) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        String pipelineName = stage.getIdentifier().getPipelineName();
        Integer pipelineCounter = new Integer(stage.getIdentifier().getPipelineCounter());

        String pipelineGroup = goConfigService.findGroupNameByPipeline(new CaseInsensitiveString(pipelineName));

        data.put("pipeline-group", pipelineGroup);
        data.put("pipeline-name", pipelineName);
        data.put("pipeline-counter", pipelineCounter.toString());
        data.put("stage-name", stage.getIdentifier().getStageName());
        data.put("stage-counter", stage.getIdentifier().getStageCounter());
        data.put("stage-state", stage.getState().toString());
        data.put("stage-result", stage.getResult().toString());

        Map<String, Object> pipelineMap = createPipelineDataMap(pipelineName, pipelineCounter, stage);
        data.put("pipeline", pipelineMap);

        return data;
    }

    private Map<String, Object> createPipelineDataMap(String pipelineName, Integer pipelineCounter, Stage stage) {
        Map<String, Object> pipelineMap = new LinkedHashMap<String, Object>();
        pipelineMap.put("name", pipelineName);
        pipelineMap.put("counter", pipelineCounter.toString());

        BuildCause buildCause = pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(pipelineName, pipelineCounter);
        List<Map> materialRevisionList = createBuildCauseDataMap(buildCause.getMaterialRevisions());
        pipelineMap.put("build-cause", materialRevisionList);

        Map<String, Object> stageMap = createStageDataMap(stage);
        pipelineMap.put("stage", stageMap);

        return pipelineMap;
    }

    private List<Map> createBuildCauseDataMap(MaterialRevisions materialRevisions) {
        List<Map> materialRevisionList = new ArrayList<Map>();
        for (MaterialRevision currentRevision : materialRevisions) {
            Map<String, Object> materialRevisionMap = new LinkedHashMap<String, Object>();

            Map<String, Object> materialMap = createMaterialDataMap(currentRevision.getMaterial());
            materialRevisionMap.put("material", materialMap);
            materialRevisionMap.put("changed", currentRevision.isChanged());

            List<Map> modificationList = new ArrayList<Map>();
            for (Modification modification : currentRevision.getModifications()) {
                Map<String, Object> modificationMap = createModificationDataMap(modification);
                modificationList.add(modificationMap);
            }
            materialRevisionMap.put("modifications", modificationList);

            materialRevisionList.add(materialRevisionMap);
        }
        return materialRevisionList;
    }

    private Map<String, Object> createMaterialDataMap(Material material) {
        Map<String, Object> materialMap = new LinkedHashMap<String, Object>();
        if (material instanceof GitMaterial) {
            materialMap.put("type", "git");
            Map<String, Object> gitMap = getGitDataMap((GitMaterial) material);
            materialMap.put("git-configuration", gitMap);
        } else if (material instanceof HgMaterial) {
            materialMap.put("type", "mercurial");
            Map<String, Object> mercurialMap = createMercurialDataMap((HgMaterial) material);
            materialMap.put("mercurial-configuration", mercurialMap);
        } else if (material instanceof SvnMaterial) {
            materialMap.put("type", "svn");
            Map<String, Object> svnMap = createSVNDataMap((SvnMaterial) material);
            materialMap.put("svn-configuration", svnMap);
        } else if (material instanceof TfsMaterial) {
            materialMap.put("type", "tfs");
            Map<String, Object> tfsMap = createTFSDataMap((TfsMaterial) material);
            materialMap.put("tfs-configuration", tfsMap);
        } else if (material instanceof P4Material) {
            materialMap.put("type", "perforce");
            Map<String, Object> perforceMap = createPerforceDataMap((P4Material) material);
            materialMap.put("perforce-configuration", perforceMap);
        } else if (material instanceof DependencyMaterial) {
            materialMap.put("type", "pipeline");
            Map<String, Object> pipelineMap = createPipelineMaterialDataMap((DependencyMaterial) material);
            materialMap.put("pipeline-configuration", pipelineMap);
        } else if (material instanceof PackageMaterial) {
            PackageDefinition packageConfig = ((PackageMaterial) material).getPackageDefinition();
            PackageRepository repositoryConfig = packageConfig.getRepository();
            materialMap.put("type", "package");
            materialMap.put("plugin-id", repositoryConfig.getPluginConfiguration().getId());
            Map<String, Object> repositoryConfigurationMap = createConfigurationDataMap(repositoryConfig.getConfiguration());
            materialMap.put("repository-configuration", repositoryConfigurationMap);
            Map<String, Object> packageConfigurationMap = createConfigurationDataMap(packageConfig.getConfiguration());
            materialMap.put("package-configuration", packageConfigurationMap);
        } else if (material instanceof PluggableSCMMaterial) {
            SCM scmConfig = ((PluggableSCMMaterial) material).getScmConfig();
            materialMap.put("type", "scm");
            materialMap.put("plugin-id", scmConfig.getPluginConfiguration().getId());
            Map<String, Object> configurationMap = createConfigurationDataMap(scmConfig.getConfiguration());
            materialMap.put("scm-configuration", configurationMap);
        }
        return materialMap;
    }

    private Map<String, Object> getGitDataMap(GitMaterial material) {
        Map<String, Object> gitMap = new LinkedHashMap<String, Object>();
        gitMap.put("url", material.getUrl());
        gitMap.put("branch", material.getBranch());
        return gitMap;
    }

    private Map<String, Object> createMercurialDataMap(HgMaterial material) {
        Map<String, Object> mercurialMap = new LinkedHashMap<String, Object>();
        mercurialMap.put("url", material.getUrl());
        return mercurialMap;
    }

    private Map<String, Object> createSVNDataMap(SvnMaterial material) {
        Map<String, Object> svnMap = new LinkedHashMap<String, Object>();
        svnMap.put("url", material.getUrl());
        svnMap.put("username", material.getUserName());
        svnMap.put("password", material.getPassword());
        svnMap.put("check-externals", material.isCheckExternals());
        return svnMap;
    }

    private Map<String, Object> createTFSDataMap(TfsMaterial material) {
        Map<String, Object> tfsMap = new LinkedHashMap<String, Object>();
        tfsMap.put("url", material.getUrl());
        tfsMap.put("domain", material.getDomain());
        tfsMap.put("username", material.getUserName());
        tfsMap.put("password", material.getPassword());
        tfsMap.put("project-path", material.getProjectPath());
        return tfsMap;
    }

    private Map<String, Object> createPerforceDataMap(P4Material material) {
        Map<String, Object> perforceMap = new LinkedHashMap<String, Object>();
        perforceMap.put("url", material.getUrl());
        perforceMap.put("username", material.getUserName());
        perforceMap.put("password", material.getPassword());
        perforceMap.put("view", material.getView());
        perforceMap.put("use-tickets", material.getUseTickets());
        return perforceMap;
    }

    private Map<String, Object> createPipelineMaterialDataMap(DependencyMaterial material) {
        Map<String, Object> gitMap = new LinkedHashMap<String, Object>();
        gitMap.put("pipeline-name", material.getPipelineName().toString());
        gitMap.put("stage-name", material.getStageName().toString());
        return gitMap;
    }

    private Map<String, Object> createConfigurationDataMap(Configuration configurations) {
        Map<String, Object> configurationMap = new LinkedHashMap<String, Object>();
        for (ConfigurationProperty currentConfiguration : configurations) {
            configurationMap.put(currentConfiguration.getConfigKeyName(), currentConfiguration.getValue());
        }
        return configurationMap;
    }

    private Map<String, Object> createModificationDataMap(Modification modification) {
        Map<String, Object> modificationMap = new LinkedHashMap<String, Object>();
        modificationMap.put("revision", modification.getRevision());
        modificationMap.put("modified-time", dateToString(modification.getModifiedTime()));
        modificationMap.put("data", modification.getAdditionalDataMap());
        return modificationMap;
    }

    private Map<String, Object> createStageDataMap(Stage stage) {
        Map<String, Object> stageMap = new LinkedHashMap<String, Object>();
        stageMap.put("name", stage.getName());
        stageMap.put("counter", new Integer(stage.getCounter()).toString());
        stageMap.put("state", stage.getState().toString());
        stageMap.put("result", stage.getResult().toString());
        stageMap.put("create-time", timestampToString(stage.getCreatedTime()));
        stageMap.put("last-transition-time", timestampToString(stage.getLastTransitionedTime()));

        List<Map> jobsList = new ArrayList<Map>();
        for (JobInstance currentJob : stage.getJobInstances()) {
            Map<String, Object> jobMap = createJobDataMap(currentJob);
            jobsList.add(jobMap);
        }
        stageMap.put("jobs", jobsList);

        return stageMap;
    }

    private Map<String, Object> createJobDataMap(JobInstance job) {
        Map<String, Object> jobMap = new LinkedHashMap<String, Object>();
        jobMap.put("name", job.getName());
        jobMap.put("schedule-time", dateToString(job.getScheduledDate()));
        jobMap.put("complete-time", dateToString(job.getCompletedDate()));
        jobMap.put("state", job.getState().toString());
        jobMap.put("result", job.getResult().toString());
        jobMap.put("agent-uuid", job.getAgentUuid());
        return jobMap;
    }

    private String timestampToString(Timestamp timestamp) {
        return timestamp == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(timestamp);
    }

    private String dateToString(Date date) {
        return date == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}
