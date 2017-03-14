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

const m           = require('mithril');
const Stream      = require('mithril/stream');
const s           = require('string-plus');
const Mixins      = require('models/mixins/model_mixins');
const Stages      = require('models/pipeline_configs/stages');
const mrequest    = require('helpers/mrequest');
const Validatable = require('models/mixins/validatable_mixin');
const Routes      = require('gen/js-routes');
const $           = require('jquery');

const Template = function (data) {
  this.constructor.modelType = 'template';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.name   = Stream(data.name);
  this.stages = s.collectionToJSON(Stream(s.defaultToIfBlank(data.stages, new Stages())));
};

Template.fromJSON = function (data) {
  return new Template({
    name:   data.name,
    stages: Stages.fromJSON(data.stages)
  });
};

Template.find = function (name) {
  return $.Deferred(function () {
    const deferred = this;

    const jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv3AdminTemplatePath({template_name: name}), //eslint-disable-line camelcase
      beforeSend:  mrequest.xhrConfig.forVersion('v3'),
      contentType: false
    });

    jqXHR.done((data, _textStatus, _jqXHR) => {
      deferred.resolve(Template.fromJSON(data));
    });

    jqXHR.always(m.redraw);

  }).promise();
};

module.exports = Template;
