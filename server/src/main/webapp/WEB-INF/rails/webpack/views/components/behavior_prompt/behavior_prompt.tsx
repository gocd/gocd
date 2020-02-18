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

import {bind} from "classnames/bind";
import {mixins as s} from "helpers/string-plus";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import * as style from "./behavior_prompt.scss";

const classnames = bind(style);
type Styles = typeof style;

interface Attrs {
  promptText: string;
  key: string;
  query: string;
  direction: Direction;
  size?: Size;
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
    if (this.show()) {
      return (
        <div class={classnames(style.behaviorPrompt, SizeTransformer.transform(vnode.attrs.size))} style={positionToCSS(vnode.attrs.position)} data-test-id="behavior-prompt">
          {vnode.attrs.promptText}&nbsp;
          <i class={DirectionTransformer.transform(vnode.attrs.direction)} data-test-id="behavior-prompt-dir"/>
        </div>
      );
    }
  }
}

function positionToCSS(position?: Position): string  {
  return _.reduce(position, (stylesString, value, key) => {
    stylesString = `${stylesString} ${key}: ${value};`;
    return stylesString;
  }, "");
}

interface Position {
  top?: string;
  bottom?: string;
  left?: string;
  right?: string;
}

export enum Direction {
  LEFT, RIGHT, UP, DOWN
}

class DirectionTransformer {
  static transform(direction?: Direction, css: Styles = style) {
    switch (direction) {
      case Direction.LEFT:
        return css.arrowLeft;
      case Direction.RIGHT:
        return css.arrowRight;
      case Direction.UP:
        return css.arrowUp;
      case Direction.DOWN:
        return css.arrowDown;
      default:
        return css.arrowRight;
    }
  }
}

export enum Size {
  SMALL, MEDIUM
}

class SizeTransformer {
  static transform(size?: Size, css: Styles = style) {
    switch (size) {
      case Size.SMALL:
        return css.small;
      case Size.MEDIUM:
        return css.medium;
      default:
        return css.small;
    }
  }
}
