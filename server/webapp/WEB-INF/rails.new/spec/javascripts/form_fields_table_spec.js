/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

describe("form_fields_table", function () {
    var addLink;
    var table;
    var input_x;
    var input_y;
    var fields_table;

    beforeEach(function () {
        setFixtures("<div>\n" +
            "    <div>\n" +
            "        <input id=\"x\" name=\"xx\"/>\n" +
            "        <input id=\"y\" name=\"yy\"/>\n" +
            "        <span id=\"env_error_message\"></span>\n" +
            "        <a id=\"interpolate_and_add\" href=\"#\">Add em up</a>\n" +
            "    </div>\n" +
            "    <div id=\"table\">\n" +
            "    </div>\n" +
            "</div>");
    });

    beforeEach(function () {
        if (!fields_table) {
            addLink = jQuery('#interpolate_and_add');
            table = jQuery('#table');
            input_x = jQuery('#x');
            input_y = jQuery('#y');
        }
        table.html('');
    });

    afterEach(function () {
        addLink.unbind('click');
    });

    it("test_adds_field_values_to_form_table", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x, input_y],
            rowManager: new FormFieldsTable.Row('<span class="sample_row">${xx} and ${yy}</span>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message'))),
            errorMessageContainer: jQuery('#env_error_message'),
            valuePreProcessor: function (name, value) {
                return {name: name, value: name === "xx" ? jQuery.trim(value) : value};
            }
        });
        input_x.attr("value", " name1 ");
        input_y.attr("value", "value1");
        fire_event(addLink.get(0), 'click');
        assertEquals('name1 and value1', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('', input_x.val());
        assertEquals('', input_y.val());
        input_x.attr("value", "name2");
        input_y.attr("value", "value2");
        fire_event(addLink.get(0), 'click');
        assertEquals(2, table.find("div span.sample_row").length);
        assertEquals('name1 and value1', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('name2 and value2', table.find("div span.sample_row")[1].innerHTML);
        assertEquals('', input_x.val());
        assertEquals('', input_y.val());
    });

    it("test_cannot_add_duplicate", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x, input_y],
            errorMessageContainer: jQuery('#env_error_message'),
            rowManager: new FormFieldsTable.Row('<span class="sample_row">${xx} and ${yy}</span>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message')))
        });
        input_x.attr("value", "name1");
        input_y.attr("value", "value1");
        fire_event(addLink.get(0), 'click');
        assertEquals('name1 and value1', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('', input_x.val());
        assertEquals('', input_y.val());
        input_x.attr("value", "name1");
        input_y.attr("value", "value1");
        fire_event(addLink.get(0), 'click');
        assertEquals('name1 and value1', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('A variable with this name has already been added', jQuery("#env_error_message").html());
        assertEquals('name1', input_x.val());
        assertEquals('value1', input_y.val());

        // spaces should be trimmed before checking for duplicates
        jQuery("#env_error_message").html('');
        input_x.attr("value", " name1");
        input_y.attr("value", "new value");
        fire_event(addLink.get(0), 'click');
        assertEquals('name1 and value1', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('A variable with this name has already been added', jQuery("#env_error_message").html());
        assertEquals(' name1', input_x.val());
    });

    it("test_cannot_add_variable_with_blank_name", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x, input_y],
            errorMessageContainer: jQuery('#env_error_message'),
            rowManager: new FormFieldsTable.Row('<span class="sample_row">${xx} and ${yy}</span>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message')))
        });
        input_x.attr("value", "   ");
        input_y.attr("value", "bar");
        fire_event(addLink.get(0), 'click');
        assertEquals('Name cannot be blank', jQuery("#env_error_message").html());
    });

    it("test_cannot_add_variable_with_spaces_in_the_name", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x, input_y],
            errorMessageContainer: jQuery('#env_error_message'),
            rowManager: new FormFieldsTable.Row('<span class="sample_row">${xx} and ${yy}</span>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message')))
        });
        input_x.attr("value", "name with space");
        input_y.attr("value", "bar");
        fire_event(addLink.get(0), 'click');
        assertEquals('Name cannot have spaces', jQuery("#env_error_message").html());
    });

    it("test_error_message_is_removed_on_add", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x, input_y],
            errorMessageContainer: jQuery('#env_error_message'),
            rowManager: new FormFieldsTable.Row('<span class="sample_row">${xx} and ${yy}</span>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message')))
        });
        jQuery("#env_error_message").html('A variable with this name has already been added');
        input_x.attr("value", "name1");
        input_y.attr("value", "value1");
        fire_event(addLink.get(0), 'click');
        assertEquals("", jQuery("#env_error_message").html());
    });

    it("test_row_gets_deteted_when_delete_link_clicked", function () {
        fields_table = new FormFieldsTable({
            addControl: addLink,
            appendTo: table,
            fields: [input_x],
            errorMessageContainer: jQuery('#env_error_message'),
            rowManager: new FormFieldsTable.DeleteableRow('<span class="sample_row">${xx}</span><a href="#" class="delete_action">delete_me</a>', "div", new EnvironmentVariablesFormFieldValidator("xx", jQuery('#env_error_message')))
        });
        input_x.attr("value", "value_x");
        fire_event(addLink.get(0), 'click');
        assertEquals('value_x', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('delete_me', table.find("div a.delete_action")[0].innerHTML);
        var value_x_delete = jQuery(".delete_action").get(0);
        input_x.attr("value", "another_x");
        fire_event(addLink.get(0), 'click');
        assertEquals('value_x', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('another_x', table.find("div span.sample_row")[1].innerHTML);
        assertEquals('delete_me', table.find("div a.delete_action")[0].innerHTML);
        assertEquals('delete_me', table.find("div a.delete_action")[1].innerHTML);
        fire_event(table.find("div a.delete_action")[0], 'click');
        assertEquals('another_x', table.find("div span.sample_row")[0].innerHTML);
        assertEquals('delete_me', table.find("div a.delete_action")[0].innerHTML);
        fire_event(table.find("div a.delete_action")[0], 'click');
        assertEquals("", table.html());
    });
});
