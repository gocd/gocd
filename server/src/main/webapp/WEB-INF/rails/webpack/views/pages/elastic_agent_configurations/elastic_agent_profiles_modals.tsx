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
import {bind} from "classnames/bind";
import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ElasticAgentProfilesCRUD} from "models/elastic_profiles/elastic_agent_profiles_crud";
import {ClusterProfile, ClusterProfiles, ElasticAgentProfile, ProfileUsage} from "models/elastic_profiles/types";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Link} from "views/components/link";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {Table} from "views/components/table";
import styles from "views/pages/elastic_agent_configurations/index.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const AngularPluginNew = require("views/shared/angular_plugin_new").AngularPluginNew;

const foundationClassNames = bind(foundationStyles);

enum ModalType {
  edit, clone, create
}

abstract class BaseElasticProfileModal extends Modal {
  protected elasticProfile: Stream<ElasticAgentProfile>;
  private readonly pluginInfo: Stream<PluginInfo>;
  private readonly pluginInfos: PluginInfos;
  private readonly modalType: ModalType;
  private errorMessage: string | undefined;
  private noClusterProfileError: string | undefined;
  private readonly clusterProfiles: ClusterProfiles;

  protected constructor(pluginInfos: PluginInfos,
                        type: ModalType,
                        clusterProfiles: ClusterProfiles,
                        elasticProfile?: ElasticAgentProfile) {
    super(Size.extraLargeHackForEaProfiles);
    this.clusterProfiles = clusterProfiles;
    this.elasticProfile  = Stream(elasticProfile!);
    this.pluginInfos     = pluginInfos;
    this.pluginInfo      = Stream();
    this.modalType       = type;
  }

  abstract performSave(): void;

  abstract modalTitle(): string;

  validateAndPerformSave() {
    if (!this.elasticProfile() || !this.elasticProfile().isValid()) {
      return;
    }
    this.performSave();
  }

