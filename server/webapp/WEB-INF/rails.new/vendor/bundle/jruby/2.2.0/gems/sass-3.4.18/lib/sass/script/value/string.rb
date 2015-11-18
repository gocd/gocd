# -*- coding: utf-8 -*-
module Sass::Script::Value
  # A SassScript object representing a CSS string *or* a CSS identifier.
  class String < Base
    # The Ruby value of the string.
    #
    # @return [String]
    attr_reader :value

    # Whether this is a CSS string or a CSS identifier.
    # The difference is that strings are written with double-quotes,
    # while identifiers aren't.
    #
    # @return [Symbol] `:string` or `:identifier`
    attr_reader :type

    def self.value(contents)
      contents.gsub("\\\n", "").gsub(/\\(?:([0-9a-fA-F]{1,6})\s?|(.))/) do
        next $2 if $2
        # Handle unicode escapes as per CSS Syntax Level 3 section 4.3.8.
        code_point = $1.to_i(16)
        if code_point == 0 || code_point > 0x10FFFF ||
            (code_point >= 0xD800 && code_point <= 0xDFFF)
          '�'
        else
          [code_point].pack("U")
        end
      end
    end

    def self.quote(contents, quote = nil)
      # Short-circuit if there are no characters that need quoting.
      unless contents =~ /[\n\\"']/
        quote ||= '"'
        return "#{quote}#{contents}#{quote}"
      end

      if quote.nil?
        if contents.include?('"')
          if contents.include?("'")
            quote = '"'
          else
            quote = "'"
          end
        else
          quote = '"'
        end
      end

      # Replace single backslashes with multiples.
      contents = contents.gsub("\\", "\\\\\\\\")

      if quote == '"'
        contents = contents.gsub('"', "\\\"")
      else
        contents = contents.gsub("'", "\\'")
      end

      contents = contents.gsub(/\n(?![a-fA-F0-9\s])/, "\\a").gsub("\n", "\\a ")
      "#{quote}#{contents}#{quote}"
    end

    # Creates a new string.
    #
    # @param value [String] See \{#value}
    # @param type [Symbol] See \{#type}
    def initialize(value, type = :identifier)
      super(value)
      @type = type
    end

    # @see Value#plus
    def plus(other)
      other_value = if other.is_a?(Sass::Script::Value::String)
                      other.value
                    else
                      other.to_s(:quote => :none)
                    end
      Sass::Script::Value::String.new(value + other_value, type)
    end

    # @see Value#to_s
    def to_s(opts = {})
      return @value.gsub(/\n\s*/, ' ') if opts[:quote] == :none || @type == :identifier
      Sass::Script::Value::String.quote(value, opts[:quote])
    end

    # @see Value#to_sass
    def to_sass(opts = {})
      to_s
    end

    def inspect
      String.quote(value)
    end
  end
end
