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
import Stream from "mithril/stream";
import {PipelineGroups, Pipelines, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";

export class PipelinesViewModel {
  readonly searchText: Stream<string> = Stream("");
  readonly errorMessage: Stream<string | undefined>;
  readonly environment: EnvironmentWithOrigin;
  readonly environments: Environments;
  private readonly allPipelinesFromAllGroups: Stream<Pipelines>;

  // the following fields are private and set only once, after pipelineGroup information is fetched.
  private _currentEnvConfigRepoPipelines: PipelineWithOrigin[] = [];
  private readonly _allPipelinesWhichAreDefinedInAnotherEnvironments: PipelineWithOrigin[] = [];
  private readonly _allNonEnvironmentPipelinesDefinedInConfigRepository: PipelineWithOrigin[] = [];
  private readonly _availablePipelines: PipelineWithOrigin[] = [];

  constructor(environment: EnvironmentWithOrigin, environments: Environments) {
    this.environment    = environment;
    this.environments   = environments;
    this.errorMessage   = Stream();
    this.allPipelinesFromAllGroups = Stream(new Pipelines());
  }

  filteredPipelines(): PipelineWithOrigin[] {
    return this.filterResult(this.allPipelines());
  }

  allPipelines(): Pipelines {
    return this.allPipelinesFromAllGroups();
  }

  availablePipelines(): PipelineWithOrigin[] {
    return this.filterResult(this._availablePipelines);
  }

  configRepoEnvironmentPipelines(): PipelineWithOrigin[] {
    return this.filterResult(this._currentEnvConfigRepoPipelines);
  }

  pipelinesDefinedInOtherEnvironment(): PipelineWithOrigin[] {
    return this.filterResult(this._allPipelinesWhichAreDefinedInAnotherEnvironments);
  }

  unassociatedPipelinesDefinedInConfigRepository(): PipelineWithOrigin[] {
    return this.filterResult(this._allNonEnvironmentPipelinesDefinedInConfigRepository);
  }

  //todo: this function returns a function.
  // This function is responsible to return a click handler for each pipeline, which is responsible to check/uncheck the box. (Check == pipeline belongs to env). For every pipeline, a new function will be defined (due to pipeline references).
  // Every mithril redraw (happening few millis), causes the old handler to be thrown away and a new one to be defined. With few thousand pipelines and mithril redraws, there will be alot of load on gc.
  // As of now, the (un/)checking the pipeline does not feel that sluggish (tested with 2k) pipelines.
  // But if it does, the checkboxes can be implemented as Stream<boolean> instead of computing every time (based on whether the pipeline is part of env)
  pipelineSelectedFn(pipeline: PipelineWithOrigin): (value?: any) => any {
    const self = this;
    return (value?: boolean) => {
      if (value !== undefined) {
        if (value === true) {
          self.environment.addPipelineIfNotPresent(pipeline);

        } else {
          self.environment.removePipelineIfPresent(pipeline);
        }
      }

      return self.environment.containsPipeline(pipeline.name());
    };
  }

  // This method is responsible to find all the relevant information upfront.
  // Previously, each individual method was iterating through all the pipelines multiple times, and sometimes against the
  // the pipelines from the environment. making the rendering slow
  //
  // this method will iterate over all the pipelines only once and and will compute all the required data upfront.
  //
  // allPipelinesFromAllGroups => all the viewable pipelines by the current user.
  //
  // currentEnvConfigRepoPipelines => pipelines belonging to the current environment whose association is defined in config repository. These pipelines are from the current environment but are not available for edit association, cause the association is remotely defined.
  //
  // available pipelines => all the pipelines which are available for association. This set contains all the existing pipelines from the environment as well as the pipelines which can be added to the environment.
  //
  // allPipelinesWhichAreDefinedInAnotherEnvironments => This set contains all the pipelines which are already associated with other environments. Hence these pipelines are not available for association for current environment.
  //
  // allNonEnvironmentPipelinesDefinedInConfigRepository => This set contains all the pipelines which does not exist in any environment and are defined in config repository. As the definition of the pipeline itself is defined remotely, these pipelines are not available for association for current environment.
  //
  updateModel(pipelineGroups: Stream<PipelineGroups>) {
    const allPipelines = new Pipelines();
    pipelineGroups().forEach(g => allPipelines.push(...g.pipelines()));
    this.allPipelinesFromAllGroups(allPipelines);

    this._currentEnvConfigRepoPipelines = this.environment.pipelines().filter((p) => p.origin().isDefinedInConfigRepo());

    this.allPipelinesFromAllGroups().forEach((pipeline) => {
      const isPipelineFromCurrentEnvironment = this.environment.pipelines().findByName(pipeline.name());

      if(isPipelineFromCurrentEnvironment && !isPipelineFromCurrentEnvironment.isDefinedRemotely()) {
        return this._availablePipelines.push(pipeline);
      }

      const isPipelineDefinedInAnotherEnvironment = this.environments.isPipelineDefinedInAnotherEnvironmentApartFrom(this.environment.name(), pipeline.name());
      if(isPipelineDefinedInAnotherEnvironment) {
        return this._allPipelinesWhichAreDefinedInAnotherEnvironments.push(pipeline);
      }

      if(!isPipelineFromCurrentEnvironment && !isPipelineDefinedInAnotherEnvironment && pipeline.isDefinedRemotely()) {
        return this._allNonEnvironmentPipelinesDefinedInConfigRepository.push(pipeline);
      }

      this._availablePipelines.push(pipeline);
    });
  }

  fetchAllPipelines(callback: () => void) {
    EnvironmentsAPIs.allPipelineGroups("view")
                    .then((result) =>
                            result.do((successResponse) => {
                              this.updateModel(successResponse.body.groups);
                              callback();
                            }, (errorResponse) => {
                              this.errorMessage(JSON.parse(errorResponse.body!).message);
                            })).finally(m.redraw.sync);
  }

  private filterResult(result: PipelineWithOrigin[]): PipelineWithOrigin [] {
    if (this.searchText()) {
      return result.filter((p) => p.name().indexOf(this.searchText()!) >= 0);
    }

    return result;
  }
}
