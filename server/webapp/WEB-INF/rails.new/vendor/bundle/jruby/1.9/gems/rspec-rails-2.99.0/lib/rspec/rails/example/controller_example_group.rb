RSpec.configure do |config|
  config.add_setting :infer_base_class_for_anonymous_controllers, :default => false
end

module RSpec::Rails
  module ControllerExampleGroup
    extend ActiveSupport::Concern
    include RSpec::Rails::RailsExampleGroup
    include ActionController::TestCase::Behavior
    include RSpec::Rails::ViewRendering
    include RSpec::Rails::Matchers::RedirectTo
    include RSpec::Rails::Matchers::RenderTemplate
    include RSpec::Rails::Matchers::RoutingMatchers
    include RSpec::Rails::AssertionDelegator.new(ActionDispatch::Assertions::RoutingAssertions)

    module ClassMethods
      # @private
      def controller_class
        described_class
      end

      # Supports a simple DSL for specifying behavior of ApplicationController.
      # Creates an anonymous subclass of ApplicationController and evals the
      # `body` in that context. Also sets up implicit routes for this
      # controller, that are separate from those defined in "config/routes.rb".
      #
      # @note Due to Ruby 1.8 scoping rules in anoymous subclasses, constants
      #   defined in `ApplicationController` must be fully qualified (e.g.
      #   `ApplicationController::AccessDenied`) in the block passed to the
      #   `controller` method. Any instance methods, filters, etc, that are
      #   defined in `ApplicationController`, however, are accessible from
      #   within the block.
      #
      # @example
      #
      #     describe ApplicationController do
      #       controller do
      #         def index
      #           raise ApplicationController::AccessDenied
      #         end
      #       end
      #
      #       describe "handling AccessDenied exceptions" do
      #         it "redirects to the /401.html page" do
      #           get :index
      #           response.should redirect_to("/401.html")
      #         end
      #       end
      #     end
      #
      # If you would like to spec a subclass of ApplicationController, call
      # controller like so:
      #
      #     controller(ApplicationControllerSubclass) do
      #       # ....
      #     end
      def controller(base_class = nil, &body)
        root_controller = defined?(ApplicationController) ? ApplicationController : ActionController::Base
        base_class ||= RSpec.configuration.infer_base_class_for_anonymous_controllers? ?
                         controller_class :
                         root_controller
        base_class ||= root_controller

        new_controller_class = Class.new(base_class) do
          def self.name; "AnonymousController"; end
        end
        new_controller_class.class_eval(&body)

        (class << self; self; end).__send__(:define_method, :controller_class) { new_controller_class }
        metadata[:example_group].delete(:described_class)
        metadata[:example_group].extend DescribedClassDeprecation.new(new_controller_class)

        before do
          @orig_routes = self.routes
          self.routes  = ActionDispatch::Routing::RouteSet.new.tap { |r|
            r.draw { resources :anonymous }
          }
        end

        after do
          self.routes  = @orig_routes
          @orig_routes = nil
        end
      end

      class DescribedClassDeprecation < Module
        def initialize(value)
          module_eval do
            define_method :store_computed do |key|
              return super(key) unless key == :described_class

              RSpec.warn_deprecation(<<-EOS.gsub(/^\s+\|/,''))
                |In RSpec 3, the `controller { }` macro no longer changes
                |`described_class` to refer to the generated anonymous controller
                |class. Instead of `described_class`, use `controller_class` to
                |access the generated anonymous class.
                |(`described_class` called from #{::RSpec::CallerFilter.first_non_rspec_line}.)
              EOS
              store(:described_class, value)
            end
          end
        end
      end

      # Specifies the routeset that will be used for the example group. This
      # is most useful when testing Rails engines.
      #
      # @example
      #
      #     describe MyEngine::PostsController do
      #       routes { MyEngine::Engine.routes }
      #
      #       # ...
      #     end
      def routes(&blk)
        before do
          self.routes = blk.call
        end
      end
    end

    attr_reader :controller, :routes

    # @api private
    def routes=(routes)
      @routes = routes
      assertion_instance.instance_variable_set(:@routes, routes)
    end

    module BypassRescue
      def rescue_with_handler(exception)
        raise exception
      end
    end

    # Extends the controller with a module that overrides
    # `rescue_with_handler` to raise the exception passed to it.  Use this to
    # specify that an action _should_ raise an exception given appropriate
    # conditions.
    #
    # @example
    #
    #     describe ProfilesController do
    #       it "raises a 403 when a non-admin user tries to view another user's profile" do
    #         profile = create_profile
    #         login_as profile.user
    #
    #         expect do
    #           bypass_rescue
    #           get :show, :id => profile.id + 1
    #         end.to raise_error(/403 Forbidden/)
    #       end
    #     end
    def bypass_rescue
      controller.extend(BypassRescue)
    end

    # If method is a named_route, delegates to the RouteSet associated with
    # this controller.
    def method_missing(method, *args, &block)
      if @routes && @routes.named_routes.helpers.include?(method)
        controller.send(method, *args, &block)
      elsif defined?(@orig_routes) && @orig_routes && @orig_routes.named_routes.helpers.include?(method)
        controller.send(method, *args, &block)
      else
        super
      end
    end

    included do
      subject { controller }

      metadata[:type] = :controller

      before do
        self.routes = ::Rails.application.routes
      end

      around do |ex|
        previous_allow_forgery_protection_value = ActionController::Base.allow_forgery_protection
        begin
          ActionController::Base.allow_forgery_protection = false
          ex.call
        ensure
          ActionController::Base.allow_forgery_protection = previous_allow_forgery_protection_value
        end
      end
    end
  end
end
