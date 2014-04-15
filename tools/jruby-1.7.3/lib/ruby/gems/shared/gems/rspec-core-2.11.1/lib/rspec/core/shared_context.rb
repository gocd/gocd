module RSpec
  module Core
    # Exposes {ExampleGroup}-level methods to a module, so you can include that
    # module in an {ExampleGroup}.
    #
    # @example
    #
    #     module LoggedInAsAdmin
    #       extend RSpec::Core::SharedContext
    #       before(:each) do
    #         log_in_as :admin
    #       end
    #     end
    #
    #     describe "admin section" do
    #       include LoggedInAsAdmin
    #       # ...
    #     end
    module SharedContext
      include Hooks
      include Let::ExampleGroupMethods

      def included(group)
        [:before, :after].each do |type|
          [:all, :each].each do |scope|
            group.hooks[type][scope].concat hooks[type][scope]
          end
        end
        _nested_group_declarations.each do |name, block, *args|
          group.describe name, *args, &block
        end
      end

      def describe(name, *args, &block)
        _nested_group_declarations << [name, block, *args]
      end

      alias_method :context, :describe

      private

      def _nested_group_declarations
        @_nested_group_declarations ||= []
      end
    end
  end

  SharedContext = Core::SharedContext
end
