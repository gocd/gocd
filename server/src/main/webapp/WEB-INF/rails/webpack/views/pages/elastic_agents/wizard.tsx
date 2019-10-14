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
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import {LoremIpsum} from "lorem-ipsum";
import Nameable = PipelineStructureJSON.Nameable;
import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfile, ElasticAgentProfile} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {
  Clickable,
  HasChildren, PipelineStructure
} from "models/shared/pipeline_structure/pipeline_structure";
import {PipelineStructureJSON} from "models/shared/pipeline_structure/serialization";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {Default} from "views/components/buttons";
import {
  SearchField,
  SelectField,
  SelectFieldOptions,
  TextField,
  TriStateCheckboxField
} from "views/components/forms/input_fields";
import {Refresh} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {CloseListener, Step, Wizard} from "views/components/wizard";
import styles from "views/pages/elastic_agents/index.scss";
import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";

const foundationClassNames = bind(foundationStyles);
const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;

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
        <div>
          <h2>Cluster Profile</h2>
          <img src="https://placeimg.com/200/200/tech"/>
          <p>{new LoremIpsum().generateParagraphs(1)}</p>
        </div>
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
          <h2>Elastic Profile</h2>
          <img src="https://placeimg.com/200/200/tech"/>
          <p>{new LoremIpsum().generateParagraphs(1)}</p>
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

  constructor(pluginInfos: Stream<PluginInfos>,
              clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>) {
    super();
    this.pluginInfos    = pluginInfos;
    this.clusterProfile = clusterProfile;
    this.elasticProfile = elasticProfile;
  }

  body(): m.Children {
    return <NewClusterProfileWidget pluginInfos={this.pluginInfos}
                                    clusterProfile={this.clusterProfile}
                                    elasticProfile={this.elasticProfile}/>;
  }

  header(): m.Children {
    return "Cluster profile";
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
}

class AssociateToJobsStep extends Step {
  private readonly pipelineStructure: Stream<PipelineStructure>;

  constructor(pipelineStructure: Stream<PipelineStructure>) {
    super();
    this.pipelineStructure = pipelineStructure;
  }

  body(): m.Children {
    return (
      <div class={styles.container}>
        <div>
          <div class={styles.searchFieldContainer}>
            <div class={styles.selectPipelinesButtons}>
              Select pipelines
              <div>
                {/*The span is a hack to remove margin between buttons*/}
                <span><Default small={true}>All</Default></span>
                <span><Default small={true}>None</Default></span>
              </div>
            </div>
            <div>
              <SearchField property={Stream()} placeholder="Search pipelines"/>
            </div>
          </div>
          <div class={styles.pipelineStructureWrapper}>
            <div class={styles.pipelineStructure}>
              <div class={styles.pipelineStructureHeader}><h3>Pipeline Groups</h3><Refresh/></div>
              <div class={styles.pipelineStructureContent}>{this.renderPipelineGroups()}</div>
            </div>
            <div class={styles.pipelineStructure}>
              <div class={styles.pipelineStructureHeader}><h3>Templates</h3><Refresh/></div>
              <div class={styles.pipelineStructureContent}>{this.renderTemplates()}</div>
            </div>
          </div>
        </div>

        <div>
          <h2>Associate to jobs</h2>
          <img src="https://placeimg.com/200/200/tech"/>
          <p>{new LoremIpsum().generateParagraphs(1)}</p>
        </div>
      </div>
    );
  }

  header(): m.Children {
    return "Associate to jobs";
  }

  hasChildren(object: Nameable | HasChildren<Nameable>): object is HasChildren<Nameable> {
    return "children" in object;
  }

  private renderPipelineGroups() {
    return this.pipelineStructure().groups.map((pipelineGroup) => this.renderNode(pipelineGroup));
  }

  private renderTemplates() {
    return this.pipelineStructure().templates.map((pipelineGroup) => this.renderNode(pipelineGroup));
  }

  private renderNode(thing: Nameable & Clickable) {
    const children = this.hasChildren(thing) ? this.renderChildren(thing) : undefined;

    return (
      <div class={styles.checkbox}>
        <TriStateCheckboxField label={thing.name}
                               readonly={thing.readonly()}
                               helpText={thing.readonlyReason()}
                               onchange={thing.wasClicked.bind(thing)}
                               property={thing.checkboxState()}/>
        {children}
      </div>
    );
  }

  private renderChildren(thing: HasChildren<any>) {
    return thing.children.map((eachChild) => {
      return this.renderNode(eachChild);
    });
  }

}

class ReviewAndVerifyStep extends Step {
  private readonly clusterProfile: Stream<ClusterProfile>;
  private readonly elasticProfile: Stream<ElasticAgentProfile>;

  constructor(clusterProfile: Stream<ClusterProfile>,
              elasticProfile: Stream<ElasticAgentProfile>) {
    super();
    this.clusterProfile = clusterProfile;
    this.elasticProfile = elasticProfile;
  }

  body(): m.Children {
    return <div>
      {this.showClusterProfile()}
      {this.showElasticProfile()}
      {this.showPipelineStructure()}
    </div>;
  }

  header(): m.Children {
    return "Review and verify";
  }

  private showClusterProfile() {

    return (
      <div>
        <h2>Cluster Configuration</h2>
        <KeyValuePair
          data={new Map([["Id", this.clusterProfile().id()], ["Plugin ID", this.clusterProfile().pluginId()]])}/>
        <KeyValuePair data={this.clusterProfile().properties()!.asMap()}/>
      </div>
    );
  }

  private showElasticProfile() {
    return (
      <div>
        <h2>Elastic Profile Configuration</h2>
        <KeyValuePair
          data={new Map([["Id", this.elasticProfile().id()]])}/>

        <KeyValuePair data={this.elasticProfile().properties()!.asMap()}/>
      </div>
    );
  }

  private showPipelineStructure() {
    return (
      <div>
        <h2>Associated Jobs</h2>
      </div>
    );
  }
}

export function openWizard(pluginInfos: Stream<PluginInfos>,
                           clusterProfile: Stream<ClusterProfile>,
                           elasticProfile: Stream<ElasticAgentProfile>,
                           pipelineStructure: Stream<PipelineStructure>,
                           closeListener?: CloseListener) {

  let wizard = new Wizard()
    .addStep(new NewClusterProfileStep(pluginInfos, clusterProfile, elasticProfile))
    .addStep(new NewElasticProfileStep(pluginInfos, clusterProfile, elasticProfile))
    .addStep(new AssociateToJobsStep(pipelineStructure))
    .addStep(new ReviewAndVerifyStep(clusterProfile, elasticProfile));
  if (closeListener !== undefined) {
    wizard = wizard.setCloseListener(closeListener);
  }
  return wizard.render();
}
