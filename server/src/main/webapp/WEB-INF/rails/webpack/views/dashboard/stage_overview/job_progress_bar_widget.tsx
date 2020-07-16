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

import m from "mithril";
import * as styles from "./index.scss";
import {MithrilComponent} from "../../../jsx/mithril-component";
import {JobJSON} from "./models/types";

export interface Attrs {
  job: JobJSON;

}

export class JobProgressBarWidget extends MithrilComponent<Attrs> {
  oncreate(vnode: m.Vnode<Attrs, {}>) {
    vnode.dom.children[0].onmouseover = () => {
      vnode.dom.children[1].style.visibility = 'visible';
    };

    vnode.dom.children[0].onmouseout = () => {
      vnode.dom.children[1].style.visibility = 'hidden';
    };
  }

  view(vnode: m.Vnode<Attrs, {}>): m.Children | void | null {
    return <div>
      <div data-test-id="progress-bar-container-div" class={styles.progressBarContainer}>
        <div class={`${styles.waiting}`} style="width: 70%"/>
        <div className={`${styles.building}`} style="width: 20%"/>
        <div className={`${styles.unknown}`} style="width: 10%"/>
      </div>

      <div class={styles.progressBarTooltip}>
        <div class={styles.tooltipKeyValuePair}>
          <div class={styles.tooltipKey}>Job:</div>
          <div>up42_job</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>State:</div>
          <div>Building</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Wait Time:</div>
          <div>30s</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Build Time:</div>
          <div>12m</div>
        </div>
      </div>
    </div>
  }
}
