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

// utils
import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import SparkRoutes from "helpers/spark_routes";
import * as m from "mithril";
import * as stream from "mithril/stream";
import * as s from "underscore.string";

// models and such
import {ConfigRepo} from "models/config_repos/types";
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";

export class PipelineConfigVM {
  pluginId = stream(ConfigRepo.YAML_PLUGIN_ID);
  material: Material = new Material("git", new GitMaterialAttributes());
  job: Job = new Job("", [], []);
  stage: Stage = new Stage("", [this.job]);
  pipeline: PipelineConfig = new PipelineConfig("", [this.material], []);
  isUsingTemplate = stream(false);

  whenTemplateAbsent(fn: () => m.Children) {
    const { pipeline, stage, isUsingTemplate } = this;

    if (!isUsingTemplate() ) {
      if (!pipeline.stages().has(stage)) {
        pipeline.stages(new NameableSet([stage]));
      }
      return fn();
    }
  }

  preview(pluginId: string, validate?: boolean) {
    const payload = this.pipeline.toApiPayload().pipeline;
    const group = payload.group;
    delete payload.group;

    if (!validate) {
      if (s.isBlank(payload.name)) {
        payload.name = "** UNNAMED PIPELINE **";
      }

      for (let i = payload.stages.length - 1; i >= 0; i--) {
        const stage = payload.stages[i];
        if (stage && s.isBlank(stage.name)) {
          stage.name = `** UNNAMED STAGE ${i + 1} **`;
        }

        for (let k = stage.jobs.length; k >= 0; k--) {
          const job = stage.jobs[k];
          if (job && s.isBlank(job.name)) {
            job.name = `** UNNAMED JOB ${k + 1} **`;
          }
        }
      }
    }

    return ApiRequestBuilder.POST(SparkRoutes.pacPreview(pluginId, group, validate), ApiVersion.v1, { payload });
  }
}

export interface PipelineConfigVMAware {
  vm: PipelineConfigVM;
}
