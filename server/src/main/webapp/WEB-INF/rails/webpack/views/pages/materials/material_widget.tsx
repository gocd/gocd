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

import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {MaterialModification} from "models/config_repos/types";
import {MaterialMessage, MaterialMessages, MaterialWithFingerprint, MaterialWithModification, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Edit, IconGroup, List, Refresh, Usage} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {AdditionalInfoAttrs} from "views/pages/materials";
import styles from "./index.scss";
import {MaterialHeaderWidget} from "./material_header_widget";

export interface MaterialAttrs {
  material: MaterialWithModification;
}

interface MaterialWithInfoAttrs extends MaterialAttrs, AdditionalInfoAttrs {
}

export class MaterialWidget extends MithrilViewComponent<MaterialWithInfoAttrs> {

  public static showModificationDetails(modification: MaterialModification) {
    const attrs = new Map();
    attrs.set("Username", modification.username);
    attrs.set("Email", modification.emailAddress);
    attrs.set("Revision", modification.revision);
    attrs.set("Comment", modification.comment);
    attrs.set("Modified Time", <span
      title={timeFormatter.formatInServerTime(modification.modifiedTime)}>{timeFormatter.format(modification.modifiedTime)}</span>);

    return attrs;
  }

  view(vnode: m.Vnode<MaterialWithInfoAttrs, this>): m.Children | void | null {
    const material          = vnode.attrs.material;
    const config            = material.config;
    let modificationDetails = <FlashMessage type={MessageType.info}>This material was never parsed</FlashMessage>;
    if (material.modification !== null) {
      const modDetails = MaterialWidget.showModificationDetails(material.modification);
      modDetails.set('Comment', <div class={styles.comment}>{modDetails.get('Comment')}</div>);
      modificationDetails = <KeyValuePair data={modDetails}/>;
    }

    const modificationIconTitle = material.modification === null
      ? "No modifications to show"
      : "Show Modifications";
    const actionButtons         = <IconGroup>
      <Refresh data-test-id={"trigger-update"} title={this.getTriggerUpdateTitle(material)}
               disabled={!material.canTriggerUpdate || material.materialUpdateInProgress} onclick={vnode.attrs.triggerUpdate.bind(this, material)}/>
      {this.getEditButton(vnode, config)}
      <Usage data-test-id={"show-usages"} title={"Show Usages"} onclick={vnode.attrs.showUsages.bind(this, config)}/>
      <List data-test-id={"show-modifications-material"} title={modificationIconTitle} disabled={material.modification === null}
            onclick={vnode.attrs.showModifications.bind(this, config)}/>
    </IconGroup>;

    const inProgressIcon = material.materialUpdateInProgress
      ? <span className={headerStyles.configRepoUpdateInProgress} data-test-id="material-update-in-progress"/>
      : undefined;

    const hasErrors   = !_.isEmpty(material.messages.errors());
    const hasWarnings = hasErrors ? false : !_.isEmpty(material.messages.warnings());
    return <CollapsiblePanel header={<MaterialHeaderWidget {...vnode.attrs} />}
                             actions={[inProgressIcon, actionButtons]}
                             error={hasErrors} warning={hasWarnings} expanded={hasErrors}>
      {this.renderMessages(material.messages)}
      <h3>Latest Modification Details</h3>
      <div data-test-id="latest-modification-details" className={headerStyles.configRepoProperties}>
        {modificationDetails}
      </div>
      <h3>Material Attributes</h3>
      <KeyValuePair data-test-id={"material-attributes"} data={this.getMaterialData(material.config, vnode.attrs.shouldShowPackageOrScmLink)}/>
    </CollapsiblePanel>;
  }

  private getTriggerUpdateTitle(material: MaterialWithModification): string {
    return material.canTriggerUpdate
      ? material.materialUpdateInProgress
        ? material.materialUpdateStartTime
          ? `Update in progress since ${timeFormatter.format(material.materialUpdateStartTime)}`
          : "Update in progress"
        : "Trigger Update"
      : "You do not have permission to trigger an update for this material";
  }

  private getMaterialData(material: MaterialWithFingerprint, shouldShowPackageOrScmLink: boolean): Map<string, m.Children> {
    let map = new Map();
    if (material.type() === "package") {
      const pkgAttrs = material.attributes() as PackageMaterialAttributes;

      const pkgName = shouldShowPackageOrScmLink
        ? <Link href={SparkRoutes.packageRepositoriesSPA(pkgAttrs.packageRepoName(), pkgAttrs.packageName())}>
          {pkgAttrs.packageName()}
        </Link>
        : pkgAttrs.packageName();

      map.set("Ref", pkgName);
    } else if (material.type() === "plugin") {
      const pluginAttrs = material.attributes() as PluggableScmMaterialAttributes;

      const value = shouldShowPackageOrScmLink
        ? <Link href={SparkRoutes.pluggableScmSPA(pluginAttrs.scmName())}>
          {pluginAttrs.scmName()}
        </Link>
        : pluginAttrs.scmName();

      map.set("Ref", value);
    } else {
      map = material.attributesAsMap();
    }
    return map;
  }

  private renderMessages(messages: MaterialMessages): m.Children {
    if (!messages.hasMessages()) {
      return;
    }
    const data: Map<string, m.Children> = new Map<string, m.Children>();
    if (!_.isEmpty(messages.warnings())) {
      const warningsData = messages.warnings().map(this.renderMessage);
      data.set("Warnings", <div data-test-id="warnings" class={styles.warnings}>{warningsData}</div>);
    }
    if (!_.isEmpty(messages.errors())) {
      const errorsData = messages.errors().map(this.renderMessage);
      data.set("Errors", <div data-test-id="errors" class={styles.errors}>{errorsData}</div>);
    }
    return [
      <h3>Messages</h3>,
      <KeyValuePair data-test-id={"messages"} data={data}/>
    ];
  }

  private renderMessage(msg: MaterialMessage, index: number) {
    return <div data-test-id={`message-${index}`}>
      <span>{msg.message}</span>
      <p>{msg.description}</p>
    </div>;
  }

  private getEditButton(vnode: m.Vnode<MaterialWithInfoAttrs, this>, config: MaterialWithFingerprint) {
    const materialType = config.type();
    if (materialType === "package") {
      return <Edit data-test-id={"edit-material"} title={"Edit package"}
                   onclick={vnode.attrs.onEdit.bind(this, config)}/>;
    } else if (materialType === "plugin") {
      const attrs = config.attributes() as PluggableScmMaterialAttributes;
      const title = attrs.origin().isDefinedInConfigRepo()
        ? `Cannot edit material as it is defined in config repo '${attrs.origin().id()}'`
        : "Edit pluggable material";
      return <Edit data-test-id={"edit-material"} title={title}
                   disabled={attrs.origin().isDefinedInConfigRepo()}
                   onclick={vnode.attrs.onEdit.bind(this, config)}/>;
    }
  }
}
