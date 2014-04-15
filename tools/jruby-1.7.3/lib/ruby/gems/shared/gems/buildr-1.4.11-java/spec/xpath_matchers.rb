# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

require 'rexml/document'
require 'rexml/element'

module RSpec
  module Matchers

    # check if the xpath exists one or more times
    class HaveXpath
      def initialize(xpath)
        @xpath = xpath
      end

      def matches?(response)
        @response = response
        doc = response.is_a?(REXML::Document) ? response : REXML::Document.new(@response)
        match = REXML::XPath.match(doc, @xpath)
        not match.empty?
      end

      def failure_message
        "Did not find expected xpath #{@xpath}"
      end

      def negative_failure_message
        "Did find unexpected xpath #{@xpath}"
      end

      def description
        "match the xpath expression #{@xpath}"
      end
    end

    def have_xpath(xpath)
      HaveXpath.new(xpath)
    end

    # check if the xpath has the specified value
    # value is a string and there must be a single result to match its
    # equality against
    class MatchXpath
      def initialize(xpath, val)
        @xpath = xpath
        @val= val
      end

      def matches?(response)
        @response = response
        doc = response.is_a?(REXML::Document) ? response : REXML::Document.new(@response)
        ok = true
        REXML::XPath.each(doc, @xpath) do |e|
          @actual_val = case e
          when REXML::Attribute
            e.to_s
          when REXML::Element
            e.text
          else
            e.to_s
          end
          return false unless @val == @actual_val
        end
        return ok
      end

      def failure_message
        "The xpath #{@xpath} did not have the value '#{@val}' It was '#{@actual_val}'"
      end

      def description
        "match the xpath expression #{@xpath} with #{@val}"
      end
    end

    def match_xpath(xpath, val)
      MatchXpath.new(xpath, val)
    end

    # checks if the given xpath occurs num times
    class HaveNodes  #:nodoc:
      def initialize(xpath, num)
        @xpath= xpath
        @num = num
      end

      def matches?(response)
        @response = response
        doc = response.is_a?(REXML::Document) ? response : REXML::Document.new(@response)
        match = REXML::XPath.match(doc, @xpath)
        @num_found= match.size
        @num_found == @num
      end

      def failure_message
        "Did not find expected number of nodes #{@num} in xpath #{@xpath} Found #{@num_found}"
      end

      def description
        "match the number of nodes #{@num}"
      end
    end

    def have_nodes(xpath, num)
      HaveNodes.new(xpath, num)
    end

  end
end