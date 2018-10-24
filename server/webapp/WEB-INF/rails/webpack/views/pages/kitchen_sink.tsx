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

import * as m from 'mithril';

import {HelloWorld} from "../components/hello_world/index";
import {MithrilComponent} from "../../jsx/mithril-component";

import * as c from "../components";

export = class KitchenSink extends MithrilComponent<null> {
  view() {
    return (
      <main class="main-container">
        <c.HeaderPanel title="Kitchen Sink"/>
        <h3>CollapsiblePanel</h3>
        <c.CollapsiblePanel header={<div>Collapsible Panel header</div>}
                            actions={<button>foo</button>}>
          <div> Anything can go in the body</div>
        </c.CollapsiblePanel>
        <hr/>

        <h3>Icons</h3>
        <c.IconGrop>
          <c.Icons.Settings/>
          <c.Icons.Analytics/>
          <c.Icons.Edit/>
          <c.Icons.Clone/>
          <c.Icons.Delete/>
          <c.Icons.Lock/>
        </c.IconGrop>
        <hr/>

        <HelloWorld id="foo"/>
        <br/>

        <div>
          <h1>Work in progress components!</h1>
        </div>

        <h3>Buttons:</h3>
        <c.Buttons.Primary>Primary Button</c.Buttons.Primary>
        <c.Buttons.Secondary>Secondary Button</c.Buttons.Secondary>
        <c.Buttons.Reset>Reset Button</c.Buttons.Reset>
        <c.Buttons.Cancel>Cancel</c.Buttons.Cancel>

        <br/>
        <h3>Small Buttons:</h3>
        <c.Buttons.Primary small={true}>Small Primary Button</c.Buttons.Primary>
        <c.Buttons.Secondary small={true}>Small Secondary Button</c.Buttons.Secondary>
        <c.Buttons.Reset small={true}>Small Reset Button</c.Buttons.Reset>
        <c.Buttons.Cancel small={true}>Cancel</c.Buttons.Cancel>

        <h3>Some examples of accordions</h3>

        <h3>Some examples of key value pairs</h3>
        <c.KeyValuePair data={
          {
            'First Name':                                       'Jon',
            'Last Name':                                        'Doe',
            'email':                                            'jdoe@example.com',
            'some really really really really really long key': 'This is really really really really really really really really really really long junk value'
          }
        }/>

        <h3>Some examples of inline key value pairs</h3>
        <c.KeyValuePair inline={true} data={
          {
            'Plugin':                                           'my-fancy-plugin-name',
            'some really really really really really long key': 'This is really really really really really really really really really really long junk value'
          }
        }/>
      </main>
    );
  }
};
