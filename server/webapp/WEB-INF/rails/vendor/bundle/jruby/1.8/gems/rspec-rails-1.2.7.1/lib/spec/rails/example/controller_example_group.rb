module Spec
  module Rails
    module Example
      # Controller Examples live in $RAILS_ROOT/spec/controllers/.
      #
      # Controller Examples use Spec::Rails::Example::ControllerExampleGroup,
      # which supports running specs for Controllers in two modes, which
      # represent the tension between the more granular testing common in TDD
      # and the more high level testing built into rails. BDD sits somewhere
      # in between: we want to a balance between specs that are close enough
      # to the code to enable quick fault isolation and far enough away from
      # the code to enable refactoring with minimal changes to the existing
      # specs.
      #
      # == Isolation mode (default)
      #
      # No dependencies on views because none are ever rendered. The benefit
      # of this mode is that can spec the controller completely independent of
      # the view, allowing that responsibility to be handled later, or by
      # somebody else. Combined w/ separate view specs, this also provides
      # better fault isolation.
      #
      # == Integration mode
      #
      # To run in this mode, include the +integrate_views+ declaration
      # in your controller context:
      #
      #   describe ThingController do
      #     integrate_views
      #     ...
      #
      # In this mode, controller specs are run in the same way that rails
      # functional tests run - one set of tests for both the controllers and
      # the views. The benefit of this approach is that you get wider coverage
      # from each spec. Experienced rails developers may find this an easier
      # approach to begin with, however we encourage you to explore using the
      # isolation mode and revel in its benefits.
      #
      # == Expecting Errors
      #
      # Rspec on Rails will raise errors that occur in controller actions and
      # are not rescued or handeled with rescue_from.
      #
      class ControllerExampleGroup < FunctionalExampleGroup
        class << self
                    
          # Use integrate_views to instruct RSpec to render views in
          # your controller examples in Integration mode.
          #
          #   describe ThingController do
          #     integrate_views
          #     ...
          #
          # See Spec::Rails::Example::ControllerExampleGroup for more
          # information about Integration and Isolation modes.
          def integrate_views(integrate_views = true)
            @integrate_views = integrate_views
          end
          
          def integrate_views? # :nodoc:
            @integrate_views
          end
          
          def inherited(klass) # :nodoc:
            klass.integrate_views(integrate_views?)
            klass.subject { controller }
            super
          end

          def set_description(*args) # :nodoc:
            super
            if described_class && described_class.ancestors.include?(ActionController::Base)
              controller_klass = if superclass.controller_class.ancestors.include?(ActionController::Base)
                superclass.controller_class
              else
                described_class
              end
              tests controller_klass
            end
          end

          # When you don't pass a controller to describe, like this:
          #
          #   describe ThingsController do
          #
          # ... then you must provide a controller_name within the context of
          # your controller specs:
          #
          #   describe "ThingController" do
          #     controller_name :thing
          #     ...
          def controller_name(name)
            tests "#{name}_controller".camelize.constantize
          end
        end
        
        before(:each) do
          # Some Rails apps explicitly disable ActionMailer in environment.rb
          if defined?(ActionMailer)
            @deliveries = []
            ActionMailer::Base.deliveries = @deliveries
          end

          unless @controller.class.ancestors.include?(ActionController::Base)
            Spec::Expectations.fail_with <<-MESSAGE
Controller specs need to know what controller is being specified. You can
indicate this by passing the controller to describe():

  describe MyController do
    
or by declaring the controller's name

  describe "a MyController" do
    controller_name :my #invokes the MyController
end
MESSAGE
          end
          @controller.extend ControllerInstanceMethods
          @controller.integrate_views! if integrate_views?
          @controller.session = session
        end

        attr_reader :response, :request, :controller
        
        def integrate_views?
          @integrate_views || self.class.integrate_views?
        end

        # Bypasses any error rescues defined with rescue_from. Useful
        # in cases in which you want to specify errors coming out of
        # actions that might be caught by a rescue_from clause that is
        # specified separately.
        #
        # Note that this will override the effect of rescue_action_in_public
        def bypass_rescue
          if ::Rails::VERSION::STRING >= '2.2'
            def controller.rescue_action(exception)
              raise exception
            end
          else
            def controller.rescue_action_with_handler(exception)
              raise exception
            end
          end
        end
        
      protected

        def _assigns_hash_proxy
          @_assigns_hash_proxy ||= AssignsHashProxy.new(self) {@response.template}
        end

      private
        
        module TemplateIsolationExtensions
          def file_exists?(ignore); true; end
          
          def render_file(*args)
            @first_render ||= args[0] unless args[0] =~ /^layouts/
          end
          
          # Rails 2.2
          def _pick_template(*args)
            @_first_render ||= args[0] unless args[0] =~ /^layouts/
            PickedTemplate.new
          end
          
          def render(*args)
            if file = args.last[:file].instance_eval{@template_path}
              record_render :file => file
            elsif args.last[:inline]
              super
            elsif @_rendered
              record_render(args[0])
            else
              super
            end
          end
        
        private
        
          def record_render(opts)
            (@_rendered[:template] ||= opts[:file]) if opts[:file]
            (@_rendered[:partials][opts[:partial]] += 1) if opts[:partial]
          end
          
          # Returned by _pick_template when running controller examples in isolation mode.
          class PickedTemplate 
            # Do nothing when running controller examples in isolation mode.
            def render_template(*ignore_args); end
            # Do nothing when running controller examples in isolation mode.
            def render_partial(*ignore_args);  end
          end
        end
        
        module ControllerInstanceMethods # :nodoc:
          include Spec::Rails::Example::RenderObserver

          # === render(options = nil, extra_options={}, &block)
          #
          # This gets added to the controller's singleton meta class,
          # allowing Controller Examples to run in two modes, freely switching
          # from example group to example group.
          def render(options=nil, extra_options={}, &block)
            unless block_given?
              unless integrate_views?
                @template.extend TemplateIsolationExtensions
              end
            end

            if matching_message_expectation_exists(options)
              render_proxy.render(options, &block)
              @performed_render = true
            else
              if matching_stub_exists(options)
                @performed_render = true
              else
                super
              end
            end
          end
          
          # Rails 2.3
          def default_template(action_name = self.action_name)
            if integrate_views?
              super
            else
              begin
                super
              rescue ActionView::MissingTemplate
                "#{self.class.name.sub(/Controller$/,'').underscore}/#{action_name}"
              end
            end
          end
          
          def response(&block)
            # NOTE - we're setting @update for the assert_select_spec - kinda weird, huh?
            @update = block
            super
          end

          def integrate_views!
            @integrate_views = true
          end

        private
        
          def integrate_views?
            @integrate_views
          end

          def matching_message_expectation_exists(options)
            render_proxy.__send__(:__mock_proxy).__send__(:find_matching_expectation, :render, options)
          end
        
          def matching_stub_exists(options)
            render_proxy.__send__(:__mock_proxy).__send__(:find_matching_method_stub, :render, options)
          end
        
        end

        Spec::Example::ExampleGroupFactory.register(:controller, self)
        
      end
    end
  end
end
