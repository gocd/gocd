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
import m from "mithril";
import Stream from "mithril/stream";
import {TriStateCheckbox, TristateState} from "models/tri_state_checkbox";
import {
  ButtonGroup,
  ButtonIcon,
  Cancel,
  Danger,
  Default,
  Link,
  Primary,
  Reset,
  Secondary
} from "views/components/buttons";
import {DummyDropdownButton} from "views/components/buttons/sample";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {Dropdown} from "views/components/dropdown";
import {Ellipsize} from "views/components/ellipsize";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {EncryptedValue} from "views/components/forms/encrypted_value";
import {Form} from "views/components/forms/form";
import {
  CheckboxField,
  CopyField,
  PasswordField,
  QuickAddField,
  RadioField,
  SearchField,
  SearchFieldWithButton,
  Switch,
  TextField,
  TriStateCheckboxField
} from "views/components/forms/input_fields";
import {LiveValidatingInputField} from "views/components/forms/live_validating_input";
import {HeaderPanel} from "views/components/header_panel";
import {IconGroup} from "views/components/icons";
import * as Icons from "views/components/icons/index";
import {KeyValuePair} from "views/components/key_value_pair";
import {Size} from "views/components/modal";
import {SampleModal} from "views/components/modal/sample";
import {PaginationWidget} from "views/components/pagination";
import {Pagination} from "views/components/pagination/models/pagination";
import {Tabs} from "views/components/tab";
import {SortOrder, Table, TableSortHandler} from "views/components/table";
import * as Tooltip from "views/components/tooltip";
import {TooltipSize} from "views/components/tooltip";
import {Step, Wizard} from "views/components/wizard";

let type               = "a";
const formValue        = Stream("initial value");
const searchFieldValue = Stream("");
const checkboxField    = Stream(false);

const passwordValue          = Stream(new EncryptedValue({clearText: "p@ssword"}));
const encryptedPasswordValue = Stream(new EncryptedValue({cipherText: "AES:junk:more-junk"}));

const triStateCheckbox = Stream(new TriStateCheckbox());

const radioValue = Stream("thin-crust");

const switchStream   = Stream(false);
const reallyLongText = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.";

const dropdownProperty = Stream("apple");
const dropdownItems    = [
  {
    id: "apple",
    value: "I love apple!"
  },
  {
    id: "banana",
    value: "I love banana!"
  }
];

const largeNumberOfPages = Stream(Pagination.fromJSON({offset: 50, total: 9999, page_size: 10}));
const smallNumberOfPages = Stream(Pagination.fromJSON({offset: 0, total: 70, page_size: 10}));

const pageChangeCallback = (pagination: Stream<Pagination>, newPage: number) => {
  const newOffset = pagination().pageSize * (newPage - 1);
  pagination(Pagination.fromJSON({
                                   offset: newOffset,
                                   total: pagination().total,
                                   page_size: pagination().pageSize
                                 }));
  return false;
};

export class KitchenSink extends MithrilViewComponent<null> {
  provider: DynamicSuggestionProvider = new DynamicSuggestionProvider(type);

