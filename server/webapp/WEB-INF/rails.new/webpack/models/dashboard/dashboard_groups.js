/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const _      = require('lodash');
const Routes = require('gen/js-routes');

function DashboardGroup({name, can_administer, pipelines}) { // eslint-disable-line camelcase
  const self = this;

  this.name          = name;
  this.path          = `${Routes.pipelineGroupsPath()}#group-${name}`;
  this.editPath      = `${Routes.pipelineGroupEditPath(name)}`;
  this.canAdminister = can_administer; // eslint-disable-line camelcase
  this.pipelines     = pipelines;

  this.resolvePipelines = (resolver) => {
    return _.map(self.pipelines, (pipelineName) => resolver.findPipeline(pipelineName));
  };

  this.filterBy = (filterText) => {
    const filteredPipelines = _.filter(self.pipelines, (p) => _.includes(p.toLowerCase(), filterText.toLowerCase()));

    if (filteredPipelines.length) {
      return new DashboardGroup({name, can_administer, pipelines: filteredPipelines}); // eslint-disable-line camelcase
    }
  };
}

function DashboardGroups(groups) {
  const self  = this;
  this.groups = groups;

  this.filterBy = (filterText) => {
    const filteredGroups = _.compact(_.map(self.groups, (group) => group.filterBy(filterText)));
    return new DashboardGroups(filteredGroups);
  };
}

DashboardGroups.fromJSON = (json) => {
  return new DashboardGroups(_.map(json, (group) => new DashboardGroup(group)));
};

module.exports = DashboardGroups;
