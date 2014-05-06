module Spec
  module Rails
    module Matchers

      class RedirectTo  #:nodoc:

        include ActionController::StatusCodes

        def initialize(request, expected)
          @expected = expected
          @request = request
        end

        def matches?(response_or_controller)
          response  = response_or_controller.respond_to?(:response) ?
                      response_or_controller.response :
                      response_or_controller

          @redirected = response.redirect?
          @actual = response.redirect_url
          return false unless @redirected

          if @expected_status
            @actual_status = interpret_status(response.code.to_i)
            @status_matched = @expected_status == @actual_status
          else
            @status_matched = true
          end

          if @expected.instance_of? Hash
            return false unless @actual =~ %r{^\w+://#{@request.host}}
            return false unless actual_redirect_to_valid_route
            return actual_hash == expected_hash && @status_matched
          else
            return @actual == expected_url && @status_matched
          end
        end

        def actual_hash
          hash_from_url @actual
        end

        def expected_hash
          hash_from_url expected_url
        end

        def actual_redirect_to_valid_route
          actual_hash
        end

        def hash_from_url(url)
          query_hash(url).merge(path_hash(url)).with_indifferent_access
        end

        def path_hash(url)
          path = url.sub(%r{^\w+://#{@request.host}(?::\d+)?}, "").split("?", 2)[0]
          ActionController::Routing::Routes.recognize_path path, { :method => :get }
        end

        def query_hash(url)
          query = url.split("?", 2)[1] || ""
          Rack::Utils.parse_query(query)
        end

        def with(options)
          @expected_status = interpret_status(options[:status])
          self
        end
        
       def expected_url
          case @expected
            when Hash
              return ActionController::UrlRewriter.new(@request, {}).rewrite(@expected)
            when :back
              return @request.env['HTTP_REFERER']
            when %r{^\w+://.*}
              return @expected
            else
              return "http://#{@request.host}" + (@expected.split('')[0] == '/' ? '' : '/') + @expected
          end
        end

        def failure_message_for_should
          if @redirected
            if @status_matched
              return %Q{expected redirect to #{@expected.inspect}, got redirect to #{@actual.inspect}}
            else
              return %Q{expected redirect to #{@expected.inspect} with status #{@expected_status}, got #{@actual_status}}
            end
          else
            return %Q{expected redirect to #{@expected.inspect}, got no redirect}
          end
        end

        def failure_message_for_should_not
            return %Q{expected not to be redirected to #{@expected.inspect}, but was} if @redirected
        end

        def description
          "redirect to #{@expected.inspect}"
        end
      end

      # :call-seq:
      #   response.should redirect_to(url)
      #   response.should redirect_to(:action => action_name)
      #   response.should redirect_to(:controller => controller_name, :action => action_name)
      #   response.should_not redirect_to(url)
      #   response.should_not redirect_to(:action => action_name)
      #   response.should_not redirect_to(:controller => controller_name, :action => action_name)
      #
      # Passes if the response is a redirect to the url, action or controller/action.
      # Useful in controller specs (integration or isolation mode).
      #
      # == Examples
      #
      #   response.should redirect_to("path/to/action")
      #   response.should redirect_to("http://test.host/path/to/action")
      #   response.should redirect_to(:action => 'list')
      def redirect_to(opts)
        RedirectTo.new(request, opts)
      end
    end

  end
end
