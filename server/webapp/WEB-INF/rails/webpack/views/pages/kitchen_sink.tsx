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

import * as Icons from "../components/icons/index";
import * as m from 'mithril';
import {HelloWorld} from "../components/hello_world/index";
import {KeyValuePair} from "../components/key_value_pair/index";
import {MithrilComponent} from "../../jsx/mithril-component";

const HeaderPanel = require('views/components/header_panel');
const Button = require('views/components/button');
const Accordion = require('views/components/accordion');
const CollapsiblePanel = require('views/components/collapsible_panel');

class AccordionExamples<A = {}> extends MithrilComponent<A> {
  view() {
    return (
      <div>
        <Accordion title={'Expanded accordion'}>
          <div>some text inside the accordion</div>
        </Accordion>

        <Accordion title={'Collapsed accordion'} collapse={true}>
          <div>some text inside the accordion</div>
        </Accordion>
      </div>
    );
  }
}

export = class KitchenSink extends MithrilComponent<null> {
  view() {
    return (
      <main class="main-container">
        <HeaderPanel title="Kitchen Sink"/>
        <h3>CollapsiblePanel</h3>
        <CollapsiblePanel header={<div>Collapsible Panel header</div>}
                          actions={<button>foo</button>}>
          <div> Anything can go in the body</div>
        </CollapsiblePanel>
        <hr/>

        <h3>Icons</h3>
        <Icons.Settings/>
        <Icons.Analytics/>
        <Icons.Edit/>
        <Icons.Clone/>
        <Icons.Delete/>
        <Icons.Lock/>
        <hr/>

        <HelloWorld id="foo"/>
        <br/>

        <div>
          <h1>Work in progress components!</h1>
        </div>

        <h3>This is a simple button:</h3>
        <Button/>

        <h3>Some examples of accordions</h3>
        <AccordionExamples/>

        <h3>Some examples of key value pairs</h3>
        <KeyValuePair data={
          {
            'First Name': 'Jon',
            'Last Name': 'Doe',
            'email': 'jdoe@example.com',
          }
        }/>
      </main>
    );
  }
};
