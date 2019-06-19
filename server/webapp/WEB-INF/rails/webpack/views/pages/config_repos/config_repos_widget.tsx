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
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ConfigRepo, humanizedMaterialAttributeName, ParseInfo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {Code} from "views/components/code";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Delete, Edit, IconGroup, Refresh} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Spinner} from "views/components/spinner";
import {RequiresPluginInfos} from "views/pages/page_operations";
import {CRResult} from "./config_repo_result";
import {CRVMAware, CRWidgetVM, Operations} from "./config_repo_view_model";
import * as styles from "./index.scss";

export interface SearchOperation<T> {
  initialObjects: Stream<T[]>;
  searchText: Stream<string>;
  filteredObjects: () => Stream<T[]>;
}

interface CollectionAttrs extends Operations, RequiresPluginInfos {
  objects: Stream<ConfigRepo[]>;
}

interface SingleAttrs extends Operations, RequiresPluginInfos {
  repo: ConfigRepo;
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

interface ActionsAttrs {
  inProgress: boolean;
  vm: CRWidgetVM;
}

class CRPanelActions extends MithrilViewComponent<ActionsAttrs> {
  view(vnode: m.Vnode<ActionsAttrs>): m.Children {
    const vm = vnode.attrs.vm;
    const statusIcon = vnode.attrs.inProgress ?
      <span className={styles.configRepoUpdateInProgress} data-test-id="repo-update-in-progress-icon"/> : null;

    return [
      statusIcon,
      <IconGroup>
        <Refresh data-test-id="config-repo-refresh" onclick={vm.onRefresh}/>
        <Edit data-test-id="config-repo-edit" onclick={vm.onEdit}/>
        <Delete data-test-id="config-repo-delete" onclick={vm.onDelete}/>
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
      return <div className={styles.errorMessage}>
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

class ConfigRepoWidget extends MithrilComponent<SingleAttrs, CRVMAware> {
  oninit(vnode: m.Vnode<SingleAttrs, CRVMAware>) {
    vnode.state.vm = new CRWidgetVM(vnode.attrs.repo, vnode.attrs);
  }

  view(vnode: m.Vnode<SingleAttrs, CRVMAware>): m.Children | void | null {
    const vm = vnode.state.vm;
    const repo = vnode.attrs.repo;
    const parseInfo = repo.lastParse();
    const pluginInfo = vm.findPluginWithId(vnode.attrs.pluginInfos(), repo.pluginId());
    const maybeWarning = <MaybeWarning parseInfo={parseInfo} pluginInfo={pluginInfo}/>;
    const configRepoHasErrors = !pluginInfo || _.isEmpty(parseInfo) || !!parseInfo!.error();

    return (
      <CollapsiblePanel error={configRepoHasErrors}
                        header={<HeaderWidget repo={repo} pluginInfos={vnode.attrs.pluginInfos}/>}
                        dataTestId={"config-repo-details-panel"}
                        actions={<CRPanelActions inProgress={repo.materialUpdateInProgress()} vm={vnode.state.vm}/>}
                        expanded={configRepoHasErrors} onexpand={() => vm.notify("expand")}>
        {maybeWarning}
        <CRResult repo={repo.id()} vm={vm}/>

        {this.latestModificationDetails(parseInfo)}
        {this.lastGoodModificationDetails(parseInfo)}
        {this.configRepoMetaConfigDetails(repo.id(), repo.pluginId())}
        {this.materialConfigDetails(vm.allAttributes())}
      </CollapsiblePanel>
    );
  }

  private lastGoodModificationDetails(parseInfo: ParseInfo | null): m.Children {
    if (parseInfo && parseInfo.goodRevision() === parseInfo.latestRevision()) {
      // it's redundant to print this when good === latest
      return;
    }

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

export class ConfigReposWidget extends MithrilViewComponent<CollectionAttrs> {
  view(vnode: m.Vnode<CollectionAttrs>): m.Children | void | null {
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
                              repo={configRepo}
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
