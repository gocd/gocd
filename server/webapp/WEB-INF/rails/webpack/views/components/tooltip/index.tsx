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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";

import {bind} from "classnames/bind";
import * as styles from "./index.scss";

const classnames = bind(styles);

export enum TooltipSize {
  small,
  medium,
  large
}

export interface Attrs {
  content: string;
  size?: TooltipSize;
}

class Tooltip extends MithrilViewComponent<Attrs> {
  tooltipType: string;

  constructor(tooltipType: string) {
    super();
    this.tooltipType = tooltipType;
  }

  view(vnode: m.Vnode<Attrs>) {
    // @ts-ignore
    const size = styles[TooltipSize[vnode.attrs.size || TooltipSize.small]];

    return (
      <div data-test-id="tooltip-wrapper">
        <div className={this.tooltipType}>
          <span data-test-id="tooltip-content"
                className={classnames(styles.tooltiptext, size)}>
            {vnode.attrs.content}
          </span>
        </div>
      </div>);
  }
}

export class Info extends Tooltip {
  constructor() {
    super(styles.infoTooltip);
  }
}

export class Help extends Tooltip {
  constructor() {
    super(styles.helpTooltip);
  }
}
