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

import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Rules} from "models/secret_configs/rules";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {SecretConfigsCRUD} from "models/secret_configs/secret_configs_crud";
import {SecretConfigJSON} from "models/secret_configs/secret_configs_json";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {SecretSettings} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Size} from "views/components/modal";
import {EntityModal} from "views/components/modal/entity_modal";

const AngularPluginNew = require("views/shared/angular_plugin_new");

export abstract class SecretConfigModal extends EntityModal<SecretConfig> {
  protected readonly originalEntityId: string;
  protected entities: Stream<SecretConfigs>;
  private disableId: boolean;

  constructor(entities: Stream<SecretConfigs>,
              entity: SecretConfig,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any,
              disableId: boolean = false,
              size: Size         = Size.large) {
    super(entity, pluginInfos, onSuccessfulSave, size);
    this.entities         = entities;
    this.originalEntityId = entity.id();
    this.disableId        = disableId;
  }

  protected modalBody(): m.Children {
    const pluginList     = _.map(this.pluginInfos, (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });
    const pluginInfo     = this.findPluginInfo(this.pluginInfos, this.entity().pluginId());
    const pluginSettings = (pluginInfo.extensionOfType(ExtensionType.SECRETS)! as SecretSettings).secretConfigSettings;

    return <div>
      <FormHeader>
        <Form>
          <TextField label="Id"
                     placeholder="Enter an Id for the config"
                     property={this.entity().id}
                     errorText={this.entity().errors().errorsForDisplay("id")}
                     readonly={this.disableId}
                     required={true}/>

          <SelectField label="Plugin"
                       property={this.pluginIdProxy.bind(this)}
                       required={true}
                       errorText={this.entity().errors().errorsForDisplay("pluginId")}>
            <SelectFieldOptions selected={this.entity().pluginId()}
                                items={pluginList}/>
          </SelectField>
        </Form>
      </FormHeader>

      <div>
        <div className="row collapse">
          <AngularPluginNew
            pluginInfoSettings={stream(pluginSettings)}
            configuration={this.entity().properties()}
            key={this.entity().id}/>
        </div>
      </div>
    </div>;
  }

  protected onPluginChange(entity: Stream<SecretConfig>, pluginInfo: PluginInfo<any>): void {
    this.entity(new SecretConfig(entity().id(),
                                 entity().description(),
                                 pluginInfo.id,
                                 new Configurations([]),
                                 new Rules()));
  }

  protected parseJsonToEntity(json: object): SecretConfig {
    return SecretConfig.fromJSON(json as SecretConfigJSON);
  }

  protected performFetch(entity: SecretConfig): Promise<any> {
    return SecretConfigsCRUD.get(entity);
  }

  protected afterSuccess(): void {
    this.entities().push(this.entity);
  }

  private findPluginInfo(pluginInfos: Array<PluginInfo<any>>, pluginId: string): PluginInfo<any> {
    return pluginInfos.find((pluginInfo) => pluginInfo.id === pluginId) as PluginInfo<any>;
  }
}

export class CreateSecretConfigModal extends SecretConfigModal {

  constructor(entities: Stream<SecretConfigs>,
              entity: SecretConfig,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entities, entity, pluginInfos, onSuccessfulSave, false);
    this.isStale(false);
  }

  title(): string {
    return "Add secret configuration";
  }

  protected operationPromise(): Promise<any> {
    return SecretConfigsCRUD.create(this.entity());
  }

  protected successMessage(): m.Children {
    return <span>The secret configuration <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}

export class EditSecretConfigModal extends SecretConfigModal {
  constructor(entities: Stream<SecretConfigs>,
              entity: SecretConfig,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entities, entity, pluginInfos, onSuccessfulSave, true);
  }

  title(): string {
    return `Edit secret configuration ${this.originalEntityId}`;
  }

  protected operationPromise(): Promise<any> {
    return SecretConfigsCRUD.update(this.entity(), this.etag());
  }

  protected successMessage(): m.Children {
    return <span>The secret configuration <em>{this.entity().id()}</em> was updated successfully!</span>;
  }

  protected afterSuccess(): void {
    const filteredEntities = this.entities().filter((entity) => {
      return entity().id() !== this.entity().id();
    });
    this.entities(filteredEntities);
    super.afterSuccess();
  }

}

export class CloneSecretConfigModal extends SecretConfigModal {
  constructor(entities: Stream<SecretConfigs>,
              entity: SecretConfig,
              pluginInfos: Array<PluginInfo<any>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(entities, entity, pluginInfos, onSuccessfulSave, false);
  }

  title(): string {
    return `Clone secret configuration ${this.originalEntityId}`;
  }

  fetchCompleted() {
    this.entity().id("");
  }

  protected operationPromise(): Promise<any> {
    return SecretConfigsCRUD.create(this.entity());
  }

  protected successMessage(): m.Children {
    return <span>The secret configuration <em>{this.entity().id()}</em> was created successfully!</span>;
  }
}
