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

var Stream    = require('mithril/stream');
var _         = require('lodash');
var $         = require('jquery');
var Routes    = require('gen/js-routes');
var mrequest  = require('helpers/mrequest');
var Pipelines = Stream([]);

Pipelines.Pipeline = function (data) {
  this.name   = data.name;
  this.stages = _.map(data.stages, stage => new function () {
    this.name = stage.name;
    this.jobs = stage.jobs;
  });
};

Pipelines.init = rejectPipeline => {
  var jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv1AdminInternalPipelinesPath(),
    background:  true,
    beforeSend:  mrequest.xhrConfig.forVersion('v1'),
    contentType: false
  });

  var didFulfill = (data, _textStatus, _jqXHR) => {
    var pipelines = _.reject(data._embedded.pipelines, pipeline => pipeline.name === rejectPipeline);

    Pipelines(_.map(pipelines, pipeline => new Pipelines.Pipeline(pipeline)));
  };

  jqXHR.then(didFulfill);

  return Pipelines;
};

module.exports = Pipelines;
