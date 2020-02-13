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
import {applyMixins} from "models/mixins/mixins";
import {PackageJSON, PackageRepositoryJSON, PackageRepositorySummaryJSON, PackageUsageJSON, PackageUsagesJSON, PluginMetadataJSON} from "./package_repositories_json";
import {Configurations} from "../shared/configuration";

class PackageRepositorySummary extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;

  constructor(id?: string, name?: string) {
    super();
    ValidatableMixin.call(this);
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
    }
  }
}

applyMixins(PackageRepositorySummary, ValidatableMixin);

export class Package extends ValidatableMixin {
  id: Stream<string>;
  name: Stream<string>;
  autoUpdate: Stream<boolean>;
  packageRepo: Stream<PackageRepositorySummary>;
  configuration: Stream<Configurations>;

  constructor(id: string, name: string, autoUpdate: boolean, configuration: Configurations, packageRepo: PackageRepositorySummary) {
    super();
    ValidatableMixin.call(this);
    this.id            = Stream(id);
    this.name          = Stream(name);
    this.autoUpdate    = Stream(autoUpdate);
    this.configuration = Stream(configuration);
    this.packageRepo   = Stream(packageRepo);

    this.validatePresenceOf("id");
    this.validateFormatOf("id",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("id", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated('packageRepo')
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

applyMixins(Package, ValidatableMixin);

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
    ValidatableMixin.call(this);
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

applyMixins(PluginMetadata, ValidatableMixin);

export class PackageRepository extends ValidatableMixin {
  repoId: Stream<string>;
  name: Stream<string>;
  pluginMetadata: Stream<PluginMetadata>;
  configuration: Stream<Configurations>;
  packages: Stream<Packages>;

  constructor(repoId: string, name: string, pluginMetadata: PluginMetadata, configuration: Configurations, packages: Packages) {
    super();
    ValidatableMixin.call(this);
    this.repoId         = Stream(repoId);
    this.name           = Stream(name);
    this.pluginMetadata = Stream(pluginMetadata);
    this.configuration  = Stream(configuration);
    this.packages       = Stream(packages);

    this.validatePresenceOf("repoId");
    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
    this.validateAssociated("pluginMetadata");
    this.validateEach("packages");
  }

  static fromJSON(data: PackageRepositoryJSON): PackageRepository {
    return new PackageRepository(data.repo_id, data.name, PluginMetadata.fromJSON(data.plugin_metadata), Configurations.fromJSON(data.configuration), Packages.fromJSON(data._embedded.packages));
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
    }
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

applyMixins(PackageRepository, ValidatableMixin);

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
