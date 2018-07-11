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

CustomTreeView = function (url) {
    CustomTreeView.prototype.init = function () {
        bind();
    }

    CustomTreeView.prototype.bindTreeSearch = function bindSearch(inputTextBox){
        jQuery(inputTextBox).keyup(function () {
            var typedText = $j(this).val().trim().toLowerCase();
            jQuery('.repositories > li:not(.empty-node)').each(function (e) {
                var repositories = $j(this).find('a').text().trim().toLowerCase();
                if (repositories.indexOf(typedText) != -1) {
                    $j(this).show();
                }
                else {
                    $j(this).hide();
                }
            });

            jQuery('.packages > li').each(function (e) {
                var packageName = $j(this).find('a').text().trim().toLowerCase();
                if (packageName.indexOf(typedText) != -1) {
                    $j(this).show();
                    $j(this).parent('li').show();
                }
                else {
                    $j(this).hide();
                }
            });

            var noItemsMessage = jQuery('.no-items');
            if (jQuery('.repositories > li').is(":visible")) {
                noItemsMessage.hide();
            }
            else {
                noItemsMessage.show();
            }
        });
    }

    function bind() {

        jQuery('.treenav li:has(ul)').addClass('has-children collapsed');
        jQuery('.treenav li .packages li:last-child').addClass('last');
        jQuery('.treenav li:has(ul) .handle').click(function () {
            jQuery(this).closest('li').toggleClass('collapsed');
            jQuery(this).siblings('ul li:last-child').addClass('last')
        });

        if (jQuery('.repositories > li.selected').count <= 0 || jQuery('.repositories > li.grey_selected').count <= 0) {
            var firstNonEmptyNode = jQuery('.repositories > li:not(.empty-node)').first();
            firstNonEmptyNode.addClass('selected').removeClass('collapsed');
            firstNonEmptyNode.first().addClass('grey_selected').removeClass('collapsed');
        } else {
            jQuery('.repositories > li.selected, .repositories > li.grey_selected').removeClass('collapsed');
        }
    }
};
