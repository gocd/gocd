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

var m        = require('mithril');
var Stream   = require('mithril/stream');
var $        = require('jquery');
var _        = require('lodash');
var s        = require('string-plus');
var mrequest = require('helpers/mrequest');
var Routes   = require('gen/js-routes');
var Mixins   = require('models/mixins/model_mixins');

var Templates = function (data) {
  Mixins.HasMany.call(this, {
    factory:    Templates.Template.fromJSON,
    as:         'Template',
    collection: data,
    uniqueOn:   'name'
  });
};

Templates.Template = function (data) {
  this.parent    = Mixins.GetterSetter();
  this.name      = Stream(s.defaultToIfBlank(data.name, ''));
  this.url       = Stream(s.defaultToIfBlank(data.url, ''));
  this.pipelines = Stream(s.defaultToIfBlank(data.pipelines, []));

  this.delete = function () {
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
        method:      'DELETE',
        url:         Routes.apiv3AdminTemplatePath({template_name: data.name}), //eslint-disable-line camelcase
        beforeSend:  mrequest.xhrConfig.forVersion('v3'),
        contentType: false
      });

      jqXHR.done(function (data, _textStatus, _jqXHR) {
        deferred.resolve(data);
      });

      jqXHR.fail(function (jqXHR, _textStatus, _errorThrown) {
        deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
      });

    }).promise();
  };
};

Templates.Template.fromJSON = function (data) {
  return new Templates.Template({
    name:      data.name,
    url:       data._links.self.href,
    pipelines: _.map(data._embedded.pipelines, function (pipeline) {
      return new function () {
        this.name = Stream(pipeline.name);
        this.url  = Stream(pipeline._links.self.href);
      };
    })
  });
};

Templates.all = function () {
  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         Routes.apiv3AdminTemplatesPath(),
      beforeSend:  mrequest.xhrConfig.forVersion('v3'),
      contentType: false
    });

    jqXHR.done(function (data, _textStatus, _jqXHR) {
      deferred.resolve(Templates.fromJSON(data._embedded.templates));
    });

    jqXHR.fail(function (jqXHR, _textStatus, _errorThrown) {
      deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
    });

  }).promise();
};

Mixins.fromJSONCollection({
  parentType: Templates,
  childType:  Templates.Template,
  via:        'addTemplate'
});

module.exports = Templates;
