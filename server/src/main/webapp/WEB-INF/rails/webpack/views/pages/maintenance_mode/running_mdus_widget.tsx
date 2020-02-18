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
import {timeFormatter} from "helpers/time_formatter";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Materials, ScmMaterialAttributes} from "models/maintenance_mode/material";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {KeyValuePair} from "views/components/key_value_pair";
import styles from "views/pages/maintenance_mode/index.scss";

class MDUInfoAttrs {
  materials?: Materials;
}

export class MDUInfoWidget extends MithrilViewComponent<MDUInfoAttrs> {
  view(vnode: m.Vnode<MDUInfoAttrs>): m.Children {
    let inProgressMaterials;

    if (!vnode.attrs.materials || vnode.attrs.materials.count() === 0) {
      inProgressMaterials = <em data-test-id="no-running-mdus">No material update is in progress.</em>;
    } else {
      inProgressMaterials = vnode.attrs.materials.allScmMaterials().map((material) => {
        const attributes = material.attributes() as ScmMaterialAttributes;
        const nameOrUrl  = attributes.name() ? attributes.name() : attributes.url();
        const headerMap  = new Map([
                                     ["Type", material.type()],
                                     ["Name", nameOrUrl],
                                     ["Started At", timeFormatter.format(material.mduStartTime())]
                                   ]);

        const dataTestId = `${nameOrUrl.replace(" ", "-").toLowerCase()}`;
        return (
          <CollapsiblePanel header={<KeyValuePair data-test-id={`header-for-${dataTestId}`} inline={true} data={headerMap}/>}>
            <KeyValuePair data-test-id={`body-for-${dataTestId}`} data={material.attributesAsMap()}/>
          </CollapsiblePanel>
        );
      });
    }

    return (
      <CollapsiblePanel header={<h3 class={styles.runningSystemHeader}>Running MDUs</h3>} expanded={true}>
        {inProgressMaterials}
      </CollapsiblePanel>
    );
  }
}
