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

const _        = require('lodash');
const $        = require('jquery');
const m        = require('mithril');
const mrequest = require('helpers/mrequest');
const Stream   = require('mithril/stream');

const PipelineInstance = require('models/dashboard/pipeline_instance');

const Pipeline = function (info) {
  this.name          = Stream(info.name);
  this.settingsPath  = Stream(info._links.settings_path.href);
  this.historyPath   = Stream(info._links.self.href);
  this.instances     = Stream(_.map(info._embedded.instances, (instance) => new PipelineInstance(instance)));
  this.canAdminister = info.can_administer;

  const triggerPath = Stream(info._links.trigger.href);

  this.xhrPost = (url) => {
    const config = (xhr) => {
      xhr.setRequestHeader("Confirm", "true");
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:     'POST',
        url,
        timeout:    mrequest.timeout,
        beforeSend: config
      });

      jqXHR.then((data, _textStatus, _jqXHR) => {
        deferred.resolve(data);
      });

      jqXHR.fail(({responseJSON}, _textStatus, _error) => {
        // todo: handle trigger failure
        deferred.reject(responseJSON);
      });

      jqXHR.always(m.redraw);

    }).promise();
  };

  this.trigger = () => {
    return this.xhrPost(triggerPath());
  };
};

module.exports = Pipeline;
