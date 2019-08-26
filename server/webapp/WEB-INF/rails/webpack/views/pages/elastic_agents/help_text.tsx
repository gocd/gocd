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
import {LoremIpsum} from "lorem-ipsum";
import m from "mithril";

export class HelpText extends MithrilViewComponent {
  view(vnode: m.Vnode) {
    return (
      <table>
        <tr>
          <td><img src="https://placeimg.com/160/120/tech"/></td>
          <td>
            <h3>Cluster Profile</h3>
            {new LoremIpsum().generateParagraphs(1)}</td>
        </tr>
        <tr>
          <td><img src="https://placeimg.com/160/120/tech"/></td>
          <td>
            <h3>Elastic Profile</h3>
            {new LoremIpsum().generateParagraphs(1)}
          </td>
        </tr>
        <tr>
          <td><img src="https://placeimg.com/160/120/tech"/></td>
          <td>
            <h3>Link to Jobs</h3>
            {new LoremIpsum().generateParagraphs(1)}</td>
        </tr>
      </table>
    );
  }
}
