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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import {TextFieldAttrs} from "./input_fields";
import {LiveValidatingInputField} from "./live_validating_input";

const ID_RE = /^([-a-zA-Z0-9_])([-a-zA-Z0-9_.]{0,254})$/;
const NEG_ID_BODY_RE = /[^-a-zA-Z0-9_.]/;
const NEG_ID_HEAD_RE = /[^-a-zA-Z0-9_]/;

interface State {
  valid(s: string): string | undefined;
}

export class IdentifierInputField extends MithrilComponent<TextFieldAttrs, State> {
  static INVALID_CHARS = "Only letters, numbers, hyphens, underscores, and periods are allowed.";
  static INVALID_START = "The first character must be a letter, number, hyphen, or underscore.";
  static TOO_LONG = "The maximum length is 255 characters.";
  static INVALID_FALLBACK = "This does not match the correct format.";
  static MISSING = "This field cannot be empty.";

  oninit(vnode: m.Vnode<TextFieldAttrs, State>) {
    vnode.state.valid = (val) => {
      if (!val) {
        if (vnode.attrs.required) {
          return IdentifierInputField.MISSING;
        }
        return;
      }

      if (!ID_RE.test(val)) {
        if (NEG_ID_BODY_RE.test(val)) {
          return IdentifierInputField.INVALID_CHARS;
        }

        if (NEG_ID_HEAD_RE.test(val[0])) {
          return IdentifierInputField.INVALID_START;
        }

        if (val.length > 255) {
          return IdentifierInputField.TOO_LONG;
        }

        // we really shouldn't get here. placing here for paranoia.
        return IdentifierInputField.INVALID_FALLBACK;
      }
    };
  }

  view(vnode: m.Vnode<TextFieldAttrs, State>) {
    const attrs = Object.assign({ validator: vnode.state.valid }, vnode.attrs);

    return <LiveValidatingInputField {...attrs} />;
  }
}
