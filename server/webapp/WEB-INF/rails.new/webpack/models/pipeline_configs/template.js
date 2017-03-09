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

var m           = require('mithril');
var Stream      = require('mithril/stream');
var s           = require('string-plus');
var Mixins      = require('models/mixins/model_mixins');
var Stages      = require('models/pipeline_configs/stages');
var mrequest    = require('helpers/mrequest');
var Validatable = require('models/mixins/validatable_mixin');
var Routes      = require('gen/js-routes');
var $           = require('jquery');

var Template = function (data) {
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
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv3AdminTemplatePath({template_name: name}), //eslint-disable-line camelcase
      beforeSend:  mrequest.xhrConfig.forVersion('v3'),
      contentType: false
    });

    jqXHR.done(function (data, _textStatus, _jqXHR) {
      deferred.resolve(Template.fromJSON(data));
    });

    jqXHR.always(m.redraw);

  }).promise();
};

module.exports = Template;
