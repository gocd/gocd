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

export interface PluginInfosJSON {
  _embedded: EmbeddedJSON;
}

export interface EmbeddedJSON {
  plugin_info: PluginInfoJSON[];
}

export interface PluginInfoJSON {
  _links: LinksJSON;
  id: string;
  status: StatusJSON;
  plugin_file_location: string;
  bundled_plugin: boolean;
  about?: AboutJSON;
  extensions: ExtensionJSON[];
}

export interface LinksJSON {
  image?: DocJSON;
}

export interface DocJSON {
  href: string;
}

export interface VendorJSON {
  name: string;
  url: string;
}

export interface AboutJSON {
  name: string;
  version: string;
  target_go_version: string;
  description: string;
  target_operating_systems: string[];
  vendor: VendorJSON;
}

export type PluginState = "active" | "disabled" | "invalid";

export interface StatusJSON {
  state: PluginState;
  // only present if state is invalid
  messages?: string[];
}

export interface HasPluginSettings {
  plugin_settings?: PluginSettingJSON;
}

export interface ExtensionJSONBase extends HasPluginSettings {
  type: string;
}

export interface ArtifactExtensionJSON extends ExtensionJSONBase {
  store_config_settings: PluginSettingJSON;
  artifact_config_settings: PluginSettingJSON;
  fetch_artifact_settings: PluginSettingJSON;
}

export interface AnalyticsCapabilityJSON {
  id: string;
  title: string;
  type: string;
}

export interface AnalyticsCapabilitiesJSON {
  supported_analytics: AnalyticsCapabilityJSON[];
}

export interface AnalyticsExtensionJSON extends ExtensionJSONBase {

  capabilities: AnalyticsCapabilitiesJSON;
}

export interface ConfigRepoExtensionJSON extends ExtensionJSONBase {
  capabilities: ConfigRepoCapabilitiesJSON;
}

export interface ConfigRepoCapabilitiesJSON {
  supports_pipeline_export: boolean;
  supports_parse_content: boolean;
}

export interface SecretConfigExtensionJSON extends ExtensionJSONBase {
  secret_config_settings: PluginSettingJSON;
}

export interface ElasticAgentCapabilitiesJSON {
  supports_plugin_status_report: boolean;
  supports_agent_status_report: boolean;
  supports_cluster_status_report: boolean;
}

export interface PluginSettingJSON {
  configurations?: ConfigurationJSON[];
  view?: ViewJSON;
}

export interface ElasticAgentExtensionJSON extends ExtensionJSONBase {
  elastic_agent_profile_settings: PluginSettingJSON;
  supports_cluster_profiles: boolean;
  cluster_profile_settings?: PluginSettingJSON;
  capabilities: ElasticAgentCapabilitiesJSON;
}

export interface AuthorizationExtensionJSON extends ExtensionJSONBase {
  auth_config_settings: PluginSettingJSON;
  role_settings: PluginSettingJSON;
  capabilities: AuthorizationCapabilitiesJSON;
}

export interface MetadataJSON {
  secure: boolean;
  required: boolean;
}

export interface ConfigurationJSON {
  key: string;
  metadata: MetadataJSON;
}

export interface ViewJSON {
  template: string;
}

export interface AuthorizationCapabilitiesJSON {
  can_search: boolean;
  supported_auth_type: string;
  can_authorize: boolean;
}

export interface SCMExtensionJSON extends ExtensionJSONBase {
  display_name: string;
  scm_settings: SCMSettingJSON;
}

export interface SCMSettingJSON {
  configurations: SCMSettingsConfiguration[];
  view: ViewJSON;
}

export interface SCMSettingsConfiguration {
  key: string;
  metadata: SCMMetadataJSON;
}

export interface SCMMetadataJSON extends MetadataJSON {
  part_of_identity: boolean;
}

export interface TaskExtensionJSON extends ExtensionJSONBase {
  display_name: string;
  task_settings: PluginSettingJSON;
}

export interface PackageMetadataJSON extends MetadataJSON {
  part_of_identity: boolean;
  display_name: string;
  display_order: number;
}

export interface PackageSettingsConfiguration {
  key: string;
  metadata: PackageMetadataJSON;
}

export interface PackageRepoSettingJSON {
  configurations: PackageSettingsConfiguration[];
  view: ViewJSON;
}

export interface PackageRepoExtensionJSON extends ExtensionJSONBase {
  package_settings: PackageRepoSettingJSON;
  repository_settings: PackageRepoSettingJSON;
}

// tslint:disable-next-line:no-empty-interface
export interface NotificationExtensionJSON extends ExtensionJSONBase {
}

export type ExtensionJSON =
  ElasticAgentExtensionJSON
  | ConfigRepoExtensionJSON
  | AuthorizationExtensionJSON
  | SCMExtensionJSON
  | TaskExtensionJSON
  | PackageRepoExtensionJSON
  | NotificationExtensionJSON
  | SecretConfigExtensionJSON
  | ArtifactExtensionJSON
  | AnalyticsExtensionJSON;
