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

import {HttpResponseWithEtag} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {
  ConfigRepo,
  GitMaterialAttributes, HgMaterialAttributes,
  humanizedMaterialAttributeName,
  MaterialAttributes, P4MaterialAttributes, SvnMaterialAttributes
} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {
  CheckboxField,
  SelectField,
  SelectFieldOptions,
  TextAreaField,
  TextField
} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {RequiresPluginInfos, SaveOperation} from "views/pages/config_repos/config_repos_widget";

type EditableMaterial = SaveOperation & { repo: ConfigRepo } & { isNew: boolean } & RequiresPluginInfos;

class MaterialEditWidget extends MithrilViewComponent<EditableMaterial> {
  view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
    const materialAttributes = vnode.attrs.repo.material.attributes as MaterialAttributes;

    const pluginList = _.map(vnode.attrs.pluginInfos(), (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    return (
      <div>
        <SelectField label="Plugin ID"
                     oninput={(value: string) => vnode.attrs.repo.plugin_id = value}
                     value={vnode.attrs.repo.plugin_id}>
          <SelectFieldOptions selected={vnode.attrs.repo.plugin_id}
                              items={pluginList}/>
        </SelectField>

        <SelectField label={"Material type"}
                     oninput={(value: string) => {
                       vnode.attrs.repo.material.type = value;
                     }}
                     value={vnode.attrs.repo.material.type}>
          <SelectFieldOptions selected={vnode.attrs.repo.plugin_id}
                              items={Object.keys(MATERIAL_TO_COMPONENT_MAP)}/>
        </SelectField>

        <TextField label="Config repository ID"
                   disabled={!vnode.attrs.isNew}
                   oninput={(value: string) => vnode.attrs.repo.id = value}
                   value={vnode.attrs.repo.id}/>

        <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                       oninput={(value: boolean) => materialAttributes.auto_update = value}
                       value={materialAttributes.auto_update}/>

        <TextField label={humanizedMaterialAttributeName("name")}
                   oninput={(value: string) => materialAttributes.name = value}
                   value={materialAttributes.name}/>
        {vnode.children}
      </div>
    );
  }
}

const NewMaterialComponent = {
  view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
    return (
      <MaterialEditWidget isNew={true} {...vnode.attrs}>
      </MaterialEditWidget>
    );
  }
} as MithrilViewComponent<EditableMaterial>;

const MATERIAL_TO_COMPONENT_MAP: { [key: string]: MithrilViewComponent<EditableMaterial> } = {
  git: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.repo.material.attributes as GitMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     oninput={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

          <TextField label={humanizedMaterialAttributeName("branch")}
                     oninput={(value: string) => materialAttributes.branch = value}
                     value={materialAttributes.branch}/>

        </MaterialEditWidget>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  svn: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.repo.material.attributes as SvnMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     oninput={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     oninput={(value: string) => materialAttributes.username = value}
                     value={materialAttributes.username}/>

          <CheckboxField label={humanizedMaterialAttributeName("check_externals")}
                         oninput={(value: boolean) => materialAttributes.check_externals = value}
                         value={materialAttributes.check_externals}/>
        </MaterialEditWidget>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  hg: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.repo.material.attributes as HgMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     oninput={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

        </MaterialEditWidget>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  p4: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.repo.material.attributes as P4MaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("port")}
                     oninput={(value: string) => materialAttributes.port = value}
                     value={materialAttributes.port}/>

          <CheckboxField label={humanizedMaterialAttributeName("use_tickets")}
                         oninput={(value: boolean) => materialAttributes.use_tickets = value}
                         value={materialAttributes.use_tickets}/>

          <TextAreaField label={humanizedMaterialAttributeName("view")}
                         oninput={(value: string) => materialAttributes.view = value}
                         value={materialAttributes.view}/>
        </MaterialEditWidget>
      );
    }

  } as MithrilViewComponent<EditableMaterial>
};

