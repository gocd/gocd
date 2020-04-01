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

import {RestyleAttrs, RestyleComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {PasswordField, Switch, TextField} from "views/components/forms/input_fields";
import {Delete, Plus} from "views/components/icons";
import {EntriesVM, EntryVM} from "./vms";

type Styles = any;

interface CollectionAttrs extends RestyleAttrs<Styles> {
  headers?: m.Children;
  model: EntriesVM;
}

interface Attrs extends RestyleAttrs<Styles> {
  model: EntryVM;
  destroy(): void;
}

export class KeyValEditor extends RestyleComponent<Styles, CollectionAttrs> {
  css: Styles = {};

  view(vnode: m.Vnode<CollectionAttrs>) {
    const model = vnode.attrs.model;

    return <table>
      {this.headers(vnode)}
      <tbody>
        {_.map(this.entries(vnode), (entry) => <KeyValEntryEditor model={entry} destroy={() => model.excise(entry)}/>)}
      </tbody>
      <tfoot>
        <tr><td colspan="4"><Plus onclick={() => this.append(vnode)} title="Add another property">Add another</Plus></td></tr>
      </tfoot>
    </table>;
  }

  entries(vnode: m.Vnode<CollectionAttrs>): EntryVM[] {
    const model = vnode.attrs.model;

    // always guarantee at least one entry, even if it's blank.
    if (!model.entries.length) {
      model.appendBlank();
    }

    return model.entries;
  }

  headers(vnode: m.Vnode<CollectionAttrs>) {
    if ("headers" in vnode.attrs) {
      const { headers } = vnode.attrs;
      if (headers === null || headers === void 0) { return; }
      return <thead>{headers}</thead>;
    }

    return <thead>
      <tr>
        <th>Encrypt</th>
        <th>Name</th>
        <th colspan="2">Value</th>
      </tr>
    </thead>;
  }

  append(vnode: m.Vnode<CollectionAttrs>) {
    vnode.attrs.model.appendBlank();
  }
}

class KeyValEntryEditor extends RestyleComponent<Styles, Attrs> {
  css: Styles = {};

  view(vnode: m.Vnode<Attrs>) {
    const entry = vnode.attrs.model;
    return <tr>
      <td><Switch property={entry.isSecure.bind(entry)}/></td>
      <td><TextField placeholder="Name" property={entry.name}/></td>
      <td>{this.valueField(entry)}</td>
      <td><Delete onclick={vnode.attrs.destroy}/></td>
    </tr>;
  }

  valueField(entry: EntryVM) {
    if (entry.isSecure()) {
      return <PasswordField placeholder="Secret Value" property={entry.secretValue}/>;
    }

    return <TextField placeholder="Value" property={entry.value}/>;
  }
}
