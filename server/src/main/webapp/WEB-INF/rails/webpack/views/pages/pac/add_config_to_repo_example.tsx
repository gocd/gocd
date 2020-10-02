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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import css from "./styles.scss";

export class AddToRepoCommandExample extends MithrilComponent {
  view(vnode: m.Vnode) {
    return <section class={css.addToRepoExample}>
      <h4 class={css.minorHeading}>Example Using Git: Add File to Root of the Repo</h4>
      <pre>
        <code>
          <div class={css.line}>cd /path/to/repo</div>
          <div class={css.line}>git add -- pipeline.gocd.yaml</div>
          <div class={css.line}>git commit -m "My PaC configs"</div>
          <div class={css.line}>git push</div>
        </code>
      </pre>
    </section>;
  }
}
