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

const DashboardGroup = function (name, path, editPath, canAdminister, pipelines) {
  const self = this;

  this.name          = name;
  this.path          = path;
  this.editPath      = editPath;
  this.canAdminister = canAdminister;
  this.pipelines     = pipelines;

  this.filterBy = (filterText) => {
    const filteredPipelines = _.filter(self.pipelines, (pipeline) => _.includes(pipeline.toLowerCase(), filterText));
    if (filteredPipelines.length) {
      return new DashboardGroup(self.name, self.path, self.editPath, self.canAdminister, filteredPipelines);
    }
  };
};

DashboardGroup.fromJSON = (json) => {
  const path     = `${Routes.pipelineGroupsPath()}#group-${json.name}`;
  const editPath = `${Routes.pipelineGroupEditPath(json.name)}`;
  return new DashboardGroup(json.name, path, editPath, json.can_administer, json.pipelines);
};


const DashboardGroups = function (groups) {
  const self  = this;
  this.groups = groups;

  this.filterBy = (filterText) => {
    const filteredGroups = _.compact(_.map(self.groups, (group) => group.filterBy(filterText)));
    return new DashboardGroups(filteredGroups);
  };
};

DashboardGroups.fromJSON = (json) => {
  const pipelineGroups = _.map(json, (group) => DashboardGroup.fromJSON(group));
  return new DashboardGroups(pipelineGroups);
};

module.exports = DashboardGroups;
