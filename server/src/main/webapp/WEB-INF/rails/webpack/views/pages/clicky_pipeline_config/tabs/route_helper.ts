/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import m from "mithril";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigRouteParams, RouteInfo} from "views/pages/clicky_pipeline_config/pipeline_config";

export interface OldNewName {
  old: string;
  new: string;
}

export interface PipelineEntityRename {
  stage?: OldNewName;
  job?: OldNewName;
}

export class PipelineConfigSPARouteHelper {
  static routeInfo(): RouteInfo<PipelineConfigRouteParams> {
    return {route: m.route.get(), params: m.route.param()};
  }

  static getPossibleRenames(pipelineConfig: PipelineConfig) {
    const entityNames: PipelineEntityRename = {};

    const routeInfo = PipelineConfigSPARouteHelper.routeInfo();
    if (routeInfo.params.stage_name) {
      const stage = pipelineConfig.stages().items!.find(x => {
        return x.getOriginalName() === routeInfo.params.stage_name;
      })!;

      entityNames.stage = {
        old: stage.getOriginalName(),
        new: stage.name()
      };
    }

    if (routeInfo.params.job_name) {
      const stage = pipelineConfig.stages().items!.find(stage => {
        return stage.getOriginalName() === routeInfo.params.stage_name;
      })!;

      const job = stage.jobs().items!.find(job => {
        return job.getOriginalName() === routeInfo.params.job_name;
      })!;

      entityNames.job = {
        old: job.getOriginalName(),
        new: job.name()
      };
    }

    return entityNames;
  }

  static updateRouteIfEntityRenamed(entityNames: PipelineEntityRename) {
    const routeInfo = PipelineConfigSPARouteHelper.routeInfo();
    let route       = routeInfo.route;

    if (entityNames.stage) {
      route = route.replace(entityNames.stage.old, entityNames.stage.new);
    }

    if (entityNames.job) {
      route = route.replace(entityNames.job.old, entityNames.job.new);
    }

    m.route.set(route);
  }
}
