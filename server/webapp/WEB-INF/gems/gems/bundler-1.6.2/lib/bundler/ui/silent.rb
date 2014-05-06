module Bundler
  module UI
    class Silent
      def info(message, newline = nil)
      end

      def confirm(message, newline = nil)
      end

      def warn(message, newline = nil)
      end

      def error(message, newline = nil)
      end

      def debug(message, newline = nil)
      end

      def debug?
        false
      end

      def quiet?
        false
      end

      def ask(message)
      end

      def level=(name)
      end

      def level(name = nil)
      end

      def trace(message, newline = nil)
      end

      def silence
        yield
      end
    end
  end
end
