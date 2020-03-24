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

import Stream from "mithril/stream";
import {Configurations} from "../shared/configuration";
import {PackageJSON, PackageRepositorySummaryJSON} from "./package_repositories_json";

class PackageRepositorySummary {
  id: Stream<string>;
  name: Stream<string>;

  constructor(id?: string, name?: string) {
    this.id   = Stream(id || "");
    this.name = Stream(name || "");
  }

  static fromJSON(data?: PackageRepositorySummaryJSON): PackageRepositorySummary {
    if (data) {
      return new PackageRepositorySummary(data.id, data.name);
    }
    return new PackageRepositorySummary();
  }

  toJSON(): object {
    return {
      id: this.id()
    }
  }
}

export class Package {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  packageRepo: Stream<PackageRepositorySummary>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, configuration: Configurations, packageRepo: PackageRepositorySummary) {
    this.id            = Stream(id);
    this.name          = Stream(name);
    this.autoUpdate    = Stream(autoUpdate);
    this.configuration = Stream(configuration);
    this.packageRepo   = Stream(packageRepo);
  }

  static fromJSON(data: PackageJSON): Package {
    const configurations = data.configuration ? Configurations.fromJSON(data.configuration) : new Configurations([]);
    return new Package(data.id, data.name, data.auto_update, configurations, PackageRepositorySummary.fromJSON(data.package_repo));
  }

  static default() {
    return new Package("", "", false, new Configurations([]), new PackageRepositorySummary());
  }

  toJSON(): object {
    return {
      id:            this.id(),
      name:          this.name(),
      auto_update:   this.autoUpdate(),
      package_repo:  this.packageRepo.toJSON(),
      configuration: this.configuration().toJSON()
    }
  }
}

export class Packages extends Array<Package> {
  constructor(...pkg: Package[]) {
    super(...pkg);
    Object.setPrototypeOf(this, Object.create(Packages.prototype));
  }

  static fromJSON(data: PackageJSON[]): Packages {
    return new Packages(...data.map((a) => Package.fromJSON(a)));
  }
}
