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
import {KeyValuePair} from "views/components/key_value_pair";
import {Spinner} from "views/components/spinner";
import {
  DeleteOperation,
  EditOperation,
  RefreshOperation, RequiresPluginInfos
} from "views/pages/page_operations";
import * as styles from "./index.scss";

export interface SearchOperation<T> {
  initialObjects: Stream<T[]>;
  searchText: Stream<string>;
  filteredObjects: () => Stream<T[]>;
}

export interface Operations<T> extends EditOperation<T>, DeleteOperation<T>, RefreshOperation<T> {
}

export interface Attrs<T> extends Operations<T>, RequiresPluginInfos {
  objects: Stream<T[]>;
}

interface ShowObjectAttrs<T> extends Operations<T>, RequiresPluginInfos {
  obj: T;
}

interface HeaderWidgetAttrs extends RequiresPluginInfos {
  repo: ConfigRepo;
}

function findPluginWithId(infos: Array<PluginInfo<any>>, pluginId: string) {
  return _.find(infos, {id: pluginId});
}

class HeaderWidget extends MithrilViewComponent<HeaderWidgetAttrs> {
  private static readonly MAX_COMMIT_MSG_LENGTH: number            = 84;
  private static readonly MAX_USERNAME_AND_REVISION_LENGTH: number = 40;

  view(vnode: m.Vnode<HeaderWidgetAttrs>): m.Children | void | null {
    const materialUrl = vnode.attrs.repo.material().materialUrl();
    return [
      this.pluginIcon(vnode),
      <div class={styles.headerTitle}>
        <h4 className={styles.headerTitleText}>{vnode.attrs.repo.id()}</h4>
        <span className={styles.headerTitleUrl}>{materialUrl}</span>
      </div>,
      <div>{this.latestCommitDetails(vnode.attrs.repo.lastParse())}</div>
    ];
  }

  private latestCommitDetails(lastParsedCommit: ParseInfo | null) {
    let parseStatus: m.Children = "This config repository was never parsed";

    if (lastParsedCommit && lastParsedCommit.latestParsedModification) {
      const latestParsedModification = lastParsedCommit.latestParsedModification;
      const comment                  = _.truncate(latestParsedModification.comment,
                                                  {length: HeaderWidget.MAX_COMMIT_MSG_LENGTH});
      const username                 = _.truncate(latestParsedModification.username,
                                                  {length: HeaderWidget.MAX_USERNAME_AND_REVISION_LENGTH});
      const revision                 = _.truncate(latestParsedModification.revision,
                                                  {length: HeaderWidget.MAX_USERNAME_AND_REVISION_LENGTH});

      parseStatus = (
        <div class={styles.commitInfo}>
          <span className={styles.comment}>
          {comment}
          </span>
          <div class={styles.committerInfo}>
            <span className={styles.committer}>{username}</span> | {revision}</div>
        </div>
      );
    }

    return parseStatus;
  }

  private pluginIcon(vnode: m.Vnode<HeaderWidgetAttrs>) {
    const pluginInfo = findPluginWithId(vnode.attrs.pluginInfos(), vnode.attrs.repo.pluginId());
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    } else {
      return <HeaderIcon name="Plugin does not have an icon"/>;
    }
  }
}

interface SectionHeaderAttrs {
  title: m.Children;
  image: m.Children;
  titleTestId?: string;
}

