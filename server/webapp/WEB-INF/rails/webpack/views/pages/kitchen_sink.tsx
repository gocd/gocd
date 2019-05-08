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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {TriStateCheckbox} from "models/tri_state_checkbox";
import {ButtonGroup, ButtonIcon} from "views/components/buttons";
import * as Buttons from "views/components/buttons/index";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Ellipsize} from "views/components/ellipsize";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {EncryptedValue} from "views/components/forms/encrypted_value";
import {Form} from "views/components/forms/form";
import {
  CheckboxField, CopyField,
  PasswordField, QuickAddField,
  SearchField, SearchFieldWithButton,
  Switch,
  TextField,
  TriStateCheckboxField
} from "views/components/forms/input_fields";
import {HeaderPanel} from "views/components/header_panel";
import {IconGroup} from "views/components/icons";
import * as Icons from "views/components/icons/index";
import {KeyValuePair} from "views/components/key_value_pair";
import {Size} from "views/components/modal";
import {SampleModal} from "views/components/modal/sample";
import {Tabs} from "views/components/tab";
import {Table, TableSortHandler} from "views/components/table";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";

let type               = "a";
const formValue        = stream("initial value");
const searchFieldValue = stream("");
const checkboxField    = stream(false);

const passwordValue          = stream(new EncryptedValue({clearText: "p@ssword"}));
const encryptedPasswordValue = stream(new EncryptedValue({cipherText: "AES:junk:more-junk"}));

const triStateCheckbox = stream(new TriStateCheckbox());

const switchStream: Stream<boolean> = stream(false);
const reallyLongText                = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.";

export class KitchenSink extends MithrilViewComponent<null> {
  provider: DynamicSuggestionProvider = new DynamicSuggestionProvider(type);

