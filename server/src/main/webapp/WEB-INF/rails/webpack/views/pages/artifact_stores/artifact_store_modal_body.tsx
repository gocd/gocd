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

import { MithrilViewComponent } from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import { ArtifactStore } from "models/artifact_stores/artifact_stores";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import { ArtifactExtension } from "models/shared/plugin_infos_new/extensions";
import { PluginInfo, PluginInfos } from "models/shared/plugin_infos_new/plugin_info";
import { Form, FormHeader } from "views/components/forms/form";
import { SelectField, SelectFieldOptions, TextField } from "views/components/forms/input_fields";

const AngularPluginNew = require('views/shared/angular_plugin_new').AngularPluginNew;

interface Attrs {
  pluginInfos: PluginInfos;
  artifactStore: ArtifactStore;
  disableId: boolean;
  pluginIdProxy: (newPluginId?: string) => any;
}

export class ArtifactStoreModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    const pluginList = _.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return { id: pluginInfo.id, text: pluginInfo.about.name };
    });

    const pluginInfo      = this.findPluginInfo(vnode.attrs.pluginInfos, vnode.attrs.artifactStore.pluginId());
    const pluginSettings = pluginInfo.extensionOfType<ArtifactExtension>(ExtensionTypeString.ARTIFACT)!.storeConfigSettings;

    return (
      <div>
        <FormHeader>
          <Form>
            <TextField label="Id"
              readonly={vnode.attrs.disableId}
              property={vnode.attrs.artifactStore.id}
              errorText={vnode.attrs.artifactStore.errors().errorsForDisplay("id")}
              required={true} />

            <SelectField label="Plugin Id"
              property={vnode.attrs.pluginIdProxy}
              required={true}
              errorText={vnode.attrs.artifactStore.errors().errorsForDisplay("pluginId")}>
              <SelectFieldOptions selected={vnode.attrs.artifactStore.pluginId()}
                items={pluginList} />
            </SelectField>
          </Form>
        </FormHeader>

        <div>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={Stream(pluginSettings)}
              configuration={vnode.attrs.artifactStore.properties()}
              key={pluginInfo.id} />
          </div>
        </div>
      </div>
    );
  }

  private findPluginInfo(pluginInfos: PluginInfos, pluginId: string): PluginInfo {
    return pluginInfos.find((pluginInfo) => pluginInfo.id === pluginId) as PluginInfo;
  }
}
