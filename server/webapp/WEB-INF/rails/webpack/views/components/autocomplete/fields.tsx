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

import * as Awesomplete from "awesomplete";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {BaseAttrs, TextField} from "views/components/forms/input_fields";
import * as defaultStyles from "./index.scss";

export type SuggestionWriter = (data: Awesomplete.Suggestion[]) => void;

const AWESOMPLETE_KEYS = ["list", "minChars", "maxItems", "autoFirst", "data", "filter", "sort", "item", "replace"];

interface Attrs {
  css?: typeof defaultStyles;
  provider: SuggestionProvider;
}

type AutoCompAttrs = BaseAttrs<string> & Attrs & Awesomplete.Options;

function stripAwesompleteOpts(config: Awesomplete.Options): any {
  return _.omit(config, AWESOMPLETE_KEYS);
}

function onlyAwesompleteOpts(config: any): Awesomplete.Options {
  return _.pick(config, AWESOMPLETE_KEYS);
}

function stripNativeAttrs(config: Attrs): any {
  return _.omit(config, ["css", "provider"]);
}

export abstract class SuggestionProvider {
  abstract getData(setData: SuggestionWriter): void;
}

export class AutocompleteField extends MithrilViewComponent<AutoCompAttrs> {
  private _asm?: Awesomplete;

  ensureInited(vnode: m.VnodeDOM<AutoCompAttrs, {}>): void {
    const css = vnode.attrs.css || defaultStyles;

    if (!this._asm && vnode.dom) {
      const input = Awesomplete.$("input", vnode.dom!) as HTMLInputElement;

      this._asm = new Awesomplete(input, _.assign({
        sort: false,
        minChars: 0,
        container(input: HTMLElement): Element {
          vnode.dom.classList.add(css.awesomplete);
          return vnode.dom!;
        },
        replace(text: Awesomplete.Suggestion) {
          vnode.attrs.property(input.value = text.toString());
          m.redraw();
        }
      }, onlyAwesompleteOpts(vnode.attrs)));

      input.addEventListener("awesomplete-selectcomplete", (e: Event) => {
        vnode.attrs.property(input.value);
      });

      this._asm.status.classList.remove("visually-hidden");
      this._asm.status.classList.add(css.visuallyHidden);

      vnode.attrs.provider.getData((data: Awesomplete.Suggestion[]) => {
        this._asm!.list = data;
      });
    }
  }

  oncreate(vnode: m.VnodeDOM<AutoCompAttrs, {}>) {
    this.ensureInited(vnode);
  }

  onupdate(vnode: m.VnodeDOM<AutoCompAttrs, {}>) {
    this.ensureInited(vnode);
  }

  view(vnode: m.Vnode<AutoCompAttrs, {}>): m.Children | void | null {
    const attrs = stripNativeAttrs(stripAwesompleteOpts(vnode.attrs)) as BaseAttrs<string>;
    return <TextField {...attrs} />;
  }
}
