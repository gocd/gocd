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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigRepo, ParseInfo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {Anchor, ScrollManager} from "views/components/anchor/anchor";
import {Code} from "views/components/code";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Delete, Edit, IconGroup, Refresh} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {RequiresPluginInfos} from "views/pages/page_operations";
import {allAttributes, resolveHumanReadableAttributes} from "./config_repo_attribute_helper";
import {CRResult} from "./config_repo_result";
import {ConfigRepoVM, CRVMAware} from "./config_repo_view_model";
import styles from "./index.scss";

interface CollectionAttrs extends RequiresPluginInfos {
  models: Stream<ConfigRepoVM[]>;
  sm: ScrollManager;
}

interface SingleAttrs extends CRVMAware {
  pluginInfo?: PluginInfo<any>;
  sm: ScrollManager;
}

class HeaderWidget extends MithrilViewComponent<SingleAttrs> {
  private static readonly MAX_COMMIT_MSG_LENGTH: number            = 84;
  private static readonly MAX_USERNAME_AND_REVISION_LENGTH: number = 40;

  view(vnode: m.Vnode<SingleAttrs>): m.Children | void | null {
    const repo = vnode.attrs.vm.repo;
    const materialUrl = repo.material()!.materialUrl();
    return [
      this.pluginIcon(vnode.attrs.pluginInfo),
      <div class={styles.headerTitle}>
        <h4 data-test-id="config-repo-header" class={styles.headerTitleText}>{repo.id()}</h4>
        <span class={styles.headerTitleUrl}>{materialUrl}</span>
      </div>,
      <div>{this.latestCommitDetails(repo.lastParse()!)}</div>
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
          <span class={styles.comment}>
          {comment}
          </span>
          <div class={styles.committerInfo}>
            <span class={styles.committer}>{username}</span> | {revision}</div>
        </div>
      );
    }

    return parseStatus;
  }

  private pluginIcon(pluginInfo?: PluginInfo<any>) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    } else {
      return <HeaderIcon name="Plugin does not have an icon"/>;
    }
  }
}

interface SectionHeaderAttrs {
  title: m.Children;
  image?: m.Children;
  titleTestId?: string;
}

class SectionHeader extends MithrilViewComponent<SectionHeaderAttrs> {
  view(vnode: m.Vnode<SectionHeaderAttrs>) {
    return <h3 class={styles.sectionHeader} data-test-id={vnode.attrs.titleTestId}>
      {vnode.attrs.image}
      <span class={styles.sectionHeaderTitle}>{vnode.attrs.title}</span>
    </h3>;
  }
}

interface ActionsAttrs extends CRVMAware {
  inProgress: boolean;
}

class CRPanelActions extends MithrilViewComponent<ActionsAttrs> {
  view(vnode: m.Vnode<ActionsAttrs>): m.Children {
    const vm = vnode.attrs.vm;
    const statusIcon = vnode.attrs.inProgress ?
      <span class={styles.configRepoUpdateInProgress} data-test-id="repo-update-in-progress-icon"/> : null;

    return [
      statusIcon,
      <IconGroup>
        <Refresh data-test-id="config-repo-refresh" onclick={vm.reparseRepo}/>
        <Edit data-test-id="config-repo-edit" onclick={vm.showEditModal}/>
        <Delete data-test-id="config-repo-delete" onclick={vm.showDeleteModal}/>
      </IconGroup>];
  }
}

interface WarningAttrs {
  parseInfo: ParseInfo | null;
  pluginInfo?: PluginInfo<any>;
}

class MaybeWarning extends MithrilViewComponent<WarningAttrs> {
  view(vnode: m.Vnode<WarningAttrs>) {
    const parseInfo = vnode.attrs.parseInfo;
    const pluginInfo = vnode.attrs.pluginInfo;

    if (!pluginInfo) {
      return <div class={styles.errorMessage}>
        <FlashMessage type={MessageType.alert} message="This plugin is missing."/>
      </div>;
    } else {
      if (_.isEmpty(parseInfo)) {
        return <FlashMessage type={MessageType.info} message="This configuration repository has not been parsed yet."/>;
      } else if (parseInfo && parseInfo.error() && !parseInfo.latestParsedModification) {
        return <FlashMessage type={MessageType.alert}>
          There was an error parsing this configuration repository:
          <Code>{parseInfo.error()}</Code>
        </FlashMessage>;
      }
    }
  }
}

class ConfigRepoWidget extends MithrilComponent<SingleAttrs> {
  expanded: Stream<boolean> = Stream();

