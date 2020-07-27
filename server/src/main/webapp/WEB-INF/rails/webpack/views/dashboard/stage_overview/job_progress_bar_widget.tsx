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
import m from "mithril";
import Stream from "mithril/stream";
import {MithrilComponent} from "../../../jsx/mithril-component";
import * as styles from "./index.scss";
import {JobDuration, JobDurationStrategyHelper} from "./models/job_duration_stratergy_helper";
import {StageInstance} from "./models/stage_instance";
import {JobJSON} from "./models/types";

const classnames = bind(styles);

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
    const isJobInProgress = jobDuration.isJobInProgress;

    const tooltipItems = {
      'Waiting':             jobDuration.waitTimeForDisplay,
      'Preparing':           jobDuration.preparingTimeForDisplay,
      'Building':            jobDuration.buildTimeForDisplay,
      'Uploading Artifacts': jobDuration.uploadingArtifactTimeForDisplay,
      'Total Time':          jobDuration.totalTimeForDisplay,
      'Scheduled At':        jobDuration.startTimeForDisplay,
      'Completed At':        jobDuration.endTimeForDisplay
    };

    return (<div>
      <div data-test-id="progress-bar-container-div" class={styles.progressBarContainer}>
        <div class={`${styles.waiting}`} style={`width: ${jobDuration.waitTimePercentage}%`}/>
        <div class={`${styles.preparing}`} style={`width: ${jobDuration.preparingTimePercentage}%`}/>
        <div className={`${styles.building}`} style={`width: ${jobDuration.buildTimePercentage}%`}/>
        <div className={`${styles.uploadingArtifacts}`} style={`width: ${jobDuration.uploadingArtifactTimePercentage}%`}/>
        <div className={`${styles.unknown}`} style={`width: ${jobDuration.unknownTimePercentage}%`}/>
      </div>

      <div data-test-id="progress-bar-tooltip" class={styles.progressBarTooltip}>
        {
          Object.keys(tooltipItems).map(key => {
            const value = tooltipItems[key];
            const isUnknownProperty = isUnknown(value, isJobInProgress);
            const valueForDisplay = isUnknownProperty ? "unknown" : value;
            return (
              <div className={styles.tooltipKeyValuePair}>
                <div className={styles.tooltipKey}>{key} :</div>
                <div className={classnames({[styles.unknownProperty]: isUnknownProperty})}>{valueForDisplay}</div>
              </div>
            );
          })
        }
      </div>
    </div>);
  }
}

function isUnknown(val: string, isJobInProgress: boolean) {
  return isJobInProgress && val.toLowerCase() === "00s";
}
