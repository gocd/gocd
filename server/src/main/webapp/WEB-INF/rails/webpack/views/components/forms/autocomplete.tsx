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

import Awesomplete from "awesomplete";
import {RestyleAttrs, RestyleViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {TextField, TextFieldAttrs} from "views/components/forms/input_fields";
import defaultStyles from "./autocomplete.scss";
import fieldStyles from "./forms.scss";

type Styles = typeof defaultStyles;
type FieldStyles = typeof fieldStyles;

type SuggestionWriter = (data: Awesomplete.Suggestion[]) => void;

const AWESOMPLETE_KEYS = ["list", "minChars", "maxItems", "autoFirst", "data", "filter", "sort", "item", "replace"];

interface Attrs extends RestyleAttrs<Styles> {
  provider: SuggestionProvider;
  autoEvaluate?: boolean;
  fieldCss?: FieldStyles;
}

interface State {
  _asm: Awesomplete;
}

type AutoCompAttrs = TextFieldAttrs & Attrs & Awesomplete.Options;

function onlyAwesompleteOpts(config: any): Awesomplete.Options {
  return _.pick(config, AWESOMPLETE_KEYS);
}

function onlyTextFieldAttrs(config: any): TextFieldAttrs {
  return _.omit(config, ["css", "provider", "autoEvaluate"].concat(AWESOMPLETE_KEYS)) as TextFieldAttrs;
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
    this.getData().then(this.receiver).catch(this.errorMsg).finally(this.done);
  }

  abstract getData(): Promise<Awesomplete.Suggestion[]>;
}

export class AutocompleteField extends RestyleViewComponent<Styles, AutoCompAttrs> {
  css: Styles = defaultStyles;

  ensureInited(vnode: m.VnodeDOM<AutoCompAttrs, State>): void {
    const css = this.css;
    const dom = vnode.dom && vnode.dom.children[1];

    if (!vnode.state._asm && dom) {
      dom.classList.add(css.awesomplete);
      const input = Awesomplete.$("input", dom!) as HTMLInputElement;

      const asm = new Awesomplete(input, _.assign(
        {
          sort: false,
          minChars: 0,
          container(input: HTMLElement): Element {
            return dom;
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

    if (vnode.attrs.fieldCss) {
      attrs.css = vnode.attrs.fieldCss;
    }

    return <TextFieldForAutoComplete {...attrs} />;
  }
}

export class TextFieldForAutoComplete extends TextField {
  renderInputField(vnode: m.Vnode<TextFieldAttrs>) {
    return <div id="autocomplete-input-container">
      {super.renderInputField(vnode)}
    </div>;
  }
}
