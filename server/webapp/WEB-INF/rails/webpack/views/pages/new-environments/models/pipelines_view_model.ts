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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineWithOrigin} from "models/new-environments/environment_pipelines";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {PipelineGroup, PipelineGroups} from "models/new-environments/pipeline_groups";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";

export class PipelinesViewModel {
  readonly searchText: Stream<string | undefined>;
  readonly errorMessage: Stream<string | undefined>;
  readonly pipelineGroups: Stream<PipelineGroups | undefined>;
  private readonly environment: EnvironmentWithOrigin;
  private readonly pipelineGroupCollapsedState: Map<string, boolean>;

  constructor(environment: EnvironmentWithOrigin) {
    this.environment                 = environment;
    this.searchText                  = Stream();
    this.errorMessage                = Stream();
    this.pipelineGroups              = Stream();
    this.pipelineGroupCollapsedState = new Map();
  }

  updatePipelineGroups(pipelineGroups: PipelineGroups) {
    this.pipelineGroups(pipelineGroups);
    this.pipelineGroups()!.forEach((group) => {
      this.pipelineGroupCollapsedState.set(group.name(), false);
    });
  }

  togglePipelineGroupState(name: string): void {
    this.pipelineGroupCollapsedState.set(name, !this.isPipelineGroupExpanded(name));
  }

  isPipelineGroupExpanded(name: string): boolean {
    return !!this.pipelineGroupCollapsedState.get(name);
  }

  filteredPipelineGroups(): PipelineGroups | undefined {
    if (!this.pipelineGroups()) {
      return;
    }

    const self                 = this;
    const searchString         = self.searchText() ? self.searchText()! : "";
    const filterPipelineGroups = new PipelineGroups();

    self.pipelineGroups()!.forEach((pipelineGroup) => {
      const pipelines = pipelineGroup.pipelines().filter((p) => (p.name().indexOf(searchString) >= 0));
      if (pipelines.length > 0) {
        filterPipelineGroups.push(new PipelineGroup(pipelineGroup.name(), pipelines));
      }
    });

    return filterPipelineGroups;
  }

  groupSelectedFn(group: PipelineGroup): any {
    const self = this;
    return () => {
      const areAllPipelinesSelected = _.every(group.pipelines(), (pipeline) => {
        return self.environment.containsPipeline(pipeline.name());
      });

      const areSomePipelinesSelected = _.some(group.pipelines(), (pipeline) => {
        return self.environment.containsPipeline(pipeline.name());
      });

      if (areAllPipelinesSelected) {
        return new TriStateCheckbox(TristateState.on);
      } else if (areSomePipelinesSelected) {
        return new TriStateCheckbox(TristateState.indeterminate);
      } else {
        return new TriStateCheckbox(TristateState.off);
      }
    };
  }

  toggleGroupSelectionFn(group: PipelineGroup): any {
    const self = this;
    return (value: MouseEvent): any => {
      if ((value.target! as HTMLInputElement).checked) {
        group.pipelines().forEach(self.environment.addPipelineIfNotPresent.bind(self.environment));
      } else {
        group.pipelines().forEach(self.environment.removePipelineIfPresent.bind(self.environment));
      }
    };
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
    EnvironmentsAPIs.allPipelines()
                    .then((result) =>
                            result.do((successResponse) => {
                              this.updatePipelineGroups(successResponse.body);
                              callback();
                            }, (errorResponse) => {
                              this.errorMessage(JSON.parse(errorResponse.body!).message);
                            })).finally(m.redraw);
  }
}
