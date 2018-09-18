/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const _         = require('lodash');
const $         = require('jquery');
const Routes    = require('gen/js-routes');
const mrequest  = require('helpers/mrequest');
const Pipelines = {};

Pipelines.Pipeline = function ({name, stages}) {
  this.name   = name;
  this.stages = _.map(stages, ({name, jobs}) => new function () {
    this.name = name;
    this.jobs = jobs;
  });
};


Pipelines.all = () => $.Deferred(function () {
  const deferred = this;

  const jqXHR = $.ajax({
    method:      'GET',
    url:         Routes.apiv1AdminInternalPipelinesPath(),
    background:  true,
    beforeSend:  mrequest.xhrConfig.forVersion('v1'),
    contentType: false
  });

  const didFulfill = ({_embedded}, _textStatus, _jqXHR) => {
    deferred.resolve(_.map(_embedded.pipelines, (pipeline) => new Pipelines.Pipeline(pipeline)));
  };

  const didReject = (jqXHR) => {
    deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
  };

  jqXHR.then(didFulfill, didReject);
}).promise();

module.exports = Pipelines;
