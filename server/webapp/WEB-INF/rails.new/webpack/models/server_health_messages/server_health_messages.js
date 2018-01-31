/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const CrudMixins = require('models/mixins/crud_mixins');
const _          = require('lodash');
const inflection = require('lodash-inflection');

const ServerHealthMessages = function (messages) {

  const countErrors   = () => _.filter(messages, {level: 'ERROR'}).length;
  const countWarnings = () => _.filter(messages, {level: 'WARNING'}).length;

  this.hasMessages = () => messages.length > 0;

  this.summaryMessage = () => {
    const messages = [];
    if (countErrors() > 0) {
      messages.push(inflection.pluralize('error', countErrors(), true));
    }
    if (countWarnings() > 0) {
      messages.push(inflection.pluralize('warning', countWarnings(), true));
    }
    return _.join(messages, ' and ');
  };

  this.collect = (cb) => _.map(messages, cb);
};

ServerHealthMessages.fromJSON = (messages) => new ServerHealthMessages(messages);

ServerHealthMessages.API_VERSION = 'v1';

CrudMixins.Index({
  type:     ServerHealthMessages,
  indexUrl: '/go/api/server_health_messages',
  version:  ServerHealthMessages.API_VERSION
});

module.exports = ServerHealthMessages;

