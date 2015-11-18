module Test
  module Unit
    class CodeSnippetFetcher
      def initialize
        @sources = {}
      end

      def fetch(file, line, options={})
        n_context_line = options[:n_context_line] || 3
        lines = source(file)
        return [] if lines.nil?
        min_line = [line - n_context_line, 1].max
        max_line = [line + n_context_line, lines.length].min
        window = min_line..max_line
        window.collect do |n|
          attributes = {:target_line? => (n == line)}
          [n, lines[n - 1].chomp, attributes]
        end
      end

      def source(file)
        @sources[file] ||= read_source(file)
      end

      private
      def read_source(file)
        return nil unless File.exist?(file)
        File.readlines(file)
      end
    end
  end
end
