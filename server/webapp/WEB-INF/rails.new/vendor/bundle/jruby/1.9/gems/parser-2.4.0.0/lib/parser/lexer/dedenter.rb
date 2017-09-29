module Parser

  class Lexer::Dedenter
    def initialize(dedent_level)
      @dedent_level = dedent_level
      @at_line_begin = true
      @indent_level  = 0
    end

    def dedent(string)
      space_begin = space_end = offset = 0
      last_index  = string.length - 1
      string.chars.each_with_index do |char, index|
        if @at_line_begin
          if char == ?\n || @indent_level >= @dedent_level
            string.slice!(space_begin...space_end)
            offset += space_end - space_begin - 1
            @at_line_begin = false
            redo if char == ?\n
          end

          case char
          when ?\s
            @indent_level += 1
            space_end += 1
          when ?\t
            @indent_level += 8 - @indent_level % 8
            space_end += 1
          end
        elsif char == ?\n && index == last_index
          @at_line_begin = true
          @indent_level  = 0
          space_begin = space_end = index - offset + 1
        end
      end

      if @at_line_begin
        string.slice!(space_begin..space_end)
      end

      nil
    end

    def interrupt
      @at_line_begin = false
    end
  end

end
