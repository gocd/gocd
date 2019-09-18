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
import {EnvironmentVariable, EnvironmentVariableJSON} from "models/environment_variables/types";
import {Origin, OriginJSON} from "models/new-environments/origin";

export interface EnvironmentEnvironmentVariableJSON extends EnvironmentVariableJSON {
  origin: OriginJSON;
}

export class EnvironmentVariableWithOrigin extends EnvironmentVariable {
  readonly origin: Stream<Origin>;

  constructor(name: string, origin: Origin, value?: string, secure?: boolean, encryptedValue?: string) {
    super(name, value, secure, encryptedValue);
    this.origin = Stream(origin);
  }

  static fromJSON(data: EnvironmentEnvironmentVariableJSON) {
    return new EnvironmentVariableWithOrigin(data.name,
                                             Origin.fromJSON(data.origin),
                                             data.value,
                                             data.secure,
                                             data.encrypted_value);
  }
}

export class EnvironmentVariables extends Array<EnvironmentVariableWithOrigin> {
  constructor(...environmentVariables: EnvironmentVariableWithOrigin[]) {
    super(...environmentVariables);
    Object.setPrototypeOf(this, Object.create(EnvironmentVariables.prototype));
  }

  static fromJSON(environmentVariables: EnvironmentEnvironmentVariableJSON[]) {
    return new EnvironmentVariables(...environmentVariables.map(EnvironmentVariableWithOrigin.fromJSON));
  }

  secureVariables(): EnvironmentVariables {
    return new EnvironmentVariables(...this.filter((envVar) => envVar.secure()));
  }

  plainTextVariables(): EnvironmentVariables {
    return new EnvironmentVariables(...this.filter((envVar) => !envVar.secure()));
  }

  remove(envVar: EnvironmentVariableWithOrigin) {
    _.remove(this, envVar);
  }
}
