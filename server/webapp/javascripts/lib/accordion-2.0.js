// accordion.js v2.0
//
// Copyright (c) 2007 stickmanlabs
// Author: Kevin P Miller | http://www.stickmanlabs.com
//
// Accordion is freely distributable under the terms of an MIT-style license.
//
// I don't care what you think about the file size...
//   Be a pro:
//	    http://www.thinkvitamin.com/features/webapps/serving-javascript-fast
//      http://rakaz.nl/item/make_your_pages_load_faster_by_combining_and_compressing_javascript_and_css_files
//

/*-----------------------------------------------------------------------------------------------*/

if (typeof Effect == 'undefined')
	throw("accordion.js requires including script.aculo.us' effects.js library!");

var accordion = Class.create();
accordion.prototype = {

	//
	//  Setup the Variables
	//
	showAccordion : null,
	currentAccordion : null,
	duration : null,
	effects : [],
	animating : false,

	//
	//  Initialize the accordions
	//
	initialize: function(container, options) {
	  if (!$(container)) {
	    throw(container+" doesn't exist!");
	    return false;
	  }

		this.options = Object.extend({
			resizeSpeed : 8,
			classNames : {
				toggle : 'accordion_toggle',
				toggleActive : 'accordion_toggle_active',
				content : 'accordion_content'
			},
			defaultSize : {
				height : null,
				width : null
			},
			direction : 'vertical',
			onEvent : 'click'
		}, options || {});

		this.duration = ((11-this.options.resizeSpeed)*0.15);

		var accordions = $$('#'+container+' .'+this.options.classNames.toggle);
		accordions.each(function(accordion) {
			Event.observe(accordion, this.options.onEvent, this.activate.bind(this, accordion), false);
			if (this.options.onEvent == 'click') {
			  accordion.onclick = function() {return false;};
			}

			if (this.options.direction == 'horizontal') {
				var options = {width: '0px', display:'none'};
			} else {
				var options = {height: '0px', display:'none'};
			}
//			options.merge({display: 'none'});

			this.currentAccordion = $(accordion.next(0)).setStyle(options);
		}.bind(this));
	},

	//
	//  Activate an accordion
	//
	activate : function(accordion) {
		if (this.animating) {
			return false;
		}

		this.effects = [];

		this.currentAccordion = $(accordion.next(0));
		this.currentAccordion.setStyle({
			display: 'block'
		});

		this.currentAccordion.previous(0).addClassName(this.options.classNames.toggleActive);

		if (this.options.direction == 'horizontal') {
			this.scaling = $H({
				scaleX: true,
				scaleY: false
			});
		} else {
			this.scaling = $H({
				scaleX: false,
				scaleY: true
			});
		}

		if (this.currentAccordion == this.showAccordion) {
		  this.deactivate();
		} else {
		  this._handleAccordion();
		}
	},
	//
	// Deactivate an active accordion
	//
	deactivate : function() {
		var options = $H({
		  duration: this.duration,
			scaleContent: false,
			transition: Effect.Transitions.sinoidal,
			queue: {
				position: 'end',
				scope: 'accordionAnimation'
			},
			scaleMode: {
				originalHeight: this.options.defaultSize.height ? this.options.defaultSize.height : this.currentAccordion.scrollHeight,
				originalWidth: this.options.defaultSize.width ? this.options.defaultSize.width : this.currentAccordion.scrollWidth
			},
			afterFinish: function() {
				this.showAccordion.setStyle({
          height: 'auto',
					display: 'none'
				});
				this.showAccordion = null;
				this.animating = false;
			}.bind(this)
		});
//    options.merge(this.scaling);

    this.showAccordion.previous(0).removeClassName(this.options.classNames.toggleActive);

		new Effect.Scale(this.showAccordion, 0, options.update(this.scaling).toObject());
	},

  //
  // Handle the open/close actions of the accordion
  //
	_handleAccordion : function() {
		var options = $H({
			sync: true,
			scaleFrom: 0,
			scaleContent: false,
			transition: Effect.Transitions.sinoidal,
			scaleMode: {
				originalHeight: this.options.defaultSize.height ? this.options.defaultSize.height : this.currentAccordion.scrollHeight,
				originalWidth: this.options.defaultSize.width ? this.options.defaultSize.width : this.currentAccordion.scrollWidth
			}
		});
		options.merge(this.scaling);

		this.effects.push(
			new Effect.Scale(this.currentAccordion, 100, options.update(this.scaling).toObject())
		);

		if (this.showAccordion) {
			this.showAccordion.previous(0).removeClassName(this.options.classNames.toggleActive);

			options = $H({
				sync: true,
				scaleContent: false,
				transition: Effect.Transitions.sinoidal
			});
			options.merge(this.scaling);

			this.effects.push(
				new Effect.Scale(this.showAccordion, 0, options.update(this.scaling).toObject())
			);
		}

    new Effect.Parallel(this.effects, {
			duration: this.duration,
			queue: {
				position: 'end',
				scope: 'accordionAnimation'
			},
			beforeStart: function() {
				this.animating = true;
			}.bind(this),
			afterFinish: function() {
				if (this.showAccordion) {
					this.showAccordion.setStyle({
						display: 'none'
					});
				}
				$(this.currentAccordion).setStyle({
				  height: 'auto'
				});
				this.showAccordion = this.currentAccordion;
				this.animating = false;
			}.bind(this)
		});
	}
}