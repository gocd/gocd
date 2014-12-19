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

function ajaxy_microcontent_popup(do_after_auto_refresh_of, content_box_id, url_namespace, button_class) {
    var showers = {};
    var ajaxyPopupBox = jQuery('#' + content_box_id).get(0);
    var ajaxUrls = Util.namespace(url_namespace);
    AjaxRefreshers.main().afterRefreshOf(do_after_auto_refresh_of, function() {
        jQuery('.' + button_class).each(function() {
            if (!showers[this.id]) {
                var popup = new MicroContentPopup(ajaxyPopupBox, new AjaxPopupHandler(ajaxUrls.get(this.id), ajaxyPopupBox));
                showers[this.id] = new MicroContentPopup.ClickShower(popup, {cleanup: true});
            }
            showers[this.id].cleanup();
            showers[this.id].bindShowButton(this);
        });
    }, true);
}