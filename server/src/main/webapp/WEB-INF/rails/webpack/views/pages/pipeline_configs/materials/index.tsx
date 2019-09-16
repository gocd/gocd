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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {Material, Materials} from "models/new_pipeline_configs/materials";
import {Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Edit, IconGroup} from "views/components/icons";
import {Delete} from "views/components/icons";
import {MaterialOperations, SUPPORTED_MATERIALS} from "../../create_pipeline_page";
import styles from "../index.scss";
import materialStyles from "./index.scss";
import {AddMaterialModal, EditMaterialModal} from "./modals";

interface Attrs {
  materials: Materials;
  materialOperations: MaterialOperations;
}

export class MaterialsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let materials = [];
    let expanded  = true;
    if (vnode.attrs.materials.length === 0) {
      expanded  = true;
      materials = ["No materials defined"];
    } else {
      materials = [<table className={materialStyles.table} data-test-id={"materials-index-table"}>
        <thead data-test-id="table-header">
        <tr data-test-id="table-header-row">
          <th>Material Name</th>
          <th>Type</th>
          <th>Url</th>
          <th></th>
        </tr>
        </thead>
        {this.materialsData(vnode)}
      </table>];
    }

    return <CollapsiblePanel header="Materials"
                             actions={
                               <Secondary onclick={(e: MouseEvent) => this.addMaterialModal(e, vnode)}
                                          data-test-id={"add-material-button"}>Add
                                 Material</Secondary>
                             }
                             expanded={expanded}
                             dataTestId="pipeline-materials-container">
      {materials}
    </CollapsiblePanel>;
  }

  materialsData(vnode: m.Vnode<Attrs>): any {
    const tbodyContent = _.map(vnode.attrs.materials, (material: Material) => {
      return <tr>
        <td>{material.name()}</td>
        <td>{this.getTypeForDisplay(material.type())}</td>
        <td>{material.materialUrl()}</td>
        <td className={styles.textAlignRight}>
          <IconGroup>
            <Edit onclick={() => this.editMaterialModal(material, vnode)} title={"edit-material"}
                  data-test-id={"edit-material-button"}/>
            <Delete onclick={() => vnode.attrs.materialOperations.onDelete(material)} title={"delete-material"}
                    data-test-id={"delete-material-button"}/>
          </IconGroup>
        </td>
      </tr>;
    });
    return <tbody>{tbodyContent}</tbody>;
  }

  getTypeForDisplay(type: string | undefined) {
    const material = SUPPORTED_MATERIALS.find((record) => {
      return record.id === type;
    });

    if (!_.isUndefined(material)) {
      return material.text;
    }
    return type;
  }

  addMaterialModal(e: MouseEvent, vnode: m.Vnode<Attrs>) {
    new AddMaterialModal(new Material("git"), vnode.attrs.materialOperations.onAdd).render();
    e.stopPropagation();
  }

  editMaterialModal(material: Material, vnode: m.Vnode<Attrs>) {
    new EditMaterialModal(material, vnode.attrs.materialOperations.onUpdate).render();
  }
}
