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

import {ClusterProfile} from "models/elastic_profiles/types";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {BaseClusterProfileModal, ModalType} from "views/pages/elastic_agent_configurations/cluster_profiles_modals";

export class TestClusterProfile extends BaseClusterProfileModal {
  constructor(pluginInfos: PluginInfos, type: ModalType, clusterProfile?: ClusterProfile) {
    super(pluginInfos, type, clusterProfile);
  }

  modalTitle(): string {
    return "Modal title";
  }

  performSave(): void {
    return;
  }

  setErrorMessageForTest(message: string): void {
    this.onError(message);
  }
}
