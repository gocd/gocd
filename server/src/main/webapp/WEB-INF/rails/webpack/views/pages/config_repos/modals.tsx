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

import {docsUrl} from "gen/gocd_version";
import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import {showIf} from "helpers/utils";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigReposCRUD} from "models/config_repos/config_repos_crud";
import {ConfigRepo, humanizedMaterialAttributeName, humanizedMaterialNameForMaterialType} from "models/config_repos/types";
import {GitMaterialAttributes, HgMaterialAttributes, Material, P4MaterialAttributes, SvnMaterialAttributes, TfsMaterialAttributes} from "models/materials/types";
import {USER_NS} from "models/mixins/configuration_properties";
import {Configuration} from "models/shared/configuration";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {KeyValEditor} from "views/components/encryptable_key_value/editor";
import {EntriesVM} from "views/components/encryptable_key_value/vms";
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
  & { userProps: EntriesVM }
  & { isNew: boolean }
  & RequiresPluginInfos
  & { error?: m.Children }
  & { resourceAutocompleteHelper: Map<string, string[]> };

class MaterialEditWidget extends MithrilViewComponent<EditableMaterial> {
  view(vnode: m.Vnode<EditableMaterial>) {
    const { repo, userProps, isNew, error, pluginInfos, resourceAutocompleteHelper } = vnode.attrs;

    const pluginList = _.map(pluginInfos(), (p) => ({ id: p.id, text: p.about.name }));
    const allowUserProperties = !!pluginInfos().
      configRepoPluginsWhich("supportsUserDefinedProperties").
      findByPluginId(repo.pluginId()!);

    const infoMsg = <span>Configure rules to allow which environment/pipeline group/pipeline the config repository can refer to. By default, the config repository cannot refer to an entity unless explicitly allowed. <Link
      href={docsUrl("advanced_usage/pipelines_as_code.html")} target="_blank" externalLinkIcon={true}>Learn More</Link></span>;

    return [
      showIf(!!error, () => <div class={styles.errorWrapper}>{error}</div>),
      <FormHeader>
        <Form>
          <TextField label="Config repository name"
                     readonly={!isNew}
                     property={repo.id}
                     errorText={repo.errors().errorsForDisplay("id")}
                     css={styles}
                     required={true}/>
          <SelectField label="Plugin ID"
                       property={repo.pluginId}
                       required={true}
                       css={styles}
                       errorText={repo.errors().errorsForDisplay("pluginId")}>
            <SelectFieldOptions selected={repo.pluginId()}
                                items={pluginList}/>
          </SelectField>
        </Form>
        <Form>
          <SelectField label={"Material type"}
                       css={styles}
                       property={repo.material()!.typeProxy.bind(repo.material()!)}
                       required={true}
                       errorText={repo.errors().errorsForDisplay("material")}>
            <SelectFieldOptions selected={repo.material()!.type()}
                                items={this.materialSelectOptions()}/>
          </SelectField>
        </Form>
      </FormHeader>,
      <div>
        <div class={styles.materialConfigWrapper}>
          <FormBody>
            <Form last={true}>
              {vnode.children}
            </Form>
          </FormBody>
          <TestConnection material={repo.material()!} configRepo={repo}/>
        </div>
        <div class={styles.pluginFilePatternConfigWrapper}>
          <FormBody>
            <Form>
              {MaterialEditWidget.pluginConfigView(vnode)}
            </Form>
          </FormBody>
        </div>
      </div>,
      showIf(
        allowUserProperties,
        () => <div class={styles.configProperties}>
          <h2>User-defined Properties/Variables</h2>
          {showIf(
            repo.errors().hasErrors("configuration"),
            () => <FlashMessage type={MessageType.alert} message={repo.errors().errorsForDisplay("configuration")}/>
          )}
          <KeyValEditor model={userProps} onchange={() => repo.userProps(userProps.toJSON())}/>
        </div>
      ),
      <div>
        <ConfigureRulesWidget infoMsg={infoMsg}
                              rules={repo.rules}
                              types={[RulesType.PIPELINE, RulesType.PIPELINE_GROUP, RulesType.ENVIRONMENT]}
                              resourceAutocompleteHelper={resourceAutocompleteHelper}/>
      </div>
    ];
  }

