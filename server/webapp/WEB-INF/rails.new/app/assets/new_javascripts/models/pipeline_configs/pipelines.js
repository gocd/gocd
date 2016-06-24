/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'js-routes', 'helpers/mrequest'], function (m, _, Routes, mrequest) {
  var Pipelines = m.prop([]);

  Pipelines.Pipeline = function (data) {
    this.name   = data.name;
    this.stages = _.map(data.stages, function (stage) {
      return new function () {
        this.name = stage.name;
        this.jobs = stage.jobs;
      };
    });
  };

  Pipelines.init = function (rejectPipeline) {
    var unwrap = function (response) {
      return _.reject(response._embedded.pipelines, function(pipeline) {
        return pipeline.name === rejectPipeline;
      });
    };

    m.request({
      method:        'GET',
      url:           Routes.apiv1AdminInternalPipelinesPath(),
      background:    true,
      config:        mrequest.xhrConfig.v1,
      unwrapSuccess: unwrap,
      type:          Pipelines.Pipeline
    }).then(Pipelines);

    return Pipelines;
  };

  return Pipelines;
});
