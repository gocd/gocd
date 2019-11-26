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

import {PipelineActivity} from "../pipeline_activity";
import {PipelineActivityData} from "./test_data";

describe("PipelineActivity", () => {
  it("should parse pipeline activity json", () => {
    const activity = PipelineActivity.fromJSON(PipelineActivityData.oneStage());

    expect(activity.pipelineName()).toEqual("up43");
    expect(activity.paused()).toEqual(false);
    expect(activity.pauseCause()).toEqual("");
    expect(activity.pauseBy()).toEqual("");
    expect(activity.canForce()).toEqual(true);
    expect(activity.nextLabel()).toEqual("");
    expect(activity.forcedBuild()).toEqual(false);
    expect(activity.showForceBuildButton()).toEqual(false);
    expect(activity.canPause()).toEqual(true);
    expect(activity.count()).toEqual(1);
    expect(activity.start()).toEqual(0);
    expect(activity.perPage()).toEqual(10);

    expect(activity.groups()).toHaveLength(1);

    const group = activity.groups()[0];
    expect(group.config().stages()).toHaveLength(1);
    expect(group.config().stages()[0].name()).toEqual("up42_stage");
    expect(group.config().stages()[0].isAutoApproved()).toEqual(true);

    expect(group.history()).toHaveLength(1);

    const history = group.history()[0];
    expect(history.pipelineId()).toEqual(42);
    expect(history.label()).toEqual("1");
    expect(history.counterOrLabel()).toEqual("1");
    expect(history.scheduledDate()).toEqual("22 Nov, 2019 at 1:5:59 [+0530]");
    expect(history.scheduledTimestamp()).toEqual(new Date(1574404139615));
    expect(history.buildCauseBy()).toEqual("Triggered by changes");
    expect(history.modificationDate()).toEqual("about 22 hours ago");
    expect(history.revision()).toEqual("b0982fa2ff92d126ad003c9e007959b4b8dd96a9");
    expect(history.comment()).toEqual("Initial commit");

    expect(history.stages()).toHaveLength(1);
    expect(history.stages()[0].stageId()).toEqual(96);
    expect(history.stages()[0].stageStatus()).toEqual("Passed");
    expect(history.stages()[0].stageLocator()).toEqual("up43/1/up42_stage/1");
    expect(history.stages()[0].getCanRun()).toEqual(true);
    expect(history.stages()[0].getCanCancel()).toEqual(false);
    expect(history.stages()[0].scheduled()).toEqual(true);
    expect(history.stages()[0].stageCounter()).toEqual(1);
    expect(history.stages()[0].approvedBy()).toEqual("changes");

    expect(history.materialRevisions()).toHaveLength(1);
    const materialRevision = history.materialRevisions()[0];
    expect(materialRevision.revision()).toEqual("b0982fa2ff92d126ad003c9e007959b4b8dd96a9");
    expect(materialRevision.revisionHref()).toEqual("b0982fa2ff92d126ad003c9e007959b4b8dd96a9");
    expect(materialRevision.user()).toEqual("Bob <bob@go.cd>");
    expect(materialRevision.date()).toEqual("2019-11-21T1:3:20+0:30");
    expect(materialRevision.changed()).toEqual(true);
    expect(materialRevision.folder()).toEqual("");
    expect(materialRevision.scmType()).toEqual("Git");
    expect(materialRevision.location()).toEqual("http://github.com/bdpiparva/sample_repo");
    expect(materialRevision.action()).toEqual("Modified");
  });

});
