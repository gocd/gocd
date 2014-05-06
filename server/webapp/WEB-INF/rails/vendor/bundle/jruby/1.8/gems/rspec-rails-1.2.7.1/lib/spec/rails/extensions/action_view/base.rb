module ActionView #:nodoc:
  class Base #:nodoc:
    include Spec::Rails::Example::RenderObserver
    cattr_accessor :base_view_path

    def render_partial_with_base_view_path_handling(partial_path, local_assigns = nil, deprecated_local_assigns = nil) #:nodoc:
      if partial_path.is_a?(String)
        unless partial_path.include?("/")
          unless self.class.base_view_path.nil?
            partial_path = "#{self.class.base_view_path}/#{partial_path}"
          end
        end
      end
      begin
        render_partial_without_base_view_path_handling(partial_path, local_assigns, deprecated_local_assigns)
      rescue ArgumentError # edge rails > 2.1 changed render_partial to accept only one arg
        render_partial_without_base_view_path_handling(partial_path)
      end
    end
    alias_method_chain :render_partial, :base_view_path_handling

    def render_with_mock_proxy(options = {}, old_local_assigns = {}, &block)
      if render_proxy.__send__(:__mock_proxy).__send__(:find_matching_expectation, :render, options)
        render_proxy.render(options)
      else
        unless render_proxy.__send__(:__mock_proxy).__send__(:find_matching_method_stub, :render, options)
          render_without_mock_proxy(options, old_local_assigns, &block)
        end
      end
    end
    alias_method_chain :render, :mock_proxy
  end
end
