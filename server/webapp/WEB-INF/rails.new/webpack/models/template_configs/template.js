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
var _           = require('lodash');
var s           = require('string-plus');
var $           = require('jquery');
var mrequest    = require('helpers/mrequest');
var Routes      = require('gen/js-routes');
var Validatable = require('models/mixins/validatable_mixin');

var Stages = require('models/pipeline_configs/stages');
var Jobs   = require('models/pipeline_configs/jobs');

var Template = function (data) {
  Validatable.call(this, data);

  var self                     = this;
  this.name                    = Stream(s.defaultToIfBlank(data.name, ''));
  this.isExtractedFromPipeline = Stream(s.defaultToIfBlank(data.isExtractedFromPipeline, false));
  this.pipeline                = Stream(s.defaultToIfBlank(data.pipeline, ''));
  this.authorization           = Stream(s.defaultToIfBlank(data.authorization, new Template.Authorization({})));
  this.stages                  = s.collectionToJSON(Stream(s.defaultToIfBlank(data.stages, new Stages())));

  this.validatePresenceOf('name');
  this.validateAssociated('stages');

  this.update = function (etag, extract) {
    var self = this;

    var config = function (xhr) {
      mrequest.xhrConfig.v3(xhr);
      xhr.setRequestHeader("If-Match", etag);
    };

    return m.request({
      method:  'PUT',
      url:     Routes.apiv3AdminTemplatePath({template_name: self.name()}), //eslint-disable-line camelcase
      config:  config,
      extract: extract,
      data:    JSON.parse(JSON.stringify(this, s.snakeCaser))
    });
  };

  var createNew = function () {
    var unwrap = function (response) {
      return Template.fromJSON(response);
    };

    return m.request({
      method:        'POST',
      url:           Routes.apiv3AdminTemplatesPath(),
      config:        mrequest.xhrConfig.v3,
      data:          JSON.parse(JSON.stringify(self, s.snakeCaser)),
      unwrapSuccess: unwrap
    });
  };

  this.create = function () {
    if (self.isExtractedFromPipeline()) {
      return extractFromPipeline();
    }
    return createNew();
  };

  var extractFromPipeline = function () {
    var unwrap = function (response) {
      return Template.fromJSON(response);
    };

    return m.request({
      method:        'POST',
      url:           Routes.apiv1AdminInternalExtractTemplatesPath(),
      config:        mrequest.xhrConfig.v1,
      data:          JSON.parse(JSON.stringify(self, s.snakeCaser)),
      unwrapSuccess: unwrap
    });
  };

};

Template.fromJSON = function (data) {
  return new Template({
    name:          data.name,
    authorization: Template.Authorization.formJSON(data.authorization),
    stages:        Stages.fromJSON(data.stages)
  });
};

Template.Authorization = function (data) {
  this.admins = Stream(new Template.Authorization.AdminConfig(data.admins || {}));
};

Template.Authorization.formJSON = function (data) {
  return new Template.Authorization(data || {});
};

Template.Authorization.AdminConfig = function (data) {
  this.roles = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.roles, '')), s.stringToArray);
  this.users = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.users, '')), s.stringToArray);
};

Template.get = function (url, extract) {
  return $.Deferred(function () {
    var deferred = this;

    var jqXHR = $.ajax({
      method:      'GET',
      url:         url,
      beforeSend:  mrequest.xhrConfig.forVersion('v3'),
      contentType: false
    });

    jqXHR.done(function (data, _textStatus, jqXHR) {
      extract(jqXHR);
      deferred.resolve(Template.fromJSON(data));
    });

    jqXHR.fail(function (jqXHR, _textStatus, _errorThrown) {
      deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
    });

  }).promise();
};

Template.getPipelinesForNewTemplate = function () {
  return m.request({
    method: 'GET',
    url:    Routes.apiv1AdminInternalNonTemplatePipelinesPath(),
    config: mrequest.xhrConfig.v1
  });
};

Template.defaultTemplate = function () {
  var defaultJob   = new Jobs.Job({name: "defaultJob"});
  var defaultStage = new Stages.Stage({name: "defaultStage", jobs: new Jobs([defaultJob])});

  return new Template({stages: new Stages([defaultStage])});
};

Template.vm = function () {
  this.saveState = Stream('');
  var errors     = [];

  this.updating = function () {
    this.saveState('in-progress disabled');
  };

  this.saveFailed = function (data) {
    errors.push(data.message);

    if (data.data) {
      if (data.data.errors) {
        errors = _.concat(errors, _.flattenDeep(_.values(data.data.errors)));
      }
    }

    this.saveState('alert');
  };

  this.saveSuccess = function () {
    this.saveState('success');
  };

  this.saveDefault = function () {
    this.saveState('');
  };

  this.clearErrors = function () {
    errors = [];
  };

  this.errors = function () {
    return errors;
  };

  this.hasErrors = function () {
    return !_.isEmpty(errors);
  };

  this.markClientSideErrors = function () {
    errors.push('There are errors on the page, fix them and save');
  };
};

module.exports = Template;
