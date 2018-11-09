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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ConfigRepo, ConfigRepos, humanizedMaterialAttributeName} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {MessageType} from "views/components/flash_message";
import {Delete, QuestionMark, Settings} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Spinner} from "views/components/spinner";

export interface SaveOperation {
  onSuccessfulSave: (msg: m.Children) => any;
  onError: (msg: m.Children) => any;
}

interface Operations<T> {
  onEdit: (obj: T, e: MouseEvent) => void;
  onDelete: (obj: T, e: MouseEvent) => void;
}

export interface RequiresPluginInfos {
  pluginInfos: Stream<Array<PluginInfo<any>>>;
}

interface Attrs<T> extends Operations<T>, RequiresPluginInfos {
  objects: Stream<T | null>;
}

export interface State extends Operations<ConfigRepo>, SaveOperation, RequiresPluginInfos {
  configRepos: Stream<ConfigRepos | null>;

  onAdd: (e: MouseEvent) => void;
  message: m.Children;
  messageType: MessageType;
  clearMessage: () => void;
}

interface ShowObjectAttrs<T> extends Operations<T> {
  obj: T;
}

class HeaderWidget extends MithrilViewComponent<ConfigRepo> {
  view(vnode: m.Vnode<ConfigRepo, this>): m.Children | void | null {
    return [
      (<QuestionMark/>),
      (<div data-test-id="repo-id">{vnode.attrs.id}</div>),
      (<div>{vnode.attrs.plugin_id}</div>),
      (<div>{vnode.attrs.material.type}</div>),
    ];
  }
}

class ConfigRepoWidget extends MithrilViewComponent<ShowObjectAttrs<ConfigRepo>> {
  view(vnode: m.Vnode<ShowObjectAttrs<ConfigRepo>>): m.Children | void | null {

    const filteredAttributes = _.reduce(vnode.attrs.obj.material.attributes, (accumulator: any,
                                                                              value: any,
                                                                              key: string) => {
      let renderedValue = value;

      const renderedKey = humanizedMaterialAttributeName(key);

      if (_.isString(value) && value.startsWith("AES:")) {
        renderedValue = "******";
      }

      accumulator[renderedKey] = renderedValue;
      return accumulator;
    }, {});

    const settingsButton = (
      <Settings data-test-id="edit-config-repo" onclick={vnode.attrs.onEdit.bind(vnode.attrs)}/>
    );

    const deleteButton = (
      <Delete data-test-id="delete-config-repo" onclick={vnode.attrs.onDelete.bind(vnode.attrs)}/>
    );

    const actionButtons = [
      settingsButton, deleteButton
    ];

    return (
      <CollapsiblePanel header={<HeaderWidget {...vnode.attrs.obj}/>}
                        actions={actionButtons}>
        <KeyValuePair data={filteredAttributes}/>
      </CollapsiblePanel>
    );
  }
}

export class ConfigReposWidget extends MithrilViewComponent<Attrs<ConfigRepos>> {
  view(vnode: m.Vnode<Attrs<ConfigRepos>>): m.Children | void | null {
    if (!vnode.attrs.objects()) {
      return <Spinner/>;
    }

    return (
      <div>
        {(vnode.attrs.objects() as ConfigRepos)._embedded.config_repos.map((configRepo) => {
          return (
            <ConfigRepoWidget key={configRepo.id}
                              obj={configRepo}
                              onEdit={vnode.attrs.onEdit.bind(vnode.state, configRepo)}
                              onDelete={vnode.attrs.onDelete.bind(vnode.state, configRepo)}
            />
          );
        })}
      </div>
    );
  }
}
