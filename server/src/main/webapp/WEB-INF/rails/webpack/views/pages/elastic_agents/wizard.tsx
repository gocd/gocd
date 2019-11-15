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
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfile, ElasticAgentProfile} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import wizardButtonStyles from "views/components/buttons/wizard_buttons.scss";
import {ConceptDiagram} from "views/components/concept_diagram";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {PageLoadError} from "views/components/page_load_error";
import {Spinner} from "views/components/spinner";
import {CloseListener, Step, Wizard} from "views/components/wizard";
import styles from "views/pages/elastic_agents/index.scss";
import conceptDiagramStyles from "views/pages/elastic_agents/wizard_concept_diagrams.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";
import {PageState} from "views/pages/page";

const foundationClassNames = bind(foundationStyles);
const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;

const clusterProfileImg      = require("../../../../app/assets/images/elastic_agents/cluster_profile.svg");
const elasticAgentProfileImg = require("../../../../app/assets/images/elastic_agents/elastic_agent_profile.svg");

interface Attrs {
  pluginInfos: Stream<PluginInfos>;
  clusterProfile: Stream<ClusterProfile>;
  elasticProfile: Stream<ElasticAgentProfile>;
  readonly: boolean;
  etag?: string;
}

class ClusterProfileWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>) {
    const pluginList = _.map(vnode.attrs.pluginInfos(), (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    return (
      <div class={styles.container}>
        <div>
          <div class={styles.profileForm}>
            <div><TextField label="Cluster Profile Name"
                            readonly={vnode.attrs.etag !== undefined || vnode.attrs.readonly}
                            property={vnode.attrs.clusterProfile().id}
                            errorText={vnode.attrs.clusterProfile().errors().errorsForDisplay("id")}
                            required={true}/></div>

            <div><SelectField label="Plugin ID"
                              property={this.pluginIdProxy.bind(this, vnode)}
                              required={true}
                              readonly={vnode.attrs.readonly}
                              errorText={vnode.attrs.clusterProfile().errors().errorsForDisplay("pluginId")}>
              <SelectFieldOptions selected={vnode.attrs.clusterProfile().pluginId()}
                                  items={pluginList}/>
            </SelectField>
            </div>
          </div>
          <div>
            <div
              className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
              <div class="row collapse" data-test-id="properties-form">
                {this.clusterProfileForm(vnode)}
              </div>
            </div>
          </div>
        </div>
        <ConceptDiagram image={clusterProfileImg} css={conceptDiagramStyles}>
          <h2>What is a Cluster Profile?</h2>
          <p>A Cluster Profile is the connection configuration of the environment where Elastic Agents run. Typically,
            this includes the cluster connection URL, credentials, network, permission settings etc. Eg: Kubernetes
            Cluster Configurations.</p>
        </ConceptDiagram>
      </div>
    );
  }

  private pluginIdProxy(vnode: m.Vnode<Attrs>, newValue?: string) {
    if (newValue) {
      // if newValue is different from current value
      if (this.pluginInfoForCurrentClusterProfileOrDefaultToFirstPlugin(vnode).id !== newValue) {
        const newPluginInfo = this.findPluginWithId(vnode, newValue);

        vnode.attrs.clusterProfile().pluginId(newPluginInfo!.id);
        vnode.attrs.clusterProfile().properties(new Configurations([]));

        vnode.attrs.elasticProfile().pluginId(newPluginInfo!.id);
        vnode.attrs.elasticProfile().properties(new Configurations([]));
      }
    }
    return vnode.attrs.clusterProfile().pluginId();
  }

  private pluginInfoForCurrentClusterProfileOrDefaultToFirstPlugin(vnode: m.Vnode<Attrs>) {
    let result;
    if (vnode.attrs.clusterProfile() && vnode.attrs.clusterProfile().pluginId()) {
      const pluginId = vnode.attrs.clusterProfile().pluginId();
      result         = this.findPluginWithId(vnode, pluginId);
    }

    if (!result) {
      result = vnode.attrs.pluginInfos()[0];
    }

    return result;
  }

  private findPluginWithId(vnode: m.Vnode<Attrs>, pluginId?: string) {
    return vnode.attrs.pluginInfos().find((pluginInfo) => pluginInfo.id === pluginId);
  }

  private clusterProfileForm(vnode: m.Vnode<Attrs, this>) {
    const elasticAgentSettings = this.pluginInfoForCurrentClusterProfileOrDefaultToFirstPlugin(vnode)
                                     .extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS)!;
    return (
      <AngularPluginNew pluginInfoSettings={Stream(elasticAgentSettings.clusterProfileSettings)}
                        configuration={vnode.attrs.clusterProfile().properties()}
                        key={vnode.attrs.clusterProfile().pluginId()}/>
    );
  }
}

class ElasticProfileWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>) {
    const elasticAgentExtension        = this.pluginInfoForCurrentElasticProfileOrDefaultToFirstPlugin(vnode)
                                             .extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS)!;
    const elasticProfileConfigurations = elasticAgentExtension.profileSettings;

    return (
      <div class={styles.container}>
        <div>
          <div class={styles.profileForm}>
            <TextField label="Elastic Profile Name"
                       property={vnode.attrs.elasticProfile().id}
                       errorText={vnode.attrs.elasticProfile().errors().errorsForDisplay("id")}
                       readonly={vnode.attrs.etag !== undefined}
                       required={true}/>
          </div>
          <div>
            <div
              className={foundationClassNames(foundationStyles.foundationGridHax, foundationStyles.foundationFormHax)}>
              <div class="row collapse" data-test-id="properties-form">
                <AngularPluginNew
                  pluginInfoSettings={Stream(elasticProfileConfigurations)}
                  configuration={vnode.attrs.elasticProfile().properties()}
                  key={vnode.attrs.elasticProfile().pluginId}/>
              </div>
            </div>
          </div>
        </div>
        <div>
          <ConceptDiagram image={elasticAgentProfileImg} css={conceptDiagramStyles}>
            <h2>What is an Elastic Agent Profile?</h2>
            <p>An Elastic Agent Profile usually contains the configuration for your agent.<br/>
              Depending on the plugin used, this may contain the machine image (ami, docker image), size of the
              CPU/memory/disk, network settings among other things.</p>
          </ConceptDiagram>
        </div>
      </div>
    );
  }

  private findPluginWithId(vnode: m.Vnode<Attrs>, pluginId?: string) {
    return vnode.attrs.pluginInfos().find((pluginInfo) => pluginInfo.id === pluginId);
  }

  private pluginInfoForCurrentElasticProfileOrDefaultToFirstPlugin(vnode: m.Vnode<Attrs>) {
    let result;
    const pluginId = vnode.attrs.clusterProfile().pluginId();
    // console.log("plugin Id in elastic profile " + pluginId);
    if (vnode.attrs.elasticProfile() && pluginId) {
      result = this.findPluginWithId(vnode, pluginId);
    }

    if (!result) {
      result = vnode.attrs.pluginInfos()[0];
    }
    return result;
  }
}

enum Mode {
  ADD, EDIT
}

class ClusterProfileStep extends Step {
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly clusterProfile: Stream<ClusterProfile>;
  private readonly elasticProfile: Stream<ElasticAgentProfile>;
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onError: (msg: m.Children) => any;
  private footerError: Stream<string> = Stream("");
  private mode: Mode;
  private etag?: string;

  constructor(pluginInfos: Stream<PluginInfos>,
              clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>,
              onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any,
              mode: Mode = Mode.ADD) {
    super();
    this.pluginInfos      = pluginInfos;
    this.clusterProfile   = clusterProfile;
    this.elasticProfile   = elasticProfile;
    this.onSuccessfulSave = onSuccessfulSave;
    this.onError          = onError;
    this.mode             = mode;
  }

  body(): m.Children {
    return <ClusterProfileWidget pluginInfos={this.pluginInfos}
                                 clusterProfile={this.clusterProfile}
                                 elasticProfile={this.elasticProfile}
                                 etag={this.etag}
                                 readonly={this.mode === Mode.EDIT}/>;
  }

  header(): m.Children {
    return "Cluster profile";
  }

  footer(wizard: Wizard): m.Children {
    switch (this.mode) {
      case Mode.ADD:
        return [
          <Buttons.Cancel css={wizardButtonStyles} onclick={wizard.close.bind(wizard)}
                          data-test-id="cancel">Cancel</Buttons.Cancel>,
          <Buttons.Primary css={wizardButtonStyles} data-test-id="next"
                           onclick={this.saveClusterProfileAndNext.bind(this, wizard)}
                           align="right">Save + Next</Buttons.Primary>,
          <Buttons.Secondary css={wizardButtonStyles} data-test-id="save-cluster-profile"
                             onclick={this.saveClusterProfileAndExit.bind(this, wizard)}
                             align="right">Save Cluster Profile + Exit</Buttons.Secondary>,
          <span class={styles.footerError}>{this.footerError()}</span>,
        ];
      case Mode.EDIT:
        return [
          <Buttons.Cancel css={wizardButtonStyles} onclick={wizard.close.bind(wizard)}
                          data-test-id="cancel">Cancel</Buttons.Cancel>,
          <Buttons.Primary css={wizardButtonStyles} data-test-id="next"
                           onclick={() => wizard.next()}
                           align="right">Show Elastic Profile</Buttons.Primary>,
          <span class={styles.footerError}>{this.footerError()}</span>,
        ];
    }
  }

