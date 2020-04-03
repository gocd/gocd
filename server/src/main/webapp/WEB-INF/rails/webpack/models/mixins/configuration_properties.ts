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

import _ from "lodash";
import {Accessor, basicAccessor, serialize, serializing} from "models/base/accessor";

export interface PropertyLike {
  key: string;
  value?: string;
  encrypted_value?: string;
  secure?: boolean;
}

export const USER_NS        = "userdef";

export const USER_NS_PREFIX = `${USER_NS}.`;

export class ConfigurationProperties {
  knownProps = basicAccessor<PropertyLike[]>([]);
  userProps = basicAccessor<PropertyLike[]>([]);

  private readonly allowUserDef: boolean;

  constructor(allowUserDef: boolean = false) {
    this.allowUserDef = allowUserDef;
    this.configuration = serializing(this.configuration, this);
  }

  configuration(configs?: PropertyLike[]): PropertyLike[] {
    if (arguments.length) {
      this.knownProps([]);
      this.userProps([]);

      if (configs) {
        if (this.allowUserDef) {
          _.each(configs, (c) => { (c.key.startsWith(USER_NS_PREFIX) ? this.userProps() : this.knownProps()).push(c); });
        } else {
          this.knownProps([].slice.call(configs));
        }
      }
    }

    return this.allowUserDef ? this.knownProps().concat(this.userProps()) : this.knownProps();
  }

  /**
   * Creates an {@link Accessor} facade to manage an arbitrary, canonical (i.e., not
   * user-defined) configuration property. This is useful provide a {@link Stream}-like
   * interaction with a specific property's value as if it were a regular string field
   * on your model.
   *
   * @param key the property key
   */
  protected propertyAsAccessor(key: string): Accessor<string> {
    const get = (key: string) => this.findProperty(key);
    const set = (key: string, value: string) => {
      let current;
      // remove all matching keys, in case the key is not unique
      while (current = get(key)) { // tslint:disable-line no-conditional-assignment
        this.knownProps().splice(this.knownProps().indexOf(current), 1);
      }
      this.knownProps().unshift({ key, value });
    };

    // tslint:disable-next-line only-arrow-functions
    return function (v?: string): string {
      if (arguments.length) {
        set(key, v!);
      }

      const current = get(key);
      return (current ? current.value : undefined) as string;
    };
  }

  protected findProperty(key: string) {
    return _.find(this.knownProps(), (p) => p.key === key);
  }
}

/** Merges property collections from right to left, returning a copy */
export function mergeProps(left: PropertyLike[], right: PropertyLike[]) {
  const existing = right.map((c) => c.key);
  const more: PropertyLike[] = [];
  for (const prop of left) {
    if (existing.indexOf(prop.key) < 0) {
      more.push(serialize(prop));
    }
  }

  return right.concat(more);
}
