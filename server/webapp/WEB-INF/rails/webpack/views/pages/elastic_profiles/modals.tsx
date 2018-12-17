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

import {bind} from "classnames/bind";
import {ApiResult, ErrorResponse, ObjectWithEtag} from "helpers/api_request_builder";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ElasticProfilesCRUD} from "models/elastic_profiles/elastic_profiles_crud";
import {ElasticProfile, ProfileUsage} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ElasticAgentSettings, Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Form, FormHeader} from "views/components/forms/form";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Spinner} from "views/components/spinner";
import {Table} from "views/components/table";
import * as styles from "views/pages/elastic_profiles/index.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);
const AngularPluginNew     = require("views/shared/angular_plugin_new");
import * as _ from "lodash";

enum ModalType {
  edit, clone, create
}

abstract class BaseElasticProfileModal extends Modal {
  protected elasticProfile: Stream<ElasticProfile>;
  private errorMessage: null;
  private readonly pluginInfo: Stream<PluginInfo<Extension>>;
  private readonly pluginInfos: Array<PluginInfo<Extension>>;
  private readonly modalType: ModalType;

  protected constructor(elasticProfile: ElasticProfile,
                        pluginInfos: Array<PluginInfo<Extension>>,
                        type: ModalType) {
    super(Size.large);
    this.elasticProfile = stream(elasticProfile);
    this.pluginInfos    = pluginInfos;
    this.pluginInfo     = stream(pluginInfos.find(
      (pluginInfo) => pluginInfo.id === elasticProfile.pluginId()) || pluginInfos[0]);
    this.modalType      = type;
  }

  abstract performSave(): void;

  abstract modalTitle(elasticProfile: ElasticProfile): string;

  validateAndPerformSave() {
    if (!this.elasticProfile().isValid()) {
      return;
    }
    this.performSave();
  }

  showErrors(apiResult: ApiResult<ObjectWithEtag<ElasticProfile>>, errorResponse: ErrorResponse) {
    if (apiResult.getStatusCode() === 422 && errorResponse.body) {
      const profile = ElasticProfile.fromJSON(JSON.parse(errorResponse.body).data);
      this.elasticProfile(profile);
    }
  }

  buttons() {
    return [<Buttons.Primary data-test-id="button-ok"
                             onclick={this.validateAndPerformSave.bind(this)}>Save</Buttons.Primary>];
  }

  body() {
    if (this.errorMessage) {
      return (<FlashMessage type={MessageType.alert} message={this.errorMessage}/>);
    }

    if (!this.elasticProfile()) {
      return <Spinner/>;
    }

    const pluginList = _.map(this.pluginInfos, (pluginInfo: PluginInfo<any>) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    const pluginSettings = (this.pluginInfo()
                                .firstExtensionWithPluginSettings()! as ElasticAgentSettings).profileSettings;

    return (
      <div class={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
        <div>
          <FormHeader>
            <Form>
              <TextField label="Id"
                         disabled={this.modalType === ModalType.edit}
                         property={this.elasticProfile().id}
                         errorText={this.elasticProfile().errors().errorsForDisplay("id")}
                         required={true}/>

              <SelectField label="Plugin ID"
                           property={this.pluginIdProxy.bind(this)}
                           required={true}
                           errorText={this.elasticProfile().errors().errorsForDisplay("pluginId")}>
                <SelectFieldOptions selected={this.elasticProfile().pluginId()}
                                    items={pluginList}/>
              </SelectField>
            </Form>
          </FormHeader>

        </div>
        <div class={styles.elasticProfileModalFormBody}>
          <div class="row collapse">
            <AngularPluginNew
              pluginInfoSettings={stream(pluginSettings)}
              configuration={this.elasticProfile().properties()}
              key={this.pluginInfo().id}/>
          </div>
        </div>
      </div>
    );
  }

  title() {
    return this.modalTitle(this.elasticProfile());
  }

  private pluginIdProxy(newValue ?: string) {
    if (newValue) {
      if (this.pluginInfo().id !== newValue) {
        const pluginInfo = _.find(this.pluginInfos, (p) => p.id === newValue);
        this.pluginInfo(pluginInfo!);
        this.elasticProfile(new ElasticProfile(this.elasticProfile().id(), pluginInfo!.id, new Configurations([])));
      }
    }
    return this.pluginInfo().id;
  }
}

export class EditElasticProfileModal extends BaseElasticProfileModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private etag: string;

  constructor(elasticProfile: ObjectWithEtag<ElasticProfile>,
              pluginInfos: Array<PluginInfo<Extension>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    super(elasticProfile.object, pluginInfos, ModalType.edit);
    this.etag             = elasticProfile.etag;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  performSave() {
    ElasticProfilesCRUD
      .update(this.elasticProfile(), this.etag)
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>
                      The elastic profile <em>{this.elasticProfile().id()}</em> was updated successfully!
                     </span>
            );
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      });
  }

  modalTitle(elasticProfile: ElasticProfile) {
    return "Edit profile " + elasticProfile.id();
  }
}

