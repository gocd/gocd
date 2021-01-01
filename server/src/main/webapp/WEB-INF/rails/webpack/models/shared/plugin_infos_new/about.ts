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
import {AboutJSON, VendorJSON} from "models/shared/plugin_infos_new/serialization";

export class About {
  readonly name: string;
  readonly version: string;
  readonly targetGoVersion: string;
  readonly description: string;
  readonly targetOperatingSystems: string[];
  readonly vendor: Vendor;

  constructor(name: string,
              version: string,
              targetGoVersion: string,
              description: string,
              targetOperationSystems: string[],
              vendor: Vendor) {
    this.name                   = name;
    this.version                = version;
    this.targetGoVersion        = targetGoVersion;
    this.description            = description;
    this.targetOperatingSystems = targetOperationSystems;
    this.vendor                 = vendor;
  }

  static fromJSON(data: AboutJSON) {
    return new About(data.name,
                     data.version,
                     data.target_go_version,
                     data.description,
                     data.target_operating_systems,
                     Vendor.fromJSON(data.vendor || {})
    );
  }

  targetOperatingSystemsDisplayValue(): string {
    if (_.isEmpty(this.targetOperatingSystems)) {
      return "No restrictions";
    }
    return _.join(this.targetOperatingSystems, ",");
  }
}

export class Vendor {
  readonly name: string;
  readonly url: string;

  constructor(name: string, url: string) {
    this.name = name;
    this.url  = url;
  }

  static fromJSON(data: VendorJSON) {
    return new Vendor(data.name, data.url);
  }
}
