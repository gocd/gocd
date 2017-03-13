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

const Stream    = require('mithril/stream');
const _         = require('lodash');
const $         = require('jquery');
const Routes    = require('gen/js-routes');
const mrequest  = require('helpers/mrequest');
const Pipelines = Stream([]);

Pipelines.Pipeline = function (data) {
  this.name   = data.name;
  this.stages = _.map(data.stages, stage => new function () {
    this.name = stage.name;
    this.jobs = stage.jobs;
  });
};

Pipelines.init = rejectPipeline => {
  const jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv1AdminInternalPipelinesPath(),
    background:  true,
    beforeSend:  mrequest.xhrConfig.forVersion('v1'),
    contentType: false
  });

  const didFulfill = (data, _textStatus, _jqXHR) => {
    const pipelines = _.reject(data._embedded.pipelines, pipeline => pipeline.name === rejectPipeline);

    Pipelines(_.map(pipelines, pipeline => new Pipelines.Pipeline(pipeline)));
  };

  jqXHR.then(didFulfill);

  return Pipelines;
};

module.exports = Pipelines;
