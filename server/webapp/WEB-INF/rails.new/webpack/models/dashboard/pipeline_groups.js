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

const _               = require('lodash');
const Routes          = require('gen/js-routes');
const Pipelines       = require('models/dashboard/pipelines');

const PipelineGroup = function (name, path, editPath, canAdminister, pipelines) {
  const self = this;

  this.name          = name;
  this.path          = path;
  this.editPath      = editPath;
  this.canAdminister = canAdminister;
  this.pipelines     = pipelines;

  this.filterBy = (filterText) => {
    var filteredPipelines = [];
    if (_.includes(name.toLowerCase(), filterText)) {
      filteredPipelines = self.pipelines;
    } else {
      filteredPipelines = _.filter(self.pipelines, (pipeline) => {
        if (_.includes(pipeline.name.toLowerCase(), filterText)) {
          return true;
        }
        return _.some(self.pipelines, pipeline => pipeline.hasStatus(filterText));
      });
    }
    if (filteredPipelines.length) {
      return new PipelineGroup(self.name, self.path, self.editPath, self.canAdminister, filteredPipelines);
    }
  };

  this.replacePipelineNamesWithData = (pipelines) => {
    const pipelinesList = [];
    self.pipelines.forEach(pipeline => {
      pipelinesList.push(_.find(pipelines.pipelines, pipelineData => pipelineData.name === pipeline));
    });
    self.pipelines = pipelinesList;
  };
};

PipelineGroup.fromJSON = (json) => {
  const path     = `${Routes.pipelineGroupsPath()}#group-${json.name}`;
  const editPath = `${Routes.pipelineGroupEditPath(json.name)}`;
  return new PipelineGroup(json.name, path, editPath, json.can_administer, json.pipelines);
};


const PipelineGroups = function (groups) {
  const self  = this;
  this.groups = groups;

  this.filterBy = (filterText) => {
    const filteredGroups = _.compact(_.map(self.groups, (group) => group.filterBy(filterText)));
    return new PipelineGroups(filteredGroups);
  };
};

PipelineGroups.fromJSON = (json) => {
  const pipelines         = Pipelines.fromJSON(_.get(json, '_embedded.pipelines', []));
  const pipelineGroupJson = _.get(json, '_embedded.pipeline_groups', []);
  const pipelineGroups    = _.map(pipelineGroupJson, (group) => {
    const pipelineGroup = PipelineGroup.fromJSON(group);
    pipelineGroup.replacePipelineNamesWithData(pipelines);
    return pipelineGroup;
  });
  return new PipelineGroups(pipelineGroups);
};

module.exports = PipelineGroups;