  saveClusterProfileAndExit(wizard: Wizard) {
    this.save()
        .then((result) => {
          result.do(
            (result) => {
              this.etag = result.body.etag;
              this.onSuccessfulSave(<span>The cluster profile <em>{this.clusterProfile().id()}</em> was created successfully!</span>);
              wizard.close();
            },
            (errorResponse) => {
              this.handleError(result, errorResponse);
            }
          );
        });
  }

  saveClusterProfileAndNext(wizard: Wizard) {
    this.save()
        .then((result) => {
          result.do(
            (successResponse) => {
              this.etag = successResponse.body.etag;
              this.clusterProfile(successResponse.body.object);
              wizard.next();
            },
            (errorResponse) => {
              this.handleError(result, errorResponse);
            }
          );
        });
  }

  private save() {
    if (this.etag) {
      return this.clusterProfile().update(this.etag);
    } else {
      return this.clusterProfile().create();
    }
  }

  private handleError(result: ApiResult<ObjectWithEtag<ClusterProfile>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.clusterProfile(ClusterProfile.fromJSON(json.data));
      this.footerError("Please fix the validation errors above before proceeding.");
    } else {
      this.onError(JSON.parse(errorResponse.body!).message);
    }
  }
}

class ElasticProfileStep extends Step {
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly clusterProfile: Stream<ClusterProfile>;
  private readonly elasticProfile: Stream<ElasticAgentProfile>;
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onError: (msg: m.Children) => any;
  private footerError: Stream<string> = Stream("");
  private readonly elasticProfileMode: Mode;
  private readonly clusterProfileMode: Mode;
  private etag?: string;
  private pageState: PageState        = PageState.OK;

  constructor(pluginInfos: Stream<PluginInfos>,
              clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>,
              onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any,
              elasticProfileMode: Mode = Mode.ADD,
              clusterProfileMode: Mode = Mode.ADD) {
    super();
    this.pluginInfos        = pluginInfos;
    this.clusterProfile     = clusterProfile;
    this.elasticProfile     = elasticProfile;
    this.onSuccessfulSave   = onSuccessfulSave;
    this.onError            = onError;
    this.elasticProfileMode = elasticProfileMode;
    this.clusterProfileMode = clusterProfileMode;
    if (this.elasticProfileMode === Mode.EDIT) {
      this.loadData();
    }
  }

  body(): m.Children {
    switch (this.pageState) {
      case PageState.LOADING:
        return <Spinner/>;
      case PageState.OK:
        return <ElasticProfileWidget pluginInfos={this.pluginInfos}
                                     elasticProfile={this.elasticProfile}
                                     clusterProfile={this.clusterProfile}
                                     readonly={false}
                                     etag={this.etag}/>;
      case PageState.FAILED:
        return <PageLoadError message="There was a problem fetching the Elastic Profile"/>;
    }
  }

  header(): m.Children {
    return "Elastic profile";
  }

  footer(wizard: Wizard): m.Children {
    if (this.clusterProfileMode === Mode.EDIT) {
      return [
        <Buttons.Cancel onclick={wizard.close.bind(wizard)} css={wizardButtonStyles}
                        data-test-id="cancel">Cancel</Buttons.Cancel>,
        <Buttons.Primary data-test-id="finish" align="right"
                         onclick={this.saveAndFinish.bind(this, wizard)}>Save</Buttons.Primary>,
        <Buttons.Primary data-test-id="previous" onclick={wizard.previous.bind(wizard, 1)}
                         align="right">Show Cluster Profile</Buttons.Primary>,
        <span class={styles.footerError}>{this.footerError()}</span>
      ];
    } else {
      return [
        <Buttons.Cancel onclick={wizard.close.bind(wizard)} css={wizardButtonStyles}
                        data-test-id="cancel">Cancel</Buttons.Cancel>,
        <Buttons.Primary data-test-id="finish" align="right" css={wizardButtonStyles}
                         onclick={this.saveAndFinish.bind(this, wizard)}>Finish</Buttons.Primary>,
        <Buttons.Primary data-test-id="previous" onclick={wizard.previous.bind(wizard, 1)} css={wizardButtonStyles}
                         align="right">Previous</Buttons.Primary>,
        <span class={styles.footerError}>{this.footerError()}</span>
      ];
    }
  }

