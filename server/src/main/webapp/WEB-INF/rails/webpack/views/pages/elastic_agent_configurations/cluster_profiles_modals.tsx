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
import { bind } from "classnames/bind";
import { ApiResult, ErrorResponse, ObjectWithEtag } from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import { ClusterProfilesCRUD } from "models/elastic_profiles/cluster_profiles_crud";
import { ClusterProfile } from "models/elastic_profiles/types";
import { Configurations } from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import { ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import { PluginInfo, PluginInfos } from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import { FlashMessage, MessageType } from "views/components/flash_message";
import { Form, FormHeader } from "views/components/forms/form";
import { SelectField, SelectFieldOptions, TextField } from "views/components/forms/input_fields";
import { Modal, Size } from "views/components/modal";
import { Spinner } from "views/components/spinner";
import styles from "views/pages/elastic_agent_configurations/index.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const AngularPluginNew = require('views/shared/angular_plugin_new').AngularPluginNew;

const foundationClassNames = bind(foundationStyles);

export enum ModalType {
  edit, create
}

export abstract class BaseClusterProfileModal extends Modal {
  protected clusterProfile: Stream<ClusterProfile>;
  private readonly elasticAgentPluginInfo: Stream<PluginInfo>;
  private readonly pluginInfos: PluginInfos;
  private readonly modalType: ModalType;
  private errorMessage: string | undefined | null = null;

  protected constructor(pluginInfos: PluginInfos, type: ModalType, clusterProfile?: ClusterProfile) {
    super(Size.extraLargeHackForEaProfiles);
    this.clusterProfile = Stream(clusterProfile!);
    this.pluginInfos = pluginInfos;
    this.elasticAgentPluginInfo = Stream();
    this.modalType = type;
  }

  abstract performSave(): void;

  abstract modalTitle(): string;

  showErrors(apiResult: ApiResult<ObjectWithEtag<ClusterProfile>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      const profile = ClusterProfile.fromJSON(JSON.parse(errorResponse.body).data);
      this.clusterProfile(profile);
    }
  }

  buttons() {
    if (this.supportsClusterProfile()) {
      return [<Buttons.Primary onclick={this.performSave.bind(this)} data-test-id={"cluster-profile-save-btn"}>Save</Buttons.Primary>];
    }
    return [];
  }

  pluginInfo() {
    if (this.clusterProfile() && this.clusterProfile().pluginId()) {
      return this.pluginInfos.find(
        (pluginInfo) => pluginInfo.id === this.clusterProfile().pluginId());
    }
  }

  supportsClusterProfile() {
    const extensionPluginInfo = this.pluginInfo();
    if (extensionPluginInfo) {
      const elasticAgentExtensionInfo = extensionPluginInfo.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS) as ElasticAgentExtension;
      return elasticAgentExtensionInfo && elasticAgentExtensionInfo.supportsClusterProfiles;
    }
    return false;
  }

  body() {
    let errorSection: m.Children;

    if (this.errorMessage) {
      errorSection = (<FlashMessage type={MessageType.alert} message={this.errorMessage} />);
    }

    if (!this.clusterProfile()) {
      return <div class={styles.spinnerWrapper} data-test-id="spinner-wrapper"><Spinner /></div>;
    }

    const pluginList = _.map(this.pluginInfos, (pluginInfo: PluginInfo) => {
      return { id: pluginInfo.id, text: pluginInfo.about.name };
    });

    const extensionPluginInfo = this.pluginInfo();
    this.elasticAgentPluginInfo(extensionPluginInfo || this.pluginInfos[0]);

    const elasticAgentSettings = this.elasticAgentPluginInfo().extensionOfType(ExtensionTypeString.ELASTIC_AGENTS) as ElasticAgentExtension;

    let clusterProfileForm, alertMessage;

    if (this.supportsClusterProfile()) {
      clusterProfileForm = <AngularPluginNew pluginInfoSettings={Stream(elasticAgentSettings.clusterProfileSettings)}
        configuration={this.clusterProfile().properties()}
        key={this.elasticAgentPluginInfo().id} />;
    } else {
      const pluginName = extensionPluginInfo ? extensionPluginInfo.about.name : "";
      if (this.modalType === ModalType.create) {
        alertMessage = <FlashMessage type={MessageType.alert} message={`Can not define Cluster profiles for '${pluginName}' plugin as it does not support cluster profiles.`} />;
      } else {
        alertMessage = <FlashMessage type={MessageType.warning} message={`Can not edit Cluster profile for '${pluginName}' plugin as it does not support cluster profiles.`} />;
      }
    }

    return (
      <div>
        <div>
          <FormHeader dataTestId={"cluster-profile-form-header"}>
            {alertMessage}
            <Form>
              <TextField label="Id"
                readonly={this.modalType === ModalType.edit}
                property={this.clusterProfile().id}
                errorText={this.clusterProfile().errors().errorsForDisplay("id")}
                required={true} />

              <SelectField label="Plugin ID"
                property={this.pluginIdProxy.bind(this)}
                required={true}
                errorText={this.clusterProfile().errors().errorsForDisplay("pluginId")}>
                <SelectFieldOptions selected={this.clusterProfile().pluginId()}
                  items={pluginList} />
              </SelectField>
            </Form>
          </FormHeader>
        </div>
        <div>
          {errorSection}
          <div className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
            <div class="row collapse" data-test-id="cluster-profile-properties-form">
              {clusterProfileForm}
            </div>
          </div>
        </div>
      </div>
    );
  }

  title() {
    return this.modalTitle();
  }

  protected onError(error: string) {
    this.errorMessage = error;
  }

  private pluginIdProxy(newValue?: string) {
    if (newValue) {
      if (this.elasticAgentPluginInfo().id !== newValue) {
        const pluginInfo = _.find(this.pluginInfos, (p) => p.id === newValue);
        this.elasticAgentPluginInfo(pluginInfo!);
        this.clusterProfile(new ClusterProfile(
          this.clusterProfile().id(),
          pluginInfo!.id,
          this.clusterProfile().canAdminister(),
          new Configurations([])
        ));

        this.clusterProfile(new ClusterProfile(
          this.clusterProfile().id(),
          pluginInfo!.id,
          this.clusterProfile().canAdminister(),
          new Configurations([])
        ));
      }
    }
    return this.elasticAgentPluginInfo().id;
  }
}

export class CloneClusterProfileModal extends BaseClusterProfileModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly clusterProfileId: string;

  constructor(clusterProfileId: string, pluginInfos: PluginInfos, onSuccessfulSave: (msg: m.Children) => any) {
    super(pluginInfos, ModalType.create);
    this.clusterProfileId = clusterProfileId;
    this.onSuccessfulSave = onSuccessfulSave;

    ClusterProfilesCRUD.get(clusterProfileId)
      .then((result) => {
        result.do(
          (successResponse) => {
            this.clusterProfile(successResponse.body.object);
            this.clusterProfile().id("");
          },
          ((errorResponse) => this.onError(JSON.parse(errorResponse.body!).message))
        );
      });
  }

  performSave() {
    ClusterProfilesCRUD.create(this.clusterProfile())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(<span>The cluster profile <em>{this.clusterProfile().id()}</em> was created successfully!</span>);
            this.close();
          },
          (errorResponse) => {
            this.onError(JSON.parse(errorResponse.body!).message);
            this.showErrors(result, errorResponse);
          }
        );
      });
  }

  modalTitle() {
    return "Clone cluster profile " + this.clusterProfileId;
  }
}
