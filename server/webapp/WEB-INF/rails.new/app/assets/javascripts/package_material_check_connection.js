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

PackageMaterialCheckConnection = function (url) {
    _bind = function (parent_container_selector, check_connection_selector, connection_message_selector) {
        jQuery(parent_container_selector).on('click', check_connection_selector, function (e) {
            var connection_message_element = jQuery(connection_message_selector);
            var authToken = jQuery(parent_container_selector).parents('form').find('[name=authenticity_token]');
            var inputs = jQuery(parent_container_selector + " :input");
            var formData = jQuery.merge(inputs, authToken).serialize();
            formData = formData.replace("_method=PUT", "_method=POST");

            jQuery.ajax({
                type: "POST",
                url: url,
                data: formData,
                dataType: 'text',
                beforeSend: function(xhr) {
	                  xhr.setRequestHeader('Confirm', 'true');
                    connection_message_element.removeClass("error_message").removeClass("ok_message").text('Checking connection...');
                },
                complete: function (response) {
                    var value = JSON.parse(response.responseText);
                    if (value.error) {
                        connection_message_element.removeClass("ok_message").addClass("error_message").text(value.error);
                    } else {
                        connection_message_element.addClass("ok_message").removeClass("error_message").text(value.success);
                    }
                }
            });
            return false;
        });
    };

    return {
        bind: _bind
    }
};
