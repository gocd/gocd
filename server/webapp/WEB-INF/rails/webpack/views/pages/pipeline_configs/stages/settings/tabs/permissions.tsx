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
import {StageAuthorization, StageConfig, StringListInterface} from "models/new_pipeline_configs/stage_configuration";
import {QuickAddField} from "views/components/forms/input_fields";
import * as Icons from "views/components/icons/index";
import {SwitchBtn} from "views/components/switch";
import styles from "../stage_settings.scss";

interface Attrs {
  stageConfig: Stream<StageConfig>;
}

interface PermissionDefinitionState {
  property: Stream<string>;
  onAdd: () => void;
}

interface PermissionDefinitionAttrs {
  entity: StringListInterface;
}

class PermissionDefinitionWidget extends MithrilComponent<PermissionDefinitionAttrs, PermissionDefinitionState> {
  oninit(vnode: m.Vnode<PermissionDefinitionAttrs, PermissionDefinitionState>) {
    vnode.state.property = Stream();
    vnode.state.onAdd    = () => {
      vnode.attrs.entity.add(vnode.state.property());
      vnode.state.property("");
    };
  }

  view(vnode: m.Vnode<PermissionDefinitionAttrs, PermissionDefinitionState>) {
    return <div class={styles.userAndRolesPermissions} data-test-id={`${vnode.attrs.entity.type()}-permissions`}>
      <div class={styles.permissionTitle}>{vnode.attrs.entity.type()}s</div>
      <QuickAddField onclick={vnode.state.onAdd}
                     dataTestId={`${vnode.attrs.entity.type()}-input`}
                     property={vnode.state.property}
                     buttonDisableReason={`Specify ${vnode.attrs.entity.type()}!`}/>
      {
        vnode.attrs.entity.list().map((entity) => {
          return <div class={styles.userCell}>
            <span data-test-id={`show-${vnode.attrs.entity.type()}-${entity}`}>{entity}</span>
            <Icons.Close iconOnly={true} onclick={vnode.attrs.entity.remove.bind(vnode.attrs.entity, entity)}/>
          </div>;
        })
      }
    </div>;
  }
}

class AddStagePermissionWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stageConfig();
    if (stage.approval().inheritFromPipelineGroup()) {
      return;
    }
    const authorization: StageAuthorization = vnode.attrs.stageConfig().approval().authorization()!;

    return (<div data-test-id="specify-stage-permissions-locally">
      <span class={styles.specifyLocallyMessage}>Specify locally:</span>
      <div class={styles.usersAndRolesContainer}>
        <PermissionDefinitionWidget entity={authorization.users}/>
        <PermissionDefinitionWidget entity={authorization.roles}/>
      </div>
    </div>);
  }
}

export class StagePermissionsTab extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stageConfig();

    return <div data-test-id="stage-permissions-tab">
      <div class={styles.defaultPermissionMessageContainer} data-test-id="default-permission-message-container">
        <span class={styles.defaultPermissionMessageBody} data-test-id="default-permission-message-body">All System Administrators and Pipeline Group Administrators can operate on this pipeline.</span>
        <span class={styles.defaultPermissionHelpMessage} data-test-id="default-permission-help-message">(This permission can not be overridden!)</span>
      </div>
      <div class={styles.permissionsContainer}>
        <SwitchBtn label="Inherit permissions from Pipeline Group:"
                   field={stage.approval().inheritFromPipelineGroup} small={true}/>
      </div>
      <AddStagePermissionWidget {...vnode.attrs}/>
    </div>;
  }
}
