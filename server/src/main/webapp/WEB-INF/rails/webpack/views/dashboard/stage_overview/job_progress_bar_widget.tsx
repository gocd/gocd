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
import {JobJSON} from "./models/types";
import {JobDuration, JobDurationStrategyHelper} from "./models/job_duration_stratergy_helper";
import {StageInstance} from "./models/stage_instance";

export interface Attrs {
  job: JobJSON;
  lastPassedStageInstance: Stream<StageInstance | undefined>;
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
    const jobDuration: JobDuration = JobDurationStrategyHelper.getDuration(vnode.attrs.job, vnode.attrs.lastPassedStageInstance());

    return <div>
      <div data-test-id="progress-bar-container-div" class={styles.progressBarContainer}>
        <div class={`${styles.waiting}`} style={`width: ${jobDuration.waitTimePercentage}%`}/>
        <div className={`${styles.building}`} style={`width: ${jobDuration.buildTimePercentage}%`}/>
        <div className={`${styles.unknown}`} style={`width: ${jobDuration.unknownTimePercentage}%`}/>
      </div>

      <div class={styles.progressBarTooltip}>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Wait Time:</div>
          <div>{jobDuration.waitTimeForDisplay}</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Build Time:</div>
          <div>{jobDuration.buildTimeForDisplay}</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Total Time:</div>
          <div>{jobDuration.totalTimeForDisplay}</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Scheduled At:</div>
          <div>{jobDuration.startTimeForDisplay}</div>
        </div>
        <div className={styles.tooltipKeyValuePair}>
          <div className={styles.tooltipKey}>Completed At:</div>
          <div>{jobDuration.endTimeForDisplay}</div>
        </div>
      </div>
    </div>
  }
}
