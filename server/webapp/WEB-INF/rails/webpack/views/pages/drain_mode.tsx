/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as m from "mithril";
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import {HeaderPanel} from "views/components/header_panel";
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {Page} from "views/pages/page";

interface State {
  drainModeSettings: DrainModeSettings;
}

export class DrainModePage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): JSX.Element | undefined {
    return <DrainModeWidget settings={vnode.state.drainModeSettings} />;
  }

  headerPanel() {
    return <HeaderPanel title="Server Drain Mode"/>;
  }

  fetchData(vnode: m.Vnode<null, State>) {
    return DrainModeSettings.get().then((settings) => {
      settings.do((successResponse) => vnode.state.drainModeSettings = successResponse.body,
                  () => this.setErrorState());
    });
  }

  pageName(): string {
    return "Server Drain Mode";
  }
}
