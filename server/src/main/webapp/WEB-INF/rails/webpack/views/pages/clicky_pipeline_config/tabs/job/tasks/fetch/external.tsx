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

import {bind} from "classnames/bind";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {FetchTaskAttributes} from "models/pipeline_configs/task";
import {ArtifactExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {ArtifactIdAutocompletionProvider} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/artifact_id_autocompletion_provider";
import {UpstreamJobToFetchArtifactFromWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch/upstream_job_info_to_fetch_artifact_from_widget";

import * as foundationStyles from "views/pages/new_plugins/foundation_hax.scss";
import styles from "../fetch.scss";

const AngularPluginNew     = require("views/shared/angular_plugin_new").AngularPluginNew;
const foundationClassNames = bind(foundationStyles);

export interface Attrs {
  attributes: FetchTaskAttributes;
  autoSuggestions: Stream<any>;
  artifactPluginInfos: PluginInfos;
  readonly: boolean;
}

export interface State {
  artifactIdSuggestions: ArtifactIdAutocompletionProvider;
  pluginId: Stream<string | undefined>;
}

export class ExternalFetchArtifactView extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.artifactIdSuggestions = new ArtifactIdAutocompletionProvider(
      vnode.attrs.attributes.pipeline,
      vnode.attrs.attributes.stage,
      vnode.attrs.attributes.job,
      vnode.attrs.autoSuggestions
    );

    vnode.state.pluginId = Stream();
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const onJobSuggestionChange = vnode.state.artifactIdSuggestions.update.bind(vnode.state.artifactIdSuggestions);

    if (vnode.attrs.artifactPluginInfos.length === 0) {
      const msg = "Can not create/edit external fetch artifact task as No Artifact Plugins are installed.";
      return <FlashMessage type={MessageType.info} message={msg}/>;
    }

    return (<div data-test-id="external-fetch-artifact-view">
      <UpstreamJobToFetchArtifactFromWidget {...vnode.attrs} onJobSuggestionChange={onJobSuggestionChange}/>
      {this.getExternalFetchArtifactView(vnode)}
    </div>);
  }

  private getExternalFetchArtifactView(vnode: m.Vnode<Attrs, State>): m.Children {
    const pluginIdDeterminedFrom = this.getArtifactPluginId(vnode);
    const allArtifactPluginIds   = vnode.attrs.artifactPluginInfos.map(p => p.id);

    let couldNotAutoSelectPluginError: string | undefined;
    // if plugin id can not be determined based on the specified artifact id..
    if (!pluginIdDeterminedFrom) {
      if (!vnode.state.pluginId()) {
        vnode.state.pluginId(allArtifactPluginIds[0]);
      }
      couldNotAutoSelectPluginError = "The plugin with which the artifact is associated cannot be determined because: the pipeline, stage, job or artifact id is a parameter or is non-existent. Please choose a plugin to configure the plugin properties.";
    } else {
      vnode.state.pluginId(pluginIdDeterminedFrom);
    }

    const pluginInfo        = vnode.attrs.artifactPluginInfos.findByPluginId(pluginIdDeterminedFrom || vnode.state.pluginId())!;
    const artifactExtension = pluginInfo.extensionOfType(ExtensionTypeString.ARTIFACT)! as ArtifactExtension;

    const attributes         = vnode.attrs.attributes;
    const artifactIdHelpText = "The id of the external artifact uploaded by the upstream job.";

    let optionalPluginIdDropdown: m.Children;

    if (attributes.artifactId()) {
      optionalPluginIdDropdown = (<SelectField property={vnode.state.pluginId}
                                               label="Plugin Id"
                                               errorText={couldNotAutoSelectPluginError}
                                               readonly={vnode.attrs.readonly || !!pluginIdDeterminedFrom}>
        <SelectFieldOptions selected={vnode.state.pluginId()}
                            items={allArtifactPluginIds}/>
      </SelectField>);
    }

    return (<div data-test-id="plugin-configuration">
      <div class={styles.artifactIdPluginIdGroup}>
        <AutocompleteField helpText={artifactIdHelpText}
                           readonly={vnode.attrs.readonly}
                           autoEvaluate={!vnode.attrs.readonly}
                           required={true}
                           errorText={attributes.errors().errorsForDisplay("artifactId")}
                           provider={vnode.state.artifactIdSuggestions}
                           label="Artifact Id"
                           property={attributes.artifactId}/>
        {optionalPluginIdDropdown}
      </div>
      <div className={`${foundationClassNames(foundationStyles.foundationGridHax,
                                              foundationStyles.foundationFormHax)}`}>
        <AngularPluginNew pluginInfoSettings={Stream(artifactExtension.fetchArtifactSettings)}
                          key={vnode.state.pluginId()}
                          disabled={vnode.attrs.readonly}
                          configuration={vnode.attrs.attributes.configuration()}/>
      </div>
    </div>);
  }

  private getArtifactPluginId(vnode: m.Vnode<Attrs, State>) {
    const attrs      = vnode.attrs.attributes;
    const pipeline   = attrs.pipeline();
    const stage      = attrs.stage();
    const job        = attrs.job();
    const artifactId = attrs.artifactId();

    return _.get(vnode.attrs.autoSuggestions(), `${pipeline}.${stage}.${job}.${artifactId}`);
  }
}
