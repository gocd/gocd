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

import * as Routes from "gen/ts-routes";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {ButtonIcon} from "views/components/buttons";
import * as Buttons from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";

interface PluginHeaderAttrs {
  image: JSX.Element;
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
  pluginInfo: PluginInfo<any>;
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

    if (pluginInfo.supportsStatusReport()) {
      const statusReportPath: string = Routes.adminStatusReportPath(pluginInfo.id);

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

    let pluginData = new Map<string, string | JSX.Element>([
      ["Id", pluginInfo.id],
      ["Description", pluginInfo.about.description],
      ["Author", this.getAuthorInfo(pluginInfo)],
      ["Supported operating systems", pluginInfo.about.targetOperatingSystemsDisplayValue()],
      ["Plugin file location", pluginInfo.pluginFileLocation],
      ["Bundled", pluginInfo.bundledPlugin ? "Yes" : "No"],
      ["Target Go Version", pluginInfo.about.targetGoVersion],
    ]);
    if (pluginInfo.hasErrors()) {
      pluginData = pluginData.set("There were errors loading the plugin", pluginInfo.getErrors());
    }

    return (
      <CollapsiblePanel dataTestId="plugin-row"
                        header={<PluginHeaderWidget image={<HeaderIcon imageUrl={pluginInfo.imageUrl}/>}
                                                    pluginName={pluginInfo.about.name}
                                                    pluginVersion={pluginInfo.about.version}
                                                    pluginId={pluginInfo.id}/>}
                        actions={[statusReportButton, settingsButton]}
                        error={pluginInfo.hasErrors()}
                        expanded={pluginInfo.status.isInvalid()}>
        <KeyValuePair data={pluginData}/>
      </CollapsiblePanel>
    );
  }

  private getAuthorInfo(pluginInfo: PluginInfo<any>): JSX.Element {
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
}
