# frozen_string_literal: true
module Capybara
  module RSpecMatchers
    class Matcher
      if defined?(::RSpec::Expectations::Version) && (Gem::Version.new(RSpec::Expectations::Version::STRING) >= Gem::Version.new('3.0'))
        require 'capybara/rspec/compound'
        include ::Capybara::RSpecMatchers::Compound
      end

      attr_reader :failure_message, :failure_message_when_negated

      def wrap(actual)
        @context_el = if actual.respond_to?("has_selector?")
          actual
        else
          Capybara.string(actual.to_s)
        end
      end

      # RSpec 2 compatibility:
      def failure_message_for_should; failure_message end
      def failure_message_for_should_not; failure_message_when_negated end

      private

      def wrap_matches?(actual)
        yield(wrap(actual))
      rescue Capybara::ExpectationNotMet => e
        @failure_message = e.message
        return false
      end

      def wrap_does_not_match?(actual)
        yield(wrap(actual))
      rescue Capybara::ExpectationNotMet => e
        @failure_message_when_negated = e.message
        return false
      end

      def session_query_args
        if @args.last.is_a? Hash
          @args.last[:session_options] = session_options
        else
          @args.push(session_options: session_options)
        end
        @args
      end

      def session_options
        @context_el ||= nil
        if @context_el.respond_to? :session_options
          @context_el.session_options
        elsif @context_el.respond_to? :current_scope
          @context_el.current_scope.session_options
        else
          Capybara.session_options
        end
      end
    end

    class HaveSelector < Matcher

      def initialize(*args, &filter_block)
        @args = args
        @filter_block = filter_block
      end

      def matches?(actual)
        wrap_matches?(actual){ |el| el.assert_selector(*@args, &@filter_block) }
      end

      def does_not_match?(actual)
        wrap_does_not_match?(actual){ |el| el.assert_no_selector(*@args, &@filter_block) }
      end

      def description
        "have #{query.description}"
      end

      def query
        @query ||= Capybara::Queries::SelectorQuery.new(*session_query_args, &@filter_block)
      end
    end

    class MatchSelector < HaveSelector
      def matches?(actual)
        wrap_matches?(actual) { |el| el.assert_matches_selector(*@args, &@filter_block) }
      end

      def does_not_match?(actual)
        wrap_does_not_match?(actual) { |el| el.assert_not_matches_selector(*@args, &@filter_block) }
      end

      def description
        "match #{query.description}"
      end

      def query
        @query ||= Capybara::Queries::MatchQuery.new(*session_query_args, &@filter_block)
      end
    end

    class HaveText < Matcher
      attr_reader :type, :content, :options

      def initialize(*args)
        @args = args.dup

        # are set just for backwards compatability
        @type = args.shift if args.first.is_a?(Symbol)
        @content = args.shift
        @options = (args.first.is_a?(Hash))? args.first : {}
      end

      def matches?(actual)
        wrap_matches?(actual) { |el| el.assert_text(*@args) }
      end

      def does_not_match?(actual)
        wrap_does_not_match?(actual) { |el| el.assert_no_text(*@args) }
      end

      def description
        "text #{format(content)}"
      end

      def format(content)
        content = Capybara::Helpers.normalize_whitespace(content) unless content.is_a? Regexp
        content.inspect
      end
    end

    class HaveTitle < Matcher
      attr_reader :title

      def initialize(*args)
        @args = args

        # are set just for backwards compatability
        @title = args.first
      end

      def matches?(actual)
        wrap_matches?(actual) { |el| el.assert_title(*@args) }
      end

      def does_not_match?(actual)
        wrap_does_not_match?(actual) { |el| el.assert_no_title(*@args) }
      end

      def description
        "have title #{title.inspect}"
      end
    end

    class HaveCurrentPath < Matcher
      attr_reader :current_path

      def initialize(*args)
        @args = args

        # are set just for backwards compatability
        @current_path = args.first
      end

      def matches?(actual)
        wrap_matches?(actual) { |el| el.assert_current_path(*@args) }
      end

      def does_not_match?(actual)
        wrap_does_not_match?(actual) { |el| el.assert_no_current_path(*@args) }
      end

      def description
        "have current path #{current_path.inspect}"
      end
    end

    class BecomeClosed
      def initialize(options)
        @options = options
      end

      def matches?(window)
        @window = window
        @wait_time = Capybara::Queries::BaseQuery.wait(@options, window.session.config.default_max_wait_time)
        start_time = Capybara::Helpers.monotonic_time
        while window.exists?
          return false if (Capybara::Helpers.monotonic_time - start_time) > @wait_time
          sleep 0.05
        end
        true
      end

      def failure_message
        "expected #{@window.inspect} to become closed after #{@wait_time} seconds"
      end

      def failure_message_when_negated
        "expected #{@window.inspect} not to become closed after #{@wait_time} seconds"
      end

      # RSpec 2 compatibility:
      alias_method :failure_message_for_should, :failure_message
      alias_method :failure_message_for_should_not, :failure_message_when_negated
    end

    # RSpec matcher for whether the element(s) matching a given selector exist
    # See {Capybara::Node::Matcher#assert_selector}
    def have_selector(*args, &optional_filter_block)
      HaveSelector.new(*args, &optional_filter_block)
    end

    # RSpec matcher for whether the current element matches a given selector
    # See {Capybara::Node::Matchers#assert_matches_selector}
    def match_selector(*args, &optional_filter_block)
      MatchSelector.new(*args, &optional_filter_block)
    end
    # defined_negated_matcher was added in RSpec 3.1 - it's syntactic sugar only since a user can do
    # expect(page).not_to match_selector, so not sure we really need to support not_match_selector for prior to RSpec 3.1
    ::RSpec::Matchers.define_negated_matcher :not_match_selector, :match_selector if defined?(::RSpec::Expectations::Version) && (Gem::Version.new(RSpec::Expectations::Version::STRING) >= Gem::Version.new('3.1'))


    # RSpec matcher for whether elements(s) matching a given xpath selector exist
    # See {Capybara::Node::Matchers#has_xpath?}
    def have_xpath(xpath, options={}, &optional_filter_block)
      HaveSelector.new(:xpath, xpath, options, &optional_filter_block)
    end

    # RSpec matcher for whether the current element matches a given xpath selector
    def match_xpath(xpath, options={}, &optional_filter_block)
      MatchSelector.new(:xpath, xpath, options, &optional_filter_block)
    end

    # RSpec matcher for whether elements(s) matching a given css selector exist
    # See {Capybara::Node::Matchers#has_css?}
    def have_css(css, options={}, &optional_filter_block)
      HaveSelector.new(:css, css, options, &optional_filter_block)
    end

    # RSpec matcher for whether the current element matches a given css selector
    def match_css(css, options={}, &optional_filter_block)
      MatchSelector.new(:css, css, options, &optional_filter_block)
    end

    # RSpec matcher for text on the page
    # See {Capybara::SessionMatchers#assert_text}
    def have_text(*args)
      HaveText.new(*args)
    end
    alias_method :have_content, :have_text

    def have_title(title, options = {})
      HaveTitle.new(title, options)
    end

    # RSpec matcher for the current path
    # See {Capybara::SessionMatchers#assert_current_path}
    def have_current_path(path, options = {})
      HaveCurrentPath.new(path, options)
    end

    # RSpec matcher for links
    # See {Capybara::Node::Matchers#has_link?}
    def have_link(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:link, locator, options, &optional_filter_block)
    end

    # RSpec matcher for buttons
    # See {Capybara::Node::Matchers#has_button?}
    def have_button(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:button, locator, options, &optional_filter_block)
    end

    # RSpec matcher for links
    # See {Capybara::Node::Matchers#has_field?}
    def have_field(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:field, locator, options, &optional_filter_block)
    end

    # RSpec matcher for checked fields
    # See {Capybara::Node::Matchers#has_checked_field?}
    def have_checked_field(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:field, locator, options.merge(checked: true), &optional_filter_block)
    end

    # RSpec matcher for unchecked fields
    # See {Capybara::Node::Matchers#has_unchecked_field?}
    def have_unchecked_field(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:field, locator, options.merge(unchecked: true), &optional_filter_block)
    end

    # RSpec matcher for select elements
    # See {Capybara::Node::Matchers#has_select?}
    def have_select(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:select, locator, options, &optional_filter_block)
    end

    # RSpec matcher for table elements
    # See {Capybara::Node::Matchers#has_table?}
    def have_table(locator=nil, options={}, &optional_filter_block)
      locator, options = nil, locator if locator.is_a? Hash
      HaveSelector.new(:table, locator, options, &optional_filter_block)
    end

    ##
    # Wait for window to become closed.
    # @example
    #   expect(window).to become_closed(wait: 0.8)
    # @param options [Hash] optional param
    # @option options [Numeric] :wait (Capybara.default_max_wait_time) Maximum wait time
    def become_closed(options = {})
      BecomeClosed.new(options)
    end
  end
end