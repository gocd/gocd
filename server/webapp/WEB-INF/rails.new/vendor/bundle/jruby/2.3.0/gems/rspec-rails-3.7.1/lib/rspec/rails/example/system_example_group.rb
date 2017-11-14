if ActionPack::VERSION::STRING >= "5.1"
  require 'action_dispatch/system_test_case'
  module RSpec
    module Rails
      # @api public
      # Container class for system tests
      module SystemExampleGroup
        extend ActiveSupport::Concern
        include RSpec::Rails::RailsExampleGroup
        include ActionDispatch::Integration::Runner
        include ActionDispatch::Assertions
        include RSpec::Rails::Matchers::RedirectTo
        include RSpec::Rails::Matchers::RenderTemplate
        include ActionController::TemplateAssertions

        include ActionDispatch::IntegrationTest::Behavior

        # @private
        module BlowAwayAfterTeardownHook
          # @private
          def after_teardown
          end
        end

        original_after_teardown = ::ActionDispatch::SystemTesting::TestHelpers::SetupAndTeardown.instance_method(:after_teardown)

        include ::ActionDispatch::SystemTesting::TestHelpers::SetupAndTeardown
        include ::ActionDispatch::SystemTesting::TestHelpers::ScreenshotHelper
        include BlowAwayAfterTeardownHook

        # for the SystemTesting Screenshot situation
        def passed?
          RSpec.current_example.exception.nil?
        end

        # @private
        def method_name
          @method_name ||= [
            self.class.name.underscore,
            RSpec.current_example.description.underscore,
            rand(1000)
          ].join("_").gsub(/[\/\.:, ]/, "_")
        end

        # Delegates to `Rails.application`.
        def app
          ::Rails.application
        end

        included do
          attr_reader :driver

          if ActionDispatch::SystemTesting::Server.respond_to?(:silence_puma=)
            ActionDispatch::SystemTesting::Server.silence_puma = true
          end

          def initialize(*args, &blk)
            super(*args, &blk)
            @driver = nil
          end

          def driven_by(*args, &blk)
            @driver = ::ActionDispatch::SystemTestCase.driven_by(*args, &blk).tap(&:use)
          end

          before do
            # A user may have already set the driver, so only default if driver
            # is not set
            driven_by(:selenium) unless @driver
            @routes = ::Rails.application.routes
          end

          after do
            orig_stdout = $stdout
            $stdout = StringIO.new
            begin
              original_after_teardown.bind(self).call
            ensure
              myio = $stdout
              RSpec.current_example.metadata[:extra_failure_lines] = myio.string
              $stdout = orig_stdout
            end
          end
        end
      end
    end
  end
end
