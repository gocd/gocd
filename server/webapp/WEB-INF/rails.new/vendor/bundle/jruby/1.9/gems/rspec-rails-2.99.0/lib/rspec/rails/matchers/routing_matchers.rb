module RSpec::Rails::Matchers
  module RoutingMatchers
    extend RSpec::Matchers::DSL

    class RouteToMatcher < RSpec::Matchers::BuiltIn::BaseMatcher

      def initialize(scope, *expected)
        @scope = scope
        @expected = expected[1] || {}
        if Hash === expected[0]
          @expected.merge!(expected[0])
        else
          controller, action = expected[0].split('#')
          @expected.merge!(:controller => controller, :action => action)
        end
      end
      
      # @api private
      def matches?(verb_to_path_map)
        @actual = @verb_to_path_map = verb_to_path_map
        # assert_recognizes does not consider ActionController::RoutingError an
        # assertion failure, so we have to capture that and Assertion here.
        match_unless_raises ActiveSupport::TestCase::Assertion, ActionController::RoutingError do
          path, query = *verb_to_path_map.values.first.split('?')
          @scope.assert_recognizes(
            @expected,
            {:method => verb_to_path_map.keys.first, :path => path},
            Rack::Utils::parse_query(query)
          )
        end
      end

      # @api private
      def failure_message_for_should
        rescued_exception.message
      end

      def failure_message_for_should_not
        "expected #{@actual.inspect} not to route to #{@expected.inspect}"
      end

      def description
        "route #{@actual.inspect} to #{@expected.inspect}"
      end
    end

    # Delegates to `assert_recognizes`. Supports short-hand controller/action
    # declarations (e.g. `"controller#action"`).
    #
    # @example
    #
    #     { :get => "/things/special" }.should route_to(
    #       :controller => "things",
    #       :action     => "special"
    #     )
    #
    #     { :get => "/things/special" }.should route_to("things#special")
    #
    # @see http://api.rubyonrails.org/classes/ActionDispatch/Assertions/RoutingAssertions.html#method-i-assert_recognizes
    def route_to(*expected)
      RouteToMatcher.new(self, *expected)
    end

    class BeRoutableMatcher < RSpec::Matchers::BuiltIn::BaseMatcher

      def initialize(scope)
        @scope = scope
      end

      # @api private
      def matches?(path)
        @actual = path
        match_unless_raises ActionController::RoutingError do
          @routing_options = @scope.routes.recognize_path(
            path.values.first, :method => path.keys.first
          )
        end
      end

      def failure_message_for_should
        "expected #{@actual.inspect} to be routable"
      end

      def failure_message_for_should_not
        "expected #{@actual.inspect} not to be routable, but it routes to #{@routing_options.inspect}"
      end
    end

    # Passes if the route expression is recognized by the Rails router based on
    # the declarations in `config/routes.rb`. Delegates to
    # `RouteSet#recognize_path`.
    #
    # @example
    #
    # You can use route helpers provided by rspec-rails.
    #     {:get =>  "/a/path"}.should be_routable
    #     {:post => "/another/path"}.should be_routable
    #     {:put => "/yet/another/path"}.should_not be_routable
    def be_routable
      BeRoutableMatcher.new(self)
    end

    module RouteHelpers
      %w(get post put patch delete options head).each do |method|
        define_method method do |path|
          { method.to_sym => path }
        end
      end
    end
  end
end
