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

import {makeEvent} from "helpers/compat";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {TextField, TextFieldAttrs} from "views/components/forms/input_fields";

export interface LiveInputAttrs extends TextFieldAttrs {
  validator: (value: string) => string | undefined;
}

interface State {
  doValidate: EventListener;
  errors(errorText?: string): string | undefined;
}

/**
 * Only report the validation message if we set one explicitly on the input; otherwise, if
 * the `required` attribute is set, the default error message will show and the input field
 * will show an error on initial page load. This implies that the validator function set on
 * the LiveValidatingInputField should check the `required` attribute and handle empty values
 * _if so desired_.
 */
function liveValidationError(dom: HTMLInputElement) {
  return dom.validity.customError ? dom.validationMessage : "";
}

export class LiveValidatingInputField extends MithrilComponent<LiveInputAttrs, State> {
  oncreate(vnode: m.VnodeDOM<LiveInputAttrs, State>) {
    const dom = vnode.dom.querySelector("input") as HTMLInputElement;
    dom.setCustomValidity("");

    const validator = vnode.attrs.validator;

    // prefers errorText attribute and otherwise injects any validator error message
    vnode.state.errors = (errorText) => (errorText || liveValidationError(dom));

    if ("function" === typeof validator) {
      vnode.state.doValidate = (e) => {
        dom.setCustomValidity(validator(dom.value) || "");

        if (!!dom.validationMessage) {
          dom.dispatchEvent(makeEvent("invalid", false, true));
        }
        m.redraw();

      };

      dom.addEventListener("input", vnode.state.doValidate);
    }
  }

  onbeforeremove(vnode: m.VnodeDOM<LiveInputAttrs, State>) {
    const dom = vnode.dom as HTMLInputElement;

    dom.setCustomValidity(""); // clear message

    if ("function" === typeof vnode.state.doValidate) {
      dom.removeEventListener("input", vnode.state.doValidate);
    }
  }

  view(vnode: m.Vnode<LiveInputAttrs, State>) {
    const attrs = Object.assign({}, vnode.attrs);

    delete attrs.validator;

    if ("function" === typeof vnode.state.errors) {
      attrs.errorText = vnode.state.errors(vnode.attrs.errorText);
    }

    return <TextField {...attrs}/>;
  }
}
