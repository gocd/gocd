/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {mixins as s} from "helpers/string-plus";

interface ClearTextData {
  clearText?: string;
}

interface EncryptedData {
  cipherText?: string;
}

export class EncryptedValue {
  private static readonly MASKED = "********************************";

  private readonly _originalValue: Stream<string>;
  private readonly _value: Stream<string>;
  private readonly _isEncrypted: Stream<boolean>;
  private readonly _canEdit: Stream<boolean>;

  constructor(data: EncryptedData | ClearTextData) {
    if (_.has(data, "clearText") && _.has(data, "cipherText")) {
      throw new Error("You cannot initialize an encrypted value with both clear text and cipher text!");
    }

    this._originalValue = Stream(_.has(data, "cipherText") ? _.get(data, "cipherText") : _.get(data, "clearText"));
    this._value         = Stream(_.has(data, "cipherText") ? _.get(data, "cipherText") : _.get(data, "clearText"));
    this._isEncrypted   = Stream(_.has(data, "cipherText"));
    this._canEdit       = Stream(!this.isSecure());
  }

  value(...args: any): string {
    if (args.length) {
      const any: string = args[0];
      if (this.isPlain()) {
        this._value(any);
        return this._value();
      } else {
        if (this._canEdit()) {
          this._value(any);
          return this._value();
        } else {
          throw new Error("You cannot edit a cipher text value!");
        }
      }
    }
    return this._value();
  }

  valueForDisplay() {
    if (!_.isEmpty(this.value())) {
      return EncryptedValue.MASKED;
    }
  }

  edit() {
    this._canEdit(true);
    this._value("");
  }

  isEditing() {
    return this._canEdit();
  }

  isDirty() {
    return this.value() !== this._originalValue();
  }

  isPlain() {
    return !this.isSecure();
  }

  isSecure() {
    return this._isEncrypted();
  }

  becomeSecure() {
    this._isEncrypted(true);
    this.preventEdit();
  }

  preventEdit() {
    this._canEdit(false);
  }

  becomeUnSecure() {
    this._isEncrypted(false);
    this._canEdit(true);
  }

  getOriginal() {
    return this._originalValue();
  }

  resetToOriginal() {
    if (s.isBlank(this._originalValue())) {
      this.edit();
    } else {
      this._value(this._originalValue());
      this._canEdit(false);
    }
  }

}

export interface PasswordLike {
  cipherText?: string;
  plainText?: string;
}

export function plainOrCipherValue(passwordLike: PasswordLike): EncryptedValue {
  if (passwordLike.cipherText) {
    return new EncryptedValue({cipherText: s.defaultToIfBlank(passwordLike.cipherText, "")});
  } else {
    return new EncryptedValue({clearText: s.defaultToIfBlank(passwordLike.plainText!, "")});
  }
}
