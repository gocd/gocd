/*
 * Copyright 2020 ThoughtWorks, Inc.
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
export interface ConfigValue {
  getValue(): string;

  setValue(value: string): void;

  isEncrypted(): boolean;

  getDisplayValue(): string;
}

export class EncryptedValue implements ConfigValue {
  private val: string;

  constructor(val: string) {
    this.val = val;
  }

  getValue(): string {
    return this.val;
  }

  setValue(value: string): void {
    this.val = value;
  }

  isEncrypted(): boolean {
    return true;
  }

  getDisplayValue(): string {
    if (this.getValue()) {
      return this.getValue().replace(/./gi, "*");
    }
    return this.getValue();
  }
}

export class PlainTextValue implements ConfigValue {
  private val: string;

  constructor(val: string) {
    this.val = val;
  }

  getValue(): string {
    return this.val;
  }

  setValue(value: string): void {
    this.val = value;
  }

  isEncrypted(): boolean {
    return false;
  }

  getDisplayValue(): string {
    return this.getValue();
  }
}
