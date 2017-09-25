require 'tilt/template'
require 'redcarpet'

module Tilt
  # Compatibility mode for Redcarpet 1.x
  class Redcarpet1Template < Template
    self.default_mime_type = 'text/html'

    ALIAS = {
      :escape_html => :filter_html,
      :smartypants => :smart
    }

    FLAGS = [:smart, :filter_html, :smartypants, :escape_html]

    def flags
      FLAGS.select { |flag| options[flag] }.map { |flag| ALIAS[flag] || flag }
    end

    def prepare
      @engine = RedcarpetCompat.new(data, *flags)
      @output = nil
    end

    def evaluate(scope, locals, &block)
      @output ||= @engine.to_html
    end

    def allows_script?
      false
    end
  end

  # Future proof mode for Redcarpet 2.x (not yet released)
  class Redcarpet2Template < Template
    self.default_mime_type = 'text/html'

    def self.engine_initialized?
      defined? ::Redcarpet::Render and defined? ::Redcarpet::Markdown
    end

    def generate_renderer
      renderer = options.delete(:renderer) || ::Redcarpet::Render::HTML
      return renderer unless options.delete(:smartypants)
      return renderer if renderer.is_a?(Class) && renderer <= ::Redcarpet::Render::SmartyPants

      if renderer == ::Redcarpet::Render::XHTML
        ::Redcarpet::Render::SmartyHTML.new(:xhtml => true)
      elsif renderer == ::Redcarpet::Render::HTML
        ::Redcarpet::Render::SmartyHTML
      elsif renderer.is_a? Class
        Class.new(renderer) { include ::Redcarpet::Render::SmartyPants }
      else
        renderer.extend ::Redcarpet::Render::SmartyPants
      end
    end

    def prepare
      # try to support the same aliases
      Redcarpet1Template::ALIAS.each do |opt, aka|
        next if options.key? opt or not options.key? aka
        options[opt] = options.delete(aka)
      end

      # only raise an exception if someone is trying to enable :escape_html
      options.delete(:escape_html) unless options[:escape_html]

      @engine = ::Redcarpet::Markdown.new(generate_renderer, options)
      @output = nil
    end

    def evaluate(scope, locals, &block)
      @output ||= @engine.render(data)
    end

    def allows_script?
      false
    end
  end

  # Upskirt Markdown implementation. See:
  # https://github.com/tanoku/redcarpet
  #
  # Supports both Redcarpet 1.x and 2.x
  class RedcarpetTemplate < Template
    Redcarpet1 = Redcarpet1Template
    Redcarpet2 = Redcarpet2Template

    def prepare
      klass = Redcarpet2.engine_initialized? ? Redcarpet2 : Redcarpet1
      @engine = klass.new(file, line, options) { data }
    end

    def evaluate(scope, locals, &block)
      @engine.evaluate(scope, locals, &block)
    end

    def allows_script?
      false
    end
  end

end

