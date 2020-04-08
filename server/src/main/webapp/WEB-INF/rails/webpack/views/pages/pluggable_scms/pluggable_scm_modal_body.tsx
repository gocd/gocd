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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Scm} from "models/materials/pluggable_scm";
import {ScmExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, FlashMessageModel} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";

const AngularPluginNew = require("views/shared/angular_plugin_new").AngularPluginNew;

interface Attrs {
  pluginInfos: PluginInfos;
  scm: Scm;
  disableId: boolean;
  disablePluginId: boolean;
  pluginIdProxy: (newPluginId?: string) => any;
  message?: FlashMessageModel;
}

export class PluggableScmModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const pluginList = _.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    const selectedPluginInfo = _.find(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return pluginInfo.id === vnode.attrs.pluginIdProxy();
    })!;

    const mayBeMsg = vnode.attrs.message
      ? <FlashMessage type={vnode.attrs.message.type} message={vnode.attrs.message.message}/>
      : undefined;
    const settings = (selectedPluginInfo.extensionOfType(ExtensionTypeString.SCM) as ScmExtension).scmSettings;
    return <div>
      <FormHeader>
        {mayBeMsg}
        <Form>
          <TextField label="Name"
                     readonly={vnode.attrs.disableId}
                     property={vnode.attrs.scm.name}
                     placeholder={"Enter the pluggable scm name"}
                     errorText={vnode.attrs.scm.errors().errorsForDisplay("name") || vnode.attrs.scm.errors().errorsForDisplay("scmId")}
                     required={true}/>

          <SelectField label="Plugin"
                       property={vnode.attrs.pluginIdProxy.bind(this)}
                       required={true} readonly={vnode.attrs.disablePluginId}
                       errorText={vnode.attrs.scm.errors().errorsForDisplay("pluginId")}>
            <SelectFieldOptions selected={vnode.attrs.scm.pluginMetadata().id()}
                                items={pluginList}/>
          </SelectField>
        </Form>
      </FormHeader>

      <div className="row collapse">
        <AngularPluginNew
          pluginInfoSettings={Stream(settings)}
          configuration={vnode.attrs.scm.configuration()}
          key={selectedPluginInfo.id}/>
      </div>
    </div>;
  }
}
