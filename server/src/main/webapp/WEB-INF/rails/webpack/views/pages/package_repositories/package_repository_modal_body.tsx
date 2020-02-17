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
import {PackageRepository} from "models/package_repositories/package_repositories";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Form, FormHeader} from "views/components/forms/form";

interface Attrs {
  pluginInfos: PluginInfos;
  packageRepo: PackageRepository;
  disableId: boolean;
  pluginIdProxy: (newPluginId?: string) => any;
}

export class PackageRepositoryModalBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const pluginList = _.map(vnode.attrs.pluginInfos, (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });
    return <div><FormHeader>
      <Form>
        <TextField label="Name"
                   readonly={vnode.attrs.disableId}
                   property={vnode.attrs.packageRepo.name}
                   placeholder={"Enter the package repository name"}
                   errorText={vnode.attrs.packageRepo.errors().errorsForDisplay("name")}
                   required={true}/>

        <SelectField label="Plugin"
                     property={vnode.attrs.pluginIdProxy.bind(this)}
                     required={true}
                     errorText={vnode.attrs.packageRepo.errors().errorsForDisplay("pluginId")}>
          <SelectFieldOptions selected={vnode.attrs.packageRepo.pluginMetadata().id()}
                              items={pluginList}/>
        </SelectField>
      </Form>
    </FormHeader>

      <div>
        The plugin view will come here.
      </div>
    </div>;
  }
}
