module Spec
  module Rails
    module Example
      module RoutingHelpers
        
        module ParamsFromQueryString # :nodoc:
          def params_from_querystring(querystring) # :nodoc:
            params = {}
            querystring.split('&').each do |piece|
              key, value = piece.split('=')
              params[key.to_sym] = value
            end
            params
          end
        end
        
        class RouteFor
          include ::Spec::Rails::Example::RoutingHelpers::ParamsFromQueryString
          def initialize(example, options)
            @example, @options = example, options
          end
  
          def ==(expected)
            if Hash === expected
              path, querystring = expected[:path].split('?')
              path = expected.merge(:path => path)
            else
              path, querystring = expected.split('?')
            end
            params = querystring.blank? ? {} : @example.params_from_querystring(querystring)
            @example.assert_recognizes(@options, path, params)
            true
          end
        end

        # Uses ActionController::Routing::Routes to generate
        # the correct route for a given set of options.
        # == Examples
        #   route_for(:controller => 'registrations', :action => 'edit', :id => '1')
        #     => '/registrations/1/edit'
        #   route_for(:controller => 'registrations', :action => 'create')
        #     => {:path => "/registrations", :method => :post}
        def route_for(options)
          RouteFor.new(self, options)
        end

        # Uses ActionController::Routing::Routes to parse
        # an incoming path so the parameters it generates can be checked
        # == Example
        #   params_from(:get, '/registrations/1/edit')
        #     => :controller => 'registrations', :action => 'edit', :id => '1'
        def params_from(method, path)
          ensure_that_routes_are_loaded
          path, querystring = path.split('?')
          params = ActionController::Routing::Routes.recognize_path(path, :method => method)
          querystring.blank? ? params : params.merge(params_from_querystring(querystring))
        end

      private

        include ParamsFromQueryString

        def ensure_that_routes_are_loaded
          ActionController::Routing::Routes.reload if ActionController::Routing::Routes.empty?
        end

      end
    end
  end
end
