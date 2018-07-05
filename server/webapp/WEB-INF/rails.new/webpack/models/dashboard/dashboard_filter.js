
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
const Stream = require('mithril/stream');

const DashboardFilter = function (name, pipelines, type) {
  const WHITELIST_TYPE = "whitelist";
  const BLACKLIST_TYPE = "blacklist";
  this.name      = name;
  this.pipelines = pipelines;
  this.type      = type;

  this.isBlacklist = () => {
    return this.type === BLACKLIST_TYPE;
  };

  this.toggleType = () => {
    this.type = this.isBlacklist() ? WHITELIST_TYPE : BLACKLIST_TYPE;
  };

  this.removePipeline = (pipelineName) => {
      this.pipelines = _.reject(this.pipelines, (p) => {
        return p === pipelineName;
      });
  };

  this.addPipeline = (pipelineName) => {
    this.pipelines.push(pipelineName);
  };

  this.clearPipelines = () => {
    this.pipelines = [];
  };

  this.findSelections = (pipelineGroups) => {
    const selections = {};
    _.each(_.keys(pipelineGroups), (group) => {
      _.each(pipelineGroups[group], (pipeline) => {
        selections[pipeline] = Stream(!!(this.isBlacklist() ^ _.includes(this.pipelines, pipeline)));
      });
    });
    return selections;
  };

  this.invertPipelines = (allPipelines) => {
    this.clearPipelines();
    _.each(allPipelines, (selection, pipelineName) => {
      if (this.isBlacklist() ^ selection()) {
        this.addPipeline(pipelineName);
      }
    });
  };
};

module.exports = DashboardFilter;
