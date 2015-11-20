# encoding: utf-8
#
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

    #
    # @api beta This API may be changed or removed in a future release.
    #

    class Window

      #
      # @api private
      #

      def initialize(bridge)
        @bridge = bridge
      end

      #
      # Resize the current window to the given dimension.
      #
      # @param [Selenium::WebDriver::Dimension, #width and #height] dimension The new size.
      #

      def size=(dimension)
        unless dimension.respond_to?(:width) && dimension.respond_to?(:height)
          raise ArgumentError, "expected #{dimension.inspect}:#{dimension.class}" +
                                " to respond to #width and #height"
        end

        @bridge.setWindowSize dimension.width, dimension.height
      end

      #
      # Get the size of the current window.
      #
      # @return [Selenium::WebDriver::Dimension] The size.
      #

      def size
        @bridge.getWindowSize
      end

      #
      # Move the current window to the given position.
      #
      # @param [Selenium::WebDriver::Point, #x and #y] point The new position.
      #

      def position=(point)
        unless point.respond_to?(:x) && point.respond_to?(:y)
          raise ArgumentError, "expected #{point.inspect}:#{point.class}" +
                                " to respond to #x and #y"
        end

        @bridge.setWindowPosition point.x, point.y
      end

      #
      # Get the position of the current window.
      #
      # @return [Selenium::WebDriver::Point] The position.
      #

      def position
        @bridge.getWindowPosition
      end

      #
      # Equivalent to #size=, but accepts width and height arguments.
      #
      # @example Maximize the window.
      #
      #    max_width, max_height = driver.execute_script("return [window.screen.availWidth, window.screen.availHeight];")
      #    driver.manage.window.resize_to(max_width, max_height)
      #

      def resize_to(width, height)
        @bridge.setWindowSize Integer(width), Integer(height)
      end

      #
      # Equivalent to #position=, but accepts x and y arguments.
      #
      # @example
      #
      #   driver.manage.window.move_to(300, 400)
      #

      def move_to(x, y)
        @bridge.setWindowPosition Integer(x), Integer(y)
      end

      #
      # Maximize the current window
      #

      def maximize
        @bridge.maximizeWindow
      end

    end # Window
  end # WebDriver
end # Selenium
