module Spec
  module Rails
    module Example

      class RoutingExampleGroup < ActionController::TestCase
        tests Class.new(ActionController::Base)
        
        Spec::Example::ExampleGroupFactory.register(:routing, self)
      end

    end
  end
end