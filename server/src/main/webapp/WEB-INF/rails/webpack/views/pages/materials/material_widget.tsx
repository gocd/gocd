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

import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter} from "helpers/time_formatter";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialModification} from "models/config_repos/types";
import {MaterialWithFingerprint, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Analytics, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {AdditionalInfoAttrs} from "views/pages/materials";
import styles from "./index.scss";
import {MaterialHeaderWidget} from "./material_header_widget";
import {MaterialUsageWidget} from "./material_usage_widget";
import {MaterialVM} from "./models/material_view_model";

export interface MaterialAttrs {
  materialVM: MaterialVM;
}

interface MaterialWithInfoAttrs extends MaterialAttrs, AdditionalInfoAttrs {
}

export class MaterialWidget extends MithrilViewComponent<MaterialWithInfoAttrs> {

  public static showModificationDetails(modification: MaterialModification) {
    const attrs = new Map();
    attrs.set("Username", modification.username);
    attrs.set("Email", modification.emailAddress);
    attrs.set("Revision", modification.revision);
    attrs.set("Comment", <EllipseText text={modification.comment}/>);
    attrs.set("Modified Time", <span
      title={timeFormatter.formatInServerTime(modification.modifiedTime)}>{timeFormatter.format(modification.modifiedTime)}</span>);

    return attrs;
  }

  view(vnode: m.Vnode<MaterialWithInfoAttrs, this>): m.Children | void | null {
    const vm                  = vnode.attrs.materialVM;
    const material            = vm.material;
    const config              = material.config;
    const modificationDetails = material.modification === null
      ? <FlashMessage type={MessageType.info}>This material was never parsed</FlashMessage>
      : <KeyValuePair data={MaterialWidget.showModificationDetails(material.modification)}/>;

    let maybeEditButton;

    const materialType = config.type();
    if (materialType === "package" || materialType === "plugin") {
      maybeEditButton = <Edit data-test-id={"edit-material"} title={"Edit package"}
                              onclick={vnode.attrs.onEdit.bind(this, config)}/>;
    }
    const actionButtons = <IconGroup>
      {maybeEditButton}
      <Analytics data-test-id={"show-modifications-material"} title={"Show Modifications"}
                 onclick={vnode.attrs.showModifications.bind(this, config)}/>
    </IconGroup>;

    return <CollapsiblePanel header={<MaterialHeaderWidget {...vnode.attrs} />}
                             actions={actionButtons}
                             onexpand={() => vm.notify("expand")}>
      <MaterialUsageWidget materialVM={vm}/>
      <h3>Latest Modification Details</h3>
      <div data-test-id="latest-modification-details" className={headerStyles.configRepoProperties}>
        {modificationDetails}
      </div>
      <h3>Material Attributes</h3>
      <KeyValuePair data-test-id={"material-attributes"} data={this.getMaterialData(material.config, vnode.attrs.shouldShowPackageOrScmLink)}/>
    </CollapsiblePanel>;
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
}

interface EllipseAttrs {
  text: string;
}

interface EllipseState {
  expanded: Stream<boolean>;
  setExpandedTo: (state: boolean, e: MouseEvent) => void;
}

class EllipseText extends MithrilComponent<EllipseAttrs, EllipseState> {
  private static MIN_CHAR_COUNT = 80;

  oninit(vnode: m.Vnode<EllipseAttrs, EllipseState>): any {
    vnode.state.expanded = Stream();
    vnode.state.expanded(false);

    vnode.state.setExpandedTo = (state: boolean) => {
      vnode.state.expanded(state);
    };
  }

  view(vnode: m.Vnode<EllipseAttrs, EllipseState>): m.Children | void | null {
    const charactersToShow = Math.min(this.getCharCountToShow(vnode), vnode.attrs.text.length);
    if (vnode.attrs.text.length <= EllipseText.MIN_CHAR_COUNT) {
      return <span>{vnode.attrs.text}</span>;
    }
    return <span class={styles.ellipseWrapper}
                 data-test-id="ellipsized-content">
      {vnode.state.expanded() ? vnode.attrs.text : EllipseText.getEllipsizedString(vnode, charactersToShow)}
      {vnode.state.expanded() ? EllipseText.element(vnode, "less", false) : EllipseText.element(vnode, "more", true)}
      </span>;
  }

  private static getEllipsizedString(vnode: m.Vnode<EllipseAttrs, EllipseState>, charactersToShow: number) {
    return vnode.attrs.text.substr(0, charactersToShow).concat("...");
  }

  private static element(vnode: m.Vnode<EllipseAttrs, EllipseState>, text: string, state: boolean) {
    return <span data-test-id={`ellipse-action-${text}`} class={styles.ellipsisActionButton}
                 onclick={vnode.state.setExpandedTo.bind(this, state)}>{text}</span>;
  }

  private getCharCountToShow(vnode: m.Vnode<EllipseAttrs, EllipseState>) {
    return (vnode.attrs.text.includes('\n') ? vnode.attrs.text.indexOf('\n') : EllipseText.MIN_CHAR_COUNT);
  }
}
