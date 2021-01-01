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
import {Accessor} from "models/base/accessor";
import {PropertyErrors, PropertyJSON} from "models/shared/configuration";
import {EncryptedValue} from "../forms/encrypted_value";

// Compatible with Configuration instances
interface EncryptableEntry {
  key: string;
  value: string;
  encrypted: boolean;
}

interface SerializedFreshEntry {
  key: string;
  value: string;
  secure?: boolean;
}

interface SerializedServerEntry {
  key: string;
  value?: string;
  encrypted_value?: string;
}

interface NamespacedStore {
  key: Accessor<string>;
  name: Accessor<string>;
  isNs: () => boolean;
}

export class EntriesVM {
  readonly entries: EntryVM[];
  private readonly namespace: string;

  constructor(configurations?: EncryptableEntry[], namespace="") {
    this.namespace = namespace;
    this.entries = configurations ? _.map(filteredByNamespace(configurations, namespace), (conf) => new EntryVM(conf, namespace)) : [];
  }

  appendBlank() {
    this.entries.push(new EntryVM(void 0, this.namespace));
  }

  excise(el: EntryVM) {
    const pos = this.entries.indexOf(el);
    if (pos < 0) { return; }
    this.entries.splice(pos, 1);
  }

  bindErrors(json: PropertyJSON[]) {
    const prefix = (this.namespace || "") + ".";
    json = _.filter(json, (f) => this.namespace ? (f.key.startsWith(prefix) || _.isEmpty(f.key)) : true);

    // relies on the preserved order from the server to client
    _.filter(this.entries, (v) => !v.isBlank()).forEach((vm, i) => {
      const frag = json[i];
      if (frag) {
        vm.errors = frag.errors;
      }
    });
  }

  toJSON() {
    return _.map(_.filter(this.entries, (e) => !e.isBlank()), (e) => e.toJSON());
  }
}

export class EntryVM {
  readonly key: Accessor<string> = Stream("");
  readonly name: Accessor<string>;
  readonly value: Accessor<string> = Stream("");
  readonly secretValue: Accessor<EncryptedValue> = Stream(plain(""));
  readonly isNs: () => boolean;

  private readonly original?: EncryptableEntry;
  private encrypted: boolean = false;

  constructor(original?: EncryptableEntry, namespace="") {
    if (original) {
      this.original = original;
      this.reset();
    }

    const ns = new RegExp(`^${_.escapeRegExp(namespace + ".")}`);
    const key = this.key;

    this.isNs = () => !!namespace;

    // tslint:disable-next-line only-arrow-functions
    this.name = function(v?: string): string {
      if (arguments.length) {
        if ("" !== v && !!namespace) {
          key(namespace + "." + v);
        } else {
          key(v);
        }
      }

      return !!namespace ? key().replace(ns, "") : key();
    };

    ErrorsMixin.call(this, this);
  }

  valueAlreadyEncrypted(): boolean {
    return !!(
      this.encrypted &&
      this.original &&
      this.original.encrypted &&
      this.secretValue().isSecure() && // true when constructed with ciphered text, usually.
      this.original.value === this.secretAsString()
    );
  }

  isSecure(secure?: boolean): boolean {
    if (arguments.length) {
      if (this.valueAlreadyEncrypted()) {
        if (!secure) { // setting to insecure
          this.clearAllValues();
        } else {
          // ensure value is recordered as ciphered text.
          // this is likely redundant
          this.clearAndSetSecret(ciphered(this.secretAsString()));
        }
      } else {
        if (!secure) {
          this.clearAndSetValue(this.secretAsString() || this.value());
        } else {
          this.clearAndSetSecret(plain(this.value()));
        }
      }

      this.encrypted = secure!;
    }

    return this.encrypted;
  }

  isBlank() {
    return !this.key() && !this.value() && !this.secretAsString();
  }

  toJSON(): (SerializedServerEntry | SerializedFreshEntry) {
    const key = this.key();
    const secure = this.isSecure();

    if (secure) {
      if (this.valueAlreadyEncrypted()) {
        return { key, encrypted_value: this.secretAsString() };
      }

      return { key, value: this.secretAsString(), secure };
    }

    return { key, value: this.value() || "" };
  }

  reset() {
    this.key("");
    this.clearAllValues();

    const original = this.original;
    if (original) {
      this.key(original.key);
      this.encrypted = original.encrypted;

      if (this.encrypted) {
        this.secretValue(ciphered(original.value));
      } else {
        this.value(original.value);
      }
    } else {
      this.encrypted = false;
    }
  }

  private secretAsString() {
    return this.secretValue().value();
  }

  private clearAndSetValue(val: string) {
    this.secretValue(plain(""));
    this.value(val);
  }

  private clearAndSetSecret(val: EncryptedValue) {
    this.secretValue(val);
    this.value("");
  }

  private clearAllValues() {
    this.clearAndSetValue("");
  }
}

function filteredByNamespace(arr: EncryptableEntry[], namespace="") {
  const prefix = (namespace || "") + ".";
  return namespace ? _.filter(arr, (r) => r.key.startsWith(prefix)) : arr;
}

function ciphered(val: string) {
  return new EncryptedValue({ cipherText: val });
}

function plain(val: string) {
  return new EncryptedValue({ clearText: val });
}

class ErrorsMixin {
  errors?: PropertyErrors;
  nameErrors: () => (string|undefined);
  valueErrors: () => (string|undefined);

  constructor(vm: NamespacedStore) {
    // Removes the namespace from the key in error messages.
    // Only replaces the first occurence per message, which might (?) be the
    // safest thing to do. That is merely a heuristic which may later change.
    //
    // This is only /somewhat/ reliable because namespaces are known values.
    // However, as we are operating on user free-form input, we can't guarantee
    // this transform will work all the time. E.g., a user could name a repo
    // `userdef.foo` and have a property called `foo` which may or may not trip
    // up this transform(), depending on order of occurence.
    //
    // It's the best we can do without server-side knowledge of namespaces,
    // which do not exist. It's probably not worth the added complexity vs value.
    const transform = (s: string) => s.replace(vm.key(), vm.name());

    this.nameErrors = () => {
      if (!this.errors || !this.errors.configuration_key) {
        return;
      }

      const messages = this.errors.configuration_key;
      return (vm.isNs() ? messages.map(transform) : messages).join(". ") + ".";
    };

    this.valueErrors = () => {
      if (!this.errors || (!this.errors.configuration_value && !this.errors.encrypted_value)) {
        return;
      }

      const messages = (this.errors.configuration_value || []).concat(this.errors.encrypted_value || []);
      return (vm.isNs() ? messages.map(transform) : messages).join(". ") + ".";
    };
  }
}

// tslint:disable-next-line no-empty-interface
export interface EntryVM extends ErrorsMixin {}
