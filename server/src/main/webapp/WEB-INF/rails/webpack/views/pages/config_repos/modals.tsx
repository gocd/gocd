/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import {docsUrl} from "gen/gocd_version";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo, humanizedMaterialAttributeName, humanizedMaterialNameForMaterialType} from "models/config_repos/types";
import {GitMaterialAttributes, HgMaterialAttributes, Material, P4MaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "models/materials/types";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormBody, FormHeader} from "views/components/forms/form";
import {CheckboxField, Option, PasswordField, SelectField, SelectFieldOptions, TextAreaField, TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {TestConnection} from "views/components/materials/test_connection";
import {Modal, Size} from "views/components/modal";
import {ConfigureRulesWidget, RulesType} from "views/components/rules/configure_rules_widget";
import {Spinner} from "views/components/spinner";
import styles from "views/pages/config_repos/index.scss";
import {OperationState, RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";
import materialStyles from "./materials.scss";

type EditableMaterial = SaveOperation
  & { repo: ConfigRepo }
  & { isNew: boolean }
  & RequiresPluginInfos
  & { error?: m.Children }
  & { resourceAutocompleteHelper: Map<string, string[]> };

class MaterialEditWidget extends MithrilViewComponent<EditableMaterial> {
  view(vnode: m.Vnode<EditableMaterial>) {
    const pluginList = _.map(vnode.attrs.pluginInfos(), (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    const errorMessage = vnode.attrs.error ? <div class={styles.errorWrapper}>{vnode.attrs.error}</div> : undefined;
    const infoMsg = <span>Configure rules to allow which environment/pipeline group/pipeline the config repository can refer to. By default, the config repository cannot refer to an entity unless explicitly allowed. <Link
      href={docsUrl("advanced_usage/pipelines_as_code.html")} externalLinkIcon={true}>Learn More</Link></span>;
    return (
      [
        (errorMessage),
        (<FormHeader>
          <Form>
            <TextField label="Config repository name"
                       readonly={!vnode.attrs.isNew}
                       property={vnode.attrs.repo.id}
                       errorText={vnode.attrs.repo.errors().errorsForDisplay("id")}
                       css={styles}
                       required={true}/>
            <SelectField label="Plugin ID"
                         property={vnode.attrs.repo.pluginId}
                         required={true}
                         css={styles}
                         errorText={vnode.attrs.repo.errors().errorsForDisplay("pluginId")}>
              <SelectFieldOptions selected={vnode.attrs.repo.pluginId()}
                                  items={pluginList}/>
            </SelectField>
          </Form>
          <Form>
            <SelectField label={"Material type"}
                         css={styles}
                         property={vnode.attrs.repo.material()!.typeProxy.bind(vnode.attrs.repo.material()!)}
                         required={true}
                         errorText={vnode.attrs.repo.errors().errorsForDisplay("material")}>
              <SelectFieldOptions selected={vnode.attrs.repo.material()!.type()}
                                  items={this.materialSelectOptions()}/>
            </SelectField>
          </Form>
        </FormHeader>),
        (<div>
            <div class={styles.materialConfigWrapper}>
              <FormBody>
                <Form last={true}>
                  {vnode.children}
                </Form>
              </FormBody>
              <TestConnection material={vnode.attrs.repo.material()!}/>
            </div>
            <div class={styles.pluginFilePatternConfigWrapper}>
              <FormBody>
                <Form>
                  {MaterialEditWidget.pluginConfigView(vnode)}
                </Form>
              </FormBody>
            </div>
          </div>
        ),
        <div>
          <ConfigureRulesWidget infoMsg={infoMsg}
                                rules={vnode.attrs.repo.rules}
                                types={[RulesType.PIPELINE, RulesType.PIPELINE_GROUP, RulesType.ENVIRONMENT]}
                                resourceAutocompleteHelper={vnode.attrs.resourceAutocompleteHelper}/>
        </div>
      ]
    );
  }

  private static pluginConfigView(vnode: m.Vnode<EditableMaterial>): m.Children {
    let pluginConfig = null;
    if (ConfigRepo.JSON_PLUGIN_ID === vnode.attrs.repo.pluginId()) {
      pluginConfig = [
        <TextField property={vnode.attrs.repo.__jsonPluginPipelinesPattern}
                   css={styles}
                   label="GoCD pipeline files pattern"/>,
        <TextField property={vnode.attrs.repo.__jsonPluginEnvPattern}
                   label="GoCD environment files pattern"/>
      ];
    } else if (ConfigRepo.YAML_PLUGIN_ID === vnode.attrs.repo.pluginId()) {
      pluginConfig = (<TextField property={vnode.attrs.repo.__yamlPluginPattern} css={styles} label="GoCD YAML files pattern"/>);
    }
    return pluginConfig;
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
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as GitMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>
          <div>
            <TextField label={humanizedMaterialAttributeName("url")}
                       property={materialAttributes.url}
                       required={true}
                       css={styles}
                       errorText={materialAttributes.errors().errorsForDisplay("url")}/>
            <TextField label={humanizedMaterialAttributeName("username")}
                       css={styles}
                       property={materialAttributes.username}/>
          </div>
          <div>
            <TextField label={humanizedMaterialAttributeName("branch")}
                       placeholder="master"
                       css={styles}
                       property={materialAttributes.branch}/>
            <PasswordField label={humanizedMaterialAttributeName("password")}
                           css={styles}
                           property={materialAttributes.password}/>
          </div>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  svn: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as SvnMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>
          <div>
            <TextField label={humanizedMaterialAttributeName("url")}
                       property={materialAttributes.url}
                       required={true}
                       css={styles}
                       errorText={materialAttributes.errors().errorsForDisplay("url")}/>
            <TextField label={humanizedMaterialAttributeName("username")}
                       css={styles}
                       property={materialAttributes.username}/>
          </div>
          <div class={styles.adjustHeight}>
            <CheckboxField label={humanizedMaterialAttributeName("checkExternals")}
                           css={styles}
                           property={materialAttributes.checkExternals}/>

            <PasswordField label={humanizedMaterialAttributeName("password")}
                           css={styles}
                           property={materialAttributes.password}/>
          </div>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  hg: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as HgMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>
          <div>
            <TextField label={humanizedMaterialAttributeName("url")}
                       property={materialAttributes.url}
                       required={true}
                       css={styles}
                       errorText={materialAttributes.errors().errorsForDisplay("url")}/>

            <TextField label={humanizedMaterialAttributeName("username")}
                       css={styles}
                       property={materialAttributes.username}/>
          </div>
          <div>
            <TextField label={humanizedMaterialAttributeName("branch")}
                       placeholder="default"
                       css={styles}
                       property={materialAttributes.branch}/>

            <PasswordField label={humanizedMaterialAttributeName("password")}
                           css={styles}
                           property={materialAttributes.password}/>
          </div>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  p4: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as P4MaterialAttributes;
      return (
        <MaterialEditWidget {...vnode.attrs}>
          <TextField label={humanizedMaterialAttributeName("port")}
                     property={materialAttributes.port}
                     required={true}
                     css={materialStyles}
                     errorText={materialAttributes.errors().errorsForDisplay("port")}/>

          <CheckboxField label={humanizedMaterialAttributeName("useTickets")}
                         property={materialAttributes.useTickets}/>

          <TextAreaField label={humanizedMaterialAttributeName("view")}
                         property={materialAttributes.view}
                         required={true}
                         errorText={materialAttributes.errors().errorsForDisplay("view")}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     css={materialStyles}
                     property={materialAttributes.username}/>

          <PasswordField label={humanizedMaterialAttributeName("password")}
                         property={materialAttributes.password}/>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>,

  tfs: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as TfsMaterialAttributes;

      return (
        <MaterialEditWidget {...vnode.attrs}>

          <TextField label={humanizedMaterialAttributeName("url")}
                     property={materialAttributes.url}
                     css={materialStyles}
                     required={true}
                     errorText={materialAttributes.errors().errorsForDisplay("url")}/>

          <TextField label={humanizedMaterialAttributeName("projectPath")}
                     property={materialAttributes.projectPath}
                     required={true}
                     errorText={materialAttributes.errors().errorsForDisplay("projectPath")}/>

          <TextField label={humanizedMaterialAttributeName("domain")}
                     property={materialAttributes.domain}/>

          <TextField label={humanizedMaterialAttributeName("username")}
                     property={materialAttributes.username}
                     required={true}
                     css={materialStyles}
                     errorText={materialAttributes.errors().errorsForDisplay("username")}/>

          <PasswordField label={humanizedMaterialAttributeName("password")}
                         property={materialAttributes.password}
                         required={true}
                         errorText={materialAttributes.errors().errorsForDisplay("password")}/>
        </MaterialEditWidget>
      );
    }
  } as MithrilViewComponent<EditableMaterial>
};

export abstract class ConfigRepoModal extends Modal {
  protected error: string | undefined;
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly onError: (msg: m.Children) => any;
  protected isNew: boolean = false;
  protected pluginInfos: Stream<PluginInfos>;
  private ajaxOperationMonitor = Stream<OperationState>(OperationState.UNKNOWN);
  private resourceAutocompleteHelper: Map<string, string[]>;

  protected constructor(onSuccessfulSave: (msg: m.Children) => any,
                        onError: (msg: m.Children) => any,
                        pluginInfos: Stream<PluginInfos>,
                        resourceAutocompleteHelper: Map<string, string[]>) {
    super(Size.large);
    this.onSuccessfulSave = onSuccessfulSave;
    this.onError          = onError;
    this.pluginInfos      = pluginInfos;
    this.resourceAutocompleteHelper = resourceAutocompleteHelper;
  }

  body(): m.Children {
    let errorMessage;
    if (this.error) {
      errorMessage = (<FlashMessage type={MessageType.alert} message={this.error}/>);
    }

    if (!this.getRepo()) {
      return <div class={styles.spinnerWrapper}><Spinner/></div>;
    }
    let materialtocomponentmapElement;

    const material = this.getRepo().material()!;
    if (!material.type()) {
      materialtocomponentmapElement = NewMaterialComponent;
    } else {
      materialtocomponentmapElement = MATERIAL_TO_COMPONENT_MAP[material.type()!];
    }

    return m(materialtocomponentmapElement,
             {
               onSuccessfulSave: this.onSuccessfulSave,
               onError: this.onError,
               repo: this.getRepo(),
               isNew: this.isNew,
               pluginInfos: this.pluginInfos,
               error: errorMessage,
               resourceAutocompleteHelper: this.resourceAutocompleteHelper
             });

  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" ajaxOperation={this.performSave.bind(this)}
                       ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}
                      ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Buttons.Cancel>
    ];
  }

  abstract performSave(): Promise<any>;

  protected handleAutoUpdateError() {
    const errors = this.getRepo().material()!.attributes()!.errors();
    if (errors.hasErrors("auto_update")) {
      this.error = errors.errorsForDisplay("auto_update");
    }
  }

  protected abstract getRepo(): ConfigRepo;
}

export class NewConfigRepoModal extends ConfigRepoModal {
  private readonly repo: Stream<ConfigRepo> = Stream();

  constructor(onSuccessfulSave: (msg: (m.Children)) => any,
              onError: (msg: (m.Children)) => any,
              pluginInfos: Stream<PluginInfos>,
              resourceAutocompleteHelper: Map<string, string[]>) {
    super(onSuccessfulSave, onError, pluginInfos, resourceAutocompleteHelper);

    // prefer the YAML plugin and fallback to the first plugin when not present
    const defaultPlugin = pluginInfos().find((p) => ConfigRepo.YAML_PLUGIN_ID === p.id) || pluginInfos()[0];

    this.repo(new ConfigRepo(undefined, defaultPlugin.id, new Material("git", new GitMaterialAttributes())));
    this.isNew = true;
  }

  title(): string {
    return `Create new configuration repository`;
  }

  getRepo() {
    return this.repo();
  }

  performSave() {
    if (!this.repo().isValid()) {
      return Promise.resolve();
    }
    return ConfigReposCRUD.create(this.repo())
                   .then((result) => result.do(this.onSuccess.bind(this),
                                               (errorResponse) => this.handleError(result, errorResponse)));
  }

  oncreate(vnode: m.VnodeDOM<any, {}>) {
    vnode.dom.querySelector("select")!.focus();
  }

  private onSuccess() {
    this.onSuccessfulSave(<span>The config repository <em>{this.repo().id()}</em> was created successfully!</span>);
    this.close();
  }

  private handleError(result: ApiResult<ObjectWithEtag<ConfigRepo>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.repo(ConfigRepo.fromJSON(json.data));
      this.handleAutoUpdateError();
    } else {
      this.onError(JSON.parse(errorResponse.body!).message);
      this.close();
    }
  }
}

export class EditConfigRepoModal extends ConfigRepoModal {
  private readonly repoId: string;
  private repoWithEtag: Stream<ObjectWithEtag<ConfigRepo>> = Stream();

  constructor(repoId: string,
              onSuccessfulSave: (msg: (m.Children)) => any,
              onError: (msg: (m.Children)) => any,
              pluginInfos: Stream<PluginInfos>,
              resourceAutocompleteHelper: Map<string, string[]>) {
    super(onSuccessfulSave, onError, pluginInfos, resourceAutocompleteHelper);
    this.repoId = repoId;

    ConfigReposCRUD
      .get(repoId)
      .then((result) => result.do(
        (successResponse) => this.repoWithEtag(successResponse.body), this.onRepoGetFailure));
  }

  title(): string {
    return `Edit configuration repository ${this.repoId}`;
  }

  performSave(): Promise<any> {
    if (!this.repoWithEtag().object.isValid()) {
      return Promise.resolve();
    }
    return ConfigReposCRUD.update(this.repoWithEtag())
                   .then((apiResult) => apiResult.do(this.onSuccess.bind(this),
                                                     (errorResponse) => this.handleError(apiResult, errorResponse)));
  }

  protected getRepo(): ConfigRepo {
    return this.repoWithEtag() && this.repoWithEtag().object;
  }

  private onRepoGetFailure(errorResponse: ErrorResponse) {
    this.error = JSON.parse(errorResponse.body!).message;
  }

  private onSuccess() {
    this.close();
    this.onSuccessfulSave(<span>The config repository <em>{this.getRepo().id()}</em> was updated successfully!</span>);
  }

  private handleError(result: ApiResult<ObjectWithEtag<ConfigRepo>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      const etag = this.repoWithEtag().etag;
      this.repoWithEtag({etag, object: ConfigRepo.fromJSON(json.data)});
      this.handleAutoUpdateError();
    } else {
      this.onError(JSON.parse(errorResponse.body!).message);
      this.close();
    }
  }
}
