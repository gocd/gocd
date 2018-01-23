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

const _        = require('lodash');
const Pipeline = require('models/dashboard/pipeline');

const Pipelines = function (list) {
  const self = this;

  this.pipelines = _.reduce(list, (hash, pipeline) => {
    hash[pipeline.name] = new Pipeline(pipeline);
    return hash;
  }, {});

  this.size = Object.keys(this.pipelines).length;

  this.find = (pipelineName) => {
    return self.pipelines[pipelineName];
  };
};

module.exports = Pipelines;

