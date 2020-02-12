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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroups, Pipelines, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";

export class PipelinesViewModel {
  readonly searchText: Stream<string | undefined>;
  readonly errorMessage: Stream<string | undefined>;
  readonly pipelineGroups: Stream<PipelineGroups | undefined>;
  readonly environment: EnvironmentWithOrigin;
  readonly environments: Environments;

  constructor(environment: EnvironmentWithOrigin, environments: Environments) {
    this.environment    = environment;
    this.environments   = environments;
    this.searchText     = Stream();
    this.errorMessage   = Stream();
    this.pipelineGroups = Stream();
  }

  filteredPipelines(): Pipelines {
    const groups = this.pipelineGroups();
    if (!groups) {
      return new Pipelines();
    }

    const pipelines = _.flatten(groups.map((group) => group.pipelines())).filter(this.matchesSearchText.bind(this));
    return new Pipelines(...pipelines);
  }

  allPipelines(): Pipelines {
    const groups = this.pipelineGroups();
    if (!groups) {
      return new Pipelines();
    }

    const pipelines = _.flatten(groups.map((group) => group.pipelines()));

    return new Pipelines(...pipelines);
  }

  availablePipelines(): Pipelines {
    const repoAssociatedPipelines = this.configRepoEnvironmentPipelines();
    const repoDefinedPipelines    = this.unassociatedPipelinesDefinedInConfigRepository();
    const otherEnvPipelines       = this.pipelinesDefinedInOtherEnvironment();

    return new Pipelines(...this.filteredPipelines().filter((p) => {
      const name = p.name();
      return !repoAssociatedPipelines.containsPipeline(name)
        && !repoDefinedPipelines.containsPipeline(name)
        && !otherEnvPipelines.containsPipeline(name);
    }));
  }

  configRepoEnvironmentPipelines(): Pipelines {
    const self = this;
    return new Pipelines(...this.environment.pipelines().filter((p) => {
      return p.origin().isDefinedInConfigRepo() && self.matchesSearchText(p);
    }));
  }

  pipelinesDefinedInOtherEnvironment(): Pipelines {
    const self      = this;
    const pipelines = this.filteredPipelines();
    return new Pipelines(...pipelines.filter((p) => {
      return self.environments.isPipelineDefinedInAnotherEnvironmentApartFrom(self.environment.name(), p.name());
    }));
  }

  unassociatedPipelinesDefinedInConfigRepository(): Pipelines {
    const self      = this;
    const pipelines = this.filteredPipelines();

    return new Pipelines(...pipelines.filter((p) => {
      const isDefinedInAnotherEnv = self.environments.isPipelineDefinedInAnotherEnvironmentApartFrom(self.environment.name(),
                                                                                                     p.name());
      const isDefinedInCurrentEnv = self.environment.containsPipeline(p.name());
      return !isDefinedInAnotherEnv && !isDefinedInCurrentEnv && p.origin().isDefinedInConfigRepo();
    }));
  }

  pipelineSelectedFn(pipeline: PipelineWithOrigin): (value?: any) => any {
    const self = this;
    return (value?: boolean) => {
      if (value !== undefined) {
        value ? self.environment.addPipelineIfNotPresent(pipeline) : self.environment.removePipelineIfPresent(pipeline);
      }

      return self.environment.containsPipeline(pipeline.name());
    };
  }

  fetchAllPipelines(callback: () => void) {
    EnvironmentsAPIs.allPipelines("view", "view")
                    .then((result) =>
                            result.do((successResponse) => {
                              this.pipelineGroups(successResponse.body.groups());
                              callback();
                            }, (errorResponse) => {
                              this.errorMessage(JSON.parse(errorResponse.body!).message);
                            })).finally(m.redraw);
  }

  private matchesSearchText(p: PipelineWithOrigin) {
    return p.name().indexOf(this.searchText() ? this.searchText()! : "") >= 0;
  }
}
