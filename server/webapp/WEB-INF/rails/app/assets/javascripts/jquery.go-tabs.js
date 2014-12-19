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

;(function ($, undefined) {

		var pluginName = "goTabs",
				defaults = {};

		function Plugin (element, navigation, options) {
            this.element = element;
            this.navigation = navigation;
            this.settings = $.extend( {}, defaults, options );
            this._defaults = defaults;
            this._name = pluginName;
            this.init();
//            this.highlightStep();
		}

		Plugin.prototype = {
            init: function () {

                var tab = this.element.parents('.wizard').find('.navigation li');
                this.element.find('li').first().addClass('current');

                tab.click(function(){
                    var goToStep = $(this).find('a').attr('href');
                    $('.steps > div').removeClass('current');
                    $(tab).removeClass('current');
                    $(goToStep).addClass('current');
                    $(tab+'a[href='+goToStep+']').parent().addClass('current');
                    return false;
                });
            }
//            ,
//            highlightStep: function(){
//                $('.steps > div').each(function(){
//                    if($(this).hasClass('current')){
//                        var goToStep = $(this).attr('id');
//                    }
//                    $(tab).removeClass('current');
//                    $(tab+'a[href='+goToStep+']').parent().addClass('current');
//                })
//            }

		};

		$.fn[pluginName] = function (navigation, options) {
				var goTabs = new Plugin(this, navigation, options);
				return this;
		};

})(jQuery);