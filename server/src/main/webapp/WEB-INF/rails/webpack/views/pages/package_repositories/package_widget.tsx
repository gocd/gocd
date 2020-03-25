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
import {Package} from "models/package_repositories/package_repositories";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Clone, Delete, Edit, IconGroup, Usage} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "../page_operations";

interface Attrs extends EditOperation<Package>, CloneOperation<Package>, DeleteOperation<Package> {
  package: Package;
  showUsages: (obj: Package, e: MouseEvent) => void;
  disableActions: boolean;
}

export class PackageWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const title         = <span data-test-id="package-id">{vnode.attrs.package.name()}</span>;
    const pkgProperties = vnode.attrs.package.configuration() ? vnode.attrs.package.configuration()!.asMap() : [];
    const pkgDetails    = new Map([
                                    ["Name", vnode.attrs.package.name()],
                                    ["Auto Update", vnode.attrs.package.autoUpdate() + ""],
                                    ...Array.from(pkgProperties)
                                  ]);

    const actionButtons = [
      <IconGroup>
        <Edit data-test-id="package-edit"
              disabled={vnode.attrs.disableActions}
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, vnode.attrs.package)}/>
        <Clone data-test-id="package-clone"
               disabled={vnode.attrs.disableActions}
               onclick={vnode.attrs.onClone.bind(vnode.attrs, vnode.attrs.package)}/>
        <Delete data-test-id="package-delete"
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, vnode.attrs.package)}/>
        <Usage data-test-id="package-usages"
               onclick={vnode.attrs.showUsages.bind(vnode.attrs, vnode.attrs.package)}/>
      </IconGroup>];
    return <CollapsiblePanel key={vnode.attrs.package.id()} dataTestId={"package-panel"} actions={actionButtons}
                             header={<KeyValueTitle image={null} title={title}/>}>
      <KeyValuePair data={pkgDetails}/>
    </CollapsiblePanel>;
  }
}