  view(vnode: m.Vnode<null>) {
    const model: Stream<string> = stream();
    return (
      <div>
        <HeaderPanel title="Kitchen Sink" sectionName={"Admin"}/>
        <FlashMessage type={MessageType.info} message={"This page is awesome!"}/>
        <FlashMessage type={MessageType.success} message={"Everything works as expected!"}/>
        <FlashMessage type={MessageType.warning} message={"This might not work!"}/>
        <FlashMessage type={MessageType.alert} message={"Disaster Happened!"}/>
        <FlashMessage type={MessageType.alert} dismissible={true}
                      message={"Disaster Happened! But you can ignore by closing it"}/>

        <br/>
        <h3>CollapsiblePanel</h3>
        <CollapsiblePanel header={<div>Collapsible Panel header</div>}
                          actions={<button>foo</button>}>
          <div> Anything can go in the body</div>
        </CollapsiblePanel>
        <hr/>

        <h3>Search Box</h3>
        <SearchField placeholder="Search"
                     label="Search for a username"
                     property={searchFieldValue}/>
        {/*<SearchField width={350} attrName="search" model={null} placeholder="Some placeholder"/>*/}

        <h3>Modal</h3>
        <Buttons.Primary onclick={() => {
          this.createModal(Size.small);
        }}>Open Small Modal</Buttons.Primary>
        <Buttons.Primary onclick={() => {
          this.createModal(Size.medium);
        }}>Open Medium Modal</Buttons.Primary>
        <Buttons.Primary onclick={() => {
          this.createModal(Size.large);
        }}>Open Large Modal</Buttons.Primary>

        <h3>Tooltip</h3>
        Small Info tooltip: <Tooltip.Info content={"this is a very small information tooltip."}/>
        Medium Help tooltip: <Tooltip.Help size={TooltipSize.medium}
                                           content={"Lorem Ipsum is simply dummy text of the printing and typesetting industry. " +
                                           "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, " +
                                           "when an unknown printer took a galley of type and scrambled it to make a type specimen book."}/>
        Large Info tooltip: <Tooltip.Info size={TooltipSize.large}
                                          content={"Contrary to popular belief, Lorem Ipsum is not simply random text. It has roots in a piece of classical Latin literature from 45 BC, making it over 2000 years old. Richard McClintock, a Latin professor at Hampden-Sydney College in Virginia, looked up one of the more obscure Latin words, consectetur, from a Lorem Ipsum passage, and going through the cites of the word in classical literature, discovered the undoubtable source. Lorem Ipsum comes from sections 1.10.32 and 1.10.33 of \"de Finibus Bonorum et Malorum\" (The Extremes of Good and Evil) by Cicero, written in 45 BC. This book is a treatise on the theory of ethics, very popular during the Renaissance. The first line of Lorem Ipsum, \"Lorem ipsum dolor sit amet..\", comes from a line in section 1.10.32.\n" +
                                          "\n" +
                                          "The standard chunk of Lorem Ipsum used since the 1500s is reproduced below for those interested. Sections 1.10.32 and 1.10.33 from \"de Finibus Bonorum et Malorum\" by Cicero are also reproduced in their exact original form, accompanied by English versions from the 1914 translation by H. Rackham.\n" +
                                          "\n"}/>

        <h3>Icons</h3>
        <Icons.Settings onclick={() => alert("You pressed settings button!")}/> <br/>
        <br/>
        <p>
          <IconGroup>
            <Icons.Spinner iconOnly={true}/> <br/>
            <Icons.Check iconOnly={true}/> <br/>
            <Icons.Minus iconOnly={true}/> <br/>
            <Icons.InfoCircle iconOnly={true}/> <br/>
            <Icons.QuestionCircle iconOnly={true}/> <br/>
          </IconGroup>
        </p>
        <p>
          <IconGroup>
            <Icons.Settings onclick={() => alert("You pressed settings button!")}/>
            <Icons.Analytics onclick={() => alert("You pressed analytics button!")}/>
            <Icons.Edit onclick={() => alert("You pressed edit button!")}/>
            <Icons.Clone onclick={() => alert("You pressed clone button!")}/>
            <Icons.Delete onclick={() => alert("You pressed delete button!")}/>
            <Icons.Lock onclick={() => alert("You pressed lock button!")}/>
            <Icons.Close onclick={() => alert("You pressed close button!")}/>
            <Icons.QuestionMark onclick={() => alert("You pressed question button!")}/>
          </IconGroup>
        </p>
        <p>
          <IconGroup>
            <Icons.Settings disabled={true}/>
            <Icons.Analytics disabled={true}/>
            <Icons.Edit disabled={true}/>
            <Icons.Clone disabled={true}/>
            <Icons.Delete disabled={true}/>
            <Icons.Lock disabled={true}/>
            <Icons.Close disabled={true}/>
            <Icons.QuestionMark disabled={true}/>
          </IconGroup>
        </p>
        <hr/>

        <br/>
        <h3>Switches</h3>
        <Switch property={switchStream} label={"Do you like switch?"} errorText={"Some error"}
                helpText={"Click if you like switch"}/><br/>

        <Switch property={switchStream} label={"Do you like small switch?"}
                helpText={"Click if you like small switch"} small={true}/><br/>
        <label>Switch state: {`${switchStream()}`}</label>
        <hr/>
        <br/>

        <h3>Buttons without a grouping:</h3>
        <Buttons.Secondary>Button</Buttons.Secondary>
        <Buttons.Secondary>Secondary Button</Buttons.Secondary>
        <Buttons.Reset>Reset Button</Buttons.Reset>
        <Buttons.Cancel>Cancel</Buttons.Cancel>
        <Buttons.Danger>Delete</Buttons.Danger>
        <hr/>

        <h3>Button with icon:</h3>
        <Buttons.Default dropdown={true} icon={ButtonIcon.FILTER}>Button with icon</Buttons.Default>

        <h3>Button link:</h3>
        <Buttons.Link>Button link</Buttons.Link>

        <h3>Button group with buttons:</h3>
        <ButtonGroup>
          <Buttons.Secondary>Button</Buttons.Secondary>
          <Buttons.Secondary>Button</Buttons.Secondary>
          <Buttons.Secondary>Button</Buttons.Secondary>
          <Buttons.Secondary>Button</Buttons.Secondary>
          <Buttons.Secondary>Button</Buttons.Secondary>
        </ButtonGroup>
        <hr/>

        <h3>Button group with disabled buttons:</h3>
        <ButtonGroup>
          <Buttons.Primary disabled={true}>Disabled Primary Button</Buttons.Primary>
          <Buttons.Secondary disabled={true}>Disabled Secondary Button</Buttons.Secondary>
          <Buttons.Reset disabled={true}>Disabled Reset Button</Buttons.Reset>
          <Buttons.Cancel disabled={true}>Disabled Cancel</Buttons.Cancel>
          <Buttons.Danger disabled={true}>Disabled Danger</Buttons.Danger>
        </ButtonGroup>

        <br/>
        <h3>Small Buttons:</h3>
        <ButtonGroup>
          <Buttons.Primary small={true}>Small Primary Button</Buttons.Primary>
          <Buttons.Secondary small={true}>Small Secondary Button</Buttons.Secondary>
          <Buttons.Reset small={true}>Small Reset Button</Buttons.Reset>
          <Buttons.Cancel small={true}>Cancel</Buttons.Cancel>
        </ButtonGroup>

        <h3>Some examples of key value pairs</h3>
        <KeyValuePair data={new Map(
          [
            ["First Name", "Jon"],
            ["Last Name", "Doe"],
            ["email", "jdoe@example.com"],
            ["some really really really really really long key", "This is really really really really really really really really really really long junk value"]
          ])
        }/>

        <h3>Some examples of inline key value pairs</h3>
        <KeyValuePair inline={true} data={new Map(
          [
            ["Plugin", "my-fancy-plugin-name"],
            ["some really really really really really long key", "This is really really really really really really really really really really long junk value"],
            ["Instructions", "Run a manual sweep of anomalous airborne or electromagnetic readings. Radiation levels in our atmosphere have increased by 3,000 percent."],
            ["Version", "3.11 for workgroups"]
          ])
        }/>

        <h3>Forms</h3>
        <Form>
          <TextField required={true}
                     helpText="Enter your username here"
                     label="Username"
                     placeholder="username"
                     property={formValue}/>
          <TextField required={true}
                     helpText="Enter your username here"
                     placeholder="username"
                     label="Username"
                     property={formValue}/>
          <TextField required={true}
                     errorText="This field must be present"
                     helpText="Lorem ipsum is the dummy text used by the print and typesetting industry"
                     label="Lorem ipsum"
                     property={formValue}/>
          <CheckboxField required={true}
                         errorText={!checkboxField() ? "You must get some icecream" : undefined}
                         helpText="Do you want ice cream?"
                         label="Do you want ice cream?"
                         property={checkboxField}/>

          <TriStateCheckboxField
            label="Tri state checkbox"
            property={triStateCheckbox}
            helpText={`Tristate state is: ${triStateCheckbox().state()}`}/>

          <PasswordField label="Editable password field"
                         placeholder="password"
                         property={passwordValue}/>
          <PasswordField label="Locked password field"
                         placeholder="password"
                         property={encryptedPasswordValue}/>
        </Form>

        <QuickAddField property={formValue} buttonDisableReason={"Add text to enable quick add"}/>
        <CopyField property={formValue} buttonDisableReason={"Add text to enable quick add"}/>

        <SearchFieldWithButton property={formValue} buttonDisableReason={"Add text to enable search"}/>

        <h3>Table</h3>
        <Table headers={["Pipeline", "Stage", "Job", "Action"]}
               data={[
                 ["WindowsPR", <label>test</label>, "jasmine", <a href="#!">Go to report</a>],
                 ["LinuxPR", "build", "clean", "Foo"]
               ]}/>

        <br/>
        <h3>Sortable Table</h3>
        <Table data={pipelineData()} headers={["Pipeline", "Stage", "Job"]}
               sortHandler={tableSortHandler}/>

        <br/>
        <Tabs tabs={["One", "Two"]} contents={[
          <div>
            <h3>Expandable ellipses example</h3>
            <Ellipsize text={reallyLongText} size={20}/>
            <hr/>
            <h3>Fixed ellipses example</h3>
            <Ellipsize text={reallyLongText} fixed={true}/>
          </div>,
          "Content for two"]}/>

        <br/>
        <h3>Dynamic autocomplete</h3>
        <AutocompleteField label="Dynamic" property={model} provider={this.provider}/>

        <Buttons.Primary onclick={this.toggleType.bind(this)}>Click to change type!</Buttons.Primary>
      </div>
    );
  }

