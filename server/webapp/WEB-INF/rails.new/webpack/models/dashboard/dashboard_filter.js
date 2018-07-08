
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

function DashboardFilter(config) {
  const TYPE_WHITELIST = "whitelist";
  const TYPE_BLACKLIST = "blacklist";

  this.name      = config.name || "Default";
  this.pipelines = config.pipelines || [];
  this.type      = config.type || TYPE_BLACKLIST;

  this.definition = () => {
    return _.cloneDeep({name: this.name, pipelines: this.pipelines, type: this.type});
  };

  this.isBlacklist = () => {
    return this.type === TYPE_BLACKLIST;
  };

  this.displayName = () => {
    if (!this.name) {
      return "default";
    }
    return this.name;
  };

  this.toggleType = () => {
    this.type = this.isBlacklist() ? TYPE_WHITELIST : TYPE_BLACKLIST;
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

  this.deriveSelectionMap = (pipelinesByGroup) => {
    const invert = TYPE_BLACKLIST === this.type;
    return _.reduce(pipelinesByGroup, (m, pip, _n) => {
      _.each(pip, (p) => { m[p] = Stream(invert ^ _.includes(this.pipelines, p)); });
      return m;
    }, {});
  };

  this.invertPipelines = (allPipelines) => {
    this.clearPipelines();
    _.each(allPipelines, (selection, pipelineName) => {
      if (this.isBlacklist() ^ selection()) {
        this.addPipeline(pipelineName);
      }
    });
  };
}

module.exports = DashboardFilter;
