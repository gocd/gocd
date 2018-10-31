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

import * as _ from 'lodash';

const CrudMixins = require('models/mixins/crud_mixins');

const inflection = require('lodash-inflection');

export interface ServerHealthMessage {
  message: string;
  detail: string;
  level: string;
  time: string;
}

export class ServerHealthMessages {

  static API_VERSION = 'v1';
  private readonly messages: ServerHealthMessage[];

  constructor(messages: ServerHealthMessage[]) {
    this.messages = messages;
  }

  static fromJSON(messages: ServerHealthMessage[]) {
    return new ServerHealthMessages(messages);
  }

  //Typescript requires all to be defined on the model. This is overriden by CrudMixins
  static all(xhrCB: () => any): any {
    return null;
  }

  countErrors   = () => _.filter(this.messages, {level: 'ERROR'}).length;
  countWarnings = () => _.filter(this.messages, {level: 'WARNING'}).length;

  hasMessages(): boolean {
    return this.messages.length > 0;
  }

  summaryMessage(): string {
    const messages = [];
    if (this.countErrors() > 0) {
      messages.push(inflection.pluralize('error', this.countErrors(), true));
    }
    if (this.countWarnings() > 0) {
      messages.push(inflection.pluralize('warning', this.countWarnings(), true));
    }
    return _.join(messages, ' and ');
  }

  collect<T, K extends keyof T>(cb: any): Array<T[K]> {
    return _.map(this.messages, cb);
  }
}

CrudMixins.Index({
  type:     ServerHealthMessages,
  indexUrl: '/go/api/server_health_messages',
  version:  ServerHealthMessages.API_VERSION
});
