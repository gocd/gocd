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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import {Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import * as Icons from "views/components/icons/index";
import styles from "./stages.scss";

interface StageBoxAttrs {
  stage: StageConfig;
}

export class StageBoxWidget extends MithrilViewComponent<StageBoxAttrs> {
  view(vnode: m.Vnode<StageBoxAttrs>) {
    return <div class={styles.stageBox}>
      <div class={styles.stageNameHeader} data-test-id={`stage-header-for-${vnode.attrs.stage.name()}`}>
        <span>{vnode.attrs.stage.name()}</span>
        <div class={styles.stageSettingsIconWrapper}><Icons.Settings iconOnly={true}/></div>
      </div>
      <div class={styles.stageContent} data-test-id={`stage-body-for-${vnode.attrs.stage.name()}`}>
        {"this is some text! ".repeat(Math.round(Math.random() * 30))}
      </div>
      <div class={styles.stageBtnGroup} data-test-id={`stage-btn-group-for-${vnode.attrs.stage.name()}`}>
        <Secondary small={true}>Add Job</Secondary>
      </div>
    </div>;
  }
}

interface State {
  visibleStagesCount: Stream<number>;
  firstVisibleStageIndex: Stream<number>;
  moveStageToLeft: () => void;
  moveStageToRight: () => void;
  canMoveToLeft: () => boolean;
  canMoveToRight: () => boolean;
}

interface Attrs {
  stages: Stream<StageConfig[]>;
}

export class StagesWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.visibleStagesCount     = Stream(5);
    vnode.state.firstVisibleStageIndex = Stream(0);

    vnode.state.canMoveToLeft = () => {
      return vnode.state.firstVisibleStageIndex() > 0;
    };

    vnode.state.canMoveToRight = () => {
      const totalStages = vnode.attrs.stages().length;
      return (totalStages - (vnode.state.firstVisibleStageIndex()) > vnode.state.visibleStagesCount());
    };

    vnode.state.moveStageToLeft = () => {
      if (vnode.state.canMoveToLeft()) {
        vnode.state.firstVisibleStageIndex(vnode.state.firstVisibleStageIndex() - 1);
      }
    };

    vnode.state.moveStageToRight = () => {
      if (vnode.state.canMoveToRight()) {
        vnode.state.firstVisibleStageIndex(vnode.state.firstVisibleStageIndex() + 1);
      }
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const firstVisibleStageIndex = vnode.state.firstVisibleStageIndex();
    const visibleStagesCount     = vnode.state.visibleStagesCount();
    const stagesToRender         = vnode.attrs.stages().slice().splice(firstVisibleStageIndex, visibleStagesCount);

    return <div data-test-id="stages-component" class={styles.stagesMainComponent}>
      <CollapsiblePanel header={<div>Stages</div>} expanded={true}>
        <div class={styles.stagesBody}>
          <div onclick={vnode.state.moveStageToLeft} className={styles.moveStageToLeftRight}>{"<"}</div>
          <div className={styles.stagesContainer}>
            {
              stagesToRender.map((stage: StageConfig, index) => [
                <StageBoxWidget stage={stage}/>,
                stagesToRender.length - 1 !== index
                  ? <div className={styles.forwardIconWrapper}><Icons.Forward iconOnly={true}/></div>
                  : undefined
              ])
            }
          </div>
          <div onclick={vnode.state.moveStageToRight} className={styles.moveStageToLeftRight}>{">"}</div>
        </div>
      </CollapsiblePanel>
    </div>;
  }
}
