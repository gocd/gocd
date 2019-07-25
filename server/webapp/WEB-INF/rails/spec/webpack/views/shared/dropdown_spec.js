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
import {TestHelper} from "views/pages/spec/test_helper";

const m             = require('mithril');
const Stream        = require('mithril/stream');
const Dropdown      = require('views/shared/dropdown');

describe('Dropdown widget', () => {

  const helper = new TestHelper();

  const model = {
    selectedAnimal: Stream("cat"),
    animals:        [{
      id:   "cat",
      text: "Cat"
    },
      {
        id:   "dog",
        text: "Dog"
      },
      {
        id:   "rabbit",
        text: "Rabbit"
      }
    ]
  };

  beforeEach(() => {
    helper.mount(() => {
      return <div>
        <span className="other-node">Some text</span>
        <Dropdown model={model}
                  label="Select an item:"
                  attrName="selectedAnimal"
                  items={model.animals}/>;
      </div>;
    });
  });

  afterEach(() => {
    helper.unmount();
  });

  it('should open dropdown on click', () => {
    expect(helper.find('.c-dropdown')).not.toHaveClass('open');
    helper.click('.c-dropdown_head');
    expect(helper.find('.c-dropdown')).toHaveClass('open');
  });

  it('should close dropdown when an item is selected', () => {
    helper.click('.c-dropdown_head');
    expect(helper.find('.c-dropdown')).toHaveClass('open');
    helper.click('.c-dropdown_item');
    expect(helper.find('.c-dropdown')).not.toHaveClass('open');
  });

  it('should open drowndown when down-arrow is clicked', () => {
    expect(helper.find('.c-dropdown')).not.toHaveClass('open');
    helper.click('.c-down-arrow');
    expect(helper.find('.c-dropdown')).toHaveClass('open');
  });

});
