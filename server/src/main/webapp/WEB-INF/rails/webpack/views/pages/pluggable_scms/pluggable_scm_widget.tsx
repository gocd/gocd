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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Scm} from "models/materials/pluggable_scm";
import s from "underscore.string";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Clone, Delete, Edit, IconGroup, InfoCircle, Usage} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "../page_operations";
import styles from "./index.scss";

interface Attrs extends EditOperation<Scm>, CloneOperation<Scm>, DeleteOperation<Scm> {
  scm: Scm;
  disableActions: boolean;
  showUsages: (scm: Scm, e: MouseEvent) => void;
}

export class PluggableScmWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const header = <KeyValuePair inline={true}
                                 data={PluggableScmWidget.headerMap(vnode.attrs.scm)}/>;

    const scm            = vnode.attrs.scm;
    const scmRepoDetails = new Map([
                                     ["Id", scm.id()],
                                     ["Name", scm.name()],
                                     ["Plugin Id", scm.pluginMetadata().id()],
                                     ...Array.from(scm.configuration().asMap())
                                   ]);

    const disabled      = vnode.attrs.disableActions;
    const warningIcon   = disabled
      ? <span className={styles.warning}><InfoCircle title={`Plugin '${scm.pluginMetadata().id()}' was not found!`} iconOnly={true}/></span>
      : undefined;
    const actionButtons = <IconGroup>
      <Edit data-test-id="pluggable-scm-edit"
            disabled={disabled}
            title={PluggableScmWidget.getMsgForOperation(disabled, scm, "edit")}
            onclick={vnode.attrs.onEdit.bind(this, scm)}/>
      <Clone data-test-id="pluggable-scm-clone"
             disabled={disabled}
             title={PluggableScmWidget.getMsgForOperation(disabled, scm, "clone")}
             onclick={vnode.attrs.onClone.bind(this, scm)}/>
      <Delete data-test-id="pluggable-scm-delete"
              title={PluggableScmWidget.getMsgForOperation(disabled, scm, "delete")}
              onclick={vnode.attrs.onDelete.bind(this, scm)}/>
      <Usage data-test-id="pluggable-scm-usages"
             title={PluggableScmWidget.getMsgForOperation(disabled, scm, "show usages for")}
             onclick={vnode.attrs.showUsages.bind(this, scm)}/>
    </IconGroup>;

    return <CollapsiblePanel key={vnode.attrs.scm.id()} error={disabled}
                             header={header} actions={[warningIcon, actionButtons]}
                             dataTestId={"pluggable-scm-panel"}>
      <KeyValuePair data-test-id={"pluggable-scm-details"} data={scmRepoDetails}/>
    </CollapsiblePanel>;
  }

  private static headerMap(scm: Scm) {
    const map = new Map();
    map.set("Name", scm.name());
    map.set("Plugin Id", scm.pluginMetadata().id());
    return map;
  }

  private static getMsgForOperation(disabled: boolean, scm: Scm, operation: "edit" | "clone" | "delete" | "show usages for"): string {
    if (disabled && (operation === "edit" || operation === "clone")) {
      return `Plugin '${scm.pluginMetadata().id()}' not found!`;
    }
    return `${s.capitalize(operation)} scm '${scm.name}'`;
  }
}
