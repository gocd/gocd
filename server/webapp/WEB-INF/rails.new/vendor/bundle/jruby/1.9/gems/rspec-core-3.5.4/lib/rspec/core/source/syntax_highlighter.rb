module RSpec
  module Core
    class Source
      # @private
      # Provides terminal syntax highlighting of code snippets
      # when coderay is available.
      class SyntaxHighlighter
        def initialize(configuration)
          @configuration = configuration
        end

        def highlight(lines)
          implementation.highlight_syntax(lines)
        end

      private

        if RSpec::Support::OS.windows?
          # :nocov:
          def implementation
            WindowsImplementation
          end
          # :nocov:
        else
          def implementation
            return color_enabled_implementation if @configuration.color_enabled?
            NoSyntaxHighlightingImplementation
          end
        end

        def color_enabled_implementation
          @color_enabled_implementation ||= begin
            require 'coderay'
            CodeRayImplementation
          rescue LoadError
            NoSyntaxHighlightingImplementation
          end
        end

        # @private
        module CodeRayImplementation
          RESET_CODE = "\e[0m"

          def self.highlight_syntax(lines)
            highlighted = begin
              CodeRay.encode(lines.join("\n"), :ruby, :terminal)
            rescue Support::AllExceptionsExceptOnesWeMustNotRescue
              return lines
            end

            highlighted.split("\n").map do |line|
              line.sub(/\S/) { |char| char.insert(0, RESET_CODE) }
            end
          end
        end

        # @private
        module NoSyntaxHighlightingImplementation
          def self.highlight_syntax(lines)
            lines
          end
        end

        # @private
        # Not sure why, but our code above (and/or coderay itself) does not work
        # on Windows, so we disable the feature on Windows.
        WindowsImplementation = NoSyntaxHighlightingImplementation
      end
    end
  end
end
