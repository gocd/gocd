module RSpec::Rails::Matchers
  module RenderTemplate
    class RenderTemplateMatcher < RSpec::Matchers::BuiltIn::BaseMatcher

      def initialize(scope, expected, message=nil)
        @expected = Symbol === expected ? expected.to_s : expected
        @message = message
        @scope = scope
      end

      # @api private
      def matches?(*)
        match_unless_raises ActiveSupport::TestCase::Assertion do
          @scope.assert_template expected, @message
        end
      end

      # @api private
      def failure_message_for_should
        rescued_exception.message
      end

      # @api private
      def failure_message_for_should_not
        "expected not to render #{expected.inspect}, but did"
      end
    end

    # Delegates to `assert_template`
    #
    # @example
    #
    #     response.should have_rendered("new")
    def have_rendered(options, message=nil)
      RenderTemplateMatcher.new(self, options, message)
    end

    alias_method :render_template, :have_rendered
  end
end