export class CloneElasticProfileModal extends BaseElasticProfileModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly sourceProfileId: string;

  constructor(elasticProfile: ElasticProfile,
              pluginInfos: Array<PluginInfo<Extension>>,
              onSuccessfulSave: (msg: m.Children) => any) {
    const _sourceProfileId = elasticProfile.id();
    elasticProfile.id("");

    super(elasticProfile, pluginInfos, ModalType.create);
    this.sourceProfileId  = _sourceProfileId;
    this.onSuccessfulSave = onSuccessfulSave;
  }

  performSave() {
    ElasticProfilesCRUD
      .create(this.elasticProfile())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(
              <span>
                      The elastic profile <em>{this.elasticProfile().id()}</em> was created successfully!
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

  modalTitle(elasticProfile: ElasticProfile) {
    return "Clone profile " + this.sourceProfileId;
  }
}

export class NewElasticProfileModal extends BaseElasticProfileModal {
  private readonly onSuccessfulSave: (msg: m.Children) => any;

  constructor(pluginInfos: Array<PluginInfo<Extension>>, onSuccessfulSave: (msg: m.Children) => any) {
    const elasticProfile = new ElasticProfile("", pluginInfos[0].id, new Configurations([]));
    super(elasticProfile, pluginInfos, ModalType.create);
    this.onSuccessfulSave = onSuccessfulSave;
  }

  performSave() {
    ElasticProfilesCRUD
      .create(this.elasticProfile())
      .then((result) => {
        result.do(
          () => {
            this.onSuccessfulSave(<span>The elastic profile <em>{this.elasticProfile().id()}</em> was created successfully!</span>);
            this.close();
          },
          (errorResponse) => {
            this.showErrors(result, errorResponse);
          }
        );
      });
  }

  modalTitle(elasticProfile: ElasticProfile) {
    return "Add a new profile";
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
      <span className={styles.tableCell}>{usage.pipelineName()}</span>,
      <span className={styles.tableCell}>{usage.stageName()}</span>,
      <span className={styles.tableCell}>{usage.jobName()}</span>,
      UsageElasticProfileModal.anchorToSettings(usage)
    ]);
    return (<Table headers={["Pipeline", "Stage", "Job", " "]} data={data}/>);
  }

  title() {
    return "Usages for " + this.profileId;
  }

  private static anchorToSettings(usage: ProfileUsage) {
    let link = `/go/admin/pipelines/${usage.pipelineName()}/stages/${usage.stageName()}/job/${usage.jobName()}/settings`;

    if (usage.templateName()) {
      link = `/go/admin/templates/${usage.templateName()}/stages/${usage.stageName()}/job/${usage.jobName()}/settings`;
    }

    return <span class={styles.jobSettingsLink}><a href={link}>Go to job settings</a></span>;
  }
}
