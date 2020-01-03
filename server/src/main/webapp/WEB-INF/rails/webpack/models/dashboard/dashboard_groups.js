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
import {SparkRoutes} from "helpers/spark_routes";
import _ from 'lodash';
import Stream from 'mithril/stream';

class Group {

  /* eslint-disable camelcase */
  constructor({name, can_administer, pipelines, has_defined_structures}) {
    this.name             = name;
    this.canAdminister    = can_administer;
    this.pipelines        = pipelines;
    this.definesPipelines = has_defined_structures;
  }

  /* eslint-enable camelcase */

  resolvePipelines(resolver) {
    return _.map(this.pipelines, (pipelineName) => resolver.findPipeline(pipelineName));
  }
}

class PipelineGroup extends Group {
  showEmptyGroups = false;
  show = Stream(false);

  constructor(data, showEmptyGroups) {
    super(data);
    this.showEmptyGroups = showEmptyGroups;
  }

  label() {
    return `Pipeline Group '${this.name}'`;
  }

  tooltipForEdit() {
    if (!this.canAdminister) {
      return "You don't have permission to edit this pipeline group";
    }
    return "";
  }

  ariaLabelForEdit() {
    return this.tooltipForEdit() || `Edit ${this.label()}`;
  }

  titleForEdit() {
    if (this.canAdminister) {
      return `Edit ${this.label()}`;
    }
    return "";
  }

  tooltipForNewPipeline() {
    if (!this.canAdminister) {
      return "You don't have permission to create new pipeline within this pipeline group";
    }
    return "";
  }

  ariaLabelForNewPipeline() {
    return this.tooltipForNewPipeline() || 'Create a new pipeline within this group';
  }

  titleForNewPipeline() {
    if (this.canAdminister) {
      return 'Create a new pipeline within this group';
    }
    return "";
  }

  select(filter) {
    if (this.showEmptyGroups && !this.definesPipelines) {
      return this;
    }

    const pipelines = _.filter(this.pipelines, filter);
    if (pipelines.length === 0) {
      return false;
    }
    return this.subset(pipelines);
  }

  subset(pipelines) {
    return new PipelineGroup({
      name:           this.name, pipelines,
      can_administer: this.canAdminister, has_defined_structures: this.definesPipelines // eslint-disable-line camelcase
    });
  }

  routes() {
    if (!this.name) {
      return {};
    }
    return {
      show:   `${SparkRoutes.pipelineGroupsSPAPath(this.name)}`,
      edit:   `${SparkRoutes.pipelineGroupsSPAPath(this.name)}/edit`,
      new:    `${SparkRoutes.newCreatePipelinePath()}?group=${this.name}`,
      asCode: `${SparkRoutes.createPipelineAsCodePath()}?group=${this.name}`
    };
  }
}

class Environment extends Group {
  label() {
    return `Environment '${this.name}'`;
  }

  tooltipForEdit() {
    if (!this.canAdminister) {
      return "You don't have permission to edit this environment";
    }
    return "";
  }

  ariaLabelForEdit() {
    return this.tooltipForEdit() || `Edit ${this.label()}`;
  }

  titleForEdit() {
    if (this.canAdminister) {
      return `Edit ${this.label()}`;
    }
    return "";
  }

  tooltipForNewPipeline() {
    return "";
  }

  ariaLabelForNewPipeline() {
    return "";
  }

  titleForNewPipeline() {
    return "";
  }

  select(filter) {
    const pipelines = _.filter(this.pipelines, filter);
    if (pipelines.length === 0) {
      return false;
    }
    return this.subset(pipelines);
  }

  subset(pipelines) {
    return new Environment({
      name:           this.name, pipelines,
      can_administer: this.canAdminister, has_defined_structures: this.definesPipelines // eslint-disable-line camelcase
    });
  }

  routes() {
    if (!this.name) {
      return {};
    }
    return {
      edit: SparkRoutes.getEnvironmentPathOnSPA(this.name),
      show: SparkRoutes.getEnvironmentPathOnSPA(this.name)
    };
  }
}

export function DashboardGroups(groups) {
  this.groups = groups;

  this.select = (filter) => {
    return new DashboardGroups(_.compact(_.map(this.groups, (group) => {
      return group.select(filter);
    })));
  };
}

DashboardGroups.fromPipelineGroupsJSON = (json, showEmptyGroups) => {
  return new DashboardGroups(_.map(json, (group) => new PipelineGroup(group, showEmptyGroups)));
};

DashboardGroups.fromEnvironmentsJSON = (json) => {
  return new DashboardGroups(_.map(json, (group) => new Environment(group)));
};

DashboardGroups.PipelineGroup = PipelineGroup;
DashboardGroups.Environment   = Environment;
