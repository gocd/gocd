module YARD
  module CLI
    # Lists all markup types
    # @since 0.8.6
    class MarkupTypes < Command
      def description; 'Lists all available markup types and libraries' end

      # Runs the commandline utility, parsing arguments and displaying a
      # list of markup types
      #
      # @param [Array<String>] args the list of arguments.
      # @return [void]
      def run(*args)
        log.puts "Available markup types for `doc' command:"
        log.puts
        types = Templates::Helpers::MarkupHelper::MARKUP_PROVIDERS
        exts = Templates::Helpers::MarkupHelper::MARKUP_EXTENSIONS
        types.sort_by {|name, _| name.to_s }.each do |name, providers|
          log.puts "[#{name}]"
          libs = providers.map {|p| p[:lib] }.compact
          if libs.size > 0
            log.puts "  Providers: #{libs.join(" ")}"
          end
          if exts[name]
            log.puts "  Extensions: #{exts[name].map {|e| ".#{e}"}.join(" ")}"
          end

          log.puts
        end
      end
    end
  end
end
