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
import {Approval, ApprovalType, Stage} from "models/pipeline_configs/stage";
import {Form, FormBody} from "views/components/forms/form";
import {TextField} from "views/components/forms/input_fields";
import {TooltipSize} from "views/components/tooltip";
import * as Tooltip from "views/components/tooltip";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import * as css from "./components.scss";

interface Attrs {
  stage: Stage;
}

export class StageEditor extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stage;
    return <FormBody>
      <Form last={true} compactForm={true}>
        <TextField label="Stage Name" placeholder="e.g., Test-and-Report" required={true} property={stage.name} errorText={stage.errors().errorsForDisplay("name")}/>
        <AdvancedSettings>
          <div class={css.approvalTypeSelectors}>
            <label>This stage runs:</label>
            <input class={css.approvalTypeSelector} type="radio" checked={stage.approval().isSuccessType()} value="success" onclick={this.changeApprovalType.bind(this, ApprovalType.success, stage.approval())} />
            <label>Automatically</label>
            <input class={css.approvalTypeSelector} type="radio" checked={!stage.approval().isSuccessType()} value="manual" onclick={this.changeApprovalType.bind(this, ApprovalType.manual, stage.approval())} />
            <label>Manually</label>
            <Tooltip.Help size={TooltipSize.medium}
                          content="Automatically or Manually trigger your stage to run after preceding stage completes" />
          </div>
        </AdvancedSettings>
      </Form>
    </FormBody>;
  }

  changeApprovalType(type: ApprovalType, approval: Approval, event: Event) {
    event.stopPropagation();
    approval.type(type);
  }
}
