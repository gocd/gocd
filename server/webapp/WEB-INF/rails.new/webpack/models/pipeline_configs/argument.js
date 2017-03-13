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
const _      = require('lodash');

const Argument = function (data) {
  this.data = Stream(data);

  this.isList = function () {
    return _.isArray(this.data());
  };

  this.toString = function () {
    return this.isList() ? this.data().join(' ') : this.data();
  };

  this.toJSON = function () {
    if (_.isEmpty(this.data())) {
      return {};
    }
    return this.isList() ? {arguments: this.data()} : {args: this.data()};
  };
};

Argument.create = (args, argsList) => args ? new Argument(args) : new Argument(argsList || []);

Argument.vm = (model) => {
  const listVM = function () {
    this.data = (val) => {
      if (val === undefined) {
        return model.data().join('\n');
      }
      const args = _.isEmpty(val) ? [] : val.split('\n');
      model.data(args);
    };
  };

  const stringVM = function () {
    this.data = (val) => {
      if (val === undefined) {
        return model.data();
      }
      model.data(val);
    };
  };

  return model.isList() ? new listVM() : new stringVM();
};

module.exports = Argument;
