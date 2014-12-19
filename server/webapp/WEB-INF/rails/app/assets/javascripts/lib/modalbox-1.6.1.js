/*!
ModalBox - The pop-up window thingie with AJAX, based on prototype and script.aculo.us.

Copyright Andrey Okonetchnikov (andrej.okonetschnikow@gmail.com), 2006-2007
All rights reserved.
 
VERSION 1.6.1
Last Modified: 04/13/2008
*/

if (!window.Modalbox)
	var Modalbox = new Object();

Modalbox.Methods = {
	overrideAlert: false, // Override standard browser alert message with ModalBox
	focusableElements: new Array,
	currFocused: 0,
	initialized: false,
	active: true,
	options: {
		title: "ModalBox Window", // Title of the ModalBox window
		overlayClose: true, // Close modal box by clicking on overlay
		width: 500, // Default width in px
		height: 90, // Default height in px
		overlayOpacity: .85, // Default overlay opacity
		overlayDuration: .25, // Default overlay fade in/out duration in seconds
		//slideDownDuration: .5, // Default Modalbox appear slide down effect in seconds
		slideUpDuration: .5, // Default Modalbox hiding slide up effect in seconds
		resizeDuration: .25, // Default resize duration seconds
		inactiveFade: true, // Fades MB window on inactive state
		transitions: false, // Toggles transition effects. Transitions are enabled by default
		loadingString: "Please wait. Loading...", // Default loading string message
		closeString: "Close window", // Default title attribute for close window link
		closeValue: "", // Default string for close link in the header
		params: {},
		method: 'get', // Default Ajax request method
		autoFocusing: true, // Toggles auto-focusing for form elements. Disable for long text pages.
		aspnet: false // Should be use then using with ASP.NET costrols. Then true Modalbox window will be injected into the first form element.
	},
	_options: new Object,
	
	setOptions: function(options) {
		Object.extend(this.options, options || {});
	},
	
	_init: function(options) {
		// Setting up original options with default options
		Object.extend(this._options, this.options);
		this.setOptions(options);
		
		//Creating the overlay
		this.MBoverlay = new Element("div", { id: "MB_overlay", style: "opacity: 0" });

		//Creating the modal window
		this.MBwindow = new Element("div", {id: "MB_window", style: "display: none"}).update(
            this.MBframe = new Element("div", {id: "MB_frame"}).update(
                this.MBframe_right = new Element("div", {id: "MB_frame_right"}).update(
                    this.MBframe_content = new Element("div", {id: "MB_frame_content"}).update(
                        this.MBheader = new Element("div", {id: "MB_header"}).update(
                            this.MBcaption = new Element("div", {id: "MB_caption"})
                        )
                    )
                )
            )
		);
		this.MBclose = new Element("a", {id: "MB_close", title: this.options.closeString, href: "javascript:void(0);"}).update("<div>" + this.options.closeValue + "</div>");
		this.MBframe.insert({'bottom':this.MBclose});
		
		this.MBcontent = new Element("div", {id: "MB_content"}).update(
			this.MBloading = new Element("div", {id: "MB_loading"}).update(this.options.loadingString)
		);
		this.MBframe_content.insert({'bottom':this.MBcontent});
		
		// Inserting into DOM. If parameter set and form element have been found will inject into it. Otherwise will inject into body as topmost element.
		// Be sure to set padding and marging to null via CSS for both body and (in case of asp.net) form elements. 
		var injectToEl = this.options.aspnet ? $(document.body).down('form') : $(document.body);
		injectToEl.insert({'top':this.MBwindow});
		injectToEl.insert({'top':this.MBoverlay});
		
		// Initial scrolling position of the window. To be used for remove scrolling effect during ModalBox appearing
		this.initScrollX = window.pageXOffset || document.body.scrollLeft || document.documentElement.scrollLeft;
		this.initScrollY = window.pageYOffset || document.body.scrollTop || document.documentElement.scrollTop;
		
		//Adding event observers
		this.hideObserver = this._hide.bindAsEventListener(this);
		this.kbdObserver = this._kbdHandler.bindAsEventListener(this);
		this._initObservers();

        //add chrome to corners/sides
        this.MBframeTop = new Element("div", {id: "MB_frame_top"})
        this.MBframeTop.appendChild(
            this.MBframeTopLeft = new Element("div", {id: "MB_frame_tl"})
        );
        this.MBframeTop.appendChild(
            this.MBframeTopRight = new Element("div", {id: "MB_frame_tr"})          
        );
        this.MBwindow.insertBefore(this.MBframeTop, this.MBframe);


        this.MBframeBottom = new Element("div", {id: "MB_frame_bottom"})
        this.MBframeBottom.appendChild(
            this.MBframeBottomLeft = new Element("div", {id: "MB_frame_bl"})
        );
        this.MBframeBottom.appendChild(
            this.MBframeBottomRight = new Element("div", {id: "MB_frame_br"})
        );

        this.MBwindow.appendChild(this.MBframeBottom);
















		this.initialized = true; // Mark as initialized
	},
	
	show: function(content, options) {
		if(!this.initialized) this._init(options); // Check for is already initialized
		
		this.content = content;
		this.setOptions(options);
		
		if(this.options.title) // Updating title of the MB
			$(this.MBcaption).update(this.options.title);
		else { // If title isn't given, the header will not displayed
			$(this.MBheader).hide();
			$(this.MBcaption).hide();
		}
		
		if(this.MBwindow.style.display == "none") { // First modal box appearing
			this._appear();
			this.event("onShow"); // Passing onShow callback
		}
		else { // If MB already on the screen, update it
			this._update();
			this.event("onUpdate"); // Passing onUpdate callback
		} 
	},
	
	hide: function(options) { // External hide method to use from external HTML and JS
		if(this.initialized) {
			// Reading for options/callbacks except if event given as a pararmeter
			if(options && typeof options.element != 'function') Object.extend(this.options, options); 
			// Passing beforeHide callback
			this.event("beforeHide");
			if(this.options.transitions)
				Effect.SlideUp(this.MBwindow, { duration: this.options.slideUpDuration, transition: Effect.Transitions.sinoidal, afterFinish: this._deinit.bind(this) } );
			else {
				$(this.MBwindow).hide();
				this._deinit();
			}
		} else throw("Modalbox is not initialized.");
	},
	
	_hide: function(event) { // Internal hide method to use with overlay and close link
		event.stop(); // Stop event propaganation for link elements
		/* Then clicked on overlay we'll check the option and in case of overlayClose == false we'll break hiding execution [Fix for #139] */
		if(event.element().id == 'MB_overlay' && !this.options.overlayClose) return false;
		this.hide();
	},
	
	alert: function(message){
		var html = '<div class="MB_alert"><p>' + message + '</p><input type="button" onclick="Modalbox.hide()" value="OK" /></div>';
		Modalbox.show(html, {title: 'Alert: ' + document.title, width: 300});
	},
		
	_appear: function() { // First appearing of MB
		if(Prototype.Browser.IE && !navigator.appVersion.match(/\b7.0\b/)) { // Preparing IE 6 for showing modalbox
			window.scrollTo(0,0);
			this._prepareIE("100%", "hidden"); 
		}
		this._setWidth();
		this._setPosition();
		if(this.options.transitions) {
			$(this.MBoverlay).setStyle({opacity: 0});
			new Effect.Fade(this.MBoverlay, {
					from: 0, 
					to: this.options.overlayOpacity, 
					duration: this.options.overlayDuration, 
					afterFinish: function() {
						new Effect.SlideDown(this.MBwindow, {
							duration: this.options.slideDownDuration, 
							transition: Effect.Transitions.sinoidal, 
							afterFinish: function(){ 
								this._setPosition(); 
								this.loadContent();
							}.bind(this)
						});
					}.bind(this)
			});
		} else {
			$(this.MBoverlay).setStyle({opacity: this.options.overlayOpacity});
			$(this.MBwindow).show();
			this._setPosition(); 
			this.loadContent();
		}
		this._setWidthAndPosition = this._setWidthAndPosition.bindAsEventListener(this);
		Event.observe(window, "resize", this._setWidthAndPosition);
	},
	
	resize: function(byWidth, byHeight, options) { // Change size of MB without loading content
		var oWidth = $(this.MBoverlay).getWidth();
		var wHeight = $(this.MBwindow).getHeight();
		var wWidth = $(this.MBwindow).getWidth();
		var hHeight = $(this.MBheader).getHeight();
		var cHeight = $(this.MBcontent).getHeight();
		var newHeight = ((wHeight - hHeight + byHeight) < cHeight) ? (cHeight + hHeight) : (wHeight + byHeight);
		var newWidth = wWidth + byWidth;
        this.options.width = newWidth;
		if(options) this.setOptions(options); // Passing callbacks
		if(this.options.transitions) {
			new Effect.Morph(this.MBwindow, {
				style: "width:" + newWidth + "px; height:" + newHeight + "px; left:" + ((oWidth - newWidth)/2) + "px",
				duration: this.options.resizeDuration, 
				beforeStart: function(fx){
					fx.element.setStyle({overflow:"hidden"}); // Fix for MSIE 6 to resize correctly
				},
				afterFinish: function(fx) {
					fx.element.setStyle({overflow:"visible"});
					this.event("_afterResize"); // Passing internal callback
					this.event("afterResize"); // Passing callback
				}.bind(this)
			});
		} else {
			this.MBwindow.setStyle({width: newWidth + "px", height: newHeight + "px"});
			setTimeout(function() {
				this.event("_afterResize"); // Passing internal callback
				this.event("afterResize"); // Passing callback
			}.bind(this), 1);
		}
		
	},
	
	resizeToContent: function(options){
		
		// Resizes the modalbox window to the actual content height.
		// This might be useful to resize modalbox after some content modifications which were changed ccontent height.
		
		var byHeight = this.options.height - $(this.MBwindow).getHeight();
		if(byHeight != 0) {
			if(options) this.setOptions(options); // Passing callbacks
			Modalbox.resize(0, byHeight);
		}
	},
	
	resizeToInclude: function(element, options){
		
		// Resizes the modalbox window to the camulative height of element. Calculations are using CSS properties for margins and border.
		// This method might be useful to resize modalbox before including or updating content.
		
		var el = $(element);
		var elHeight = el.getHeight() + parseInt(el.getStyle('margin-top'), 0) + parseInt(el.getStyle('margin-bottom'), 0) + parseInt(el.getStyle('border-top-width'), 0) + parseInt(el.getStyle('border-bottom-width'), 0);
		if(elHeight > 0) {
			if(options) this.setOptions(options); // Passing callbacks
			Modalbox.resize(0, elHeight);
		}
	},
	
	_update: function() { // Updating MB in case of wizards
		$(this.MBcontent).update($(this.MBloading).update(this.options.loadingString));
		this.loadContent();
	},
	
	loadContent: function () {
		if(this.event("beforeLoad") != false) { // If callback passed false, skip loading of the content
			if(typeof this.content == 'string') {
				var htmlRegExp = new RegExp(/<\/?[^>]+>/gi);
				if(htmlRegExp.test(this.content)) { // Plain HTML given as a parameter
					this._insertContent(this.content.stripScripts(), function(){
						this.content.extractScripts().map(function(script) { 
							return eval(script.replace("<!--", "").replace("// -->", ""));
						}.bind(window));
					}.bind(this));
				} else // URL given as a parameter. We'll request it via Ajax
					new Ajax.Request( this.content, { method: this.options.method.toLowerCase(), parameters: this.options.params, 
						onSuccess: function(transport) {
							var response = new String(transport.responseText);
							this._insertContent(transport.responseText.stripScripts(), function(){
								response.extractScripts().map(function(script) { 
									return eval(script.replace("<!--", "").replace("// -->", ""));
								}.bind(window));
							});
						}.bind(this),
						onException: function(instance, exception){
							Modalbox.hide();
							throw('Modalbox Loading Error: ' + exception);
						}
					});
					
			} else if (typeof this.content == 'object') {// HTML Object is given
				this._insertContent(this.content);
			} else {
				Modalbox.hide();
				throw('Modalbox Parameters Error: Please specify correct URL or HTML element (plain HTML or object)');
			}
		}
	},
	
	_insertContent: function(content, callback){
		$(this.MBcontent).hide().update("");
		if(typeof content == 'string') { // Plain HTML is given
			this.MBcontent.update(new Element("div", { style: "display: none" }).update(content)).down().show();
		} else if (typeof content == 'object') { // HTML Object is given
			var _htmlObj = content.cloneNode(true); // If node already a part of DOM we'll clone it
			// If clonable element has ID attribute defined, modifying it to prevent duplicates
			if(content.id) content.id = "MB_" + content.id;
			/* Add prefix for IDs on all elements inside the DOM node */
			$(content).select('*[id]').each(function(el){ el.id = "MB_" + el.id; });
			this.MBcontent.update(_htmlObj).down('div').show();
			if(Prototype.Browser.IE) // Toggling back visibility for hidden selects in IE
				$$("#MB_content select").invoke('setStyle', {'visibility': ''});
		}
		
		// Prepare and resize modal box for content
		if(this.options.height == this._options.height) {
			Modalbox.resize((this.options.width - $(this.MBwindow).getWidth()), $(this.MBcontent).getHeight() - $(this.MBwindow).getHeight() + $(this.MBheader).getHeight(), {
				afterResize: function(){
					setTimeout(function(){ // MSIE fix
						this._putContent(callback);
					}.bind(this),1);
				}.bind(this)
			});
		} else { // Height is defined. Creating a scrollable window
			this._setWidth();
			this.MBcontent.setStyle({overflow: 'auto', height: $(this.MBwindow).getHeight() - $(this.MBheader).getHeight() - 13 + 'px'});
			setTimeout(function(){ // MSIE fix
				this._putContent(callback);
			}.bind(this),1);
		}
	},
	
	_putContent: function(callback){
		this.MBcontent.show();
		this.focusableElements = this._findFocusableElements();
		this._setFocus(); // Setting focus on first 'focusable' element in content (input, select, textarea, link or button)
		if(callback != undefined)
			callback(); // Executing internal JS from loaded content
		this.event("afterLoad"); // Passing callback
	},
	
	activate: function(options){
		this.setOptions(options);
		this.active = true;
		$(this.MBclose).observe("click", this.hideObserver);
		if(this.options.overlayClose)
			$(this.MBoverlay).observe("click", this.hideObserver);
		$(this.MBclose).show();
		if(this.options.transitions && this.options.inactiveFade)
			new Effect.Appear(this.MBwindow, {duration: this.options.slideUpDuration});
	},
	
	deactivate: function(options) {
		this.setOptions(options);
		this.active = false;
		$(this.MBclose).stopObserving("click", this.hideObserver);
		if(this.options.overlayClose)
			$(this.MBoverlay).stopObserving("click", this.hideObserver);
		$(this.MBclose).hide();
		if(this.options.transitions && this.options.inactiveFade)
			new Effect.Fade(this.MBwindow, {duration: this.options.slideUpDuration, to: .75});
	},
	
	_initObservers: function(){
		$(this.MBclose).observe("click", this.hideObserver);
		if(this.options.overlayClose)
			$(this.MBoverlay).observe("click", this.hideObserver);
		if(Prototype.Browser.Gecko)
			Event.observe(document, "keypress", this.kbdObserver); // Gecko is moving focus a way too fast
		else
			Event.observe(document, "keydown", this.kbdObserver); // All other browsers are okay with keydown
	},
	
	_removeObservers: function(){
		$(this.MBclose).stopObserving("click", this.hideObserver);
		if(this.options.overlayClose)
			$(this.MBoverlay).stopObserving("click", this.hideObserver);
		if(Prototype.Browser.Gecko)
			Event.stopObserving(document, "keypress", this.kbdObserver);
		else
			Event.stopObserving(document, "keydown", this.kbdObserver);
	},
	
	_setFocus: function() { 
		/* Setting focus to the first 'focusable' element which is one with tabindex = 1 or the first in the form loaded. */
		if(this.focusableElements.length > 0 && this.options.autoFocusing == true) {
			var firstEl = this.focusableElements.find(function (el){
				return el.tabIndex == 1;
			}) || this.focusableElements.first();
			this.currFocused = this.focusableElements.toArray().indexOf(firstEl);
			firstEl.focus(); // Focus on first focusable element except close button
		} else if($(this.MBclose).visible())
			$(this.MBclose).focus(); // If no focusable elements exist focus on close button
	},
	
	_findFocusableElements: function(){ // Collect form elements or links from MB content
		this.MBcontent.select('input:not([type~=hidden]), select, textarea, button, a[href]').invoke('addClassName', 'MB_focusable');
		return this.MBcontent.select('.MB_focusable');
	},
	
	_kbdHandler: function(event) {
		var node = event.element();
		switch(event.keyCode) {
			case Event.KEY_TAB:
				event.stop();
				try {
                    /* Switching currFocused to the element which was focused by mouse instead of TAB-key. Fix for #134 */
                    if (node != this.focusableElements[this.currFocused]) {
                        this.currFocused = this.focusableElements.toArray().indexOf(node);
                    }

                    if (!event.shiftKey) { //Focusing in direct order
                        if (this.currFocused == this.focusableElements.length - 1) {
                            this.focusableElements.first().focus();
                            this.currFocused = 0;
                        } else {
                            this.currFocused++;
                            this.focusableElements[this.currFocused].focus();
                        }
                    } else { // Shift key is pressed. Focusing in reverse order
                        if (this.currFocused == 0) {
                            this.focusableElements.last().focus();
                            this.currFocused = this.focusableElements.length - 1;
                        } else {
                            this.currFocused--;
                            this.focusableElements[this.currFocused].focus();
                        }
                    }
                }
                catch(e) {
                    //In IE, on hitting tab key, an exception is thrown when an un-focusable element is found. This is unwanted and unpleasant. Hence gobbling exception.
                }
				break;			
			case Event.KEY_ESC:
				if(this.active) this._hide(event);
				break;
			case 32:
				this._preventScroll(event);
				break;
			case 0: // For Gecko browsers compatibility
				if(event.which == 32) this._preventScroll(event);
				break;
			case Event.KEY_UP:
			case Event.KEY_DOWN:
			case Event.KEY_PAGEDOWN:
			case Event.KEY_PAGEUP:
			case Event.KEY_HOME:
			case Event.KEY_END:
				// Safari operates in slightly different way. This realization is still buggy in Safari.
				if(Prototype.Browser.WebKit && !["textarea", "select"].include(node.tagName.toLowerCase()))
					event.stop();
				else if( (node.tagName.toLowerCase() == "input" && ["submit", "button"].include(node.type)) || (node.tagName.toLowerCase() == "a") )
					event.stop();
				break;
		}
	},
	
	_preventScroll: function(event) { // Disabling scrolling by "space" key
		if(!["input", "textarea", "select", "button"].include(event.element().tagName.toLowerCase())) 
			event.stop();
	},
	
	_deinit: function()
	{	
		this._removeObservers();
		Event.stopObserving(window, "resize", this._setWidthAndPosition );
		if(this.options.transitions) {
			Effect.toggle(this.MBoverlay, 'appear', {duration: this.options.overlayDuration, afterFinish: this._removeElements.bind(this) });
		} else {
			this.MBoverlay.hide();
			this._removeElements();
		}
		$(this.MBcontent).setStyle({overflow: '', height: ''});
	},
	
	_removeElements: function () {
		$(this.MBoverlay).remove();
		$(this.MBwindow).remove();
		if(Prototype.Browser.IE && !navigator.appVersion.match(/\b7.0\b/)) {
			this._prepareIE("", ""); // If set to auto MSIE will show horizontal scrolling
			window.scrollTo(this.initScrollX, this.initScrollY);
		}
		
		/* Replacing prefixes 'MB_' in IDs for the original content */
		if(typeof this.content == 'object') {
			if(this.content.id && this.content.id.match(/MB_/)) {
				this.content.id = this.content.id.replace(/MB_/, "");
			}
			this.content.select('*[id]').each(function(el){ el.id = el.id.replace(/MB_/, ""); });
		}
		/* Initialized will be set to false */
		this.initialized = false;
		this.event("afterHide"); // Passing afterHide callback
		this.setOptions(this._options); //Settings options object into intial state
	},
	
	_setWidth: function () { //Set size
		$(this.MBwindow).setStyle({width: this.options.width + "px", height: this.options.height + "px"});
	},
	
	_setPosition: function () {
		$(this.MBwindow).setStyle({left: (($(this.MBoverlay).getWidth() - $(this.MBwindow).getWidth()) / 2 ) + "px"});
	},
	
	_setWidthAndPosition: function () {
		$(this.MBwindow).setStyle({width: this.options.width + "px"});
		this._setPosition();
	},
	
	_getScrollTop: function () { //From: http://www.quirksmode.org/js/doctypes.html
		var theTop;
		if (document.documentElement && document.documentElement.scrollTop)
			theTop = document.documentElement.scrollTop;
		else if (document.body)
			theTop = document.body.scrollTop;
		return theTop;
	},
	_prepareIE: function(height, overflow){
		$$('html, body').invoke('setStyle', {width: height, height: height, overflow: overflow}); // IE requires width and height set to 100% and overflow hidden
		$$("select").invoke('setStyle', {'visibility': overflow}); // Toggle visibility for all selects in the common document
	},
	event: function(eventName) {
		if(this.options[eventName]) {
			var returnValue = this.options[eventName](); // Executing callback
			this.options[eventName] = null; // Removing callback after execution
			if(returnValue != undefined) 
				return returnValue;
			else 
				return true;
		}
		return true;
	}
};

Object.extend(Modalbox, Modalbox.Methods);

if(Modalbox.overrideAlert) window.alert = Modalbox.alert;