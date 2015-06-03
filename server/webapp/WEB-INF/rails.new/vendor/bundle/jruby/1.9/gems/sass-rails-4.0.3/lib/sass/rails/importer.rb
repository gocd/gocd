require 'sprockets/sass_importer'

module Sprockets
  class SassImporter < Sass::Importers::Filesystem
    GLOB = /\*|\[.+\]/

    attr_reader :context
    private :context

    def extensions
      {
        'css'          => :scss,
        'css.scss'     => :scss,
        'css.sass'     => :sass,
        'css.erb'      => :scss,
        'scss.erb'     => :scss,
        'sass.erb'     => :sass,
        'css.scss.erb' => :scss,
        'css.sass.erb' => :sass
      }.merge!(super)
    end

    def find_relative(name, base, options)
      if name =~ GLOB
        glob_imports(name, Pathname.new(base), options)
      else
        engine_from_path(name, File.dirname(base), options)
      end
    end

    def find(name, options)
      if name =~ GLOB
        nil # globs must be relative
      else
        engine_from_path(name, root, options)
      end
    end

    def each_globbed_file(glob, base_pathname, options)
      Dir["#{base_pathname}/#{glob}"].sort.each do |filename|
        next if filename == options[:filename]
        yield filename if File.directory?(filename) || context.asset_requirable?(filename)
      end
    end

    def glob_imports(glob, base_pathname, options)
      contents = ""
      each_globbed_file(glob, base_pathname.dirname, options) do |filename|
        if File.directory?(filename)
          depend_on(filename)
        elsif context.asset_requirable?(filename)
          depend_on(filename)
          contents << "@import #{Pathname.new(filename).relative_path_from(base_pathname.dirname).to_s.inspect};\n"
        end
      end
      return nil if contents.empty?
      Sass::Engine.new(contents, options.merge(
        :filename => base_pathname.to_s,
        :importer => self,
        :syntax => :scss
      ))
    end

    private

      def depend_on(filename)
        context.depend_on(filename)
        context.depend_on(globbed_file_parent(filename))
      end

      def globbed_file_parent(filename)
        if File.directory?(filename)
          File.expand_path('..', filename)
        else
          File.dirname(filename)
        end
      end

      def engine_from_path(name, dir, options)
        full_filename, syntax = Sass::Util.destructure(find_real_file(dir, name, options))
        return unless full_filename && File.readable?(full_filename)

        engine = Sass::Engine.new(evaluate(full_filename), options.merge(
          syntax: syntax,
          filename: full_filename,
          importer: self
        ))

        if engine && (filename = engine.options[:filename])
          @context.depend_on(filename)
        end

        engine
      end

      def evaluate(filename)
        attributes = context.environment.attributes_for(filename)
        processors = context.environment.preprocessors(attributes.content_type) +
          attributes.engines.reverse - [Sprockets::ScssTemplate, Sprockets::SassTemplate]

        context.evaluate(filename, processors: processors)
      end
  end
end
