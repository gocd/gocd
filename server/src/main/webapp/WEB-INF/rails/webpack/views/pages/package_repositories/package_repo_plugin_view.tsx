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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Configurations} from "models/shared/configuration";
import {PluginSettings} from "models/shared/plugin_infos_new/extensions";
import {PackageSettingsConfiguration} from "models/shared/plugin_infos_new/serialization";
import {EncryptedValue} from "views/components/forms/encrypted_value";
import {PasswordField, TextField} from "views/components/forms/input_fields";

interface Attrs {
  configurations: Configurations;
  pluginSettings: PluginSettings;
}

interface State {
  values: Map<string, Stream<any>>;
}

export class PluginView extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    vnode.state.values = new Map<string, Stream<any>>();
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    this.initializeConfigurations(vnode);
    const elements = (vnode.attrs.pluginSettings.configurations() as PackageSettingsConfiguration[]).map((config) => {
      const found = vnode.attrs.configurations.findConfiguration(config.key)!;

      if (config.metadata.secure) {
        return <PasswordField required={config.metadata.required}
                              dataTestId={`input-for-${config.metadata.display_name}`}
                              errorText={(found.errors || []).join("; ")}
                              label={config.metadata.display_name}
                              onchange={this.getMapper(vnode, config)}
                              property={vnode.state.values.get(config.key)!}/>;
      }

      return <TextField required={config.metadata.required}
                        dataTestId={`input-for-${config.metadata.display_name}`}
                        errorText={(found.errors || []).join("; ")}
                        onchange={this.getMapper(vnode, config)}
                        label={config.metadata.display_name}
                        property={vnode.state.values.get(config.key)!}/>;
    });

    return <div data-test-id="plugin-view">
      {elements}
    </div>;
  }

  private initializeConfigurations(vnode: m.Vnode<Attrs, State>) {
    (vnode.attrs.pluginSettings.configurations() as PackageSettingsConfiguration[]).forEach((config) => {
      let found = vnode.attrs.configurations.findConfiguration(config.key);
      if (!found) {
        vnode.attrs.configurations.setConfiguration(config.key, "");
        found = vnode.attrs.configurations.findConfiguration(config.key)!;
      }
      if (!vnode.state.values.has(config.key)) {
        const value = config.metadata.secure
          ? Stream(new EncryptedValue(found.isEncrypted() ? {cipherText: found.getValue()} : {clearText: found.getValue()}))
          : Stream(found.getValue());

        vnode.state.values.set(config.key, value);
      }
    });
  }

  private getMapper(vnode: m.Vnode<Attrs>, config: any) {
    const mapper = (e: any) => {
      vnode.attrs.configurations.setConfiguration(config.key, e.target.value);
    };
    return mapper;
  }
}