abstract class ConfigRepoModal extends Modal {
  protected error: string | undefined;
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly onError: (msg: m.Children) => any;
  protected isNew: boolean = false;
  protected pluginInfos: Stream<Array<PluginInfo<any>>>;

  protected constructor(onSuccessfulSave: (msg: m.Children) => any,
                        onError: (msg: m.Children) => any,
                        pluginInfos: Stream<Array<PluginInfo<any>>>) {
    super(Size.large);
    this.onSuccessfulSave = onSuccessfulSave;
    this.onError          = onError;
    this.pluginInfos      = pluginInfos;
  }

  body(): JSX.Element {
    if (this.error) {
      return (<FlashMessage type={MessageType.alert} message={this.error}/>);
    }

    if (!this.getRepo()) {
      return <Spinner/>;
    }
    let materialtocomponentmapElement;

    if (!this.getRepo().material.type) {
      materialtocomponentmapElement = NewMaterialComponent;
    } else {
      materialtocomponentmapElement = MATERIAL_TO_COMPONENT_MAP[this.getRepo().material.type];
    }

    return m(materialtocomponentmapElement,
      {
        onSuccessfulSave: this.onSuccessfulSave,
        onError: this.onError,
        repo: this.getRepo(),
        isNew: this.isNew,
        pluginInfos: this.pluginInfos
      });

  }

  buttons(): JSX.Element[] {
    return [<Buttons.Primary data-test-id="button-ok" onclick={this.performSave.bind(this)}>OK</Buttons.Primary>];
  }

  abstract performSave(): void;

  protected abstract getRepo(): ConfigRepo;
}

export class NewConfigRepoModal extends ConfigRepoModal {
  private readonly repo: Stream<ConfigRepo> = stream();

  constructor(onSuccessfulSave: (msg: (m.Children)) => any,
              onError: (msg: (m.Children)) => any,
              pluginInfos: Stream<Array<PluginInfo<any>>>) {
    super(onSuccessfulSave, onError, pluginInfos);
    this.repo({material: {type: "git", attributes: {}}, plugin_id: pluginInfos()[0].id} as ConfigRepo);
    this.isNew = true;
  }

  title(): string {
    return `Create new configuration repository`;
  }

  getRepo() {
    return this.repo();
  }

  performSave() {
    ConfigReposCRUD.create(this.repo()).then(this.close.bind(this)).then(() => {
      this.onSuccessfulSave(<span>The config repository <em>{this.repo().id}</em> was created successfully!</span>);
    });
  }
}

export class EditConfigRepoModal extends ConfigRepoModal {
  private readonly repoId: string;
  private repoWithEtag: Stream<HttpResponseWithEtag<ConfigRepo>> = stream();

  constructor(repoId: string,
              onSuccessfulSave: (msg: (m.Children)) => any,
              onError: (msg: (m.Children)) => any,
              pluginInfos: Stream<Array<PluginInfo<any>>>) {
    super(onSuccessfulSave, onError, pluginInfos);
    this.repoId = repoId;

    ConfigReposCRUD
      .get(repoId)
      .then(this.repoWithEtag)
      .catch(this.onRepoGetFailure());
  }

  title(): string {
    return `Edit configuration repository ${this.repoId}`;
  }

  performSave(): void {
    ConfigReposCRUD.update(this.repoWithEtag()).then(this.close.bind(this)).then(() => {
      this.onSuccessfulSave(<span>The config repository <em>{this.getRepo().id}</em> was updated successfully!</span>);
    }).catch(() => {
      this.error = `There was an error saving the config repository!`;
    });
  }

  protected getRepo(): ConfigRepo {
    return this.repoWithEtag() && this.repoWithEtag().object;
  }

  private onRepoGetFailure() {
    return (error: any) => {
      if (error instanceof Error) {
        this.error = error.message;
      } else {
        this.error = "There was an unknown error fetching a copy of the config repository. Please try again in some time";
      }
    };
  }
}
