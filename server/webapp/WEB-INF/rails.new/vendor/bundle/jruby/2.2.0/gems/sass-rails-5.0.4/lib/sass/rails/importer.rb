require 'active_support/deprecation/reporting'
require 'sass'
require 'sprockets/sass_importer'
require 'tilt'

module Sass
  module Rails
    class SassImporter < Sass::Importers::Filesystem
      module Globbing
        GLOB = /(\A|\/)(\*|\*\*\/\*)\z/

        def find_relative(name, base, options)
          if options[:sprockets] && m = name.match(GLOB)
            path = name.sub(m[0], "")
            base = File.expand_path(path, File.dirname(base))
            glob_imports(base, m[2], options)
          else
            super
          end
        end

        def find(name, options)
          # globs must be relative
          return if name =~ GLOB
          super
        end

        private
          def glob_imports(base, glob, options)
            contents = ""
            context = options[:sprockets][:context]
            each_globbed_file(base, glob, context) do |filename|
              next if filename == options[:filename]
              contents << "@import #{filename.inspect};\n"
            end
            return nil if contents == ""
            Sass::Engine.new(contents, options.merge(
              :filename => base,
              :importer => self,
              :syntax => :scss
            ))
          end

          def each_globbed_file(base, glob, context)
            raise ArgumentError unless glob == "*" || glob == "**/*"

            exts = extensions.keys.map { |ext| Regexp.escape(".#{ext}") }.join("|")
            sass_re = Regexp.compile("(#{exts})$")

            context.depend_on(base)

            Dir["#{base}/#{glob}"].sort.each do |path|
              if File.directory?(path)
                context.depend_on(path)
              elsif sass_re =~ path
                yield path
              end
            end
          end
      end

      module ERB
        def extensions
          {
            'css.erb'  => :scss_erb,
            'scss.erb' => :scss_erb,
            'sass.erb' => :sass_erb
          }.merge(super)
        end

        def erb_extensions
          {
            :scss_erb => :scss,
            :sass_erb => :sass
          }
        end

        def find_relative(*args)
          process_erb_engine(super)
        end

        def find(*args)
          process_erb_engine(super)
        end

        private
          def process_erb_engine(engine)
            if engine && engine.options[:sprockets] && syntax = erb_extensions[engine.options[:syntax]]
              template = Tilt::ERBTemplate.new(engine.options[:filename])
              contents = template.render(engine.options[:sprockets][:context], {})

              Sass::Engine.new(contents, engine.options.merge(:syntax => syntax))
            else
              engine
            end
          end
      end

      module Deprecated
        def extensions
          {
            'css.scss'     => :scss,
            'css.sass'     => :sass,
            'css.scss.erb' => :scss_erb,
            'css.sass.erb' => :sass_erb
          }.merge(super)
        end

        def find_relative(*args)
          deprecate_extra_css_extension(super)
        end

        def find(*args)
          deprecate_extra_css_extension(super)
        end

        private
          def deprecate_extra_css_extension(engine)
            if engine && filename = engine.options[:filename]
              if filename.end_with?('.css.scss')
                msg = "Extra .css in SCSS file is unnecessary. Rename #{filename} to #{filename.sub('.css.scss', '.scss')}."
              elsif filename.end_with?('.css.sass')
                msg = "Extra .css in SASS file is unnecessary. Rename #{filename} to #{filename.sub('.css.sass', '.sass')}."
              elsif filename.end_with?('.css.scss.erb')
                msg = "Extra .css in SCSS/ERB file is unnecessary. Rename #{filename} to #{filename.sub('.css.scss.erb', '.scss.erb')}."
              elsif filename.end_with?('.css.sass.erb')
                msg = "Extra .css in SASS/ERB file is unnecessary. Rename #{filename} to #{filename.sub('.css.sass.erb', '.sass.erb')}."
              end

              ActiveSupport::Deprecation.warn(msg) if msg
            end

            engine
          end
      end

      include Deprecated
      include ERB
      include Globbing

      # Allow .css files to be @import'd
      def extensions
        { 'css' => :scss }.merge(super)
      end
    end
  end
end
