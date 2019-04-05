/*
* Copyright 2019 ThoughtWorks, Inc.
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
import {Configuration, Configurations, PropertyJSON} from "models/shared/configuration";

interface EmbeddedJSON {
  cluster_profiles: ClusterProfileJSON[];
}

export interface ClusterProfileJSON {
  id: string;
  plugin_id: string;
  properties: PropertyJSON[];
}

interface ClusterProfilesJSON {
  _embedded: EmbeddedJSON;
}

export class ClusterProfile {
  id: Stream<string>;
  pluginId: Stream<string>;
  properties: Stream<Configurations>;

  constructor(json: ClusterProfileJSON) {
    this.id         = stream(json.id);
    this.pluginId   = stream(json.plugin_id);
    this.properties = stream(new Configurations(json.properties.map((property) => Configuration.fromJSON(property))));
  }

  static fromJSON(json: ClusterProfileJSON) {
    return new ClusterProfile(json);
  }
}

export class ClusterProfiles extends Array<ClusterProfile> {
  constructor(...clusterProfiles: ClusterProfile[]) {
    super(...clusterProfiles);
    Object.setPrototypeOf(this, Object.create(ClusterProfiles.prototype));
  }

  static fromJSON(data: ClusterProfilesJSON) {
    const clusterProfiles = data._embedded.cluster_profiles.map((clusterProfile) => ClusterProfile.fromJSON(
      clusterProfile));

    return new ClusterProfiles(...clusterProfiles);
  }

  groupByPlugin() {
    return _.groupBy(this, (profile) => {
      return profile.pluginId;
    });
  }
}
