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

var EnvironmentVariableEdit = function (options) {
    var $ = jQuery;

    var defaultOptions = {
        tableRowSelector:      '.environment-variable-edit-row',
        editSelector:          '.edit',
        resetSelector:         '.reset',
        passwordFieldSelector: '[type=password]',
        changedFieldSelector:  '.is-changed-field',
        originalValueSelector: '.original-secure-variable-value',
        parentRowSelector:     'tr'
    };

    options = $.extend({}, defaultOptions, options);

    var editSelector  = options.tableRowSelector + ' ' + options.editSelector;
    var resetSelector = options.tableRowSelector + ' ' + options.resetSelector;

    var existingHooks = $.data(document.body, 'reset-environment-variable-on');

    if (!existingHooks) {
        existingHooks = $.data(document.body, 'reset-environment-variable-on', {});
    }

    if (existingHooks[editSelector] && existingHooks[resetSelector]) {
        return;
    } else {
        existingHooks[editSelector]  = true;
        existingHooks[resetSelector] = true;
    }

    $(document).on('click.resetEnvironmentVariableOn', editSelector, function (evt) {
        evt.preventDefault();
        var element                     = $(evt.target),
            environmentVariableRow      = element.parents(options.parentRowSelector),
            passwordField               = environmentVariableRow.find(options.passwordFieldSelector),
            editLink                    = environmentVariableRow.find(options.editSelector),
            resetLink                   = environmentVariableRow.find(options.resetSelector),
            isChangedHiddenField        = environmentVariableRow.find(options.changedFieldSelector),
            secureVariableOriginalValue = environmentVariableRow.find(options.originalValueSelector);

        passwordField.removeAttr("readonly");
        isChangedHiddenField.val(true);
        passwordField.val('').focus();
        editLink.toggle();
        resetLink.toggle();
    });

    $(document).on('click.resetEnvironmentVariableOn', resetSelector, function (evt) {
        evt.preventDefault();
        var element                     = $(evt.target),
            environmentVariableRow      = element.parents(options.parentRowSelector),
            passwordField               = environmentVariableRow.find(options.passwordFieldSelector),
            editLink                    = environmentVariableRow.find(options.editSelector),
            resetLink                   = environmentVariableRow.find(options.resetSelector),
            isChangedHiddenField        = environmentVariableRow.find(options.changedFieldSelector),
            secureVariableOriginalValue = environmentVariableRow.find(options.originalValueSelector);

        passwordField.attr("readonly", "readonly");
        isChangedHiddenField.val(false);
        passwordField.val(secureVariableOriginalValue.val()).focus();
        editLink.toggle();
        resetLink.toggle();
    })
};

var EnvironmentVariableAddRemove = function(parentElement, options) {
  var $ = jQuery;

  options = $.extend({}, options);

  var addButton = $(parentElement).find('.add_item');
  var template = $(parentElement).find('.template').html();
  var addRowTo = $(parentElement).find('.variables tbody');

  // add a blank row
  addRowTo.append(template);

  addButton.on('click.environmentVariableTemplateCopy', function(evt){
    evt.preventDefault();
    addRowTo.append(template);
    if(options.onAdd){
      options.onAdd(addButton);
    }
  });

  $(parentElement).on('click.environmentVariableTemplateCopy', '.delete_parent', function(evt){
    evt.preventDefault();
    var deleteLink = $(evt.target);
    deleteLink.parents('.environment-variable-edit-row').remove();
    if(options.onRemove){
      options.onRemove(deleteLink);
    }

  });

};
