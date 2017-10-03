require 'tilt/template'

module Tilt
  # RDoc template. See:
  # http://rdoc.rubyforge.org/
  #
  # It's suggested that your program `require 'rdoc/markup'` and
  # `require 'rdoc/markup/to_html'` at load time when using this template
  # engine in a threaded environment.
  class RDocTemplate < Template
    self.default_mime_type = 'text/html'

    def self.engine_initialized?
      defined? ::RDoc::Markup::ToHtml
    end

    def initialize_engine
      require_template_library 'rdoc'
      require_template_library 'rdoc/markup'
      require_template_library 'rdoc/markup/to_html'
    end

    def markup
      begin
        # RDoc 4.0
        require 'rdoc/options'
        RDoc::Markup::ToHtml.new(RDoc::Options.new, nil)
      rescue ArgumentError
        # RDoc < 4.0
        RDoc::Markup::ToHtml.new
      end
    end

    def prepare
      @engine = markup.convert(data)
      @output = nil
    end

    def evaluate(scope, locals, &block)
      @output ||= @engine.to_s
    end

    def allows_script?
      false
    end
  end
end