  private static pluginConfigView(vnode: m.Vnode<EditableMaterial>): m.Children {
    const repo = vnode.attrs.repo;

    let pluginConfig = null;
    if (ConfigRepo.JSON_PLUGIN_ID === repo.pluginId()) {
      pluginConfig = [
        <TextField property={repo.jsonPipelinesPattern}
                   css={styles}
                   label="GoCD pipeline files pattern"/>,
        <TextField property={repo.jsonEnvPattern}
                   label="GoCD environment files pattern"/>
      ];
    } else if (ConfigRepo.YAML_PLUGIN_ID === repo.pluginId()) {
      pluginConfig = (<TextField property={repo.yamlPattern} css={styles} label="GoCD YAML files pattern"/>);
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
    const attrs = Object.assign({}, vnode.attrs, {isNew: true});
    return <MaterialEditWidget {...attrs}/>;
  }
} as MithrilViewComponent<EditableMaterial>;

const MATERIAL_TO_COMPONENT_MAP: { [key: string]: MithrilViewComponent<EditableMaterial> } = {
  git: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as GitMaterialAttributes;

      return <MaterialEditWidget {...vnode.attrs}>
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
      </MaterialEditWidget>;
    }
  } as MithrilViewComponent<EditableMaterial>,

  svn: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as SvnMaterialAttributes;

      return <MaterialEditWidget {...vnode.attrs}>
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
      </MaterialEditWidget>;
    }
  } as MithrilViewComponent<EditableMaterial>,

  hg: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as HgMaterialAttributes;

      return <MaterialEditWidget {...vnode.attrs}>
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
      </MaterialEditWidget>;
    }
  } as MithrilViewComponent<EditableMaterial>,

  p4: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as P4MaterialAttributes;
      return <MaterialEditWidget {...vnode.attrs}>
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
      </MaterialEditWidget>;
    }
  } as MithrilViewComponent<EditableMaterial>,

  tfs: {
    view(vnode: m.Vnode<EditableMaterial>) {
      const materialAttributes = vnode.attrs.repo.material()!.attributes() as TfsMaterialAttributes;

      return <MaterialEditWidget {...vnode.attrs}>
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
      </MaterialEditWidget>;
    }
  } as MithrilViewComponent<EditableMaterial>
};

export abstract class ConfigRepoModal extends Modal {
  protected error: string | undefined;
  protected readonly onSuccessfulSave: (msg: m.Children) => any;
  protected readonly onError: (msg: m.Children) => any;
  protected isNew: boolean = false;
  protected pluginInfos: Stream<PluginInfos>;
  protected userProps: Stream<EntriesVM> = Stream(new EntriesVM([], USER_NS));
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
    let materialtocomponentmapElement: MithrilViewComponent<EditableMaterial>;

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
               userProps: this.userProps(),
               isNew: this.isNew,
               pluginInfos: this.pluginInfos,
               error: errorMessage,
               resourceAutocompleteHelper: this.resourceAutocompleteHelper
             });

  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" ajaxOperation={this.save.bind(this)}
                       ajaxOperationMonitor={this.ajaxOperationMonitor}>Save</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}
                      ajaxOperationMonitor={this.ajaxOperationMonitor}>Cancel</Buttons.Cancel>
    ];
  }

  save(): Promise<any> {
    const repo = this.getRepo();
    repo.userProps(this.userProps().toJSON());

    if (!repo.isValid()) {
      this.userProps().bindErrors(repo.propertyErrors());
      return Promise.resolve();
    }

    return this.performSave();
  }

  protected abstract performSave(): Promise<any>;

  protected handleAutoUpdateError() {
    const errors = this.getRepo().material()!.attributes()!.errors();
    if (errors.hasErrors("autoUpdate")) {
      this.error = errors.errorsForDisplay("autoUpdate");
    }
  }

  protected abstract getRepo(): ConfigRepo;

  protected handleError(result: ApiResult<ObjectWithEtag<ConfigRepo>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.getRepo().consumeErrorsResponse(json.data);
      this.handleAutoUpdateError();
      this.userProps().bindErrors(json.data.configuration || []);
    } else {
      this.onError(JSON.parse(errorResponse.body!).message);
      this.close();
    }
  }

  /** Sets up the view model for user-defined configuration properties. */
  protected initProperties(repo: ConfigRepo) {
    this.userProps(new EntriesVM(repo.userProps() as Configuration[], USER_NS));
    return repo;
  }
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

    this.repo(
      this.initProperties(
        new ConfigRepo(undefined, defaultPlugin.id, new Material("git", new GitMaterialAttributes()))
      )
    );
    this.isNew = true;
  }

  title(): string {
    return `Create new configuration repository`;
  }

  getRepo() {
    return this.repo();
  }

  oncreate(vnode: m.VnodeDOM<any, {}>) {
    vnode.dom.querySelector("select")!.focus();
  }

  protected performSave() {
    return ConfigReposCRUD.create(this.getRepo())
                   .then((result) => result.do(this.onSuccess.bind(this),
                                               (errorResponse) => this.handleError(result, errorResponse)));
  }

  private onSuccess() {
    this.onSuccessfulSave(<span>The config repository <em>{this.repo().id()}</em> was created successfully!</span>);
    this.close();
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
        (successResponse) => {
          this.repoWithEtag(successResponse.body);
          this.initProperties(this.getRepo());
        }, this.onRepoGetFailure));
  }

  title(): string {
    return `Edit configuration repository ${this.repoId}`;
  }

  protected performSave(): Promise<any> {
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
}
