module RSpec
  module Core
    module Formatters
      module ConsoleCodes
        VT100_CODES =
          {
            :black   => 30,
            :red     => 31,
            :green   => 32,
            :yellow  => 33,
            :blue    => 34,
            :magenta => 35,
            :cyan    => 36,
            :white   => 37,
            :bold    => 1,
          }
        VT100_CODE_VALUES = VT100_CODES.invert

        module_function

        def console_code_for(code_or_symbol)
          if VT100_CODE_VALUES.has_key?(code_or_symbol)
            code_or_symbol
          else
            VT100_CODES.fetch(code_or_symbol) do
              console_code_for(:white)
            end
          end
        end

        def wrap(text, code_or_symbol)
          if RSpec.configuration.color_enabled?
            "\e[#{console_code_for(code_or_symbol)}m#{text}\e[0m"
          else
            text
          end
        end

      end
    end
  end
end
