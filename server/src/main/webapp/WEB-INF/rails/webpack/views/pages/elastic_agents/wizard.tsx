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
import {ClusterProfilesCRUD} from "models/elastic_profiles/cluster_profiles_crud";
import {ClusterProfile, ElasticAgentProfile} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ConceptDiagram} from "views/components/concept_diagram";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {CloseListener, Step, Wizard} from "views/components/wizard";
import styles from "views/pages/elastic_agents/index.scss";
import conceptDiagramStyles from "views/pages/elastic_agents/wizard_concept_diagrams.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);
const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;

const clusterProfileImg      = require("../../../../app/assets/images/elastic_agents/cluster_profile.svg");
const elasticAgentProfileImg = require("../../../../app/assets/images/elastic_agents/elastic_agent_profile.svg");

interface Attrs {
  pluginInfos: Stream<PluginInfos>;
  clusterProfile: Stream<ClusterProfile>;
  elasticProfile: Stream<ElasticAgentProfile>;
}

class NewClusterProfileWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>) {
    const pluginList = _.map(vnode.attrs.pluginInfos(), (pluginInfo: PluginInfo) => {
      return {id: pluginInfo.id, text: pluginInfo.about.name};
    });

    return (
      <div class={styles.container}>
        <div>
          <div class={styles.profileForm}>
            <div><TextField label="Cluster Profile Name"
                            property={vnode.attrs.clusterProfile().id}
                            errorText={vnode.attrs.clusterProfile().errors().errorsForDisplay("id")}
                            required={true}/></div>

            <div><SelectField label="Plugin ID"
                              property={this.pluginIdProxy.bind(this, vnode)}
                              required={true}
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

class NewElasticProfileWidget extends MithrilViewComponent<Attrs> {
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
                         required={true}/>
              {/*<SelectField label="Cluster Profile ID"*/}
              {/*property={this.clusterProfileIdProxy.bind(this)}*/}
              {/*required={true}*/}
              {/*errorText={vnode.attrs.elasticProfile().errors().errorsForDisplay("pluginId")}>*/}
              {/*<SelectFieldOptions selected={vnode.attrs.elasticProfile().clusterProfileId()}*/}
              {/*items={clustersList as any}/>*/}
              {/*</SelectField>*/}
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

class NewClusterProfileStep extends Step {
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly clusterProfile: Stream<ClusterProfile>;
  private readonly elasticProfile: Stream<ElasticAgentProfile>;
  private readonly onSuccessfulSave: (msg: m.Children) => any;
  private readonly onError: (msg: m.Children) => any;
  private footerError: Stream<string> = Stream("");

  constructor(pluginInfos: Stream<PluginInfos>,
              clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>,
              onSuccessfulSave: (msg: m.Children) => any,
              onError: (msg: m.Children) => any) {
    super();
    this.pluginInfos    = pluginInfos;
    this.clusterProfile = clusterProfile;
    this.elasticProfile = elasticProfile;
    this.onSuccessfulSave = onSuccessfulSave;
    this.onError = onError;
  }

  body(): m.Children {
    return <NewClusterProfileWidget pluginInfos={this.pluginInfos}
                                    clusterProfile={this.clusterProfile}
                                    elasticProfile={this.elasticProfile}/>;
  }

  header(): m.Children {
    return "Cluster profile";
  }

  footer(wizard: Wizard): m.Children {
    return [
      <Buttons.Cancel onclick={wizard.close.bind(wizard)}
                      data-test-id="cancel">Cancel</Buttons.Cancel>,
      <Buttons.Primary data-test-id="next" onclick={wizard.next.bind(wizard, 1)} align="right">Next</Buttons.Primary>,
      <Buttons.Reset data-test-id="save-cluster-profile" onclick={this.saveClusterProfileAndExit.bind(this, wizard)}
                     align="right">Save Cluster Profile and Exit</Buttons.Reset>,
      <span class={styles.footerError}>{this.footerError()}</span>,
    ];
  }

  saveClusterProfileAndExit(wizard: Wizard) {
    ClusterProfilesCRUD.create(this.clusterProfile())
                       .then((result) => {
                         result.do(
                           () => {
                             this.onSuccessfulSave(<span>The cluster profile <em>{this.clusterProfile().id()}</em> was created successfully!</span>);
                             wizard.close();
                           },
                           (errorResponse) => {
                             this.handleError(result, errorResponse);
                           }
                         );
                       });
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

class NewElasticProfileStep extends Step {
  private readonly pluginInfos: Stream<PluginInfos>;
  private readonly clusterProfile: Stream<ClusterProfile>;
  private readonly elasticProfile: Stream<ElasticAgentProfile>;

  constructor(pluginInfos: Stream<PluginInfos>,
              clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>) {
    super();
    this.pluginInfos    = pluginInfos;
    this.clusterProfile = clusterProfile;
    this.elasticProfile = elasticProfile;
  }

  body(): m.Children {
    return <NewElasticProfileWidget pluginInfos={this.pluginInfos}
                                    elasticProfile={this.elasticProfile}
                                    clusterProfile={this.clusterProfile}/>;
  }

  header(): m.Children {
    return "Elastic profile";
  }

  footer(wizard: Wizard): m.Children {
    return [
      <Buttons.Cancel onclick={wizard.close.bind(wizard)}
                      data-test-id="cancel">Cancel</Buttons.Cancel>,
      <Buttons.Primary data-test-id="finish" align="right">Finish</Buttons.Primary>,
      <Buttons.Primary data-test-id="next" onclick={wizard.previous.bind(wizard, 1)}
                       align="right">Previous</Buttons.Primary>,
    ];
  }
}

export function openWizard(pluginInfos: Stream<PluginInfos>,
                           clusterProfile: Stream<ClusterProfile>,
                           elasticProfile: Stream<ElasticAgentProfile>,
                           onSuccessfulSave: (msg: m.Children) => any,
                           onError: (msg: m.Children) => any,
                           closeListener?: CloseListener) {

  let wizard = new Wizard()
    .addStep(new NewClusterProfileStep(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError))
    .addStep(new NewElasticProfileStep(pluginInfos, clusterProfile, elasticProfile));
  if (closeListener !== undefined) {
    wizard = wizard.setCloseListener(closeListener);
  }
  return wizard.render();
}
