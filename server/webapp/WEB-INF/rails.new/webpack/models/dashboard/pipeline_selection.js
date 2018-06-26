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

const _           = require('lodash');
const Stream      = require('mithril/stream');
const AjaxHelper  = require('helpers/ajax_helper');
const SparkRoutes = require('helpers/spark_routes');

const PipelineSelection = function (pipelineGroups, selections, blacklist) {
  const self = this;

  this.pipelineGroups = pipelineGroups;
  this.selections     = selections;
  this.blacklist      = blacklist;

  this.isPipelineSelected = (pipelineName) => self.selections[pipelineName]();

  this.toggleBlacklist = () => {
    self.blacklist(!self.blacklist);
  };

  this.update = () => {
    const allPipelines = self.selections;
    const selections   = [];

    _.each(allPipelines, (selection, pipelineName) => {
      if (self.blacklist() ^ selection()) {
        selections.push(pipelineName);
      }
    });

    const payload = {
      selections,
      "blacklist": self.blacklist()
    };

    return AjaxHelper.PUT({
      url:        SparkRoutes.pipelineSelectionPath(),
      apiVersion: 'v1',
      payload
    });
  };
};

PipelineSelection.fromJSON = (json) => {
  const pipelineGroups = json.pipelines;
  const selections     = {};
  const blacklist      = json.blacklist;
  _.each(_.keys(pipelineGroups), (group) => {
    _.each(pipelineGroups[group], (pipeline) => {
      selections[pipeline] = Stream(!!(blacklist ^ _.includes(json.selections, pipeline)));
    });
  });

  return new PipelineSelection(Stream(pipelineGroups), selections, Stream(blacklist));
};

PipelineSelection.get = () => {
  return AjaxHelper.GET({
    url:        SparkRoutes.pipelineSelectionPath(),
    type:       PipelineSelection,
    apiVersion: 'v1'
  });
};

module.exports = PipelineSelection;
