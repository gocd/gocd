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
import classnames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";

import {v4 as uuid4} from "uuid";
import {DataTestIdAttr, ErrorText, HelpText} from "views/components/forms/input_fields";
import styles from "./index.scss";

export interface Attrs extends DataTestIdAttr {
  inProgress?: boolean;
  small?: boolean;
  disabled?: boolean;
  label?: m.Children;
  onclick?: (e: MouseEvent) => any;
  field: (newValue?: any) => any | Stream<boolean>;
}

export class SwitchBtn extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const { disabled, field, inProgress, onclick, small } = vnode.attrs;
    const switchId = `switch-${uuid4()}`;

    const dataTestId = vnode.attrs.dataTestId? vnode.attrs.dataTestId: "switch-checkbox";
    return <div class={styles.switchWrapper}>
      <div class={classnames({[styles.switchSmall]: small}, styles.switchBtn)} data-test-id="switch-wrapper">
        {this.label(vnode.attrs, switchId)}
        <input id={switchId} type="checkbox"
                {...vnode.attrs}
                checked={field()}
                onclick={(e: MouseEvent) => {
                  if (onclick) {
                    onclick(e);
                  }

                  const target = e.target as HTMLInputElement;
                  field(target.checked);
                }}
                class={styles.switchInput}
                data-test-id={dataTestId}/>
        <label for={switchId} class={classnames({
                                                  [styles.inProgress]: field() && inProgress,
                                                  [styles.isSuccess]: field() && !inProgress,
                                                  [styles.disabled]: disabled
                                                }, styles.switchPaddle)} data-test-id="switch-paddle"/>
      </div>
      <HelpText {...vnode.attrs} helpTextId={"switch-btn-help-text"}/>
      <ErrorText {...vnode.attrs} errorId={`switch-btn-error-text`}/>
    </div>;
  }

  private label({ disabled, label }: Attrs, switchId: string) {
    if (label) {
      const classes = classnames({[styles.disabled]: disabled}, styles.switchLabel);
      return <label for={switchId} class={classes} data-test-id="switch-label">
        {label}
      </label>;
    }
  }
}
