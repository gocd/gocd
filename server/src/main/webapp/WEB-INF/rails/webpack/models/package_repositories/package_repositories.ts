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
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Errors} from "../mixins/errors";
import {Configurations} from "../shared/configuration";
import {
  PackageJSON,
  PackageRepositoryJSON,
  PackageRepositorySummaryJSON,
  PackageUsageJSON,
  PackageUsagesJSON,
  PluginMetadataJSON
} from "./package_repositories_json";

export class PackageRepositorySummary extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;

  constructor(id?: string, name?: string) {
    super();

    this.id   = Stream(id || "");
    this.name = Stream(name || "");

    this.validatePresenceOf("id");
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
    };
  }
}

export class Package extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  packageRepo: Stream<PackageRepositorySummary>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, configuration: Configurations, packageRepo: PackageRepositorySummary, errors: Errors = new Errors()) {
    super();

    this.id            = Stream(id);
    this.name          = Stream(name);
    this.autoUpdate    = Stream(autoUpdate);
    this.configuration = Stream(configuration);
    this.packageRepo   = Stream(packageRepo);
    this.errors(errors);

    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Name. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated('packageRepo');
  }

  static fromJSON(data: PackageJSON): Package {
    const errors         = new Errors(data.errors);
    const configurations = data.configuration ? Configurations.fromJSON(data.configuration) : new Configurations([]);
    return new Package(data.id, data.name, data.auto_update, configurations, PackageRepositorySummary.fromJSON(data.package_repo), errors);
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
    };
  }

  key() {
    return `${this.packageRepo().name()}_${this.name()}`;
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

class PluginMetadata extends ValidatableMixin {
  id: Stream<string>;
  version: Stream<string>;

  constructor(id: string, version: string) {
    super();

    this.id      = Stream(id);
    this.version = Stream(version);

    this.validatePresenceOf("id");
  }

  static fromJSON(data: PluginMetadataJSON): PluginMetadata {
    return new PluginMetadata(data.id, data.version);
  }

  static default() {
    return new PluginMetadata("", "");
  }
}

export class PackageRepository extends ValidatableMixin {
  repoId: Stream<string>;
  name: Stream<string>;
  pluginMetadata: Stream<PluginMetadata>;
  configuration: Stream<Configurations>;
  packages: Stream<Packages>;

  constructor(repoId: string, name: string, pluginMetadata: PluginMetadata, configuration: Configurations, packages: Packages, errors: Errors = new Errors()) {
    super();

    this.repoId         = Stream(repoId);
    this.name           = Stream(name);
    this.pluginMetadata = Stream(pluginMetadata);
    this.configuration  = Stream(configuration);
    this.packages       = Stream(packages);
    this.errors((errors));

    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Name. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated("pluginMetadata");
  }

  static fromJSON(data: PackageRepositoryJSON): PackageRepository {
    const errors = new Errors(data.errors);
    return new PackageRepository(data.repo_id, data.name, PluginMetadata.fromJSON(data.plugin_metadata), Configurations.fromJSON(data.configuration), Packages.fromJSON(data._embedded.packages), errors);
  }

  static default() {
    return new PackageRepository("", "", PluginMetadata.default(), new Configurations([]), []);
  }

  toJSON(): object {
    return {
      repo_id:         this.repoId(),
      name:            this.name(),
      plugin_metadata: {
        id:      this.pluginMetadata().id(),
        version: this.pluginMetadata().version()
      },
      configuration:   this.configuration().toJSON()
    };
  }

  matches(textToMatch: string): boolean {
    if (!textToMatch) {
      return true;
    }

    const searchableStrings = [
      this.name(),
      this.pluginMetadata().id()
    ];
    searchableStrings.push(...this.packages().map((pkg) => pkg.name()));
    return searchableStrings.some((value) => value ? value.toLowerCase().includes(textToMatch.toLowerCase()) : false);
  }
}

export class PackageRepositories extends Array<PackageRepository> {
  constructor(...pkgRepo: PackageRepository[]) {
    super(...pkgRepo);
    Object.setPrototypeOf(this, Object.create(PackageRepositories.prototype));
  }

  static fromJSON(data: PackageRepositoryJSON[]): PackageRepositories {
    return new PackageRepositories(...data.map((a) => PackageRepository.fromJSON(a)));
  }
}

class PackageUsage {
  group: string;
  pipeline: string;

  constructor(group: string, pipeline: string) {
    this.group    = group;
    this.pipeline = pipeline;
  }

  static fromJSON(data: PackageUsageJSON): PackageUsage {
    return new PackageUsage(data.group, data.pipeline);
  }
}

export class PackageUsages extends Array<PackageUsage> {
  constructor(...vals: PackageUsage[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(PackageUsages.prototype));
  }

  static fromJSON(data: PackageUsagesJSON): PackageUsages {
    return new PackageUsages(...data.usages.map((a) => PackageUsage.fromJSON(a)));
  }
}
