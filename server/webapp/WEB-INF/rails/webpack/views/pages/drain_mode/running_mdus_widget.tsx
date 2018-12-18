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
import * as m from "mithril";
import {Materials, ScmMaterialAttributes} from "models/drain_mode/material";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {KeyValuePair} from "views/components/key_value_pair";
import * as styles from "views/pages/drain_mode/index.scss";

class MDUInfoAttrs {
  materials?: Materials;
}

export class MDUInfoWidget extends MithrilViewComponent<MDUInfoAttrs> {
  view(vnode: m.Vnode<MDUInfoAttrs>): m.Children {
    let inProgressMaterials;

    if (!vnode.attrs.materials || vnode.attrs.materials.count() === 0) {
      inProgressMaterials = <em>No material update is in progress.</em>;
    } else {
      inProgressMaterials = vnode.attrs.materials.allScmMaterials().map((material) => {
        const attributes = material.attributes() as ScmMaterialAttributes;
        const nameOrUrl  = attributes.name() ? attributes.name() : attributes.url();
        const headerMap  = new Map([
                                     ["Type", material.type()],
                                     ["Name", nameOrUrl],
                                     ["Auto Update", attributes.name()],
                                     ["Started At", material.mduStartTime().toString()]
                                   ]);

        return (
          <CollapsiblePanel
            header={<KeyValuePair inline={true} data={headerMap}/>}>
            <KeyValuePair data={material.attributesAsMap()}/>
          </CollapsiblePanel>
        );
      });
    }

    return (
      <div>
        <h3 className={styles.runningSystemHeader}>Running MDUs</h3>
        {inProgressMaterials}
      </div>
    );
  }
}
