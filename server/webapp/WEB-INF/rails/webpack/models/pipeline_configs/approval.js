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

const Stream = require('mithril/stream');
const s      = require('string-plus');
const Mixins = require('models/mixins/model_mixins');
const Validatable    = require('models/mixins/validatable_mixin');

const Approval = function({type, authorization}) {
  this.constructor.modelType = 'approval';
  Mixins.HasUUID.call(this);

  this.type          = Stream(s.defaultToIfBlank(type, 'success'));
  this.authorization = Stream(s.defaultToIfBlank(authorization, new Approval.AuthConfig({})));

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

Approval.AuthConfig = function(data) {
  this.constructor.modelType = 'approvalAuthorization';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);
  this.roles = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.roles, '')), s.stringToArray);
  this.users = s.withNewJSONImpl(Stream(s.defaultToIfBlank(data.users, '')), s.stringToArray);
};

Approval.AuthConfig.fromJSON = ({roles, users, errors}) => new Approval.AuthConfig({roles, users, errors});

Approval.fromJSON = ({type, authorization}) => new Approval({
  type,
  authorization: Approval.AuthConfig.fromJSON(authorization || {})
});

module.exports = Approval;