  toggleType() {
    if (type === "a") {
      type = "b";
    } else {
      type = "a";
    }
    this.provider.setType(type);
    this.provider.update();
  }

  private createModal(size: Size) {
    new SampleModal(size).render();
  }
}

const pipelineData = stream(
  [
    ["WindowsPR", "test", "jasmine"],
    ["WindowsPR", "build", "installer"],
    ["WindowsPR", "upload", "upload"],
    ["LinuxPR", "build", "clean"],
    ["LinuxPR", "test", "clean"],
    ["LinuxPR", "build", "clean"]
  ]);

class DummyTableSortHandler extends TableSortHandler {
  private sortOrders = new Map();

  constructor() {
    super();
    this.getSortableColumns().forEach((c) => this.sortOrders.set(c, -1));
  }

  onColumnClick(columnIndex: number): void {
    this.sortOrders.set(columnIndex, this.sortOrders.get(columnIndex) * -1);
    pipelineData()
      .sort((element1, element2) => DummyTableSortHandler.compare(element1,
                                                                  element2,
                                                                  columnIndex) * this.sortOrders.get(
        columnIndex));
  }

  getSortableColumns(): number[] {
    return [0, 1];
  }

  private static compare(element1: any, element2: any, index: number) {
    return element1[index] < element2[index] ? -1 : element1[index] > element2[index] ? 1 : 0;
  }
}

const tableSortHandler = new DummyTableSortHandler();

class DynamicSuggestionProvider extends SuggestionProvider {
  private type: string;

  constructor(type: string) {
    super();
    this.type = type;
  }

  setType(value: string) {
    this.type = value;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    if (this.type === "a") {
      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve([
                  "first", "second", "third", "fourth"
                ]);
      });
    } else if (this.type === "b") {
      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve([
                  "input", "div", "span", "select", "button"
                ]);
      });
    }

    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([]);
    });
  }
}
