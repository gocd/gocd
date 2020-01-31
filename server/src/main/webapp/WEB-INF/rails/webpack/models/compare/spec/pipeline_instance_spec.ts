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

import {BuildCause, MaterialRevision, MaterialRevisions, PipelineHistory, PipelineInstance, PipelineInstances} from "../pipeline_instance";
import {MaterialRevisionJSON, PipelineHistoryJSON} from "../pipeline_instance_json";
import {PipelineInstanceData} from "./test_data";

describe('PipelineHistorySpec', () => {
  it('should parse json into history when both links are present', () => {
    const pipelineJson = PipelineInstanceData.pipeline();
    const json         = {
      _links:    {
        next:     {
          href: "next-link"
        },
        previous: {
          href: "previous-link"
        }
      },
      pipelines: [pipelineJson]
    } as PipelineHistoryJSON;

    const pipelineHistory = PipelineHistory.fromJSON("up42", json);

    expect(pipelineHistory.pipelineName).toBe("up42");
    expect(pipelineHistory.nextLink).toBe("next-link");
    expect(pipelineHistory.previousLink).toBe("previous-link");
    expect(pipelineHistory.pipelineInstances.length).toBe(1);
  });

  it('should should be able to parse json when no links are present', () => {
    const pipelineJson = PipelineInstanceData.pipeline();
    const json         = {pipelines: [pipelineJson]} as PipelineHistoryJSON;

    const pipelineHistory = PipelineHistory.fromJSON("up42", json);

    expect(pipelineHistory.pipelineName).toBe("up42");
    expect(pipelineHistory.nextLink).toBeUndefined();
    expect(pipelineHistory.previousLink).toBeUndefined();
    expect(pipelineHistory.pipelineInstances.length).toBe(1);
  });

  describe('PipelineInstanceSpec', () => {
    it('should parse json into object', () => {
      const json              = PipelineInstanceData.pipeline();
      const pipelineInstances = PipelineInstances.fromJSON([json]);
      const pipelineInstance  = pipelineInstances[0];

      expect(pipelineInstances.length).toEqual(1);
      expect(pipelineInstance.id()).toEqual(json.id);
      expect(pipelineInstance.name()).toEqual(json.name);
      expect(pipelineInstance.counter()).toEqual(json.counter);
      expect(pipelineInstance.label()).toEqual(json.label);
      expect(pipelineInstance.naturalOrder()).toEqual(json.natural_order);
      expect(pipelineInstance.canRun()).toEqual(json.can_run);
      expect(pipelineInstance.preparingToSchedule()).toEqual(json.preparing_to_schedule);
      expect(pipelineInstance.comment()).toEqual(json.comment);
      expect(pipelineInstance.scheduledDate()).toEqual(new Date(json.scheduled_date));

      const buildCause = pipelineInstance.buildCause();

      expect(buildCause.approver()).toEqual(json.build_cause.approver);
      expect(buildCause.triggerMessage()).toEqual(json.build_cause.trigger_message);
      expect(buildCause.triggerForced()).toEqual(json.build_cause.trigger_forced);

      const materialRevisions = buildCause.materialRevisions();

      expect(materialRevisions.length).toEqual(2);
      verifyMaterialRevision(materialRevisions[0], json.build_cause.material_revisions[0]);
      verifyMaterialRevision(materialRevisions[1], json.build_cause.material_revisions[1]);

      const stages = pipelineInstance.stages();
      const stage  = stages[0];

      expect(stages.length).toEqual(1);
      expect(stage.approvalType()).toEqual(json.stages[0].approval_type);
      expect(stage.approvedBy()).toEqual(json.stages[0].approved_by);
      expect(stage.canRun()).toEqual(json.stages[0].can_run);
      expect(stage.counter()).toEqual(json.stages[0].counter);
      expect(stage.id()).toEqual(json.stages[0].id);
      expect(stage.name()).toEqual(json.stages[0].name);
      expect(stage.operatePermission()).toEqual(json.stages[0].operate_permission);
      expect(stage.scheduled()).toEqual(json.stages[0].scheduled);
      expect(stage.result()).toEqual(json.stages[0].result);
      expect(stage.status()).toEqual(json.stages[0].status);

      const jobs = stage.jobs();
      const job  = jobs[0];

      expect(jobs.length).toEqual(1);
      expect(job.id()).toEqual(json.stages[0].jobs[0].id);
      expect(job.name()).toEqual(json.stages[0].jobs[0].name);
      expect(job.result()).toEqual(json.stages[0].jobs[0].result);
      expect(job.scheduledDate()).toEqual(new Date(json.stages[0].jobs[0].scheduled_date));
      expect(job.state()).toEqual(json.stages[0].jobs[0].state);
    });

    it('should return true if the current instance is a bisect', () => {
      const json             = PipelineInstanceData.pipeline();
      const pipelineInstance = PipelineInstance.fromJSON(json);

      expect(pipelineInstance.isBisect()).toBeFalsy();

      pipelineInstance.naturalOrder(2.4);

      expect(pipelineInstance.isBisect()).toBeTruthy();
    });

    function verifyMaterialRevision(materialRevision: MaterialRevision, materialRevisionJSON: MaterialRevisionJSON) {
      expect(materialRevision.changed()).toEqual(materialRevisionJSON.changed);
      expect(materialRevision.material().id()).toEqual(materialRevisionJSON.material.id);
      expect(materialRevision.material().name()).toEqual(materialRevisionJSON.material.name);
      expect(materialRevision.material().fingerprint()).toEqual(materialRevisionJSON.material.fingerprint);
      expect(materialRevision.material().type()).toEqual(materialRevisionJSON.material.type);
      expect(materialRevision.material().description()).toEqual(materialRevisionJSON.material.description);

      const modifications = materialRevision.modifications();
      const modification  = modifications[0];

      expect(modifications.length).toEqual(1);
      expect(modification.comment()).toEqual(materialRevisionJSON.modifications[0].comment);
      expect(modification.emailAddress()).toEqual(materialRevisionJSON.modifications[0].email_address);
      expect(modification.id()).toEqual(materialRevisionJSON.modifications[0].id);
      expect(modification.modifiedTime()).toEqual(new Date(materialRevisionJSON.modifications[0].modified_time));
      expect(modification.revision()).toEqual(materialRevisionJSON.modifications[0].revision);
      expect(modification.userName()).toEqual(materialRevisionJSON.modifications[0].user_name);
      expect(modification.pipelineLabel()).toEqual(materialRevisionJSON.modifications[0].pipeline_label);
    }
  });

  describe('BuildCauseSpec', () => {
    it('should return GoCD as approver if no approver is set', () => {
      const buildCause = new BuildCause("", false, "approver", new MaterialRevisions());

      expect(buildCause.getApprover()).toBe("approver");

      buildCause.approver("");

      expect(buildCause.getApprover()).toBe("GoCD");
    });
  });
});
