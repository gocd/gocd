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

const _ = require('lodash');

function DashboardFilter(config) {
  this.name      = config.name || "Default";
  this.pipelines = config.pipelines || [];
  this.type      = config.type || "blacklist";
  this.state     = config.state || [];

  this.definition = () => {
    return _.cloneDeep({name: this.name, state: this.state, type: this.type, pipelines: this.pipelines});
  };

  this.isBlacklist = () => {
    return this.type === "blacklist";
  };

  this.isPipelineVisible = (pipeline) => {
    return this.byPipelines(pipeline.name) &&
      this.byState(pipeline);
  };

  this.byPipelines = (pipelineName) => {
    const conditions = (this.pipelines !== [] && this.pipelines.includes(pipelineName));
    if (this.isBlacklist()) {
      return !conditions;
    }
    return conditions;
  };

  this.byState = (pipeline) => {
    const latestStage = pipeline.latestStage();

    if (latestStage && this.state.length) {
      if (latestStage.isBuilding()) { return _.includes(this.state, "building"); }
      if (latestStage.isFailed()) { return _.includes(this.state, "failing"); }
      return false;
    }

    return true;
  };
}

module.exports = DashboardFilter;
