/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import _ from "lodash";
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface EnvironmentVariableJSON {
  secure: boolean;
  name: string;
  value?: string;
  encrypted_value?: string;
}

export class EnvironmentVariable extends ValidatableMixin {
  secure: Stream<boolean>;
  name: Stream<string>;
  value: Stream<string | undefined>;
  encryptedValue: Stream<string | undefined>;

  constructor(name: string, value?: string, secure?: boolean, encryptedValue?: string) {
    super();
    this.secure         = Stream(secure || false);
    this.name           = Stream(name);
    this.value          = Stream(value);
    this.encryptedValue = Stream(encryptedValue);
    ValidatableMixin.call(this);
    this.validatePresenceOf("name");
    this.validateMutualExclusivityOf("value",
                                     "encryptedValue",
                                     {message: "Either 'Value' or 'Encrypted value' must be present. Both 'Value' and 'Encrypted value' cannot be defined at the same time."});
  }

  static fromJSON(data: EnvironmentVariableJSON) {
    return new EnvironmentVariable(data.name, data.value, data.secure, data.encrypted_value);
  }

  editable() {
    return true;
  }
}

export class EnvironmentVariables extends Array<EnvironmentVariable> {
  constructor(...environmentVariables: EnvironmentVariable[]) {
    super(...environmentVariables);
    Object.setPrototypeOf(this, Object.create(EnvironmentVariables.prototype));
  }

  static fromJSON(environmentVariables: EnvironmentVariableJSON[]) {
    return new EnvironmentVariables(...environmentVariables.map(EnvironmentVariable.fromJSON));
  }

  secureVariables(): EnvironmentVariables {
    return new EnvironmentVariables(...this.filter((envVar) => envVar.secure()));
  }

  plainTextVariables(): EnvironmentVariables {
    return new EnvironmentVariables(...this.filter((envVar) => !envVar.secure()));
  }

  remove(envVar: EnvironmentVariable) {
    _.remove(this, envVar);
  }
}
