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
import {TextField, TextFieldAttrs} from "views/components/forms/input_fields";
import * as defaultStyles from "./autocomplete.scss";

type SuggestionWriter = (data: Awesomplete.Suggestion[]) => void;

const AWESOMPLETE_KEYS = ["list", "minChars", "maxItems", "autoFirst", "data", "filter", "sort", "item", "replace"];

interface Attrs {
  // allows us to override stylesheet with anything that defines, imports,
  // or extends the classes defined in autocomplete.scss
  //
  // TODO: perhaps change input_fields.tsx to accept a similar property
  // to allow customization of styles and pass this through.
  css?: typeof defaultStyles;
  provider: SuggestionProvider;
  autoEvaluate?: boolean;
}

interface State {
  _asm: Awesomplete;
}

type AutoCompAttrs = TextFieldAttrs & Attrs & Awesomplete.Options;

function onlyAwesompleteOpts(config: any): Awesomplete.Options {
  return _.pick(config, AWESOMPLETE_KEYS);
}

function onlyTextFieldAttrs(config: any): TextFieldAttrs {
  return _.omit(config, ["css", "provider"].concat(AWESOMPLETE_KEYS));
}

export abstract class SuggestionProvider {
  protected receiver?: SuggestionWriter;
  protected errorMsg?: (reason?: string) => string;
  protected done?: () => void;

  onData(receiver: SuggestionWriter) {
    this.receiver = receiver;
  }

  onError(handler: (reason?: string) => string) {
    this.errorMsg = handler;
  }

  onFinally(handler: () => void) {
    this.done = handler;
  }

  update(): void {
    this.getData().
      then(this.receiver).
      catch(this.errorMsg).
      finally(this.done);
  }

  abstract getData(): Promise<Awesomplete.Suggestion[]>;
}

export class AutocompleteField extends MithrilViewComponent<AutoCompAttrs> {
  ensureInited(vnode: m.VnodeDOM<AutoCompAttrs, State>): void {
    const css     = vnode.attrs.css || defaultStyles;

    vnode.dom.classList.add(css.awesomplete);

    if (!vnode.state._asm && vnode.dom) {
      const input = Awesomplete.$("input", vnode.dom!) as HTMLInputElement;

      const asm = new Awesomplete(input, _.assign(
        {
          sort: false,
          minChars: 0,
          container(input: HTMLElement): Element {
            return vnode.dom!;
          },
          replace(text: Awesomplete.Suggestion) {
            input.value = text.toString();
            vnode.attrs.property(input.value);
            m.redraw();
          }
        }, onlyAwesompleteOpts(vnode.attrs)));

      asm.status.classList.remove("visually-hidden");
      asm.status.classList.add(css.visuallyHidden);

      vnode.attrs.provider.onData((data: Awesomplete.Suggestion[]) => {
        asm.list = data;
        if (vnode.attrs.autoEvaluate === undefined || vnode.attrs.autoEvaluate === true) {
          asm.evaluate();
        }
      });
      vnode.attrs.provider.update();
      vnode.state._asm = asm;
    }
  }

  oncreate(vnode: m.VnodeDOM<AutoCompAttrs, State>) {
    this.ensureInited(vnode);
  }

  onupdate(vnode: m.VnodeDOM<AutoCompAttrs, State>) {
    this.ensureInited(vnode);
  }

  onremove(vnode: m.VnodeDOM<AutoCompAttrs, State>): any {
    if (vnode.state._asm) {
      vnode.state._asm.destroy();
    }
  }

  view(vnode: m.Vnode<AutoCompAttrs, {}>): m.Children | void | null {
    const attrs = onlyTextFieldAttrs(vnode.attrs);
    return <TextField {...attrs} />;
  }
}
