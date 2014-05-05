module Spec
  module Rails
    module Example
      class ViewExampleGroupController < ApplicationController #:nodoc:
        include Spec::Rails::Example::RenderObserver
        attr_reader :template

        def add_helper_for(template_path)
          add_helper(template_path.split('/')[0])
        end

        def add_helper(name)
          begin
            helper_module = "#{name}_helper".camelize.constantize
          rescue
            return
          end
          (class << template; self; end).class_eval do
            include helper_module
          end
        end
        
        def forget_variables_added_to_assigns
        end
      end

      # View Examples live in $RAILS_ROOT/spec/views/.
      #
      # View Specs use Spec::Rails::Example::ViewExampleGroup,
      # which provides access to views without invoking any of your controllers.
      # See Spec::Rails::Expectations::Matchers for information about specific
      # expectations that you can set on views.
      #
      # == Example
      #
      #   describe "login/login" do
      #     before do
      #       render 'login/login'
      #     end
      # 
      #     it "should display login form" do
      #       response.should have_tag("form[action=/login]") do
      #         with_tag("input[type=text][name=email]")
      #         with_tag("input[type=password][name=password]")
      #         with_tag("input[type=submit][value=Login]")
      #       end
      #     end
      #   end
      class ViewExampleGroup < FunctionalExampleGroup
        if ActionView::Base.respond_to?(:load_helpers) # Rails 2.0.x
          ActionView::Helpers.constants.each do |name|
            const = ActionView::Helpers.const_get(name)
            include const if name.include?("Helper") && Module === const
          end
        elsif ActionView::Base.respond_to?(:helper_modules) # Rails 2.1.x
          ActionView::Base.helper_modules.each do |helper_module|
            include helper_module
          end
        else # Rails 2.2.x
          include ActionView::Helpers
        end

        tests ViewExampleGroupController
        class << self
          def inherited(klass) # :nodoc:
            klass.subject { template }
            super
          end
        end

        before {ensure_that_flash_and_session_work_properly}
        after  {ensure_that_base_view_path_is_not_set_across_example_groups}

        def ensure_that_flash_and_session_work_properly #:nodoc:
          @controller.class.__send__ :public, :flash
          @controller.__send__ :initialize_template_class, @response
          @controller.__send__ :assign_shortcuts, @request, @response
          @controller.__send__ :initialize_current_url
          @session = @controller.session
        end

        def ensure_that_base_view_path_is_not_set_across_example_groups #:nodoc:
          ActionView::Base.base_view_path = nil
        end

        def set_base_view_path(options) #:nodoc:
          ActionView::Base.base_view_path = base_view_path(options)
        end

        def base_view_path(options) #:nodoc:
          "/#{derived_controller_name(options)}/"
        end

        def derived_controller_name(options) #:nodoc:
          parts = subject_of_render(options).split('/').reject { |part| part.empty? }
          "#{parts[0..-2].join('/')}"
        end

        def derived_action_name(options) #:nodoc:
          parts = subject_of_render(options).split('/').reject { |part| part.empty? }
          "#{parts.last}".split('.').first
        end

        def subject_of_render(options) #:nodoc:
          [:template, :partial, :file].each do |render_type|
            if options.has_key?(render_type)
              return options[render_type]
            end
          end
          return ""
        end

        def add_helpers(options) #:nodoc:
          @controller.add_helper("application")
          @controller.add_helper(derived_controller_name(options))
          @controller.add_helper(options[:helper]) if options[:helper]
          options[:helpers].each { |helper| @controller.add_helper(helper) } if options[:helpers]
        end

        # Renders a template for a View Spec, which then provides access to the result
        # through the +response+. Also supports render with :inline, which you can
        # use to spec custom form builders, helpers, etc, in the context of a view.
        #
        # == Examples
        #
        #   render('/people/list')
        #   render('/people/list', :helper => MyHelper)
        #   render('/people/list', :helpers => [MyHelper, MyOtherHelper])
        #   render(:partial => '/people/_address')
        #   render(:inline => "<% custom_helper 'argument', 'another argument' %>")
        #
        # See Spec::Rails::Example::ViewExampleGroup for more information.
        def render(*args)
          options = Hash === args.last ? args.pop : {}
          
          if args.empty? 
            unless [:partial, :inline, :file, :template, :xml, :json, :update].any? {|k| options.has_key? k} 
              args << self.class.description_parts.first
            end
          end
          
          options[:template] = args.first.to_s.sub(/^\//,'') unless args.empty?
          
          set_base_view_path(options)
          add_helpers(options)

          assigns[:action_name] = @action_name
          
          @request.path_parameters = @request.path_parameters.merge(
            :controller => derived_controller_name(options),
            :action => derived_action_name(options)
          ).merge(options[:path_parameters] || {})

          defaults = { :layout => false }
          options = defaults.merge options

          @controller.__send__(:params).reverse_merge! @request.parameters

          @controller.class.instance_eval %{
            def controller_path
              "#{derived_controller_name(options)}"
            end

            def controller_name
              "#{derived_controller_name(options).split('/').last}"
            end
          }

          @controller.__send__ :forget_variables_added_to_assigns
          @controller.__send__ :render, options
          @controller.__send__ :process_cleanup
        end

        # This provides the template. Use this to set mock
        # expectations for dealing with partials
        #
        # == Example
        #
        #   describe "/person/new" do
        #     it "should use the form partial" do
        #       template.should_receive(:render).with(:partial => 'form')
        #       render "/person/new"
        #     end
        #   end
        def template
          @controller.template
        end

        Spec::Example::ExampleGroupFactory.register(:view, self)

      protected
        def _assigns_hash_proxy
          @_assigns_hash_proxy ||= AssignsHashProxy.new(self) {@response.template}
        end
      end

    end
  end
end
