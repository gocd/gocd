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

import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {
  ConfigRepo,
  GitMaterialAttributes,
  HgMaterialAttributes,
  humanizedMaterialAttributeName, humanizedMaterialNameForMaterialType,
  Material, MaterialAttributes,
  P4MaterialAttributes,
  SvnMaterialAttributes, TfsMaterialAttributes
} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {
  CheckboxField,
  Option,
  PasswordField,
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
  view(vnode: m.Vnode<EditableMaterial>) {
    const materialAttributes = vnode.attrs.repo.material().attributes() as MaterialAttributes;

    const pluginList = _.map(vnode.attrs.pluginInfos(), (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    return (
      <div>
        <SelectField label="Plugin ID"
                     property={vnode.attrs.repo.pluginId}
                     required={true}>
          <SelectFieldOptions selected={vnode.attrs.repo.pluginId()}
                              items={pluginList}/>
        </SelectField>

        <SelectField label={"Material type"}
                     property={vnode.attrs.repo.material().typeProxy.bind(vnode.attrs.repo.material())}
                     required={true}>
          <SelectFieldOptions selected={vnode.attrs.repo.material().type()}
                              items={this.materialSelectOptions()}/>
        </SelectField>

        <TextField label="Config repository ID"
                   disabled={!vnode.attrs.isNew}
                   property={vnode.attrs.repo.id}
                   required={true}/>

        <CheckboxField label={humanizedMaterialAttributeName("auto_update")}
                       property={vnode.attrs.repo.material().attributes().autoUpdate}/>

        <TextField label={humanizedMaterialAttributeName("name")}
                   property={materialAttributes.name}
                   required={true}/>
        {vnode.children}
      </div>
    );
  }

  private materialSelectOptions(): Option[] {
    return _.reduce(MATERIAL_TO_COMPONENT_MAP, (memo, ignore, materialType) => {
      memo.push({id: materialType, text: humanizedMaterialNameForMaterialType(materialType)});
      return memo;
    }, [] as Option[]);
  }
}

const NewMaterialComponent = {
  view(vnode: m.Vnode<EditableMaterial>) {
    return (
      <MaterialEditWidget isNew={true} {...vnode.attrs}>
      </MaterialEditWidget>
    );
  }
} as MithrilViewComponent<EditableMaterial>;

const MATERIAL_TO_COMPONENT_MAP: { [key: string]: MithrilViewComponent<EditableMaterial> } = {
  git: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material().attributes() as GitMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     property={materialAttributes.url}
                     required={true}/>

          <TextField label={humanizedMaterialAttributeName("branch")}
                     property={materialAttributes.branch}/>

        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  svn: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material().attributes() as SvnMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>
          <TextField label={humanizedMaterialAttributeName("url")}
                     property={materialAttributes.url}
                     required={true}/>

          <CheckboxField label={humanizedMaterialAttributeName("check_externals")}
                         property={materialAttributes.checkExternals}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     property={materialAttributes.username}/>

          <PasswordField label={humanizedMaterialAttributeName("password")}
                         property={materialAttributes.password}/>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  hg: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material().attributes() as HgMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     property={materialAttributes.url}
                     required={true}/>

        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  p4: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material().attributes() as P4MaterialAttributes;
      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("port")}
                     property={materialAttributes.port}
                     required={true}/>

          <CheckboxField label={humanizedMaterialAttributeName("use_tickets")}
                         property={materialAttributes.useTickets}/>

          <TextAreaField label={humanizedMaterialAttributeName("view")}
                         property={materialAttributes.view}
                         required={true}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     property={materialAttributes.username}/>

          <PasswordField label={humanizedMaterialAttributeName("password")}
                         property={materialAttributes.password}/>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  tfs: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material().attributes() as TfsMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     property={materialAttributes.url}
                     required={true}/>

          <TextField label={humanizedMaterialAttributeName("project_path")}
                     property={materialAttributes.projectPath}
                     required={true}/>

          <TextField label={humanizedMaterialAttributeName("domain")}
                     property={materialAttributes.domain}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     property={materialAttributes.username}
                     required={true}/>

          <PasswordField label={humanizedMaterialAttributeName("password")}
                         property={materialAttributes.password}
                         required={true}/>
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

    if (!this.getRepo().material().type()) {
      materialtocomponentmapElement = NewMaterialComponent;
    } else {
      materialtocomponentmapElement = MATERIAL_TO_COMPONENT_MAP[this.getRepo().material().type()];
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
    this.repo(new ConfigRepo(undefined, pluginInfos()[0].id, new Material("git", new GitMaterialAttributes())));
    this.isNew = true;
  }

  title(): string {
    return `Create new configuration repository`;
  }

  getRepo() {
    return this.repo();
  }

  performSave() {
    ConfigReposCRUD.create(this.repo())
                   .then((result) => result.do(this.onSuccess,
                                               (errorResponse) => this.handleError(result, errorResponse)));
  }

  private onSuccess() {
    this.onSuccessfulSave(<span>The config repository <em>{this.repo().id}</em> was created successfully!</span>);
    this.close();
  }

  private handleError(result: ApiResult<ObjectWithEtag<ConfigRepo>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.repo(ConfigRepo.fromJSON(json.data));
    } else {
      this.onError(errorResponse.message);
    }
  }
}

export class EditConfigRepoModal extends ConfigRepoModal {
  private readonly repoId: string;
  private repoWithEtag: Stream<ObjectWithEtag<ConfigRepo>> = stream();

  constructor(repoId: string,
              onSuccessfulSave: (msg: (m.Children)) => any,
              onError: (msg: (m.Children)) => any,
              pluginInfos: Stream<Array<PluginInfo<any>>>) {
    super(onSuccessfulSave, onError, pluginInfos);
    this.repoId = repoId;

    ConfigReposCRUD
      .get(repoId)
      .then((result) => result.do(
        (successResponse) => this.repoWithEtag(successResponse.body), this.onRepoGetFailure));
  }

  title(): string {
    return `Edit configuration repository ${this.repoId}`;
  }

  performSave(): void {
    ConfigReposCRUD.update(this.repoWithEtag())
                   .then((apiResult) => apiResult.do(this.onSuccess,
                                                     (errorResponse) => this.handleError(apiResult, errorResponse)));
  }

  protected getRepo(): ConfigRepo {
    return this.repoWithEtag() && this.repoWithEtag().object;
  }

  private onRepoGetFailure(errorResponse: ErrorResponse) {
    this.error = errorResponse.message;
  }

  private onSuccess() {
    this.onSuccessfulSave(<span>The config repository <em>{this.getRepo().id}</em> was updated successfully!</span>);
    this.close();
  }

  private handleError(result: ApiResult<ObjectWithEtag<ConfigRepo>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      const etag = this.repoWithEtag().etag;
      this.repoWithEtag({etag, object: ConfigRepo.fromJSON(json.data)});
    } else {
      this.onError(errorResponse.message);
    }
  }
}
