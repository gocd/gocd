/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import styles from "./index.scss";

interface PluginHeaderAttrs {
  image: m.Children;
  pluginId: string;
  pluginName: string;
  pluginVersion: string;
}

class PluginHeaderWidget extends MithrilViewComponent<PluginHeaderAttrs> {
  view(vnode: m.Vnode<PluginHeaderAttrs>) {
    const data = new Map([
                           ["Version", vnode.attrs.pluginVersion],
                         ]);
    return [
      (
        <KeyValueTitle image={vnode.attrs.image} titleTestId="plugin-name" title={vnode.attrs.pluginName}/>
      ),
      (
        <KeyValuePair inline={true} data={data}/>
      )
    ];
  }
}

export interface Attrs {
  pluginInfo: PluginInfo;
  isUserAnAdmin: boolean;
  onEdit: (e: MouseEvent) => void;
}

type OptionalElement = m.Children;

export class PluginWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pluginInfo    = vnode.attrs.pluginInfo;
    const isUserAnAdmin = vnode.attrs.isUserAnAdmin;

    let statusReportButton: OptionalElement;
    let settingsButton: OptionalElement;
    let deprecationWarningButton: OptionalElement;

    if (pluginInfo.supportsStatusReport()) {
      const statusReportPath: string = SparkRoutes.pluginStatusReportPath(pluginInfo.id);

      statusReportButton = (
        <Buttons.Secondary onclick={this.goToStatusReportPage.bind(this, statusReportPath)}
                           data-test-id="status-report-link"
                           icon={ButtonIcon.DOC}
                           disabled={!isUserAnAdmin}>
          Status Report
        </Buttons.Secondary>);
    }

    if (pluginInfo.supportsPluginSettings()) {
      settingsButton = <Icons.Settings data-test-id="edit-plugin-settings"
                                       disabled={!isUserAnAdmin}
                                       onclick={vnode.attrs.onEdit.bind(vnode.attrs)}/>;
    }

    if (this.deprecatedPluginInfo(pluginInfo)) {
      const content            = <p>Version {pluginInfo.about.version} of plugin is deprecated as it does not support <a
        onclick={(e: MouseEvent) => this.goToClusterProfileDocs(e)} href={"#"}>ClusterProfiles</a>. This version of plugin will stop
        working in upcoming release of GoCD, update to latest version of the plugin.</p>;
      deprecationWarningButton = <PluginDeprecationWarning content={content}/>;
    }

    let pluginData = new Map<string, string | m.Children>([
                                                            ["Id", pluginInfo.id],
                                                            ["Description", pluginInfo.about.description],
                                                            ["Author", this.getAuthorInfo(pluginInfo)],
                                                            ["Supported operating systems", pluginInfo.about.targetOperatingSystemsDisplayValue()],
                                                            ["Plugin file location", pluginInfo.pluginFileLocation],
                                                            ["Bundled", pluginInfo.bundledPlugin ? "Yes" : "No"],
                                                            ["Target GoCD Version", pluginInfo.about.targetGoVersion],
                                                          ]);
    if (pluginInfo.hasErrors()) {
      pluginData = pluginData.set("There were errors loading the plugin", pluginInfo.getErrors());
    }

    return (
      <CollapsiblePanel dataTestId="plugin-row"
                        header={<PluginHeaderWidget
                          image={<HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>}
                          pluginName={pluginInfo.about.name}
                          pluginVersion={pluginInfo.about.version}
                          pluginId={pluginInfo.id}/>}
                        actions={[deprecationWarningButton, statusReportButton, settingsButton]}
                        error={pluginInfo.hasErrors()}
                        warning={this.hasWarnings(pluginInfo)}
                        expanded={pluginInfo.status.isInvalid()}>
        <KeyValuePair data={pluginData}/>
      </CollapsiblePanel>
    );
  }

  private getAuthorInfo(pluginInfo: PluginInfo): m.Children {
    return (
      <a target="_blank" href={pluginInfo.about.vendor.url}>
        {pluginInfo.about.vendor.name}
      </a>
    );
  }

  private goToStatusReportPage(statusReportHref: string, event: Event): void {
    event.stopPropagation();
    window.location.href = statusReportHref;
  }

  private deprecatedPluginInfo(pluginInfo: PluginInfo) {
    const elasticAgentExtension = pluginInfo.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS) as ElasticAgentExtension;
    return elasticAgentExtension && !elasticAgentExtension.supportsClusterProfiles;
  }

  private goToClusterProfileDocs(event: Event): void {
    event.stopPropagation();
    window.open(docsUrl("configuration/elastic_agents.html"), "_blank");
  }

  private hasWarnings(pluginInfo: PluginInfo): boolean {
    return this.deprecatedPluginInfo(pluginInfo);
  }
}

interface PluginDeprecationWarningAttrs {
  content: m.Children;
}

class PluginDeprecationWarning extends MithrilViewComponent<PluginDeprecationWarningAttrs> {
  view(vnode: m.Vnode<PluginDeprecationWarningAttrs>) {
    return (
      <div data-test-id="deprecation-warning-tooltip-wrapper" class={styles.deprecationWarningTooltipWrapper}>
        <i data-test-id={"deprecation-warning-icon"} class={styles.deprecationWarningIcon}/>
        <div data-test-id="deprecation-warning-tooltip-content" class={styles.deprecationWarningTooltipContent}>
          {vnode.attrs.content}
        </div>
      </div>);
  }
}
