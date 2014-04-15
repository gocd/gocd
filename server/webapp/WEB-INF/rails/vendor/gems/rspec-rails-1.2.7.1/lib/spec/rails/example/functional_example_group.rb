require 'action_controller/test_case'

module Spec
  module Rails
    module Example
      class FunctionalExampleGroup < ActionController::TestCase
        def setup
          # no-op to override AC::TC's setup w/ conflicts with the before(:each) below
        end

        attr_reader :request, :response

        # The params hash accessed within a view or helper. Use this before
        # rendering a view or calling a helper to provide data used by the
        # view or helper.
        #
        # == Examples
        #   # in a view spec
        #   params[:name] = "David"
        #   render
        #   response.should have_tag("div.name","David")
        #    
        #   # in a helper spec
        #   params[:first_name] = "David"
        #   params[:last_name] = "Chelimsky"
        #   helper.full_name.should == "David Chelimsky"
        def params
          request.parameters
        end

        # Provides access to the flash hash. Use this after rendering a
        # view, calling a helper or calling a controller action.
        #
        # == Examples
        #   post :create
        #   flash[:notice].should == "Success!"
        def flash
          @controller.__send__ :flash
        end

        # Provides acces to the session hash. Use this before or after
        # rendering a view, calling a helper or calling a controller action.
        def session
          request.session
        end
        
        # Overrides the <tt>cookies()</tt> method in
        # ActionController::TestResponseBehaviour, returning a proxy that
        # accesses the requests cookies when setting a cookie and the
        # responses cookies when reading one. This allows you to set and read
        # cookies in examples using the same API with which you set and read
        # them in controllers.
        #
        # == Examples (Rails 2.0 > 2.2)
        #
        #   cookies[:user_id] = {:value => '1234', :expires => 1.minute.ago}
        #   get :index
        #   response.should be_redirect
        #
        # == Examples (Rails 2.3)
        #
        # Rails 2.3 changes the way cookies are made available to functional
        # tests (and therefore rspec controller specs), only making single
        # values available with no access to other aspects of the cookie. This
        # is backwards-incompatible, so you have to change your examples to
        # look like this:
        #
        #   cookies[:foo] = 'bar'
        #   get :index
        #   cookies[:foo].should == 'bar'
        def cookies
          @cookies ||= Spec::Rails::Example::CookiesProxy.new(self)
        end
        
        alias_method :orig_assigns, :assigns

        # :call-seq:
        #   assigns()
        #
        # Hash of instance variables to values that are made available to
        # views. == Examples
        #
        #   #in thing_controller.rb
        #   def new
        #     @thing = Thing.new
        #   end
        #
        #   #in thing_controller_spec
        #   get 'new'
        #   assigns[:registration].should == Thing.new
        #--
        # NOTE - Even though docs only use assigns[:key] format, this supports
        # assigns(:key) for backwards compatibility.
        #++
        def assigns(key = nil)
          if key.nil?
            _assigns_hash_proxy
          else
            _assigns_hash_proxy[key]
          end
        end

      end
    end
  end
end
