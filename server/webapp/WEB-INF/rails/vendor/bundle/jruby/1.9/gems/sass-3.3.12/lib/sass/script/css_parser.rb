require 'sass/script'
require 'sass/script/css_lexer'

module Sass
  module Script
    # This is a subclass of {Parser} for use in parsing plain CSS properties.
    #
    # @see Sass::SCSS::CssParser
    class CssParser < Parser
      private

      # @private
      def lexer_class; CssLexer; end

      # We need a production that only does /,
      # since * and % aren't allowed in plain CSS
      production :div, :unary_plus, :div

      def string
        tok = try_tok(:string)
        return number unless tok
        unless @lexer.peek && @lexer.peek.type == :begin_interpolation
          return literal_node(tok.value, tok.source_range)
        end
      end

      # Short-circuit all the SassScript-only productions
      alias_method :interpolation, :space
      alias_method :or_expr, :div
      alias_method :unary_div, :ident
      alias_method :paren, :string
    end
  end
end
