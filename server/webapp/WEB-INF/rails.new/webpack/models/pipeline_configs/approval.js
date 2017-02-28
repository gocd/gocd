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

var Stream = require('mithril/stream');
var s      = require('string-plus');
var Mixins = require('models/mixins/model_mixins');

var Approval = function (data) {
  this.constructor.modelType = 'approval';
  Mixins.HasUUID.call(this);

  this.type          = Stream(s.defaultToIfBlank(data.type, 'success'));
  this.authorization = Stream(s.defaultToIfBlank(data.authorization, new Approval.AuthConfig({})));

  this.isManual = function () {
    return this.type() === 'manual';
  };

  this.isSuccess = function () {
    return this.type() === 'success';
  };

  this.makeManual = function () {
    this.type('manual');
  };

  this.makeOnSuccess = function () {
    this.type('success');
  };
};

Approval.AuthConfig = function (data) {
  this.constructor.modelType = 'approvalAuthorization';
  Mixins.HasUUID.call(this);

  this.roles = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.roles, '')), s.stringToArray);
  this.users = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.users, '')), s.stringToArray);
};

Approval.AuthConfig.fromJSON = function (data) {
  return new Approval.AuthConfig({roles: data.roles, users: data.users});
};

Approval.fromJSON = function (data) {
  return new Approval({
    type:          data.type,
    authorization: Approval.AuthConfig.fromJSON(data.authorization || {})
  });
};

module.exports = Approval;
