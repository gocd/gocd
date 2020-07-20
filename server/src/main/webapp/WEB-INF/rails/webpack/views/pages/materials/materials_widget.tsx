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
import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {humanizedMaterialAttributeName, MaterialModification} from "models/config_repos/types";
import {MaterialWithModifications} from "models/materials/materials";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import headerStyles from "views/pages/config_repos/index.scss";
import {MaterialsAttrs} from "views/pages/materials";
import styles from "./index.scss";
import {MaterialHeaderWidget} from "./material_header_widget";

export class MaterialsWidget extends MithrilViewComponent<MaterialsAttrs> {

  static helpText() {
    return <span>
      A material is a cause for a pipeline to run. The GoCD Server continuously polls configured materials and when a new change or commit is found, the corresponding pipelines are run or "triggered".
      <Link href={docsUrl("introduction/concepts_in_go.html#materials")} target={"_blank"} externalLinkIcon={true}> Learn More</Link>
    </span>;
  }

  view(vnode: m.Vnode<MaterialsAttrs>) {
    if (vnode.attrs.materials().length === 0) {
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
      {vnode.attrs.materials().map((material) => <MaterialWidget material={material}/>)}
    </div>;
  }
}

export interface MaterialAttrs {
  material: MaterialWithModifications;
}

export class MaterialWidget extends MithrilViewComponent<MaterialAttrs> {
  view(vnode: m.Vnode<MaterialAttrs, this>): m.Children | void | null {
    const material = vnode.attrs.material;

    return <CollapsiblePanel header={<MaterialHeaderWidget {...vnode.attrs} />}>
      <h3>Latest Modification Details</h3>
      {this.showLatestModificationDetails(material.modification, material.config.fingerprint())}
      <h3>Material Attributes</h3>
      <KeyValuePair data-test-id={"material-attributes"} data={material.config.attributesAsMap()}/>
    </CollapsiblePanel>;
  }

  private showLatestModificationDetails(modification: MaterialModification | null, fingerprint: string) {
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
