require 'diff/lcs'
require 'diff/lcs/hunk'
require 'pp'

module RSpec
  module Expectations
    class Differ

      # This is snagged from diff/lcs/ldiff.rb (which is a commandline tool)
      def diff_as_string(input_data_new, input_data_old)
        encoding = pick_encoding input_data_new, input_data_old
        output = matching_encoding("", encoding)
        data_old = input_data_old.split(matching_encoding("\n", encoding)).map! { |e| e.chomp }
        data_new = input_data_new.split(matching_encoding("\n", encoding)).map! { |e| e.chomp }
        diffs = Diff::LCS.diff(data_old, data_new)
        return output if diffs.empty?
        oldhunk = hunk = nil
        file_length_difference = 0
        diffs.each do |piece|
          begin
            hunk = Diff::LCS::Hunk.new(
              data_old, data_new, piece, context_lines, file_length_difference
            )
            file_length_difference = hunk.file_length_difference
            next unless oldhunk
            # Hunks may overlap, which is why we need to be careful when our
            # diff includes lines of context. Otherwise, we might print
            # redundant lines.
            if (context_lines > 0) and hunk.overlaps?(oldhunk)
              if hunk.respond_to?(:merge)
                # diff-lcs 1.2.x
                hunk.merge(oldhunk)
              else
                # diff-lcs 1.1.3
                hunk.unshift(oldhunk)
              end
            else
              output << matching_encoding(oldhunk.diff(format).to_s, encoding)
            end
          ensure
            oldhunk = hunk
            output << matching_encoding("\n", encoding)
          end
        end
        #Handle the last remaining hunk
        output << matching_encoding(oldhunk.diff(format).to_s, encoding)
        output << matching_encoding("\n", encoding)
        color_diff output
      rescue Encoding::CompatibilityError
        if input_data_new.encoding != input_data_old.encoding
          "Could not produce a diff because the encoding of the actual string (#{input_data_old.encoding}) "+
          "differs from the encoding of the expected string (#{input_data_new.encoding})"
        else
          "Could not produce a diff because of the encoding of the string (#{input_data_old.encoding})"
        end
      end

      def diff_as_object(actual, expected)
        actual_as_string = object_to_string(actual)
        expected_as_string = object_to_string(expected)
        if diff = diff_as_string(actual_as_string, expected_as_string)
          color_diff diff
        end
      end

    protected

      def format
        :unified
      end

      def context_lines
        3
      end

      def color(text, color_code)
        "\e[#{color_code}m#{text}\e[0m"
      end

      def red(text)
        color(text, 31)
      end

      def green(text)
        color(text, 32)
      end

      def blue(text)
        color(text, 34)
      end

      def color_diff(diff)
        return diff unless RSpec::Matchers.configuration.color?

        diff.lines.map { |line|
          case line[0].chr
          when "+"
            green line
          when "-"
            red line
          when "@"
            line[1].chr == "@" ? blue(line) : line
          else
            line
          end
        }.join
      end

      def object_to_string(object)
        case object
        when Hash
          object.keys.sort_by { |k| k.to_s }.map do |key|
            pp_key   = PP.singleline_pp(key, "")
            pp_value = PP.singleline_pp(object[key], "")

            # on 1.9.3 PP seems to minimise to US-ASCII, ensure we're matching source encoding
            #
            # note, PP is used to ensure the ordering of the internal values of key/value e.g.
            # <# a: b: c:> not <# c: a: b:>
            encoding = pick_encoding pp_key, pp_value
            matching_encoding("#{pp_key} => #{pp_value}", encoding)
          end.join(",\n")
        when String
          object =~ /\n/ ? object : object.inspect
        else
          PP.pp(object,"")
        end
      end

    private

      if String.method_defined?(:encoding)
        def pick_encoding(source_a, source_b)
          Encoding.compatible?(source_a, source_b) || Encoding.default_external
        end

        def matching_encoding(string, encoding)
          string.encode encoding
        rescue Encoding::UndefinedConversionError
          string.encode(encoding, :undef => :replace)
        end
      else
        def pick_encoding(source_a, source_b)
        end

        def matching_encoding(string, encoding)
          string
        end
      end
    end

  end
end

