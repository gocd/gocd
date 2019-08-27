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
import _ from "lodash";
import s from "underscore.string";

interface EncryptedValueData {
  clearText?: string;
  cipherText?: string;
}

export class EncryptedValue {
  private readonly originalValue: Stream<string>;
  private readonly _value: Stream<string>;
  private readonly isEncrypted: Stream<boolean>;
  private readonly canEdit: Stream<boolean>;

  constructor(data: EncryptedValueData) {
    if (!_.has(data, "clearText") && !_.has(data, "cipherText")) {
      throw "Please provide either clear text or cipher text for the environment variable!";
    }

    if (_.has(data, "clearText") && _.has(data, "cipherText")) {
      throw "You cannot initialize an encrypted value with both clear text and cipher text!";
    }

    this.originalValue = Stream(_.has(data, "cipherText") ? data.cipherText! : data.clearText!);
    this._value        = Stream(_.has(data, "cipherText") ? data.cipherText! : data.clearText!);
    this.isEncrypted   = Stream(_.has(data, "cipherText"));
    this.canEdit       = Stream(!this.isEncrypted());
  }

  isSecure() {
    return this.isEncrypted();
  }

  isPlain() {
    return !this.isSecure();
  }

  isEditing() {
    return this.canEdit();
  };

  isDirty(): boolean {
    return this.value() !== this.originalValue();
  };

  value(val?: string): string {
    if (val) {
      if (this.isPlain()) {
        this._value(val);
        return this._value();
      } else {
        if (this.canEdit()) {
          this._value(val);
          return this._value();
        } else {
          throw "You cannot edit a cipher text value!";
        }
      }
    }
    return this._value();
  }

  edit() {
    this.canEdit(true);
    this._value("");
  };

  becomeSecure() {
    this.isEncrypted(true);
    this.preventEdit();
  };

  preventEdit() {
    this.canEdit(false);
  };

  becomeUnSecure() {
    this.isEncrypted(false);
    this.canEdit(true);
  };

  getOriginal() {
    this.originalValue();
  };

  resetToOriginal() {
    if (s.isBlank(this.originalValue())) {
      this.edit();
    } else {
      this._value(this.originalValue());
      this.canEdit(false);
    }
  };
}

export interface EnvironmentVariableJSON {
  name: string;
  secure: boolean;
  encrypted_value?: string;
  value?: string
}

export class EnvironmentVariable {
  readonly name: Stream<string>;
  readonly value: Stream<EncryptedValue>;

  constructor(name: string, encryptedValue: EncryptedValue) {
    this.name  = Stream(name);
    this.value = Stream(encryptedValue);
  }

  static fromJSON(json: EnvironmentVariableJSON): EnvironmentVariable {
    let value: EncryptedValue;
    if (json.secure) {
      value = new EncryptedValue({cipherText: s.isBlank(json.encrypted_value!) ? "" : json.encrypted_value});
    } else {
      value = new EncryptedValue({clearText: s.isBlank(json.value!) ? "" : json.value});
    }

    return new EnvironmentVariable(json.name, value);
  }
}

export class EnvironmentVariables {
  readonly variables: Stream<EnvironmentVariable[]>;

  constructor(variables?: EnvironmentVariable[]) {
    this.variables = Stream(variables || []);
  }

  secureVariables(): EnvironmentVariable[] {
    return this.variables().filter(variable => variable.value().isSecure());
  }

  plainVariables(): EnvironmentVariable[] {
    return this.variables().filter(variable => variable.value().isPlain());
  }

  static fromJSON(json: EnvironmentVariableJSON[]): EnvironmentVariables {
    return new EnvironmentVariables(json.map(EnvironmentVariable.fromJSON));
  }
}
