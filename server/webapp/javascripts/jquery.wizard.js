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

		var pluginName = "wizard",
				defaults = {};

		function Plugin (element, navigation, options) {
				this.element = element;
				this.navigation = navigation;
				this.settings = $.extend( {}, defaults, options );
				this._defaults = defaults;
				this._name = pluginName;
				this.init();
		}

		Plugin.prototype = {
				init: function () {
					this.element.first().addClass('current');
					var prv = this.element.find('.prev');
					var nxt = this.element.find('.next');

					prv.click(function() {
						var current = $(this).parents('.current');
						if (current.prev().length) {
							var p = current.removeClass('current').prev();
							p.addClass('current');
						}
					});

					nxt.click(function() {
                        var current = $(this).parents('.current');
						if (current.next().length) {
							var n = current.removeClass('current').next();
							n.addClass('current');
						}
					});

					if (this.navigation) {
						this.navigation.find('a').click(function() {
							$('.current').removeClass('current');
							$($(this).attr('href')).addClass('current');
						});
					}
				}
		};

		$.fn[pluginName] = function (navigation, options) {
				var wizard = new Plugin(this, navigation, options);
				return this;
		};

})(jQuery);