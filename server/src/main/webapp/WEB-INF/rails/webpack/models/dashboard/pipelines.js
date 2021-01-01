/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {Pipeline} from "models/dashboard/pipeline";

export const Pipelines = function (pipelines) {
  const self = this;

  self.pipelines = pipelines;

  self.size = Object.keys(this.pipelines).length;

  self.find = (pipelineName) => {
    return self.pipelines[pipelineName];
  };
};

Pipelines.fromJSON = (json) => {
  const pipelines = _.reduce(json, (hash, pipeline) => {
    hash[pipeline.name] = new Pipeline(pipeline);
    return hash;
  }, {});

  return new Pipelines(pipelines);
};


