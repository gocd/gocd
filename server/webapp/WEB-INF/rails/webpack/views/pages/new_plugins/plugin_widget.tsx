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

import * as m from "mithril";
import * as _ from "lodash";

import {MithrilViewComponent} from "../../../jsx/mithril-component";
import {CollapsiblePanel} from "../../components/collapsible_panel";
import {KeyValuePair} from "../../components/key_value_pair";
import * as Buttons from "../../components/buttons";
import * as Icons from "../../components/icons";

const Routes     = require('gen/js-routes');
const styles     = require('./index.scss');
const classnames = require('classnames/bind').bind(styles);

interface PluginHeaderAttrs {
  image: JSX.Element;
  pluginId: string
  pluginName: string;
  pluginVersion: string
}

class PluginHeaderWidget extends MithrilViewComponent<PluginHeaderAttrs> {
  view(vnode: m.Vnode<PluginHeaderAttrs>) {
    return [
      (
        <span class={classnames("plugin-icon")}>
          {vnode.attrs.image}
        </span>
      ),
      (
        <div class="plugin-name">{vnode.attrs.pluginName}</div>
      ),
      (
        <KeyValuePair inline={true} data={
          {
            'Version': vnode.attrs.pluginVersion,
            'Id':      vnode.attrs.pluginId
          }
        }/>
      )
    ];
  }
}

//todo: change this to pluginInfo:PluginInfo
export interface Attrs {
  pluginInfo: any,
  isUserAnAdmin: boolean
}

type OptionalElement = JSX.Element | undefined;

export class PluginWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const pluginInfo    = vnode.attrs.pluginInfo;
    const isUserAnAdmin = vnode.attrs.isUserAnAdmin;

    let statusReportButton: OptionalElement;
    let settingsButton: OptionalElement;

    if (this.doesPluginSupportStatusReport(pluginInfo)) {
      const statusReportPath: string = Routes.adminStatusReportPath(pluginInfo.id());

      statusReportButton = (
        <Buttons.Secondary onclick={this.goToStatusReportPage.bind(this, statusReportPath)}
                           disabled={!isUserAnAdmin}>
          Status Report
        </Buttons.Secondary>);
    }

    if (this.doesPluginSupportSettings(pluginInfo)) {
      settingsButton = <Icons.Settings/>
    }

    return (
      <CollapsiblePanel header={<PluginHeaderWidget image={this.createImageTag(pluginInfo)}
                                                    pluginName={pluginInfo.about().name()}
                                                    pluginVersion={pluginInfo.about().version()}
                                                    pluginId={pluginInfo.id()}/>}
                        actions={[statusReportButton, settingsButton]}>
        <KeyValuePair data={
          {
            'Description':                 pluginInfo.about().description(),
            'Author':                      this.getAuthorInfo(pluginInfo),
            'Supported operating systems': _.isEmpty(pluginInfo.about().targetOperatingSystems()) ? 'No restrictions' : pluginInfo.about().targetOperatingSystems(),
            'Plugin file location':        pluginInfo.pluginFileLocation(),
            'Bundled':                     pluginInfo.bundledPlugin() ? 'Yes' : 'No',
            'Target Go Version':           pluginInfo.about().targetGoVersion(),
          }
        }/>
      </CollapsiblePanel>
    );
  }

  private createImageTag(pluginInfo: any): JSX.Element {
    if (pluginInfo.imageUrl()) {
      return <img src={pluginInfo.imageUrl()}/>;
    }
    return <span class="unknown-plugin-icon"/>;
  }

  private getAuthorInfo(pluginInfo: any): JSX.Element {
    return (
      <a target="_blank" href={pluginInfo.about().vendor().url()}>
        {pluginInfo.about().vendor().name()}
      </a>
    );
  }

  private doesPluginSupportStatusReport(pluginInfo: any): boolean {
    const elasticAgentExtensionInfo = pluginInfo.extensions() && pluginInfo.extensions()['elastic-agent'];
    return elasticAgentExtensionInfo && elasticAgentExtensionInfo.capabilities() && elasticAgentExtensionInfo.capabilities().supportsStatusReport();
  }

  private goToStatusReportPage(statusReportHref: string): void {
    window.location.href = statusReportHref;
  }

  private doesPluginSupportSettings(pluginInfo: any): boolean {
    return pluginInfo.supportsPluginSettings();
  }

}

