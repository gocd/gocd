module Jasmine
  class Page
    def initialize(context)
      @context = context
    end

    def render
      ERB.new(::Jasmine.runner_template).result(@context.instance_eval { binding })
    end
  end
end
