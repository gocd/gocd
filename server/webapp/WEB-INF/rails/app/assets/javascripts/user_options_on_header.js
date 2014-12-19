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

UserOptionsOnHeader = function () {
    UserOptionsOnHeader.prototype.init = function(){
        var userButton = jQuery('.current_user');
        if (userButton.length > 0) {
            var userDropDown = jQuery('.current_user .enhanced_dropdown').get(0);
            var currentUser = userButton.get(0);
            var popup = new MicroContentPopup(userDropDown, new MicroContentPopup.NoOpHandler());

            var userDropDownShower = new MicroContentPopup.ClickShower(popup);
            userDropDownShower.bindShowButton(currentUser, currentUser);

            userButton.click(function (event) {
                jQuery(this).toggleClass('selected');
                event.stopPropagation();
            })

            $j(document).click(function () {
                userButton.removeClass('selected');
            });
        }
    }
}