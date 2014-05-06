module ActionController
  class TestCase
    include ::Spec::Rails::Example::RoutingHelpers

    if Rails::VERSION::STRING =~ /2\.0/
      # Introduced in Rails 2.1, but we need it for 2.0
      def rescue_action_in_public!
        # See rescue.rb in this same directory
        def request.rescue_action_in_public?
          true
        end
      end
      
    end
  end
end