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
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {
  ConfigRepo,
  ConfigRepos, GitMaterialAttributes, HgMaterialAttributes,
  humanizedMaterialAttributeName,
  Material, P4MaterialAttributes, SvnMaterialAttributes
} from "models/config_repos/types";
import * as Buttons from "../components/buttons";
import {CollapsiblePanel} from "../components/collapsible_panel";
import {AlertFlashMessage} from "../components/flash_message";
import {CheckboxField, TextAreaField, TextField} from "../components/forms/input_fields";
import {Delete, QuestionMark, Settings} from "../components/icons";
import {KeyValuePair} from "../components/key_value_pair";
import {Modal} from "../components/modal";
import {Spinner} from "../components/spinner";

const HeaderPanel = require("views/components/header_panel");

const configRepos: Stream<ConfigRepos> = stream();

interface Attrs {
  configRepos: Stream<ConfigRepos>;
}

interface SaveOperation {
  onSuccessfulSave?: (msg: string) => any;
}

interface Operations extends SaveOperation {
  edit: (repo: ConfigRepo, e: MouseEvent) => void;
  delete: (repo: ConfigRepo, e: MouseEvent) => void;
}

interface Attrs2 extends Operations {
  configRepo: ConfigRepo;
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

class ConfigRepoWidget extends MithrilComponent<Attrs2> {
  view(vnode: m.Vnode<Attrs2, this>): m.Children | void | null {

    const filteredAttributes = _.reduce(vnode.attrs.configRepo.material.attributes, (accumulator: any, value: any, key: string) => {
      let renderedValue = value;

      const renderedKey = humanizedMaterialAttributeName(key);

      if (_.isString(value) && value.startsWith("AES:")) {
        renderedValue = "******";
      }

      accumulator[renderedKey] = renderedValue;
      return accumulator;
    }, {});

    const settingsButton = (
      <Settings data-test-id="edit-config-repo" onclick={vnode.attrs.edit.bind(vnode.attrs)}/>
    );

    const deleteButton = (
      <Delete data-test-id="delete-config-repo" onclick={vnode.attrs.delete.bind(vnode.attrs)}/>
    );

    const actionButtons = [
      settingsButton, deleteButton
    ];

    return (
      <CollapsiblePanel header={<HeaderWidget {...vnode.attrs.configRepo}/>}
                        actions={actionButtons}
      >
        <KeyValuePair data={filteredAttributes}/>
      </CollapsiblePanel>
    );
  }
}

type EditableMaterial = SaveOperation & Material;

const MATERIAL_TO_COMPONENT_MAP: { [key: string]: MithrilViewComponent<EditableMaterial> } = {
  git: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.attributes as GitMaterialAttributes;

      return (
        <div>
          <TextField label={humanizedMaterialAttributeName("name")}
                     onchange={(value: string) => materialAttributes.name = value}
                     value={materialAttributes.name}/>

          <TextField label={humanizedMaterialAttributeName("url")}
                     onchange={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

          <TextField label={humanizedMaterialAttributeName("branch")}
                     onchange={(value: string) => materialAttributes.branch = value}
                     value={materialAttributes.branch}/>

          <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                         onchange={(value: boolean) => materialAttributes.auto_update = value}
                         value={materialAttributes.auto_update}/>
        </div>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  svn: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.attributes as SvnMaterialAttributes;

      return (
        <div>
          <TextField label={humanizedMaterialAttributeName("name")}
                     onchange={(value: string) => materialAttributes.name = value}
                     value={materialAttributes.name}/>

          <TextField label={humanizedMaterialAttributeName("url")}
                     onchange={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     onchange={(value: string) => materialAttributes.username = value}
                     value={materialAttributes.username}/>

          <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                         onchange={(value: boolean) => materialAttributes.auto_update = value}
                         value={materialAttributes.auto_update}/>

          <CheckboxField label={humanizedMaterialAttributeName("check_externals")}
                         onchange={(value: boolean) => materialAttributes.check_externals = value}
                         value={materialAttributes.check_externals}/>
        </div>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  hg: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.attributes as HgMaterialAttributes;

      return (
        <div>
          <TextField label={humanizedMaterialAttributeName("name")}
                     onchange={(value: string) => materialAttributes.name = value}
                     value={materialAttributes.name}/>

          <TextField label={humanizedMaterialAttributeName("url")}
                     onchange={(value: string) => materialAttributes.url = value}
                     value={materialAttributes.url}/>

          <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                         onchange={(value: boolean) => materialAttributes.auto_update = value}
                         value={materialAttributes.auto_update}/>
        </div>
      );
    }

  } as MithrilViewComponent<EditableMaterial>,
  p4: {
    view(vnode: m.Vnode<EditableMaterial>): m.Children | void | null {
      const materialAttributes = vnode.attrs.attributes as P4MaterialAttributes;

      return (
        <div>
          <TextField label={humanizedMaterialAttributeName("name")}
                     onchange={(value: string) => materialAttributes.name = value}
                     value={materialAttributes.name}/>

          <TextField label={humanizedMaterialAttributeName("port")}
                     onchange={(value: string) => materialAttributes.port = value}
                     value={materialAttributes.port}/>

          <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                         onchange={(value: boolean) => materialAttributes.auto_update = value}
                         value={materialAttributes.auto_update}/>

          <CheckboxField label={humanizedMaterialAttributeName("use_tickets")}
                         onchange={(value: boolean) => materialAttributes.use_tickets = value}
                         value={materialAttributes.use_tickets}/>

          <TextAreaField label={humanizedMaterialAttributeName("view")}
                     onchange={(value: string) => materialAttributes.view = value}
                     value={materialAttributes.view}/>
        </div>
      );
    }

  } as MithrilViewComponent<EditableMaterial>
};

class ConfigReposModal extends Modal {
  private readonly repoId: string;
  private onSuccessfulSave: ((msg: string) => any) | undefined;
  private repoWithEtag: Stream<HttpResponseWithEtag<ConfigRepo>> = stream();
  private error: string | undefined;

  constructor(repoId: string, onSuccessfulSave?: (msg: string) => any) {
    super();
    this.repoId           = repoId;
    this.onSuccessfulSave = onSuccessfulSave;

    ConfigReposCRUD
      .get(repoId)
      .then(this.repoWithEtag)
      .catch(this.onRepoGetFailure());
  }

  body(): JSX.Element {
    if (this.error) {
      return (<AlertFlashMessage message={this.error}/>);
    }

    if (!this.repoWithEtag()) {
      return <Spinner/>;
    }

    return m(MATERIAL_TO_COMPONENT_MAP[this.repoWithEtag().object.material.type], {onSuccessfulSave: this.onSuccessfulSave, ...this.repoWithEtag().object.material});
  }

  title(): string {
    return `Edit configuration repository ${this.repoId}`;
  }

  buttons(): JSX.Element[] {
    return [<Buttons.Primary onclick={this.performSave.bind(this)}>OK</Buttons.Primary>];
  }

  private performSave() {
    ConfigReposCRUD.update(this.repoWithEtag()).then(this.close.bind(this));
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

class ConfigReposWidget extends MithrilComponent<Attrs, Operations> {
  oninit(vnode: m.Vnode<Attrs, Operations>) {
    vnode.state.onSuccessfulSave = (msg: string) => {
      // do nothing
    };
    vnode.state.edit             = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
      new ConfigReposModal(repo.id, vnode.state.onSuccessfulSave).render();
    };
    vnode.state.delete           = (repo: ConfigRepo, e: MouseEvent) => {
      e.stopPropagation();
    };

  }

  view(vnode: m.Vnode<Attrs, Operations>): m.Children | void | null {
    if (!vnode.attrs.configRepos()) {
      return <Spinner/>;
    }

    return (
      <div>
        {/*<SuccessFlashMessage message={vnode.state.successMessage}/>*/}
        {vnode.attrs.configRepos()._embedded.config_repos.map((configRepo) => {
          return (
            <ConfigRepoWidget key={configRepo.id}
                              configRepo={configRepo}
                              edit={vnode.state.edit.bind(vnode.state, configRepo)}
                              delete={vnode.state.delete.bind(vnode.state, configRepo)}
            />
          );
        })}
      </div>
    );
  }
}

export class ConfigReposPage extends MithrilComponent {
  oninit() {
    ConfigReposCRUD.all().then(configRepos);
  }

  view() {
    return <main class="main-container">
      <HeaderPanel title="Config repositories"/>
      <ConfigReposWidget configRepos={configRepos}/>
    </main>;
  }
}