  showErrors(apiResult: ApiResult<ObjectWithEtag<ElasticAgentProfile>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      const profile = ElasticAgentProfile.fromJSON(JSON.parse(errorResponse.body).data);
      profile.pluginId(this.clusterProfiles.findCluster(profile.clusterProfileId()!).pluginId());
      this.elasticProfile(profile);
    } else {
      this.noClusterProfileError = JSON.parse(errorResponse.body!).message;
    }
  }

  buttons() {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.validateAndPerformSave.bind(this)}>Save</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>,
    ];
  }

  body() {
    if (this.errorMessage) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage}/>);
    }

    if (!this.elasticProfile()) {
      return <div class={styles.spinnerWrapper}><Spinner/></div>;
    }

    this.pluginInfo(this.pluginInfos.find((pluginInfo) => {
      return pluginInfo.id === this.elasticProfile().pluginId();
    }) || this.pluginInfos[0]);

    const selectedPluginId = this.pluginInfo().id;

    const clustersBelongingToPlugin = this.clusterProfiles.all().filter((cluster: ClusterProfile) => {
      return cluster.pluginId() === selectedPluginId;
    });

    if (clustersBelongingToPlugin.length === 0) {
      this.noClusterProfileError = `Can not create Elastic Agent Profile for plugin '${selectedPluginId}'. A Cluster Profile must be configured first in order to define a new Elastic Agent Profile.`;
    } else {
      this.elasticProfile()
          .clusterProfileId(this.elasticProfile().clusterProfileId() || clustersBelongingToPlugin[0].id());
    }

    const clustersList                 = _.map(clustersBelongingToPlugin, (clusterProfile: ClusterProfile) => {
      return {id: clusterProfile.id(), text: `${clusterProfile.id()} (${this.pluginInfo().about.name})`};
    }) || [];
    const elasticAgentExtension        = this.pluginInfo()
                                             .extensionOfType(ExtensionTypeString.ELASTIC_AGENTS);
    const elasticProfileConfigurations = (elasticAgentExtension as ElasticAgentExtension).profileSettings;

    return (
      <div class={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        <div>
          <FormHeader>
            <FlashMessage type={MessageType.alert} message={this.noClusterProfileError}/>
            <Form>
              <TextField label="Id"
                         readonly={this.modalType === ModalType.edit}
                         property={this.elasticProfile().id}
                         errorText={this.elasticProfile().errors().errorsForDisplay("id")}
                         required={true}/>
              <SelectField label="Cluster Profile ID"
                           property={this.clusterProfileIdProxy.bind(this)}
                           required={true}
                           errorText={this.elasticProfile().errors().errorsForDisplay("pluginId")}>
                <SelectFieldOptions selected={this.elasticProfile().clusterProfileId()}
                                    items={clustersList as any}/>
              </SelectField>
            </Form>
          </FormHeader>

        </div>
        <div class={styles.elasticProfileModalFormBody}>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={Stream(elasticProfileConfigurations)}
              configuration={this.elasticProfile().properties()}
              key={this.pluginInfo().id}/>
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

  private clusterProfileIdProxy(newValue?: string) {
    if (newValue) {
      if (this.elasticProfile().id() !== newValue) {
        this.elasticProfile(new ElasticAgentProfile(this.elasticProfile().id(),
                                                    this.elasticProfile().pluginId(),
                                                    newValue,
                                                    this.elasticProfile().canAdminister(),
                                                    this.elasticProfile().properties()));
      }
    }

    return this.elasticProfile().clusterProfileId();
  }
}

export class CloneElasticProfileModal extends BaseElasticProfileModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly sourceProfileId: string;

  constructor(elasticProfileId: string,
              pluginId: string,
              pluginInfos: PluginInfos,
              clusterProfiles: ClusterProfiles,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(pluginInfos, ModalType.create, clusterProfiles);
    this.sourceProfileId  = elasticProfileId;
    this.onSuccessfulSave = onSuccessfulSave;

    ElasticAgentProfilesCRUD
      .get(elasticProfileId)
      .then((result) => {
        result.do(
          (successResponse) => {
            const elasticProfile = successResponse.body.object;
            elasticProfile.id("");
            elasticProfile.pluginId(pluginId);
            this.elasticProfile(elasticProfile);
          },
          (errorResponse) => this.onError(JSON.parse(errorResponse.body!).message)
        );
      });
  }

  performSave() {
    ElasticAgentProfilesCRUD
      .create(this.elasticProfile())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>
                The elastic agent profile <em>{this.elasticProfile().id()}</em> was created successfully!
                     </span>
            );
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      }).finally(m.redraw);
  }

  modalTitle() {
    return "Clone elastic agent profile " + this.sourceProfileId;
  }
}

export class UsageElasticProfileModal extends Modal {
  private usages: ProfileUsage[];
  private readonly profileId: string;

  constructor(profileId: string, usages: ProfileUsage[]) {
    super(Size.large);
    this.usages    = usages;
    this.profileId = profileId;
  }

  body() {
    if (this.usages.length <= 0) {
      return (<span> No usages for profile '{this.profileId}' found.</span>);
    }

    const data = this.usages.map((usage) => [
      <span class={styles.tableCell}>{usage.pipelineName()}</span>,
      <span class={styles.tableCell}>{usage.stageName()}</span>,
      <span class={styles.tableCell}>{usage.jobName()}</span>,
      UsageElasticProfileModal.anchorToSettings(usage)
    ]);
    return (
      <div>
        <Table headers={["Pipeline", "Stage", "Job", " "]} data={data}/>
      </div>
    );
  }

  title() {
    return "Usages for " + this.profileId;
  }

  private static anchorToSettings(usage: ProfileUsage) {
    let link = `/go/admin/pipelines/${usage.pipelineName()}/stages/${usage.stageName()}/job/${usage.jobName()}/settings`;

    if (usage.templateName()) {
      link = `/go/admin/templates/${usage.templateName()}/stages/${usage.stageName()}/job/${usage.jobName()}/settings`;
    }

    return <span class={styles.jobSettingsLink}>
      <Link href={link}>Job Settings</Link>
    </span>;
  }
}
