module ActionController
  module BasicImplicitRender # :nodoc:
    def send_action(method, *args)
      super.tap { default_render unless performed? }
    end

    def default_render(*args)
      head :no_content
    end
  end
end
