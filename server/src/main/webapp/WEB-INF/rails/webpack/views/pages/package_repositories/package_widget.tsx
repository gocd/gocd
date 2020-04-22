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
import Stream from "mithril/stream";
import {Package} from "models/package_repositories/package_repositories";
import s from "underscore.string";
import {Anchor, ScrollManager} from "views/components/anchor/anchor";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Clone, Delete, Edit, IconGroup, Usage} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "../page_operations";
import {PackageRepoScrollOptions} from "./package_repositories_widget";

interface Attrs extends EditOperation<Package>, CloneOperation<Package>, DeleteOperation<Package> {
  package: Package;
  showUsages: (obj: Package, e: MouseEvent) => void;
  disableActions: boolean;
  scrollOptions: PackageRepoScrollOptions;
}

export interface PackageScrollOptions {
  sm: ScrollManager;
  shouldOpenEditView: boolean;
}

export class PackageWidget extends MithrilViewComponent<Attrs> {
  expanded: Stream<boolean> = Stream();

  public static getPkgDetails(pkg: Package) {
    const pkgProperties = pkg.configuration() ? pkg.configuration()!.asMap() : [];
    return new Map([
                     ["Id", pkg.id()],
                     ["Name", pkg.name()],
                     ["Auto Update", pkg.autoUpdate() + ""],
                     ...Array.from(pkgProperties)
                   ]);
  }

  oninit(vnode: m.Vnode<Attrs, this>): any {
    const scrollOptions = vnode.attrs.scrollOptions;
    const pkg           = vnode.attrs.package;
    const linked        = scrollOptions.package_repo_sm.sm.getTarget() === pkg.packageRepo().name()
                          && scrollOptions.package_sm.sm.getTarget() === pkg.key();

    this.expanded(linked);
  }

  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    const pkg        = vnode.attrs.package;
    const title      = <span data-test-id="package-id">{pkg.name()}</span>;
    const pkgDetails = PackageWidget.getPkgDetails(pkg);
    const disabled   = vnode.attrs.disableActions;

    const actionButtons = [
      <IconGroup>
        <Edit data-test-id="package-edit"
              disabled={disabled}
              title={PackageWidget.getMsgForOperation(disabled, pkg.name(), "edit")}
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, pkg)}/>
        <Clone data-test-id="package-clone"
               disabled={disabled}
               title={PackageWidget.getMsgForOperation(disabled, pkg.name(), "clone")}
               onclick={vnode.attrs.onClone.bind(vnode.attrs, pkg)}/>
        <Delete data-test-id="package-delete"
                title={PackageWidget.getMsgForOperation(disabled, pkg.name(), "delete")}
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, pkg)}/>
        <Usage data-test-id="package-usages"
               title={PackageWidget.getMsgForOperation(disabled, pkg.name(), "show usages for")}
               onclick={vnode.attrs.showUsages.bind(vnode.attrs, pkg)}/>
      </IconGroup>];
    const scrollOptions = vnode.attrs.scrollOptions;
    const onNavigate    = () => {
      if (scrollOptions.package_repo_sm.sm.getTarget() === pkg.packageRepo().name()) {
        this.expanded(true);
        if (scrollOptions.package_sm.sm.getTarget() === pkg.key() && scrollOptions.package_sm.shouldOpenEditView && !disabled) {
          vnode.attrs.onEdit(pkg, new MouseEvent("click"));
        }
      }
    };
    return <Anchor id={pkg.key()} sm={scrollOptions.package_sm.sm}
                   onnavigate={onNavigate}>
      <CollapsiblePanel dataTestId={"package-panel"} actions={actionButtons}
                        header={<KeyValueTitle image={null} title={title}/>}
                        vm={this}>
        <KeyValuePair data={pkgDetails}/>
      </CollapsiblePanel>
    </Anchor>;
  }

  private static getMsgForOperation(disabled: boolean, pkgName: string, operation: "edit" | "clone" | "delete" | "show usages for"): string | undefined {
    if (disabled && (operation === "edit" || operation === "clone")) {
      return "Plugin not found!";
    }
    return `${s.capitalize(operation)} package '${pkgName}'`;
  }
}
