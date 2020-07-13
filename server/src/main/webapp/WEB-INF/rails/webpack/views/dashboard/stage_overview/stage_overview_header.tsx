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
import Stream from "mithril/stream";
import * as styles from "./index.scss";
import {MithrilComponent} from "../../../jsx/mithril-component";
import * as Icons from "views/components/icons";
import {StageInstance} from "./models/stage_instance";

interface StageHeaderAttrs {
  stageName: string;
  stageCounter: string | number;
  pipelineName: string;
  stageInstance: Stream<StageInstance>;
}

interface StageHeaderState {
}

export class StageHeaderWidget extends MithrilComponent<StageHeaderAttrs, StageHeaderState> {
  view(vnode: m.Vnode<StageHeaderAttrs, StageHeaderState>): m.Children | void | null {
    return <div data-test-id="stage-overview-header" class={styles.stageHeaderContainer}>
      <div data-test-id="stage-name-and-operations-container" class={styles.flexBox}>
        <div class={styles.stageNameContainer}>
          <div data-test-id="pipeline-name-container">
            <div className={styles.stageNameTitle}>Pipeline</div>
            <div className={styles.stageName}>{vnode.attrs.pipelineName}</div>
          </div>
          <div data-test-id="stage-name-container">
            <div className={styles.stageNameTitle}>Stage</div>
            <div className={styles.stageName}>{vnode.attrs.stageName}</div>
          </div>
          <div data-test-id="stage-instance-container">
            <div className={styles.stageNameTitle}>Instance</div>
            <div className={styles.stageName}>{vnode.attrs.stageCounter}</div>
          </div>
        </div>

        <div data-test-id="stage-operations-container" class={styles.stageOperationButtonGroup}>
          <div><Icons.Trigger iconOnly={true}/></div>
          <div class={styles.stageSettings}><Icons.Settings iconOnly={true}/></div>
        </div>
      </div>

      <div data-test-id="stage-trigger-and-timing-container"
           class={`${styles.flexBox} ${styles.stageTriggerAndTimingContainer}`}>
        <div data-test-id="stage-trigger-by-container">
          <div>{vnode.attrs.stageInstance().triggeredBy()}</div>
          <div>{vnode.attrs.stageInstance().triggeredOn()}</div>
        </div>
        <div data-test-id="stage-duration-container" class={styles.stageDurationContainer}>
          <div className={styles.stageNameTitle}>Duration:</div>
          <div>{vnode.attrs.stageInstance().stageDuration()}</div>
        </div>
      </div>
    </div>;
  }
}
