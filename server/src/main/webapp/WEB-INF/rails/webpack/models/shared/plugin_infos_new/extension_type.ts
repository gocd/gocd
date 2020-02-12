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
import {
  AnalyticsExtension,
  ArtifactExtension,
  AuthorizationExtension,
  ConfigRepoExtension,
  ElasticAgentExtension, Extension, NotificationExtension, PackageRepoExtension, ScmExtension, SecretExtension, TaskExtension
} from "models/shared/plugin_infos_new/extensions";
import {
  AnalyticsExtensionJSON,
  ArtifactExtensionJSON,
  AuthorizationExtensionJSON,
  ConfigRepoExtensionJSON,
  ElasticAgentExtensionJSON,
  ExtensionJSON,
  NotificationExtensionJSON,
  PackageRepoExtensionJSON,
  SCMExtensionJSON,
  SecretConfigExtensionJSON,
  TaskExtensionJSON
} from "models/shared/plugin_infos_new/serialization";

export enum ExtensionTypeString {
  CONFIG_REPO = "configrepo",
  ELASTIC_AGENTS = "elastic-agent",
  AUTHORIZATION = "authorization",
  SCM = "scm",
  TASK = "task",
  PACKAGE_REPO = "package-repository",
  NOTIFICATION = "notification",
  ANALYTICS = "analytics",
  ARTIFACT = "artifact",
  SECRETS = "secrets"
}

export abstract class AbstractExtensionType<T extends ExtensionJSON> {
  readonly extensionType: ExtensionTypeString;

  constructor(extensionType: ExtensionTypeString) {
    this.extensionType = extensionType;
  }

  abstract humanReadableName(): string;

  abstract linkForDocs(): string;

  abstract fromJSON(data: T): Extension;
}

export class ConfigRepoExtensionType extends AbstractExtensionType<ConfigRepoExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.CONFIG_REPO);
  }

  fromJSON(data: ConfigRepoExtensionJSON) {
    return ConfigRepoExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "config repository";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#config-repo";
  }

}

export class ElasticAgentsExtensionType extends AbstractExtensionType<ElasticAgentExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.ELASTIC_AGENTS);
  }

  fromJSON(data: ElasticAgentExtensionJSON) {
    return ElasticAgentExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "elastic agent";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#elastic-agents";
  }

}

export class AuthorizationExtensionType extends AbstractExtensionType<AuthorizationExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.AUTHORIZATION);
  }

  fromJSON(data: AuthorizationExtensionJSON) {
    return AuthorizationExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "authorization";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#authorization";
  }

}

export class SCMExtensionType extends AbstractExtensionType<SCMExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.SCM);
  }

  fromJSON(data: SCMExtensionJSON) {
    return ScmExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "SCM";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#scm";
  }

}

export class TaskExtensionType extends AbstractExtensionType<TaskExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.TASK);
  }

  fromJSON(data: TaskExtensionJSON) {
    return TaskExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "task";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#task";
  }

}

export class PackageRepoExtensionType extends AbstractExtensionType<PackageRepoExtensionJSON> {

  constructor() {
    super(ExtensionTypeString.PACKAGE_REPO);
  }

  fromJSON(data: PackageRepoExtensionJSON) {
    return PackageRepoExtension.fromJSON(data);
  }

  humanReadableName(): string {
    return "Package Repository";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#package-repo";
  }

}

export class NotificationExtensionType extends AbstractExtensionType<NotificationExtensionJSON> {
  constructor() {
    super(ExtensionTypeString.NOTIFICATION);
  }

  humanReadableName(): string {
    return "Notification";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#notification";
  }

  fromJSON(data: NotificationExtensionJSON) {
    return NotificationExtension.fromJSON(data);
  }

}

export class AnalyticsExtensionType extends AbstractExtensionType<AnalyticsExtensionJSON> {
  constructor() {
    super(ExtensionTypeString.ANALYTICS);
  }

  humanReadableName(): string {
    return "analytics";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#";
  }

  fromJSON(data: AnalyticsExtensionJSON) {
    return AnalyticsExtension.fromJSON(data);
  }
}

export class ArtifactExtensionType extends AbstractExtensionType<ArtifactExtensionJSON> {
  constructor() {
    super(ExtensionTypeString.ARTIFACT);
  }

  humanReadableName(): string {
    return "artifact";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#artifact";
  }

  fromJSON(data: ArtifactExtensionJSON) {
    return ArtifactExtension.fromJSON(data);
  }

}

export class SecretsExtensionType extends AbstractExtensionType<SecretConfigExtensionJSON> {
  constructor() {
    super(ExtensionTypeString.SECRETS);
  }

  humanReadableName(): string {
    return "secrets";
  }

  linkForDocs(): string {
    return "https://www.gocd.org/plugins/#secrets";
  }

  fromJSON(data: SecretConfigExtensionJSON) {
    return SecretExtension.fromJSON(data);
  }
}

export abstract class ExtensionType {
  static readonly ALL_EXTENSION_TYPES = new Array<AbstractExtensionType<ExtensionJSON>>();

  static initialize() {
    this.ALL_EXTENSION_TYPES.push(new ConfigRepoExtensionType());
    this.ALL_EXTENSION_TYPES.push(new ElasticAgentsExtensionType());
    this.ALL_EXTENSION_TYPES.push(new AuthorizationExtensionType());
    this.ALL_EXTENSION_TYPES.push(new SCMExtensionType());
    this.ALL_EXTENSION_TYPES.push(new TaskExtensionType());
    this.ALL_EXTENSION_TYPES.push(new PackageRepoExtensionType());
    this.ALL_EXTENSION_TYPES.push(new NotificationExtensionType());
    this.ALL_EXTENSION_TYPES.push(new AnalyticsExtensionType());
    this.ALL_EXTENSION_TYPES.push(new ArtifactExtensionType());
    this.ALL_EXTENSION_TYPES.push(new SecretsExtensionType());
  }

  static fromString<T extends ExtensionJSON>(type: string): AbstractExtensionType<T> {
    const extensionType = _.find(this.ALL_EXTENSION_TYPES, (eachExtensionType: AbstractExtensionType<T>) => {
      return eachExtensionType.extensionType === type;
    });
    if (!extensionType) {
      throw new Error(`Unsupported extension type: ${type} of type(${typeof type})`);
    }
    return extensionType;
  }
}

ExtensionType.initialize();