  view(vnode: m.Vnode<null>) {
    const model: Stream<string>     = Stream();
    const textValue: Stream<string> = Stream();
    const name: Stream<string>      = Stream();

    return (
      <div>
        <HeaderPanel title="Kitchen Sink" sectionName={"Admin"}/>
        <h3>Pagination</h3>
        <PaginationWidget pagination={largeNumberOfPages()}
                          onPageChange={(newPage) => pageChangeCallback(largeNumberOfPages, newPage)}/>
        <PaginationWidget pagination={smallNumberOfPages()}
                          onPageChange={(newPage) => pageChangeCallback(smallNumberOfPages, newPage)}/>
        <br/>

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
        <Primary onclick={() => {
          this.createModal(Size.small);
        }}>Open Small Modal</Primary>
        <Primary onclick={() => {
          this.createModal(Size.medium);
        }}>Open Medium Modal</Primary>
        <Primary onclick={() => {
          this.createModal(Size.large);
        }}>Open Large Modal</Primary>

        <Primary onclick={() => {
          this.wizards();
        }}>Open Wizard</Primary>

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
            <Icons.CaretDown iconOnly={true}/> <br/>
            <Icons.Forward iconOnly={true}/> <br/>
            <Icons.ChevronRight iconOnly={true}/> <br/>
            <Icons.ChevronDown iconOnly={true}/> <br/>
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
        <Secondary>Button</Secondary>
        <Secondary>Secondary Button</Secondary>
        <Reset>Reset Button</Reset>
        <Cancel>Cancel</Cancel>
        <Danger>Delete</Danger>
        <hr/>

        <h3>Button with icon:</h3>
        <Default dropdown={true} icon={ButtonIcon.FILTER}>Button with icon</Default>

        <h3>Button link:</h3>
        <Link>Button link</Link>

        <h3>Button group with buttons:</h3>
        <ButtonGroup>
          <Secondary>Button</Secondary>
          <Secondary>Button</Secondary>
          <Secondary>Button</Secondary>
          <Secondary>Button</Secondary>
          <Secondary>Button</Secondary>
        </ButtonGroup>
        <hr/>

        <h3>Button group with dropdown buttons:</h3>
        <ButtonGroup>
          <DummyDropdownButton show={Stream()} name="Resources">
            <TriStateCheckboxField label={"Psql"} property={Stream(new TriStateCheckbox())}/>
            <TriStateCheckboxField label={"Java"} property={Stream(new TriStateCheckbox(TristateState.indeterminate))}/>
          </DummyDropdownButton>
          <DummyDropdownButton show={Stream()} name="Environments">
            <TriStateCheckboxField label={"Prod"} property={Stream(new TriStateCheckbox())}/>
            <TriStateCheckboxField label={"Dev"} property={Stream((new TriStateCheckbox(TristateState.on)))}/>
            <TriStateCheckboxField label={"QA"} property={Stream(new TriStateCheckbox())}/>
          </DummyDropdownButton>
          <Secondary>Normal Button</Secondary>
          <DummyDropdownButton show={Stream()} name="Four">This is just a text</DummyDropdownButton>
        </ButtonGroup>
        <hr/>

        <h3>Button group with disabled buttons:</h3>
        <ButtonGroup>
          <Primary disabled={true}>Disabled Primary Button</Primary>
          <Secondary disabled={true}>Disabled Secondary Button</Secondary>
          <Reset disabled={true}>Disabled Reset Button</Reset>
          <Cancel disabled={true}>Disabled Cancel</Cancel>
          <Danger disabled={true}>Disabled Danger</Danger>
        </ButtonGroup>

        <br/>
        <h3>Small Buttons:</h3>
        <ButtonGroup>
          <Primary small={true}>Small Primary Button</Primary>
          <Secondary small={true}>Small Secondary Button</Secondary>
          <Reset small={true}>Small Reset Button</Reset>
          <Cancel small={true}>Cancel</Cancel>
          <Danger small={true}>Disabled Danger</Danger>
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
          {this.formFields()}
        </Form>

        <h3>Compact form</h3>
        <Form compactForm={true}>
          {this.formFields()}
        </Form>

        <QuickAddField property={formValue} buttonDisableReason={"Add text to enable quick add"}/>
        <CopyField property={formValue} buttonDisableReason={"Add text to enable quick add"}/>

        <SearchFieldWithButton property={formValue} buttonDisableReason={"Add text to enable search"}/>
        <br/>

        <h3>Dropdown</h3>
        <Dropdown label={"Favourite Fruit"} property={dropdownProperty} possibleValues={dropdownItems}/>
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

        <h3>Validate-as-you-type field (i.e., LiveValidatingInputField)</h3>

        <LiveValidatingInputField
          label="Enter some numbers. But if you're adventurous, try any other character -- oh my, the suspense!"
          property={textValue} validator={(s) => {
          if (!(/^\d*$/.test(s))) {
            return "Only numbers are allowed! You'd better settle down, you rebel, you!";
          }
        }}/>

        <h3>Validate-as-you-type Name field (just a special case of LiveValidatingInputField)</h3>

        <IdentifierInputField label={`GoCD identier (a.k.a., "name")`} property={name}/>

        <h3>Dynamic autocomplete</h3>
        <AutocompleteField label="Dynamic" property={model} provider={this.provider}/>

        <Primary onclick={this.toggleType.bind(this)}>Click to change type!</Primary>

        <br/>
        <h3>Draggable Table</h3>
        <Table draggable={true} headers={["Pipeline", "Stage", "Job"]} data={pipelineData()}
               dragHandler={updateModel.bind(this)}/>
        <p>Model: {JSON.stringify(pipelines())}</p>
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

  private formFields() {
    return [
      <TextField required={true}
                 helpText="Enter your username here"
                 docLink="configuration/quick_pipeline_setup.html#step-2-material"
                 label="Username"
                 placeholder="username"
                 property={formValue}/>,
      <TextField required={true}
                 helpText="Enter your username here"
                 placeholder="username"
                 label="Username"
                 property={formValue}/>,
      <TextField required={true}
                 errorText="This field must be present"
                 helpText="Lorem ipsum is the dummy text used by the print and typesetting industry"
                 label="Lorem ipsum"
                 property={formValue}/>,
      <CheckboxField required={true}
                     errorText={!checkboxField() ? "You must get some icecream" : undefined}
                     helpText="Do you want ice cream?"
                     label="Do you want ice cream?"
                     property={checkboxField}/>,

      <TriStateCheckboxField
        label="Tri state checkbox"
        property={triStateCheckbox}
        helpText={`Tristate state is: ${triStateCheckbox().state()}`}/>,

      <PasswordField label="Editable password field"
                     placeholder="password"
                     property={passwordValue}
                     helpText={"Lorem ipsum dolor sit amet, consectetur adipiscing."}/>,

      <PasswordField label="Locked password field"
                     placeholder="password"
                     property={encryptedPasswordValue}
                     helpText={"Lorem ipsum dolor sit amet, consectetur adipiscing."}/>,
      <RadioField label="Choose your favorite pizza topping"
                  property={radioValue}
                  required={true}
                  possibleValues={[
                    {label: "Thin Crust", value: "thin-crust", helpText: "Thin italian-style crust"},
                    {label: "Cheese burst", value: "cheese-burst", helpText: "Filled with cheese, good for the heart"},
                    {label: "Boring regular", value: "regular", helpText: "A lack of imagination"},
                  ]}>
      </RadioField>
    ];
  }

  private createModal(size: Size) {
    new SampleModal(size).render();
  }

  private wizards() {
    const stepWithCustomFooter = new SampleStep("3. Associate Jobs", <div>This is just an example.</div>,
      <Primary>Custom
        Button</Primary>);
    return new Wizard()
      .addStep(new SampleStep("1. Cluster Profile", <div>This is an cluster profile.</div>))
      .addStep(new SampleStep("2. Elastic Profile", generateLoremIpsmeParagraphs(200)))
      .addStep(stepWithCustomFooter)
      .render();
  }
}

class SampleStep extends Step {
  private readonly name: string;
  private readonly content: m.Children;
  private readonly buttons: m.Children;

  constructor(name: string, body: m.Children, footer?: m.Children) {
    super();
    this.name    = name;
    this.content = body;
    this.buttons = footer;

  }

  body() {
    return this.content;
  }

  footer(wizard: Wizard): m.Children {
    return (<div>
      {super.footer(wizard)}
      {this.buttons}
    </div>);
  }

  header() {
    return this.name;
  }
}

class Pipeline {
  private pipeline: string;
  private stage: string;
  private job: string;

  constructor(pipeline: string, stage: string, job: string) {
    this.pipeline = pipeline;
    this.stage    = stage;
    this.job      = job;

  }

  tableData() {
    return [this.pipeline, this.stage, this.job];
  }
}

const pipelines = Stream([
                           new Pipeline("WindowsPR", "test", "jasmine"),
                           new Pipeline("WindowsPR", "build", "installer"),
                           new Pipeline("WindowsPR", "upload", "upload"),
                           new Pipeline("LinuxPR", "build", "clean"),
                           new Pipeline("LinuxPR", "test", "clean"),
                           new Pipeline("LinuxPR", "build", "clean")
                         ]);

const pipelineData = Stream(pipelines().map((e, i) => e.tableData()));

function updateModel(oldIndex: number, newIndex: number) {
  pipelines().splice(newIndex, 0, pipelines().splice(oldIndex, 1)[0]);
}

class DummyTableSortHandler implements TableSortHandler {
  private sortOrders                       = new Map();
  private currentSortedColumnIndex: number = 0;

  constructor() {
    this.getSortableColumns().forEach((c) => this.sortOrders.set(c, -1));
  }

  getCurrentSortedColumnIndex(): number {
    return this.currentSortedColumnIndex;
  }

  getCurrentSortOrder(): SortOrder {
    const currentSortOrder = this.sortOrders.get(this.getCurrentSortedColumnIndex());
    return SortOrder[currentSortOrder] ? SortOrder.ASC : SortOrder.DESC;
  }

  onColumnClick(columnIndex: number): void {
    this.currentSortedColumnIndex = columnIndex;
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

function generateLoremIpsmeParagraphs(count: number): m.Children {
  const paragraphs = [];
  for (let i = 0; i < count; i++) {
    paragraphs.push(<p>{reallyLongText}</p>);
  }

  return paragraphs;
}