class SectionHeader extends MithrilViewComponent<SectionHeaderAttrs> {
  view(vnode: m.Vnode<SectionHeaderAttrs>) {
    return <h3 className={styles.sectionHeader} data-test-id={vnode.attrs.titleTestId}>
      {vnode.attrs.image}
      <span class={styles.sectionHeaderTitle}>{vnode.attrs.title}</span>
    </h3>;
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

    const statusIcon = vnode.attrs.obj.materialUpdateInProgress() ?
      <span className={styles.configRepoUpdateInProgress} data-test-id="repo-update-in-progress-icon"/> : "";

    const actionButtons = [
      statusIcon,
      <IconGroup>
        {refreshButton}
        {editButton}
        {deleteButton}
      </IconGroup>];

    const parseInfo = vnode.attrs.obj.lastParse();

    let maybeWarning: m.Children;

    if (_.isEmpty(parseInfo)) {
      maybeWarning = (
        <FlashMessage type={MessageType.alert}>This configuration repository was never parsed.</FlashMessage>
      );
    } else if (parseInfo && parseInfo.error() && !parseInfo.latestParsedModification) {
      maybeWarning = (
        <FlashMessage type={MessageType.alert}>
          There was an error parsing this configuration repository:
          <Code>{parseInfo.error}</Code>
        </FlashMessage>
      );
    }

    const pluginInfo = findPluginWithId(vnode.attrs.pluginInfos(), vnode.attrs.obj.pluginId());

    if (!pluginInfo) {
      maybeWarning = (
        <div className={styles.errorMessage}>
          <FlashMessage type={MessageType.alert}>This plugin is missing.</FlashMessage>
        </div>
      );
    }

    const configRepoHasErrors = maybeWarning || (vnode.attrs.obj.lastParse() && vnode.attrs.obj.lastParse()!.error());

    return (
      <CollapsiblePanel error={!!configRepoHasErrors}
                        header={<HeaderWidget repo={vnode.attrs.obj} pluginInfos={vnode.attrs.pluginInfos}/>}
                        dataTestId={"config-repo-details-panel"}
                        actions={actionButtons} expanded={!!configRepoHasErrors}>
        {maybeWarning}
        {this.latestModificationDetails(parseInfo)}
        {this.lastGoodModificationDetails(parseInfo)}
        {this.configRepoMetaConfigDetails(vnode.attrs.obj.id(), vnode.attrs.obj.pluginId())}
        {this.materialConfigDetails(allAttributes)}
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

  private lastGoodModificationDetails(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.goodModification) {
      const attrs     = this.resolveHumanReadableAttributes(parseInfo.goodModification);
      const checkIcon = <span className={styles.goodModificationIcon}
                              title={`Last parsed with revision ${parseInfo.goodModification.revision}`}/>;
      return <div data-test-id="config-repo-good-modification-panel">
        <SectionHeader title={"Last known good commit currently being used"} image={checkIcon}/>
        <div className={styles.configRepoProperties}><KeyValuePair data={attrs}/></div>
      </div>;
    }
  }

  private latestModificationDetails(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.latestParsedModification) {
      const attrs    = this.resolveHumanReadableAttributes(parseInfo.latestParsedModification);
      let statusIcon = styles.goodModificationIcon;

      if (parseInfo.error()) {
        attrs.set("Error", <code class={styles.parseErrorText}>{parseInfo.error()}</code>);
        statusIcon = styles.errorLastModificationIcon;
      }

      return <div data-test-id="config-repo-latest-modification-panel">
        <SectionHeader title={"Latest commit in the repository"}
                       image={<span className={statusIcon}
                                    title={`Last parsed with revision ${parseInfo.latestParsedModification.revision}`}/>}/>
        <div class={styles.configRepoProperties}><KeyValuePair data={attrs}/></div>
      </div>;
    }
  }

  private configRepoMetaConfigDetails(id: string, pluginId: string) {
    return <div data-test-id="config-repo-plugin-panel">
      <SectionHeader title={"Config Repository Configurations"} image={undefined}/>
      <div className={styles.configRepoProperties}>
        <KeyValuePair data={new Map([["Id", id], ["Plugin Id", pluginId]])}/>
      </div>
    </div>;
  }

  private materialConfigDetails(allAttributes: Map<string, m.Children>) {
    return <div data-test-id="config-repo-material-panel">
      <SectionHeader title={"Material"} image={undefined}/>
      <div className={styles.configRepoProperties}><KeyValuePair data={allAttributes}/></div>
    </div>;
  }

  private resolveHumanReadableAttributes(obj: object) {
    const attrs  = new Map();
    const keys   = Object.keys(obj).map(humanizedMaterialAttributeName);
    const values = Object.values(obj);

    keys.forEach((key, index) => attrs.set(key, values[index]));
    return attrs;
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
        {configRepos.map((configRepo) => {
          return (
            <ConfigRepoWidget key={configRepo.id()}
                              obj={configRepo}
                              pluginInfos={vnode.attrs.pluginInfos}
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
