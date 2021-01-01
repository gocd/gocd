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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";

import {bind} from "classnames/bind";
import * as Icons from "views/components/icons";
import {InfoCircle, QuestionCircle} from "views/components/icons";
import styles from "./index.scss";

const classnames = bind(styles);

export enum TooltipSize {
  small,
  medium,
  large
}

export interface Attrs {
  content: m.Children;
  size?: TooltipSize;
}

class Tooltip extends MithrilViewComponent<Attrs> {
  private static currentId = 0;

  tooltipType: QuestionCircle | InfoCircle;
  private readonly id: number;

  constructor(tooltipType: any) {
    super();
    this.tooltipType = tooltipType;
    this.id          = Tooltip.currentId++; // added to support accessibility
  }

  view(vnode: m.Vnode<Attrs>) {
    // @ts-ignore
    const size   = styles[TooltipSize[vnode.attrs.size || TooltipSize.small]];
    const a11yId = `tooltip-desc-${this.id}`;

    return (
      <div data-test-id="tooltip-wrapper" class={styles.tooltipWrapper}>
        {m(this.tooltipType, {iconOnly: true, title: "", describedBy: a11yId})}
        <div class={styles.tooltipContentWrapper}>
          <div data-test-id="tooltip-content"
               class={classnames(styles.tooltipContent, size)}>
            <p role="tooltip" id={a11yId}>{vnode.attrs.content}</p>
          </div>
        </div>
      </div>);
  }
}

export class Info extends Tooltip {
  constructor() {
    super(Icons.InfoCircle);
  }
}

export class Help extends Tooltip {
  constructor() {
    super(Icons.QuestionCircle);
  }
}
