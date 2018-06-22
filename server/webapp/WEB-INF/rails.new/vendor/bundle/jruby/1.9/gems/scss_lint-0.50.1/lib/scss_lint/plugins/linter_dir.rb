module SCSSLint
  class Plugins
    # Load ruby files from linter plugin directories.
    class LinterDir
      attr_reader :config

      def initialize(dir)
        @dir = dir
        @config = SCSSLint::Config.new({}) # Will always be empty
      end

      def load
        ruby_files.each { |file| require file }
        self
      end

    private

      def ruby_files
        Dir.glob(File.expand_path(File.join(@dir, '**', '*.rb')))
      end
    end
  end
end
