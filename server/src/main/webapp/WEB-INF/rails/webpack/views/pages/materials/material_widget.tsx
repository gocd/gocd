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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {MaterialModification} from "models/config_repos/types";
import {MaterialWithFingerprint, PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/materials";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {MaterialHeaderWidget} from "./material_header_widget";
import {MaterialUsageWidget} from "./material_usage_widget";
import {MaterialVM} from "./models/material_view_model";

export interface MaterialAttrs {
  materialVM: MaterialVM;
}

interface MaterialWithInfoAttrs extends MaterialAttrs {
  shouldShowPackageOrScmLink: boolean;
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

    return <KeyValuePair data={attrs}/>;
  }

  view(vnode: m.Vnode<MaterialWithInfoAttrs, this>): m.Children | void | null {
    const vm                  = vnode.attrs.materialVM;
    const material            = vm.material;
    const modificationDetails = material.modification === null
      ? <FlashMessage type={MessageType.info}>This material was never parsed</FlashMessage>
      : MaterialWidget.showModificationDetails(material.modification);
    let actionButtons;

    const materialType = material.config.type();
    if (materialType === "package" || materialType === "plugin") {
      actionButtons = <IconGroup>
        <Edit data-test-id={"edit-material"} title={"Edit package"} onclick={vnode.attrs.onEdit.bind(this, material.config)}/>
      </IconGroup>;
    }

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
