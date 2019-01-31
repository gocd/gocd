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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ConfigRepo, humanizedMaterialAttributeName, ParseInfo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {Configuration} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";
import {Code} from "views/components/code";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Delete, Edit, IconGroup, Refresh} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {Spinner} from "views/components/spinner";
import {
  DeleteOperation,
  EditOperation,
  RefreshOperation, RequiresPluginInfos
} from "views/pages/page_operations";
import * as styles from "./index.scss";

const yamlIcon   = require("./yaml.svg");
const jsonIcon   = require("./json.svg");
const groovyIcon = require("./groovy.svg");

interface Operations<T> extends EditOperation<T>, DeleteOperation<T>, RefreshOperation<T> {
}

export interface Attrs<T> extends Operations<T>, RequiresPluginInfos {
  objects: Stream<T[] | null>;
}

interface ShowObjectAttrs<T> extends Operations<T>, RequiresPluginInfos {
  obj: T;
  index: number;
}

interface HeaderWidgetAttrs extends RequiresPluginInfos {
  repo: ConfigRepo;
}

function findPluginWithId(infos: Array<PluginInfo<any>>, pluginId: string) {
  return _.find(infos, {id: pluginId});
}

class StatusIcon extends MithrilViewComponent<{ name: string }> {
  view(vnode: m.Vnode<{ name: string }, this>) {
    return (
      <div className={styles.statusIcon}>
        {vnode.children}
      </div>
    );
  }

}

class HeaderWidget extends MithrilViewComponent<HeaderWidgetAttrs> {
  view(vnode: m.Vnode<HeaderWidgetAttrs>): m.Children | void | null {

    return [
      (
        <KeyValueTitle image={this.pluginIcon(vnode)} title={vnode.attrs.repo.id()}/>
      ),
      (
        <KeyValuePair inline={true} data={new Map([
                                                    ["Plugin Id", vnode.attrs.repo.pluginId()]
                                                  ])}/>
      )
    ];
  }

  private pluginIcon(vnode: m.Vnode<HeaderWidgetAttrs>) {
    const pluginInfo = findPluginWithId(vnode.attrs.pluginInfos(), vnode.attrs.repo.pluginId());

    if (!pluginInfo) {
      return <HeaderIcon name="Unknown plugin"/>;
    }

    switch (pluginInfo!.id) {
      case "yaml.config.plugin":
        return <HeaderIcon imageUrl={yamlIcon}/>;
      case "json.config.plugin":
        return <HeaderIcon imageUrl={jsonIcon}/>;
      case "cd.go.contrib.plugins.configrepo.groovy":
        return <HeaderIcon imageUrl={groovyIcon}/>;
      default:
        return <HeaderIcon name="Plugin does not have an icon"/>;
    }
  }
}

