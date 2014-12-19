# encoding: utf-8
require 'active_support/json'
require 'active_support/core_ext/string/access'
require 'active_support/core_ext/string/behavior'
require 'active_support/core_ext/module/delegation'

module ActiveSupport #:nodoc:
  module Multibyte #:nodoc:
    # Chars enables you to work transparently with UTF-8 encoding in the Ruby
    # String class without having extensive knowledge about the encoding. A
    # Chars object accepts a string upon initialization and proxies String
    # methods in an encoding safe manner. All the normal String methods are also
    # implemented on the proxy.
    #
    # String methods are proxied through the Chars object, and can be accessed
    # through the +mb_chars+ method. Methods which would normally return a
    # String object now return a Chars object so methods can be chained.
    #
    #   'The Perfect String  '.mb_chars.downcase.strip.normalize # => "the perfect string"
    #
    # Chars objects are perfectly interchangeable with String objects as long as
    # no explicit class checks are made. If certain methods do explicitly check
    # the class, call +to_s+ before you pass chars objects to them.
    #
    #   bad.explicit_checking_method 'T'.mb_chars.downcase.to_s
    #
    # The default Chars implementation assumes that the encoding of the string
    # is UTF-8, if you want to handle different encodings you can write your own
    # multibyte string handler and configure it through
    # ActiveSupport::Multibyte.proxy_class.
    #
    #   class CharsForUTF32
    #     def size
    #       @wrapped_string.size / 4
    #     end
    #
    #     def self.accepts?(string)
    #       string.length % 4 == 0
    #     end
    #   end
    #
    #   ActiveSupport::Multibyte.proxy_class = CharsForUTF32
    class Chars
      include Comparable
      attr_reader :wrapped_string
      alias to_s wrapped_string
      alias to_str wrapped_string

      delegate :<=>, :=~, :acts_like_string?, :to => :wrapped_string

      # Creates a new Chars instance by wrapping _string_.
      def initialize(string)
        @wrapped_string = string
        @wrapped_string.force_encoding(Encoding::UTF_8) unless @wrapped_string.frozen?
      end

      # Forward all undefined methods to the wrapped string.
      def method_missing(method, *args, &block)
        if method.to_s =~ /!$/
          result = @wrapped_string.__send__(method, *args, &block)
          self if result
        else
          result = @wrapped_string.__send__(method, *args, &block)
          result.kind_of?(String) ? chars(result) : result
        end
      end

      # Returns +true+ if _obj_ responds to the given method. Private methods
      # are included in the search only if the optional second parameter
      # evaluates to +true+.
      def respond_to_missing?(method, include_private)
        @wrapped_string.respond_to?(method, include_private)
      end

      # Returns +true+ when the proxy class can handle the string. Returns
      # +false+ otherwise.
      def self.consumes?(string)
        string.encoding == Encoding::UTF_8
      end

      # Works just like <tt>String#split</tt>, with the exception that the items
      # in the resulting list are Chars instances instead of String. This makes
      # chaining methods easier.
      #
      #   'Café périferôl'.mb_chars.split(/é/).map { |part| part.upcase.to_s } # => ["CAF", " P", "RIFERÔL"]
      def split(*args)
        @wrapped_string.split(*args).map { |i| self.class.new(i) }
      end

      # Works like like <tt>String#slice!</tt>, but returns an instance of
      # Chars, or nil if the string was not modified.
      def slice!(*args)
        chars(@wrapped_string.slice!(*args))
      end

      # Reverses all characters in the string.
      #
      #   'Café'.mb_chars.reverse.to_s # => 'éfaC'
      def reverse
        chars(Unicode.unpack_graphemes(@wrapped_string).reverse.flatten.pack('U*'))
      end

      # Limits the byte size of the string to a number of bytes without breaking
      # characters. Usable when the storage for a string is limited for some
      # reason.
      #
      #   'こんにちは'.mb_chars.limit(7).to_s # => "こん"
      def limit(limit)
        slice(0...translate_offset(limit))
      end

      # Converts characters in the string to uppercase.
      #
      #   'Laurent, où sont les tests ?'.mb_chars.upcase.to_s # => "LAURENT, OÙ SONT LES TESTS ?"
      def upcase
        chars Unicode.upcase(@wrapped_string)
      end

      # Converts characters in the string to lowercase.
      #
      #   'VĚDA A VÝZKUM'.mb_chars.downcase.to_s # => "věda a výzkum"
      def downcase
        chars Unicode.downcase(@wrapped_string)
      end

      # Converts characters in the string to the opposite case.
      #
      #    'El Cañón".mb_chars.swapcase.to_s # => "eL cAÑÓN"
      def swapcase
        chars Unicode.swapcase(@wrapped_string)
      end

      # Converts the first character to uppercase and the remainder to lowercase.
      #
      #  'über'.mb_chars.capitalize.to_s # => "Über"
      def capitalize
        (slice(0) || chars('')).upcase + (slice(1..-1) || chars('')).downcase
      end

      # Capitalizes the first letter of every word, when possible.
      #
      #   "ÉL QUE SE ENTERÓ".mb_chars.titleize    # => "Él Que Se Enteró"
      #   "日本語".mb_chars.titleize                 # => "日本語"
      def titleize
        chars(downcase.to_s.gsub(/\b('?\S)/u) { Unicode.upcase($1)})
      end
      alias_method :titlecase, :titleize

      # Returns the KC normalization of the string by default. NFKC is
      # considered the best normalization form for passing strings to databases
      # and validations.
      #
      # * <tt>form</tt> - The form you want to normalize in. Should be one of the following:
      #   <tt>:c</tt>, <tt>:kc</tt>, <tt>:d</tt>, or <tt>:kd</tt>. Default is
      #   ActiveSupport::Multibyte::Unicode.default_normalization_form
      def normalize(form = nil)
        chars(Unicode.normalize(@wrapped_string, form))
      end

      # Performs canonical decomposition on all the characters.
      #
      #   'é'.length                         # => 2
      #   'é'.mb_chars.decompose.to_s.length # => 3
      def decompose
        chars(Unicode.decompose(:canonical, @wrapped_string.codepoints.to_a).pack('U*'))
      end

      # Performs composition on all the characters.
      #
      #   'é'.length                       # => 3
      #   'é'.mb_chars.compose.to_s.length # => 2
      def compose
        chars(Unicode.compose(@wrapped_string.codepoints.to_a).pack('U*'))
      end

      # Returns the number of grapheme clusters in the string.
      #
      #   'क्षि'.mb_chars.length   # => 4
      #   'क्षि'.mb_chars.grapheme_length # => 3
      def grapheme_length
        Unicode.unpack_graphemes(@wrapped_string).length
      end

      # Replaces all ISO-8859-1 or CP1252 characters by their UTF-8 equivalent
      # resulting in a valid UTF-8 string.
      #
      # Passing +true+ will forcibly tidy all bytes, assuming that the string's
      # encoding is entirely CP1252 or ISO-8859-1.
      def tidy_bytes(force = false)
        chars(Unicode.tidy_bytes(@wrapped_string, force))
      end

      def as_json(options = nil) #:nodoc:
        to_s.as_json(options)
      end

      %w(capitalize downcase reverse tidy_bytes upcase).each do |method|
        define_method("#{method}!") do |*args|
          @wrapped_string = send(method, *args).to_s
          self
        end
      end

      protected

        def translate_offset(byte_offset) #:nodoc:
          return nil if byte_offset.nil?
          return 0   if @wrapped_string == ''

          begin
            @wrapped_string.byteslice(0...byte_offset).unpack('U*').length
          rescue ArgumentError
            byte_offset -= 1
            retry
          end
        end

        def chars(string) #:nodoc:
          self.class.new(string)
        end
    end
  end
end
