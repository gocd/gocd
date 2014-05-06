require 'tilt/template'

# AsciiDoc see: http://asciidoc.org/
module Tilt
  # Asciidoctor implementation for AsciiDoc see:
  # http://asciidoctor.github.com/
  #
  # Asciidoctor is an open source, pure-Ruby processor for
  # converting AsciiDoc documents or strings into HTML 5,
  # DocBook 4.5 and other formats.
  class AsciidoctorTemplate < Template
    self.default_mime_type = 'text/html'

    def self.engine_initialized?
      defined? ::Asciidoctor::Document
    end

    def initialize_engine
      require_template_library 'asciidoctor'
    end

    def prepare
      options[:header_footer] = false if options[:header_footer].nil?
    end

    def evaluate(scope, locals, &block)
      @output ||= Asciidoctor.render(data, options, &block)
    end

    def allows_script?
      false
    end
  end
end