class ConfigRepoWidget extends MithrilViewComponent<ShowObjectAttrs<ConfigRepo>> {
  view(vnode: m.Vnode<ShowObjectAttrs<ConfigRepo>>): m.Children | void | null {

    const materialNameAttribute = new Map([["Type", vnode.attrs.obj.material().type()]]);
    const filteredAttributes    = _.reduce(vnode.attrs.obj.material().attributes(),
                                           this.resolveKeyValueForAttribute,
                                           materialNameAttribute);
    const allAttributes         = _.reduce(vnode.attrs.obj.configuration(),
                                           (accumulator: Map<string, string>,
                                            value: Configuration) => this.resolveKeyValueForAttribute(accumulator,
                                                                                                      value.value,
                                                                                                      value.key),
                                           filteredAttributes);

    const refreshButton = (
      <Refresh data-test-id="config-repo-refresh" onclick={vnode.attrs.onRefresh.bind(vnode.attrs, vnode.attrs.obj)}/>
    );

    const editButton = (
      <Edit data-test-id="config-repo-edit" onclick={vnode.attrs.onEdit.bind(vnode.attrs, vnode.attrs.obj)}/>
    );

    const deleteButton = (
      <Delete data-test-id="config-repo-delete" onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.obj)}/>
    );

    const actionButtons = [
      this.statusIcon(vnode),
      <IconGroup>
        {refreshButton}
        {editButton}
        {deleteButton}
      </IconGroup>];

    const parseInfo = vnode.attrs.obj.lastParse();

    let maybeWarning: m.Children;

    if (_.isEmpty(parseInfo)) {
      maybeWarning = (
        <FlashMessage type={MessageType.warning}>This configuration repository was never parsed.</FlashMessage>
      );
    } else if (parseInfo && parseInfo.error()) {
      maybeWarning = (
        <FlashMessage type={MessageType.warning}>
          There was an error parsing this configuration repository:
          <Code>{parseInfo.error}</Code>
        </FlashMessage>
      );
    }

    const pluginInfo = findPluginWithId(vnode.attrs.pluginInfos(), vnode.attrs.obj.pluginId());

    if (!pluginInfo) {
      maybeWarning = (
        <FlashMessage type={MessageType.alert}>This plugin is missing.</FlashMessage>
      );
    }

    return (
      <CollapsiblePanel header={<HeaderWidget repo={vnode.attrs.obj} pluginInfos={vnode.attrs.pluginInfos}/>}
                        actions={actionButtons} expanded={vnode.attrs.index === 0}>
        {maybeWarning}
        {this.latestModification(parseInfo)}
        {this.lastGoodModification(parseInfo)}
        {this.configRepoMetaConfig(vnode.attrs.obj.id(), vnode.attrs.obj.pluginId())}
        {this.materialConfig(allAttributes)}
      </CollapsiblePanel>
    );
  }

  private resolveKeyValueForAttribute(accumulator: Map<string, string>, value: any, key: string) {
    if (key.startsWith("__") || key === "autoUpdate") {
      return accumulator;
    }

    let renderedValue = value;

    const renderedKey = humanizedMaterialAttributeName(key);

    // test for value being a stream
    if (_.isFunction(value)) {
      value = value();
    }

    // test for value being an EncryptedPassword
    if (value && value.valueForDisplay) {
      renderedValue = value.valueForDisplay();
    }
    accumulator.set(renderedKey, _.isFunction(renderedValue) ? renderedValue() : renderedValue);
    return accumulator;
  }

  private statusIcon(vnode: m.Vnode<ShowObjectAttrs<ConfigRepo>>) {
    const pluginInfo = findPluginWithId(vnode.attrs.pluginInfos(), vnode.attrs.obj.pluginId());

    if (!pluginInfo) {
      return <StatusIcon name="Unknown plugin">
        <span className={styles.missingPluginIcon}
              title={`This plugin is not installed or is not configured properly.`}/>
      </StatusIcon>;
    }

    let parseInfo = vnode.attrs.obj.lastParse();
    if (_.isEmpty(parseInfo)) {
      return (
        <StatusIcon name="Never Parsed">
          <span className={styles.neverParsed}
                title={`This configuration repository was never parsed.`}/>
        </StatusIcon>
      );
    }

    parseInfo = parseInfo as ParseInfo;
    if (!parseInfo.error() && parseInfo.goodModification != null) {
      return (
        <StatusIcon name="Last Parse Good">
          <span className={styles.goodLastParseIcon}
                title={`Last parsed with revision ${parseInfo.goodModification.revision}`}/>
        </StatusIcon>
      );
    } else {
      const title: string = (parseInfo.latestParsedModification != null)
        ? `Last parsed with revision ${parseInfo.latestParsedModification.revision}. The error was ${parseInfo.error}`
        : `Error: ${parseInfo.error}`;

      return (
        <StatusIcon name="Last Parse Error">
          <span className={styles.lastParseErrorIcon}
                title={title}/>
        </StatusIcon>
      );
    }
  }

  private lastGoodModification(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.goodModification) {
      const attrs = this.resolveHumanReadableAttributes(parseInfo.goodModification);
      const checkIcon = <span className={styles.goodModificationIcon}
                              title={`Last parsed with revision ${parseInfo.goodModification.revision}`}/>;
      return [
        <KeyValueTitle title={"Good Modification"} image={checkIcon}/>,
        <KeyValuePair data={attrs}/>
      ];
    }
  }

  private latestModification(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.latestParsedModification) {
      const attrs = this.resolveHumanReadableAttributes(parseInfo.latestParsedModification);
      let statusIcon = styles.goodModificationIcon;

      if (parseInfo.error()) {
        attrs.set("Error", parseInfo.error());
        statusIcon = styles.errorLastModificationIcon;
      }

      return [
        <KeyValueTitle title={"Latest Modification"}
                       image={<span className={statusIcon}
                                    title={`Last parsed with revision ${parseInfo.latestParsedModification.revision}`}/>}/>,
        <KeyValuePair data={attrs}/>
      ];
    }
  }

  private resolveHumanReadableAttributes(obj: object) {
    const attrs = new Map();
    const keys = Object.keys(obj).map(humanizedMaterialAttributeName);
    const values = Object.values(obj);

    keys.forEach((key, index) => {
      attrs.set(key, values[index]);
    });

    return attrs;
  }

  private configRepoMetaConfig(id: string, pluginId: string) {
    return [
      <KeyValueTitle title={"Config Repository Configurations"} image={undefined}/>,
      <KeyValuePair data={new Map([["Id", id], ["Plugin Id", pluginId]])}/>
    ];
  }

  private materialConfig(allAttributes: Map<string, m.Children>) {
    return [
      <KeyValueTitle title={"Material"} image={undefined}/>,
      <KeyValuePair data = {allAttributes}/>
    ];
  }
}

export class ConfigReposWidget extends MithrilViewComponent<Attrs<ConfigRepo>> {
  view(vnode: m.Vnode<Attrs<ConfigRepo>>): m.Children | void | null {
    if (!vnode.attrs.objects()) {
      return <Spinner/>;
    }

    const configRepos = (vnode.attrs.objects() as ConfigRepo[]);
    if (configRepos.length === 0) {
      return (
        <FlashMessage type={MessageType.info}>
          There are no config repositories setup. Click the "Add" button to add one.
        </FlashMessage>);
    }

    return (
      <div>
        {configRepos.map((configRepo, index) => {
          return (
            <ConfigRepoWidget key={configRepo.id()}
                              obj={configRepo}
                              pluginInfos={vnode.attrs.pluginInfos}
                              index={index}
                              onEdit={(configRepo, e) => vnode.attrs.onEdit(configRepo, e)}
                              onRefresh={(configRepo, e) => vnode.attrs.onRefresh(configRepo, e)}
                              onDelete={(configRepo, e) => vnode.attrs.onDelete(configRepo, e)}
            />
          );
        })}
      </div>
    );
  }
}