  saveAndFinish(wizard: Wizard) {
    if (this.etag) {
      this.update(wizard, this.etag!);
    } else {
      this.create(wizard);
    }
  }

  private create(wizard: Wizard) {
    this.elasticProfile().clusterProfileId(this.clusterProfile().id());
    this.elasticProfile().create()
        .then((result) => {
          result.do(
            () => {
              this.onSuccessfulSave(<span>The Elastic Agent Profile <em>{this.elasticProfile().id()}</em> was created successfully!</span>);
              wizard.close();
            },
            (errorResponse) => {
              this.handleError(result, errorResponse);
            }
          );
        });
  }

  private update(wizard: Wizard, etag: string) {
    this.elasticProfile().update(etag)
        .then((result) => {
          result.do(
            () => {
              this.onSuccessfulSave(<span>The Elastic Agent Profile <em>{this.elasticProfile().id()}</em> was updated successfully!</span>);
              wizard.close();
            },
            (errorResponse) => {
              this.handleError(result, errorResponse);
            }
          );
        });
  }

  private handleError(result: ApiResult<ObjectWithEtag<ElasticAgentProfile>>, errorResponse: ErrorResponse) {
    if (result.getStatusCode() === 422 && errorResponse.body) {
      const json = JSON.parse(errorResponse.body);
      this.elasticProfile(ElasticAgentProfile.fromJSON(json.data));
      this.footerError("Please fix the validation errors above before proceeding.");
    } else {
      this.onError(JSON.parse(errorResponse.body!).message);
    }
  }

  private loadData() {
    this.pageState = PageState.LOADING;
    if (!this.elasticProfile().id() || !this.elasticProfile().pluginId()) {
      this.pageState = PageState.FAILED;
      return;
    }
    const pluginId = this.elasticProfile().pluginId() || "";
    this.elasticProfile().get().then((result) => {
      result.do(
        (successResponse) => {
          this.elasticProfile(successResponse.body.object);
          this.elasticProfile().pluginId(pluginId);
          this.etag      = successResponse.body.etag;
          this.pageState = PageState.OK;
        },
        ((errorResponse) => this.onError(JSON.parse(errorResponse.body!).message))
      );
    });
  }
}

export function openWizardForAdd(pluginInfos: Stream<PluginInfos>,
                                 clusterProfile: Stream<ClusterProfile>,
                                 elasticProfile: Stream<ElasticAgentProfile>,
                                 onSuccessfulSave: (msg: m.Children) => any,
                                 onError: (msg: m.Children) => any,
                                 closeListener?: CloseListener) {

  let wizard = new Wizard()
    .addStep(new ClusterProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError))
    .addStep(new ElasticProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError));

  wizard.allowHeaderClick = false;
  if (closeListener !== undefined) {
    wizard = wizard.setCloseListener(closeListener);
  }
  return wizard.render();
}

export function openWizardForEdit(pluginInfos: Stream<PluginInfos>,
                                  clusterProfile: Stream<ClusterProfile>,
                                  elasticProfile: Stream<ElasticAgentProfile>,
                                  onSuccessfulSave: (msg: m.Children) => any,
                                  onError: (msg: m.Children) => any,
                                  closeListener?: CloseListener) {

  let wizard = new Wizard()
    .addStep(new ClusterProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError,
                                    Mode.EDIT))
    .addStep(new ElasticProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError,
                                    Mode.EDIT, Mode.EDIT));

  if (closeListener !== undefined) {
    wizard = wizard.setCloseListener(closeListener);
  }
  return wizard.render();
}

export function openWizardForAddElasticProfile(pluginInfos: Stream<PluginInfos>,
                                               clusterProfile: Stream<ClusterProfile>,
                                               elasticProfile: Stream<ElasticAgentProfile>,
                                               onSuccessfulSave: (msg: m.Children) => any,
                                               onError: (msg: m.Children) => any,
                                               closeListener?: CloseListener) {

  let wizard = new Wizard()
    .addStep(new ClusterProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError,
                                    Mode.EDIT))
    .addStep(new ElasticProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError,
                                    Mode.ADD, Mode.EDIT));

  if (closeListener !== undefined) {
    wizard = wizard.setCloseListener(closeListener);
  }
  return wizard.render();
}
