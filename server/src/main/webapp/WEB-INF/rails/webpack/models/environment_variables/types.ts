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
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Origin, OriginJSON, OriginType} from "models/origin";

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
    this.validatePresenceOf("name", {condition: () => !_.isEmpty(this.value()) || !_.isEmpty(this.encryptedValue())});
  }

  static fromJSON(data: EnvironmentVariableJSON) {
    return new EnvironmentVariable(data.name, data.value, data.secure, data.encrypted_value);
  }

  editable() {
    return true;
  }

  reasonForNonEditable() {
    throw Error("Environment variable is editable");
  }

  toJSON() {
    return {
      name: this.name(),
      value: this.value(),
      encrypted_value: this.encryptedValue(),
      secure: this.secure()
    };
  }

  equals(environmentVariable: EnvironmentVariable): boolean {
    return this.name() === environmentVariable.name()
      && this.value() === environmentVariable.value()
      && this.encryptedValue() === environmentVariable.encryptedValue();
  }
}

//tslint:disable-next-line
export interface EnvironmentVariables extends ValidatableMixin {
}

export class EnvironmentVariables<T extends EnvironmentVariable = EnvironmentVariable> extends Array<T> implements ValidatableMixin {
  constructor(...environmentVariables: T[]) {
    super(...environmentVariables);
    Object.setPrototypeOf(this, Object.create(EnvironmentVariables.prototype));
    ValidatableMixin.call(this);
  }

  static fromJSON(environmentVariables: EnvironmentVariableJSON[]) {
    return new EnvironmentVariables(...environmentVariables.map(EnvironmentVariable.fromJSON));
  }

  secureVariables(): EnvironmentVariables<T> {
    return new EnvironmentVariables<T>(...this.filter((envVar) => envVar.secure()));
  }

  plainTextVariables(): EnvironmentVariables<T> {
    return new EnvironmentVariables<T>(...this.filter((envVar) => !envVar.secure()));
  }

  remove(envVar: T) {
    this.splice(this.indexOf(envVar), 1);
  }

  toJSON() {
    return this.map((envVar) => envVar.toJSON());
  }
}

applyMixins(EnvironmentVariables, ValidatableMixin);

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
    return this.origin().type() === OriginType.GoCD;
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
