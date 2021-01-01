/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import classnames from "classnames";
import {mixins as s} from "helpers/string-plus";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import * as style from "./behavior_prompt.scss";

type Styles = typeof style;

interface Attrs {
  promptText: string;
  key: string;
  query: string;
  size?: keyof Pick<Styles, "small">; // currently, only size is "small"; implement more as needed.
  position?: Position;
}

export const STORAGE_KEY_PREFIX = "gocd.behaviorPrompt";

export class BehaviorPrompt extends MithrilComponent<Attrs> {
  show: Stream<boolean> = Stream();

  oninit(vnode: m.Vnode<Attrs>) {
    const storageKey = STORAGE_KEY_PREFIX.concat(".", vnode.attrs.key);
    if (!localStorage.getItem(storageKey)) {
      if (s.isBlank(vnode.attrs.query)) {
        this.show(true);
      } else {
        this.show(false);
        localStorage.setItem(storageKey, "true");
      }
    }
  }

  view(vnode: m.Vnode<Attrs>) {
    const {size, position, promptText} = vnode.attrs;

    if (this.show()) {
      return (
        <div class={classnames(style.behaviorPrompt, style[size || "small"], style.arrowRight)} style={positionToCSS(position)} data-test-id="behavior-prompt">
          {promptText + " "}
        </div>
      );
    }
  }
}

function positionToCSS(position: Position = {}): string  {
  return _.reduce(position, (memo, value, key) => `${memo} ${key}: ${value};`, "");
}

interface Position {
  top?: string;
  bottom?: string;
  left?: string;
  right?: string;
}
