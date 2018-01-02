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

package com.thoughtworks.go.server.presentation.models;

import java.util.Date;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.util.DateUtils;
import org.jdom2.Element;

public class StageCctrayPresentationModel {
    private final Pipeline pipeline;
    private final Stage stage;

    public StageCctrayPresentationModel(Pipeline pipeline, Stage stage) {
        this.pipeline = pipeline;
        this.stage = stage;
    }

    public void toCctrayXml(Element element, String cruiseContextPath) {
        createStageProject(element, cruiseContextPath);
        for (JobInstance jobInstance : stage.getJobInstances()) {
            createBuildProject(jobInstance, element, cruiseContextPath);
        }
    }

    private void createStageProject(Element parent, String cruiseContextPath) {
        String name = stageName();
        String activity = stageActivity();
        String lastBuildStatus = stage.stageState().cctrayStatus();
        String lastBuildLabel = pipeline.getLabel();
        String lastBuildTime = datetimeForCctray(stage.completedDate());
        String webUrl = cruiseContextPath + "/pipelines/" + stage.getId();

        createProjectElement(parent, name, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, webUrl);
    }

    private String datetimeForCctray(Date date) {
        return date == null ?
                DateUtils.formatIso8601ForCCTray(stage.scheduledDate()) : DateUtils.formatIso8601ForCCTray(date);
    }

    private void createBuildProject(JobInstance jobInstance, Element parent, String cruiseContextPath) {
        String name = buildName(jobInstance);
        String activity = jobInstance.getState().cctrayActivity();
        String lastBuildStatus = jobInstance.getResult().toCctrayStatus();
        String lastBuildLabel = pipeline.getLabel();
        String lastBuildTime = datetimeForCctray(jobInstance.getStartedDateFor(JobState.Completed));
        String webUrl = cruiseContextPath + "/tab/build/detail/" + jobInstance.getId();

        createProjectElement(parent, name, activity, lastBuildStatus, lastBuildLabel, lastBuildTime, webUrl);
    }

    private void createProjectElement(Element parent, String name, String activity, String lastBuildStatus,
                                      String lastBuildLabel, String lastBuildTime, String webUrl) {
        Element project = new Element("Project");
        project.setAttribute("name", name);
        project.setAttribute("activity", activity);
        project.setAttribute("lastBuildStatus", lastBuildStatus);
        project.setAttribute("lastBuildLabel", lastBuildLabel);
        project.setAttribute("lastBuildTime", lastBuildTime);
        project.setAttribute("webUrl", webUrl);
        parent.addContent(project);
    }

    String stageActivity() {
        return stage.stageState() == StageState.Building ? "Building" : "Sleeping";
    }

    private String stageName() {
        return pipeline.getName() + " :: " + stage.getName();
    }

    private String buildName(JobInstance jobInstance) {
        return pipeline.getName() + " :: " + stage.getName() + " :: " + jobInstance.getName();
    }

}
