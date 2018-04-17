# Licensed to the Software Freedom Conservancy (SFC) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SFC licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

module Selenium
  module WebDriver
    class Element
      include SearchContext

      #
      # Creates a new Element
      #
      # @api private
      #

      def initialize(bridge, id)
        @bridge = bridge
        @id = id
      end

      def inspect
        format '#<%s:0x%x id=%s>', self.class, hash * 2, @id.inspect
      end

      def ==(other)
        other.is_a?(self.class) && ref == other.ref
      end
      alias_method :eql?, :==

      def hash
        @id.hash ^ @bridge.hash
      end

      #
      # Click this element. If this causes a new page to load, this method will
      # attempt to block until the page has loaded.  At this point, you should
      # discard all references to this element and any further operations
      # performed on this element will raise a StaleElementReferenceError
      # unless you know that the element and the page will still be present. If
      # click() causes a new page to be loaded via an event or is done by
      # sending a native event then the method will *not* wait for it to be
      # loaded and the caller should verify that a new page has been loaded.
      #
      # There are some preconditions for an element to be clicked.  The element
      # must be visible and it must have a height and width greater then 0.
      #
      # Equivalent to:
      #   driver.action.click(element)
      #
      # @example Click on a button
      #
      #    driver.find_element(tag_name: "button").click
      #
      # @raise [StaleElementReferenceError] if the element no longer exists as
      #  defined
      #

      def click
        bridge.click_element @id
      end

      #
      # Get the tag name of the element.
      #
      # @example Get the tagname of an INPUT element(returns "input")
      #
      #    driver.find_element(xpath: "//input").tag_name
      #
      # @return [String] The tag name of this element.
      #

      def tag_name
        bridge.element_tag_name @id
      end

      #
      # Get the value of a the given attribute of the element. Will return the current value, even if
      # this has been modified after the page has been loaded. More exactly, this method will return
      # the value of the given attribute, unless that attribute is not present, in which case the
      # value of the property with the same name is returned. If neither value is set, nil is
      # returned. The "style" attribute is converted as best can be to a text representation with a
      # trailing semi-colon. The following are deemed to be "boolean" attributes, and will
      # return either "true" or "false":
      #
      # async, autofocus, autoplay, checked, compact, complete, controls, declare, defaultchecked,
      # defaultselected, defer, disabled, draggable, ended, formnovalidate, hidden, indeterminate,
      # iscontenteditable, ismap, itemscope, loop, multiple, muted, nohref, noresize, noshade, novalidate,
      # nowrap, open, paused, pubdate, readonly, required, reversed, scoped, seamless, seeking,
      # selected, spellcheck, truespeed, willvalidate
      #
      # Finally, the following commonly mis-capitalized attribute/property names are evaluated as
      # expected:
      #
      # class, readonly
      #
      # @param [String] name attribute name
      # @return [String, nil] attribute value
      #

      def attribute(name)
        bridge.element_attribute self, name
      end

      #
      # Get the value of a the given property with the same name of the element. If the value is not
      # set, nil is returned.
      #
      # @param [String] name property name
      # @return [String, nil] property value
      #

      def property(name)
        bridge.element_property self, name
      end

      #
      # Get the text content of this element
      #
      # @return [String]
      #

      def text
        bridge.element_text @id
      end

      #
      # Send keystrokes to this element
      #
      # @param [String, Symbol, Array] args keystrokes to send
      #
      # Examples:
      #
      #     element.send_keys "foo"                     #=> value: 'foo'
      #     element.send_keys "tet", :arrow_left, "s"   #=> value: 'test'
      #     element.send_keys [:control, 'a'], :space   #=> value: ' '
      #
      # @see Keys::KEYS
      #

      def send_keys(*args)
        bridge.send_keys_to_element @id, Keys.encode(args)
      end
      alias_method :send_key, :send_keys

      #
      # If this element is a text entry element, this will clear the value. Has no effect on other
      # elements. Text entry elements are INPUT and TEXTAREA elements.
      #
      # Note that the events fired by this event may not be as you'd expect.  In particular, we don't
      # fire any keyboard or mouse events.  If you want to ensure keyboard events are
      # fired, consider using #send_keys with the backspace key. To ensure you get a change event,
      # consider following with a call to #send_keys with the tab key.
      #

      def clear
        bridge.clear_element @id
      end

      #
      # Is the element enabled?
      #
      # @return [Boolean]
      #

      def enabled?
        bridge.element_enabled? @id
      end

      #
      # Is the element selected?
      #
      # @return [Boolean]
      #

      def selected?
        bridge.element_selected? @id
      end

      #
      # Is the element displayed?
      #
      # @return [Boolean]
      #

      def displayed?
        bridge.element_displayed? @id
      end

      #
      # Submit this element
      #

      def submit
        bridge.submit_element @id
      end

      #
      # Get the value of the given CSS property
      #
      # Note that shorthand CSS properties (e.g. background, font, border, border-top, margin,
      # margin-top, padding, padding-top, list-style, outline, pause, cue) are not returned,
      # in accordance with the DOM CSS2 specification - you should directly access the longhand
      # properties (e.g. background-color) to access the desired values.
      #
      # @see http://www.w3.org/TR/DOM-Level-2-Style/css.html#CSS-CSSStyleDeclaration
      #

      def css_value(prop)
        bridge.element_value_of_css_property @id, prop
      end
      alias_method :style, :css_value

      #
      # Get the location of this element.
      #
      # @return [WebDriver::Point]
      #

      def location
        bridge.element_location @id
      end

      #
      # Get the dimensions and coordinates of this element.
      #
      # @return [WebDriver::Rectangle]
      #

      def rect
        bridge.element_rect @id
      end

      #
      # Determine an element's location on the screen once it has been scrolled into view.
      #
      # @return [WebDriver::Point]
      #

      def location_once_scrolled_into_view
        bridge.element_location_once_scrolled_into_view @id
      end

      #
      # Get the size of this element
      #
      # @return [WebDriver::Dimension]
      #

      def size
        bridge.element_size @id
      end

      #-------------------------------- sugar  --------------------------------

      #
      #   element.first(id: 'foo')
      #

      alias_method :first, :find_element

      #
      #   element.all(class: 'bar')
      #

      alias_method :all, :find_elements

      #
      #   element['class'] or element[:class] #=> "someclass"
      #
      alias_method :[], :attribute

      #
      # for SearchContext and execute_script
      #
      # @api private
      #

      def ref
        @id
      end

      #
      # Convert to a WebElement JSON Object for transmission over the wire.
      # @see https://github.com/SeleniumHQ/selenium/wiki/JsonWireProtocol#basic-terms-and-concepts
      #
      # @api private
      #

      def to_json(*)
        JSON.generate as_json
      end

      #
      # For Rails 3 - http://jonathanjulian.com/2010/04/rails-to_json-or-as_json/
      #
      # @api private
      #

      def as_json(*)
        key = if bridge.dialect == :w3c
                'element-6066-11e4-a52e-4f735466cecf'
              else
                'ELEMENT'
              end
        @id.is_a?(Hash) ? @id : {key => @id}
      end

      private

      attr_reader :bridge

      def selectable?
        tn = tag_name.downcase
        type = attribute(:type).to_s.downcase

        tn == 'option' || (tn == 'input' && %w[radio checkbox].include?(type))
      end
    end # Element
  end # WebDriver
end # Selenium