  oninit(vnode: m.Vnode<SingleAttrs, {}>) {
    const {sm, vm, pluginInfo} = vnode.attrs;
    const repo = vm.repo;
    const parseInfo = repo.lastParse();
    const linked = sm.getTarget() === repo.id();

    // set the initial state of the collapsible panel; alternative to setting `expanded` attribute
    // and, perhaps, more obvious that this is only matters for first load
    this.expanded(linked || !pluginInfo || _.isEmpty(parseInfo) || !!parseInfo!.error());
  }

  view(vnode: m.Vnode<SingleAttrs>): m.Children | void | null {
    const {sm, vm, pluginInfo} = vnode.attrs;
    const repo = vm.repo;
    const parseInfo = repo.lastParse()!;
    const maybeWarning = <MaybeWarning parseInfo={parseInfo} pluginInfo={pluginInfo}/>;
    const configRepoHasErrors = !pluginInfo || _.isEmpty(parseInfo) || !!parseInfo!.error();

    return <Anchor id={repo.id()!} sm={sm} onnavigate={() => this.expanded(true)}>
      <CollapsiblePanel error={configRepoHasErrors}
                        header={<HeaderWidget {...vnode.attrs}/>}
                        dataTestId={"config-repo-details-panel"}
                        actions={<CRPanelActions inProgress={repo.materialUpdateInProgress()} vm={vm}/>}
                        vm={this}
                        onexpand={() => vm.notify("expand")}>
        {maybeWarning}

        {this.renderedConfigs(parseInfo, vm)}
        {this.latestModificationDetails(parseInfo)}
        {this.lastGoodModificationDetails(parseInfo)}
        {this.configRepoMetaConfigDetails(repo.id()!, repo.pluginId()!)}
        {this.materialConfigDetails(repo)}
      </CollapsiblePanel>
    </Anchor>;
  }

  private renderedConfigs(parseInfo: ParseInfo | null, vm: ConfigRepoVM): m.Children {
    if (parseInfo) {
      if (this.expanded()) {
        vm.notify("expand");
      }
      return <CRResult vm={vm} />;
    }
  }

  private lastGoodModificationDetails(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.goodRevision() === parseInfo.latestRevision()) {
      // it's redundant to print this when good === latest
      return;
    }

    if (parseInfo && parseInfo.goodModification) {
      const attrs     = resolveHumanReadableAttributes(parseInfo.goodModification);
      const checkIcon = <span class={styles.goodModificationIcon}
                              title={`Last parsed with revision ${parseInfo.goodModification.revision}`}/>;
      return <div data-test-id="config-repo-good-modification-panel">
        <SectionHeader title="Last known good commit currently being used" image={checkIcon}/>
        <div class={styles.configRepoProperties}><KeyValuePair data={attrs}/></div>
      </div>;
    }
  }

  private latestModificationDetails(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.latestParsedModification) {
      const attrs    = resolveHumanReadableAttributes(parseInfo.latestParsedModification);
      let statusIcon = styles.goodModificationIcon;

      if (parseInfo.error()) {
        attrs.set("Error", <code class={styles.parseErrorText}>{parseInfo.error()}</code>);
        statusIcon = styles.errorLastModificationIcon;
      }

      return <div data-test-id="config-repo-latest-modification-panel">
        <SectionHeader title={"Latest commit in the repository"}
                       image={<span class={statusIcon}
                                    title={`Last parsed with revision ${parseInfo.latestParsedModification.revision}`}/>}/>
        <div class={styles.configRepoProperties}><KeyValuePair data={attrs}/></div>
      </div>;
    }
  }

  private configRepoMetaConfigDetails(id: string, pluginId: string) {
    return <div data-test-id="config-repo-plugin-panel">
      <SectionHeader title="Config Repository Configurations"/>
      <div class={styles.configRepoProperties}>
        <KeyValuePair data={new Map([["Id", id], ["Plugin Id", pluginId]])}/>
      </div>
    </div>;
  }

  private materialConfigDetails(repo: ConfigRepo) {
    return <div data-test-id="config-repo-material-panel">
      <SectionHeader title="Material"/>
      <div class={styles.configRepoProperties}><KeyValuePair data={allAttributes(repo)}/></div>
    </div>;
  }
}

export class ConfigReposWidget extends MithrilViewComponent<CollectionAttrs> {
  view(vnode: m.Vnode<CollectionAttrs>): m.Children | void | null {
    const models = vnode.attrs.models();

    if (!models.length) {
      return (
        <FlashMessage type={MessageType.info}>
          There are no config repositories setup. Click the "Add" button to add one.
        </FlashMessage>);
    }

    return <div>
      {models.map((vm: any) => {
        const repo = vm.repo;
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: repo.pluginId()});
        return <ConfigRepoWidget key={repo.id()} vm={vm} pluginInfo={pluginInfo} sm={vnode.attrs.sm}/>;
      })}
    </div>;
  }
}
