/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations, PropertyJSON} from "models/shared/configuration";

export class ClusterProfiles {
  private readonly profiles: Stream<ClusterProfile[]>;

  constructor(profiles: ClusterProfile[]) {
    this.profiles = stream(profiles);
  }

  static fromJSON(profilesJson: ClusterProfileJSON[]): ClusterProfiles {
    const profiles = profilesJson.map((profile: ClusterProfileJSON) => {
      return ClusterProfile.fromJSON(profile);
    });
    return new ClusterProfiles(profiles);
  }

  all(): ClusterProfile[] {
    return this.profiles();
  }
}

export class ElasticAgentProfiles {
  private readonly profiles: Stream<ElasticAgentProfile[]>;

  constructor(profiles: ElasticAgentProfile[]) {
    this.profiles = stream(profiles);
  }

  static fromJSON(profilesJson: ElasticProfileJSON[]): ElasticAgentProfiles {
    const profiles = profilesJson.map((profile: ElasticProfileJSON) => {
      return ElasticAgentProfile.fromJSON(profile);
    });
    return new ElasticAgentProfiles(profiles);
  }

  all(): Stream<ElasticAgentProfile[]> {
    return this.profiles;
  }

  size(): number {
    return this.profiles() == null ? 0 : this.profiles().length;
  }

  empty(): boolean {
    return this.size() === 0;
  }

  groupByPlugin() {
    return _.groupBy(this.profiles(), (profile) => {
      return profile.pluginId;
    });
  }
}

export interface ProfileUsageJSON {
  pipeline_name: string;
  stage_name: string;
  job_name: string;
  template_name?: string;
  pipeline_config_origin?: string;
}

export class ProfileUsage {
  pipelineName: Stream<string>;
  jobName: Stream<string>;
  stageName: Stream<string>;
  templateName: Stream<string>;
  pipelineConfigOrigin: Stream<string>;

  constructor(pipelineName: string,
              stageName: string,
              jobName: string,
              templateName?: string,
              pipelineConfigOrigin?: string) {
    this.pipelineName         = stream(pipelineName);
    this.stageName            = stream(stageName);
    this.jobName              = stream(jobName);
    this.templateName         = stream(templateName);
    this.pipelineConfigOrigin = stream(pipelineConfigOrigin);
  }

  static fromJSON(usageJson: ProfileUsageJSON) {
    return new ProfileUsage(usageJson.pipeline_name,
                            usageJson.stage_name,
                            usageJson.job_name,
                            usageJson.template_name,
                            usageJson.pipeline_config_origin);
  }

  isPipelineOriginLocal() {
    return this.pipelineConfigOrigin() === "gocd";
  }
}

export interface ElasticProfileJSON {
  id: string;
  plugin_id: string;
  cluster_profile_id?: string;
  properties: PropertyJSON[];
  errors?: { [key: string]: string[] };
}

export interface ClusterProfileJSON {
  id: string;
  plugin_id: string;
  properties: PropertyJSON[];
  errors?: { [key: string]: string[] };
}

// tslint:disable-next-line
export interface ElasticAgentProfile extends ValidatableMixin {
}

export class ElasticAgentProfile implements ValidatableMixin {
  id: Stream<string>;
  pluginId: Stream<string>;
  clusterProfileId: Stream<string>;
  properties: Stream<Configurations>;

  constructor(id?: string, pluginId?: string, clusterProfileId?: string, properties?: Configurations) {
    this.id               = stream(id);
    this.pluginId         = stream(pluginId);
    this.clusterProfileId = stream(clusterProfileId);
    this.properties       = stream(properties);

    ValidatableMixin.call(this);
    this.validatePresenceOf("clusterProfileId");
    this.validatePresenceOf("pluginId");
    this.validatePresenceOf("id");
    this.validateFormatOf("id",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("id", 255, {message: "The maximum allowed length is 255 characters."});
  }

  static fromJSON(profileJson: ElasticProfileJSON): ElasticAgentProfile {
    const profile = new ElasticAgentProfile(profileJson.id,
                                            profileJson.plugin_id,
                                            profileJson.cluster_profile_id,
                                            Configurations.fromJSON(profileJson.properties));

    profile.errors(new Errors(profileJson.errors));
    return profile;
  }

  toJSON(): object {
    return {
      id: this.id,
      plugin_id: this.pluginId,
      cluster_profile_id: this.clusterProfileId,
      properties: this.properties
    };
  }
}

applyMixins(ElasticAgentProfile, ValidatableMixin);

//tslint:disable-next-line
export interface ClusterProfile extends ValidatableMixin {
}

export class ClusterProfile implements ValidatableMixin {
  id: Stream<string>;
  pluginId: Stream<string>;
  properties: Stream<Configurations>;

  constructor(id?: string, pluginId?: string, properties?: Configurations) {
    this.id         = stream(id);
    this.pluginId   = stream(pluginId);
    this.properties = stream(properties);

    ValidatableMixin.call(this);
    this.validatePresenceOf("pluginId");
    this.validatePresenceOf("id");
    this.validateFormatOf("id",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("id", 255, {message: "The maximum allowed length is 255 characters."});
  }

  static fromJSON(profileJson: ClusterProfileJSON): ClusterProfile {
    const profile = new ClusterProfile(profileJson.id,
                                       profileJson.plugin_id,
                                       Configurations.fromJSON(profileJson.properties));

    profile.errors(new Errors(profileJson.errors));
    return profile;
  }

  toJSON(): object {
    return {
      id: this.id(),
      plugin_id: this.pluginId(),
      properties: this.properties()
    };
  }
}

applyMixins(ClusterProfile, ValidatableMixin);
