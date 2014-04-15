module RSpec
  module Matchers
    module Pretty
      def split_words(sym)
        sym.to_s.gsub(/_/,' ')
      end

      def to_sentence(words)
        return "" unless words
        words = Array(words).map{|w| w.inspect}
        case words.length
          when 0
            ""
          when 1
            " #{words[0]}"
          when 2
            " #{words[0]} and #{words[1]}"
          else
            " #{words[0...-1].join(', ')}, and #{words[-1]}"
        end
      end

      def _pretty_print(array)
        result = ""
        array.each_with_index do |item, index|
          if index < (array.length - 2)
            result << "#{item.inspect}, "
          elsif index < (array.length - 1)
            result << "#{item.inspect} and "
          else
            result << "#{item.inspect}"
          end
        end
        result
      end

      def name_to_sentence
        split_words(name)
      end

      def expected_to_sentence
        to_sentence(@expected) if defined?(@expected)
      end

      def name
        defined?(@name) ? @name : underscore(self.class.name.split("::").last)
      end

      # Borrowed from ActiveSupport
      def underscore(camel_cased_word)
        word = camel_cased_word.to_s.dup
        word.gsub!(/([A-Z]+)([A-Z][a-z])/,'\1_\2')
        word.gsub!(/([a-z\d])([A-Z])/,'\1_\2')
        word.tr!("-", "_")
        word.downcase!
        word
      end
    end
  end
end
