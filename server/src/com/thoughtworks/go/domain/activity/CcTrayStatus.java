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

package com.thoughtworks.go.domain.activity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.JobStatusListener;
import com.thoughtworks.go.server.domain.StageStatusListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CcTrayStatus implements JobStatusListener, StageStatusListener {
    private Map<String, ProjectStatus> projects = new ConcurrentHashMap<String, ProjectStatus>();
    private List<String> noActivityYetList = new ArrayList<String>();
    private final MaterialRepository materialRepository;
    private final StageDao stageDao;

    @Autowired
    public CcTrayStatus(MaterialRepository materialRepository, StageDao stageDao) {
        this.materialRepository = materialRepository;
        this.stageDao = stageDao;
    }

    public void jobStatusChanged(JobInstance job) {
        jobStatusChanged(job, new HashSet<String>());
    }

    private void jobStatusChanged(JobInstance job, Set<String> breakers) {
        String projectName = job.getIdentifier().ccProjectName();
        projects.put(projectName, new ProjectStatus(projectName,
                job.getState().cctrayActivity(),
                lastBuildStatus(projectName, job),
                lastBuildLabel(projectName, job),
                lastBuildTime(projectName, job),
                job.getIdentifier().webUrl(),
                breakers));
    }

    public void stageStatusChanged(Stage stage) {
        if (stage instanceof NullStage) {
            return;
        }
        String projectName = stage.getIdentifier().ccProjectName();
        Set<String> breakers = computeBreakersIfStageFailed(stage,  materialRepository.findMaterialRevisionsForPipeline(stage.getPipelineId()));
        cacheStage(stage, projectName, breakers);

        noActivityYetList.remove(projectName);

        for (JobInstance jobInstance : stage.getJobInstances()) {
            Set<String> jobBreakers = jobInstance.getResult() == JobResult.Failed ? breakers : new HashSet<String>();
            jobStatusChanged(jobInstance, jobBreakers);
        }
    }

    private void cacheStage(Stage stage, String projectName, Set<String> breakers) {
        projects.put(projectName, new ProjectStatus(
                projectName,
                stage.stageState().cctrayActivity(),
                lastBuildStatus(projectName, stage),
                lastBuildLabel(projectName, stage),
                lastBuildTime(projectName, stage),
                stage.getIdentifier().webUrl(), breakers));
    }

    public static Set<String> computeBreakersIfStageFailed(Stage stage, MaterialRevisions materialRevisions) {
        Set<String> breakersForChangedMaterials = new HashSet<String>();
        Set<String> breakersForMaterialsWithNoChange = new HashSet<String>();
        if (stage.getResult() == StageResult.Failed) {
            for (MaterialRevision materialRevision : materialRevisions) {
                if (materialRevision.isChanged()) {
                    addToBreakers(breakersForChangedMaterials, materialRevision);
                }
                else {
                    addToBreakers(breakersForMaterialsWithNoChange, materialRevision);
                }
            }
        }
        return breakersForChangedMaterials.isEmpty() ? breakersForMaterialsWithNoChange : breakersForChangedMaterials;
    }

    private static void addToBreakers(Set<String> breakers, MaterialRevision materialRevision) {
        for (Modification modification : materialRevision.getModifications()) {
            Author authorInfo = Author.getAuthorInfo(materialRevision.getMaterial().getType(), modification);
            if(authorInfo != null) {
                breakers.add(authorInfo.getName());
            }
        }
    }

    public boolean containsProject(String projectName) {
        return projects.containsKey(projectName);
    }

    //used in tests
    public ProjectStatus getProject(String projectName) {
        return projects.get(projectName);
    }

    public void removeProjectsNotIn(Set<String> activeProjects) {
        Set<String> all = new HashSet<String>(projects.keySet());
        for (String projectName : all) {
            if (!activeProjects.contains(projectName)) {
                projects.remove(projectName);
            }
        }
    }

    //used in tests
    public void clear() {
        projects = new Hashtable<String, ProjectStatus>();
    }

    //used in tests
    public Collection<ProjectStatus> projects() {
        return projects.values();
    }

    private String lastBuildStatus(String projectName, JobInstance job) {
        return job.getState().isCompleted()
                ? job.getResult().toCctrayStatus()
                : projectByName(projectName).getLastBuildStatus();
    }

    private Date lastBuildTime(String projectName, JobInstance job) {
        return job.isCompleted()
                ? job.getStartedDateFor(JobState.Completed)
                : projectByName(projectName).getLastBuildTime();
    }

    private String lastBuildLabel(String projectName, JobInstance job) {
        return job.isCompleted()
                ? job.getIdentifier().getStageIdentifier().ccTrayLastBuildLabel()
                : projectByName(projectName).getLastBuildLabel();
    }

    private String lastBuildStatus(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.stageState().cctrayStatus()
                : projectByName(projectName).getLastBuildStatus();
    }

    private Date lastBuildTime(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.completedDate()
                : projectByName(projectName).getLastBuildTime();
    }

    private String lastBuildLabel(String projectName, Stage stage) {
        return stage.stageState().completed()
                ? stage.getIdentifier().ccTrayLastBuildLabel()
                : projectByName(projectName).getLastBuildLabel();
    }

    private ProjectStatus projectByName(String projectName) {
        ProjectStatus projectStatus = projects.get(projectName);
        return projectStatus == null ? new ProjectStatus.NullProjectStatus(projectName) : projectStatus;
    }

    public void dumpProject(String projectName, List<ProjectStatus> result) {
        if (containsProject(projectName)) {
            result.add(projects.get(projectName));
        }
    }

    public void updateStatusFor(String stageProjectName, Stage stage) {
        if (stage instanceof NullStage) {
            noActivityYetList.add(stageProjectName);
        } else {
            stageStatusChanged(stage);//Cache it with the right thing
        }
    }

    public boolean hasNoActivityFor(String projectName) {
        return noActivityYetList.contains(projectName);
    }
}
