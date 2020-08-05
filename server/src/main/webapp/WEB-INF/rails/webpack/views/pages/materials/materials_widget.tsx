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

import {docsUrl} from "gen/gocd_version";
import {SparkRoutes} from "helpers/spark_routes";
import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {humanizedMaterialAttributeName, MaterialModification} from "models/config_repos/types";
import {MaterialWithFingerprint} from "models/materials/materials";
import {Scms} from "models/materials/pluggable_scm";
import {PackageMaterialAttributes, PluggableScmMaterialAttributes} from "models/materials/types";
import {Packages} from "models/package_repositories/package_repositories";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {AdditionalInfoAttrs, MaterialsAttrs} from "views/pages/materials";
import styles from "./index.scss";
import {MaterialHeaderWidget} from "./material_header_widget";
import {MaterialUsageWidget} from "./material_usage_widget";
import {MaterialVM} from "./models/material_view_model";

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {

  static helpText() {
    return <span>
      A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered".
      <Link href={docsUrl("introduction/concepts_in_go.html#materials")} target={"_blank"} externalLinkIcon={true}> Learn More</Link>
    </span>;
  }

  view(vnode: m.Vnode<MaterialsAttrs>) {
    if (vnode.attrs.materialVMs().length === 0) {
      return <div>
        <FlashMessage type={MessageType.info}>
          Either no pipelines have been set up or you are not authorized to view the same.&nbsp;
          <Link href={docsUrl("configuration/dev_authorization.html#specifying-permissions-for-pipeline-groups")} target="_blank"
                externalLinkIcon={true}>Learn More</Link>
        </FlashMessage>
        <div data-test-id="materials-help" class={styles.help}>
          {MaterialsWidget.helpText()}
        </div>
      </div>;
    }
    return <div data-test-id="materials-widget">
      {vnode.attrs.materialVMs().map((materialVM) => <MaterialWidget materialVM={materialVM} scms={vnode.attrs.scms}
                                                                     packages={vnode.attrs.packages}/>)}
    </div>;
  }
}

export interface MaterialAttrs {
  materialVM: MaterialVM;
}

export interface MaterialWithInfoAttrs extends MaterialAttrs, AdditionalInfoAttrs {
}

export class MaterialWidget extends MithrilViewComponent<MaterialWithInfoAttrs> {
  view(vnode: m.Vnode<MaterialWithInfoAttrs, this>): m.Children | void | null {
    const vm       = vnode.attrs.materialVM;
    const material = vm.material;

    return <CollapsiblePanel header={<MaterialHeaderWidget {...vnode.attrs} />} onexpand={() => vm.notify("expand")}>
      <MaterialUsageWidget materialVM={vm}/>
      <h3>Latest Modification Details</h3>
      {this.showLatestModificationDetails(material.modification)}
      <h3>Material Attributes</h3>
      <KeyValuePair data-test-id={"material-attributes"} data={this.getMaterialData(material.config, vnode.attrs.packages(), vnode.attrs.scms())}/>
    </CollapsiblePanel>;
  }

  private getMaterialData(material: MaterialWithFingerprint, packages: Packages, scms: Scms): Map<string, m.Children> {
    let map = new Map();
    if (material.type() === "package") {
      const pkgAttrs = material.attributes() as PackageMaterialAttributes;
      const pkgInfo  = packages.find((pkg) => pkg.id() === pkgAttrs.ref())!;

      const link = <Link href={SparkRoutes.packageRepositoriesSPA(pkgInfo.packageRepo().name(), pkgInfo.name())}>
        {pkgInfo.name()}
      </Link>;

      map.set("Ref", link);
    } else if (material.type() === "plugin") {
      const pluginAttrs = material.attributes() as PluggableScmMaterialAttributes;
      const scmMaterial = scms.find((scm) => scm.id() === pluginAttrs.ref())!;

      const value = <Link href={SparkRoutes.pluggableScmSPA(scmMaterial.name())}>
        {scmMaterial.name()}
      </Link>;

      map.set("Ref", value);
    } else {
      map = material.attributesAsMap();
    }
    return map;
  }

  private showLatestModificationDetails(modification: MaterialModification | null) {
    if (modification === null) {
      return <FlashMessage type={MessageType.info}>This material was never parsed</FlashMessage>;
    }
    const attrs = this.resolveAttrsIntoReadableFormat(modification);
    this.setDateInReadableFormat(attrs, "modifiedTime");

    return <div data-test-id="latest-modification-details" className={headerStyles.configRepoProperties}>
      <KeyValuePair data={attrs}/>
    </div>;
  }

  private resolveAttrsIntoReadableFormat(mod: MaterialModification): Map<string, m.Children> {
    const attrs  = new Map();
    const keys   = Object.keys(mod).map(humanizedMaterialAttributeName);
    const values = Object.values(mod);

    keys.forEach((key, index) => attrs.set(key, values[index]));
    return attrs;
  }

  private setDateInReadableFormat(attrs: Map<string, m.Children>, key: string) {
    const updatedKey    = humanizedMaterialAttributeName(key);
    const originalValue = attrs.get(updatedKey);
    attrs.delete(updatedKey);
    attrs.set(updatedKey, <span title={timeFormatter.formatInServerTime(originalValue)}>{timeFormatter.format(originalValue)}</span>);
  }
}
