/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

describe("environment variable add remove", function () {
    beforeEach(function () {
        setFixtures('' +
          '<div class="parent">' +
          '  <table class="variables">' +
          '    <tbody>' +
          '    </tbody>' +
          '  </table>' +
          '  <table>' +
          '    <tbody class="template">' +
          '      <tr class="environment-variable-edit-row">' +
          '        <td>foobar</td>' +
          '        <td><span class="icon_remove delete_parent"></span></td>' +
          '      </tr>' +
          '    </tbody>' +
          '  </table>' +
          '  <a href="#" class="add_item">add item</a>' +
          '</div>')
    });

    it('should insert the template when add item is clicked', function(){
        EnvironmentVariableAddRemove(jQuery('.parent'));

        expect(jQuery('table.variables tr').length).toBe(1);
        jQuery('.add_item').click();
        expect(jQuery('table.variables tr').length).toBe(2);
    });

    it('should remove the row when add item is clicked', function(){
        EnvironmentVariableAddRemove(jQuery('.parent'));

        expect(jQuery('table.variables tr').length).toBe(1);

        jQuery('.add_item').click();
        jQuery('.add_item').click();
        jQuery('.add_item').click();
        expect(jQuery('table.variables tr').length).toBe(4);
        jQuery(jQuery('table.variables .delete_parent').get(1)).click();
        expect(jQuery('table.variables tr').length).toBe(3);
    });

});

describe("environment variable spec", function () {
    beforeEach(function () {
        setFixtures('' +
            '<div class="environment-variable-edit-row">' +
            '    <input type="password" readonly="true" value="p@ssw0rd"/>' +
            '    <input type="hidden" class="original-secure-variable-value" value="p@ssw0rd"/>' +
            '    <input type="hidden" class="is-changed-field" value="false"/>' +
            '    <span class="edit">Edit</span>' +
            '    <span class="reset" style="display: none;">Reset</span>' +
            '</div>' +
            '')
    });

    it('should edit secure variable on click', function () {
        EnvironmentVariableEdit({tableRowSelector: '.environment-variable-edit-row', parentRowSelector: 'div'});
        var row           = jQuery('.environment-variable-edit-row'),
            editLink      = row.find('.edit'),
            resetLink     = row.find('.reset'),
            changedField  = row.find('.is-changed-field'),
            passwordField = row.find('[type=password]')
            ;

        expect(editLink).toBeVisible();
        expect(resetLink).not.toBeVisible();
        expect(changedField.val()).toBe('false');
        expect(passwordField).toBeReadonly();

        jQuery('.edit').trigger('click');

        expect(editLink).not.toBeVisible();
        expect(resetLink).toBeVisible();
        expect(changedField.val()).toBe('true');
        expect(passwordField).not.toBeReadonly();

    });


    it('should reset secure variable on click', function () {
        EnvironmentVariableEdit({tableRowSelector: '.environment-variable-edit-row', parentRowSelector: 'div'});
        var row           = jQuery('.environment-variable-edit-row'),
            editLink      = row.find('.edit'),
            resetLink     = row.find('.reset'),
            changedField  = row.find('.is-changed-field'),
            passwordField = row.find('[type=password]')
            ;

        jQuery('.edit').trigger('click');
        jQuery(passwordField).val('new-password');

        jQuery('.reset').trigger('click');

        expect(editLink).toBeVisible();
        expect(resetLink).not.toBeVisible();
        expect(changedField.val()).toBe('false');
        expect(passwordField).toBeReadonly();
        expect(passwordField.val()).toBe('p@ssw0rd');
    });
});
