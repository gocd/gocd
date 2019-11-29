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

import Stream from "mithril/stream";
import {EnvironmentVariable, EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";
import {Origin, OriginJSON, OriginType} from "models/origin";

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

  editable() {
    const origin = this.origin();
    return origin === undefined || (origin.type() !== undefined && origin.type() === OriginType.GoCD);
  }

  reasonForNonEditable() {
    if (this.editable()) {
      throw Error("Environment variable is editable");
    }
    return "Cannot edit this environment variable as it is defined in config repo";
  }

  clone() {
    return new EnvironmentVariableWithOrigin(this.name(),
                                             this.origin().clone(),
                                             this.value(),
                                             this.secure(),
                                             this.encryptedValue());
  }
}

export class EnvironmentVariablesWithOrigin extends EnvironmentVariables<EnvironmentVariableWithOrigin> {
  static fromJSON(environmentVariables: EnvironmentEnvironmentVariableJSON[]) {
    return new EnvironmentVariablesWithOrigin(...environmentVariables.map(EnvironmentVariableWithOrigin.fromJSON));
  }
}
