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

import m from "mithril";
import Stream from "mithril/stream";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {SecretConfigModal} from "views/pages/secret_configs/modals";

export class TestSecretConfigModal extends SecretConfigModal {

  constructor(entities: Stream<SecretConfigs>,
              entity: SecretConfig,
              pluginInfos: PluginInfos,
              onSuccessfulSave: (msg: m.Children) => any,
              resourceAutoCompleteHelper: Map<string, string[]> = new Map(),
              disableId: boolean                                = false) {
    super(entities, entity, pluginInfos, onSuccessfulSave, resourceAutoCompleteHelper, disableId);
    this.isStale(false);
  }

  title(): string {
    return "Modal title for Secret Configuration";
  }

  setErrorMessageForTest(errorMsg: string): void {
    this.errorMessage(errorMsg);
  }

  protected operationPromise(): Promise<any> {
    return Promise.resolve();
  }

  protected successMessage(): m.Children {
    return "Success Messgae";
  }
}
