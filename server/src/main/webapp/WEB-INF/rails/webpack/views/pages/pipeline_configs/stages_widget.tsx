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

//todo: Include types for shopify/draggable once available, watch out for PR: https://github.com/Shopify/draggable/pull/115
//@ts-ignore
import Sortable from "@shopify/draggable/lib/es5/sortable";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {StageConfig} from "models/new_pipeline_configs/stage_configuration";
import {Secondary} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import * as Icons from "views/components/icons/index";
import {StageSettingsModal} from "views/pages/pipeline_configs/stages/settings/stage_settings_modal";
import styles from "./stages.scss";

interface StageBoxState {
  onStageSettingsClick: (e: MouseEvent) => void;
}

interface StageBoxAttrs {
  stage: StageConfig;
  isLastVisibleStage: Stream<boolean>;
}

export class StageBoxWidget extends MithrilComponent<StageBoxAttrs, StageBoxState> {
  oninit(vnode: m.Vnode<StageBoxAttrs, StageBoxState>) {
    vnode.state.onStageSettingsClick = (e: MouseEvent) => {
      e.stopPropagation();
      new StageSettingsModal(Stream(vnode.attrs.stage)).render();
    };
  }

  view(vnode: m.Vnode<StageBoxAttrs, StageBoxState>) {
    const triggerNextStageIcon = vnode.attrs.isLastVisibleStage()
      ? undefined
      : <div className={styles.forwardIconWrapper}><Icons.Forward iconOnly={true}/></div>;

    return <div class={`${styles.stageWithManualApprovalContainer} stage-box`}>
      <div data-test-id="stage-box" className={styles.stageBox}>
        <div className={styles.stageNameHeader} data-test-id={`stage-header-for-${vnode.attrs.stage.name()}`}>
          <i className={`icon_drag_stage ${styles.iconDrag}`}/>
          <span className={styles.stageName}>{vnode.attrs.stage.name()}</span>
          <div className={styles.stageSettingsIconWrapper}>
            <Icons.Settings iconOnly={true} onclick={vnode.state.onStageSettingsClick}/>
          </div>
        </div>
        <div className={styles.stageContent} data-test-id={`stage-body-for-${vnode.attrs.stage.name()}`}>
          {"Body for stage: " + vnode.attrs.stage.name()}
        </div>
        <div className={styles.stageBtnGroup} data-test-id={`stage-btn-group-for-${vnode.attrs.stage.name()}`}>
          <Secondary small={true}>Add Job</Secondary>
        </div>
      </div>
      {triggerNextStageIcon}
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
    const totalWindow: number       = document.body.clientWidth;
    const marginPaddings: number    = 300;
    const eachStageBoxWidth: number = 280;
    const numberOfStages            = Math.floor((totalWindow - marginPaddings) / eachStageBoxWidth);

    vnode.state.visibleStagesCount     = Stream(numberOfStages > 0 ? numberOfStages : 1);
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

  oncreate(vnode: m.Vnode<Attrs, State>) {
    const sortable = new Sortable(document.querySelectorAll(".stages-container"), {
      draggable: ".stage-box",
      handle: ".icon_drag_stage",
    });

    sortable.on("sortable:sorted", (event: any) => {
      const stageToReorder = event.data.oldIndex;
      const whereToPlace   = event.data.newIndex;
      const stages         = vnode.attrs.stages();
      stages.splice(whereToPlace, 0, stages.splice(stageToReorder, 1)[0]);
      vnode.attrs.stages(stages);
    });

    sortable.on("sortable:stop", m.redraw);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const firstVisibleStageIndex = vnode.state.firstVisibleStageIndex();
    const visibleStagesCount     = vnode.state.visibleStagesCount();
    const stagesToRender         = vnode.attrs.stages().slice().splice(firstVisibleStageIndex, visibleStagesCount);

    return <div data-test-id="stages-component" class={styles.stagesMainComponent}>
      <CollapsiblePanel header={<div>Stages</div>} expanded={true}>
        <div class={styles.stagesBody}>
          <div onclick={vnode.state.moveStageToLeft} className={styles.moveStageToLeftRight}>{"<"}</div>
          <div className={`stages-container ${styles.stagesContainer}`}>
            {
              stagesToRender.map((stage: StageConfig, index) => {
                return <StageBoxWidget stage={stage}
                                       key={stage.name() + index}
                                       isLastVisibleStage={Stream(stagesToRender.length - 1 === index)}/>;
              })
            }
          </div>
          <div onclick={vnode.state.moveStageToRight} className={styles.moveStageToLeftRight}>{">"}</div>
        </div>
      </CollapsiblePanel>
    </div>;
  }
}
