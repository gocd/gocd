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

import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {TextField, TextFieldAttrs} from "views/components/forms/input_fields";

export interface LiveInputAttrs extends TextFieldAttrs {
  validator: (value: string) => string | undefined;
}

interface State {
  errors(): string | undefined;
}

export class LiveValidatingInputField extends MithrilComponent<LiveInputAttrs, State> {
  oncreate(vnode: m.VnodeDOM<LiveInputAttrs, State>) {
    const dom = vnode.dom.querySelector("input") as HTMLInputElement;
    const validator = vnode.attrs.validator;

    vnode.state.errors = () => (dom.validationMessage || vnode.attrs.errorText);

    if ("function" === typeof validator) {
      dom.addEventListener("input", (e) => {
        dom.setCustomValidity(validator(dom.value) || "");

        if (!!dom.validationMessage) {
          dom.dispatchEvent(new Event("invalid", { bubbles: false, cancelable: true }));
        }
        m.redraw();
      });
    }
  }

  view(vnode: m.Vnode<LiveInputAttrs, State>) {
    const attrs = Object.assign({}, vnode.attrs);

    delete attrs.validator;

    if ("function" === typeof vnode.state.errors) {
      attrs.errorText = vnode.state.errors(); // injects validator error message, falling back to vnode.attrs.errorText
    }

    return <TextField {...attrs}/>;
  }
}
