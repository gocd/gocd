module Rails
  module Controller
    module Testing
      module Integration
        %w(
          get post patch put head delete xml_http_request
          xhr get_via_redirect post_via_redirect
        ).each do |method|

          define_method(method) do |*args|
            reset_template_assertion
            super(*args)
          end
        end
      end
    end
  end
end
