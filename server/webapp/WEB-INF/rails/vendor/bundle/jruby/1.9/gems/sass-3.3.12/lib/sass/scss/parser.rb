require 'set'

module Sass
  module SCSS
    # The parser for SCSS.
    # It parses a string of code into a tree of {Sass::Tree::Node}s.
    class Parser
      # Expose for the SASS parser.
      attr_accessor :offset

      # @param str [String, StringScanner] The source document to parse.
      #   Note that `Parser` *won't* raise a nice error message if this isn't properly parsed;
      #   for that, you should use the higher-level {Sass::Engine} or {Sass::CSS}.
      # @param filename [String] The name of the file being parsed. Used for
      #   warnings and source maps.
      # @param importer [Sass::Importers::Base] The importer used to import the
      #   file being parsed. Used for source maps.
      # @param line [Fixnum] The 1-based line on which the source string appeared,
      #   if it's part of another document.
      # @param offset [Fixnum] The 1-based character (not byte) offset in the line on
      #   which the source string starts. Used for error reporting and sourcemap
      #   building.
      # @comment
      #   rubocop:disable ParameterLists
      def initialize(str, filename, importer, line = 1, offset = 1)
        # rubocop:enable ParameterLists
        @template = str
        @filename = filename
        @importer = importer
        @line = line
        @offset = offset
        @strs = []
      end

      # Parses an SCSS document.
      #
      # @return [Sass::Tree::RootNode] The root node of the document tree
      # @raise [Sass::SyntaxError] if there's a syntax error in the document
      def parse
        init_scanner!
        root = stylesheet
        expected("selector or at-rule") unless root && @scanner.eos?
        root
      end

      # Parses an identifier with interpolation.
      # Note that this won't assert that the identifier takes up the entire input string;
      # it's meant to be used with `StringScanner`s as part of other parsers.
      #
      # @return [Array<String, Sass::Script::Tree::Node>, nil]
      #   The interpolated identifier, or nil if none could be parsed
      def parse_interp_ident
        init_scanner!
        interp_ident
      end

      # Parses a media query list.
      #
      # @return [Sass::Media::QueryList] The parsed query list
      # @raise [Sass::SyntaxError] if there's a syntax error in the query list,
      #   or if it doesn't take up the entire input string.
      def parse_media_query_list
        init_scanner!
        ql = media_query_list
        expected("media query list") unless ql && @scanner.eos?
        ql
      end

      # Parses an at-root query.
      #
      # @return [Array<String, Sass::Script;:Tree::Node>] The interpolated query.
      # @raise [Sass::SyntaxError] if there's a syntax error in the query,
      #   or if it doesn't take up the entire input string.
      def parse_at_root_query
        init_scanner!
        query = at_root_query
        expected("@at-root query list") unless query && @scanner.eos?
        query
      end

      # Parses a supports query condition.
      #
      # @return [Sass::Supports::Condition] The parsed condition
      # @raise [Sass::SyntaxError] if there's a syntax error in the condition,
      #   or if it doesn't take up the entire input string.
      def parse_supports_condition
        init_scanner!
        condition = supports_condition
        expected("supports condition") unless condition && @scanner.eos?
        condition
      end

      private

      include Sass::SCSS::RX

      def source_position
        Sass::Source::Position.new(@line, @offset)
      end

      def range(start_pos, end_pos = source_position)
        Sass::Source::Range.new(start_pos, end_pos, @filename, @importer)
      end

      def init_scanner!
        @scanner =
          if @template.is_a?(StringScanner)
            @template
          else
            Sass::Util::MultibyteStringScanner.new(@template.gsub("\r", ""))
          end
      end

      def stylesheet
        node = node(Sass::Tree::RootNode.new(@scanner.string), source_position)
        block_contents(node, :stylesheet) {s(node)}
      end

      def s(node)
        while tok(S) || tok(CDC) || tok(CDO) || (c = tok(SINGLE_LINE_COMMENT)) || (c = tok(COMMENT))
          next unless c
          process_comment c, node
          c = nil
        end
        true
      end

      def ss
        nil while tok(S) || tok(SINGLE_LINE_COMMENT) || tok(COMMENT)
        true
      end

      def ss_comments(node)
        while tok(S) || (c = tok(SINGLE_LINE_COMMENT)) || (c = tok(COMMENT))
          next unless c
          process_comment c, node
          c = nil
        end

        true
      end

      def whitespace
        return unless tok(S) || tok(SINGLE_LINE_COMMENT) || tok(COMMENT)
        ss
      end

      def process_comment(text, node)
        silent = text =~ %r{\A//}
        loud = !silent && text =~ %r{\A/[/*]!}
        line = @line - text.count("\n")

        if silent
          value = [text.sub(%r{\A\s*//}, '/*').gsub(%r{^\s*//}, ' *') + ' */']
        else
          value = Sass::Engine.parse_interp(
            text, line, @scanner.pos - text.size, :filename => @filename)
          string_before_comment = @scanner.string[0...@scanner.pos - text.length]
          newline_before_comment = string_before_comment.rindex("\n")
          last_line_before_comment =
            if newline_before_comment
              string_before_comment[newline_before_comment + 1..-1]
            else
              string_before_comment
            end
          value.unshift(last_line_before_comment.gsub(/[^\s]/, ' '))
        end

        type = if silent
                 :silent
               elsif loud
                 :loud
               else
                 :normal
               end
        comment = Sass::Tree::CommentNode.new(value, type)
        comment.line = line
        node << comment
      end

      DIRECTIVES = Set[:mixin, :include, :function, :return, :debug, :warn, :for,
        :each, :while, :if, :else, :extend, :import, :media, :charset, :content,
        :_moz_document, :at_root]

      PREFIXED_DIRECTIVES = Set[:supports]

      def directive
        start_pos = source_position
        return unless tok(/@/)
        name = tok!(IDENT)
        ss

        if (dir = special_directive(name, start_pos))
          return dir
        elsif (dir = prefixed_directive(name, start_pos))
          return dir
        end

        # Most at-rules take expressions (e.g. @import),
        # but some (e.g. @page) take selector-like arguments.
        # Some take no arguments at all.
        val = expr || selector
        val = val ? ["@#{name} "] + Sass::Util.strip_string_array(val) : ["@#{name}"]
        directive_body(val, start_pos)
      end

      def directive_body(value, start_pos)
        node = Sass::Tree::DirectiveNode.new(value)

        if tok(/\{/)
          node.has_children = true
          block_contents(node, :directive)
          tok!(/\}/)
        end

        node(node, start_pos)
      end

      def special_directive(name, start_pos)
        sym = name.gsub('-', '_').to_sym
        DIRECTIVES.include?(sym) && send("#{sym}_directive", start_pos)
      end

      def prefixed_directive(name, start_pos)
        sym = name.gsub(/^-[a-z0-9]+-/i, '').gsub('-', '_').to_sym
        PREFIXED_DIRECTIVES.include?(sym) && send("#{sym}_directive", name, start_pos)
      end

      def mixin_directive(start_pos)
        name = tok! IDENT
        args, splat = sass_script(:parse_mixin_definition_arglist)
        ss
        block(node(Sass::Tree::MixinDefNode.new(name, args, splat), start_pos), :directive)
      end

      def include_directive(start_pos)
        name = tok! IDENT
        args, keywords, splat, kwarg_splat = sass_script(:parse_mixin_include_arglist)
        ss
        include_node = node(
          Sass::Tree::MixinNode.new(name, args, keywords, splat, kwarg_splat), start_pos)
        if tok?(/\{/)
          include_node.has_children = true
          block(include_node, :directive)
        else
          include_node
        end
      end

      def content_directive(start_pos)
        ss
        node(Sass::Tree::ContentNode.new, start_pos)
      end

      def function_directive(start_pos)
        name = tok! IDENT
        args, splat = sass_script(:parse_function_definition_arglist)
        ss
        block(node(Sass::Tree::FunctionNode.new(name, args, splat), start_pos), :function)
      end

      def return_directive(start_pos)
        node(Sass::Tree::ReturnNode.new(sass_script(:parse)), start_pos)
      end

      def debug_directive(start_pos)
        node(Sass::Tree::DebugNode.new(sass_script(:parse)), start_pos)
      end

      def warn_directive(start_pos)
        node(Sass::Tree::WarnNode.new(sass_script(:parse)), start_pos)
      end

      def for_directive(start_pos)
        tok!(/\$/)
        var = tok! IDENT
        ss

        tok!(/from/)
        from = sass_script(:parse_until, Set["to", "through"])
        ss

        @expected = '"to" or "through"'
        exclusive = (tok(/to/) || tok!(/through/)) == 'to'
        to = sass_script(:parse)
        ss

        block(node(Sass::Tree::ForNode.new(var, from, to, exclusive), start_pos), :directive)
      end

      def each_directive(start_pos)
        tok!(/\$/)
        vars = [tok!(IDENT)]
        ss
        while tok(/,/)
          ss
          tok!(/\$/)
          vars << tok!(IDENT)
          ss
        end

        tok!(/in/)
        list = sass_script(:parse)
        ss

        block(node(Sass::Tree::EachNode.new(vars, list), start_pos), :directive)
      end

      def while_directive(start_pos)
        expr = sass_script(:parse)
        ss
        block(node(Sass::Tree::WhileNode.new(expr), start_pos), :directive)
      end

      def if_directive(start_pos)
        expr = sass_script(:parse)
        ss
        node = block(node(Sass::Tree::IfNode.new(expr), start_pos), :directive)
        pos = @scanner.pos
        line = @line
        ss

        else_block(node) ||
          begin
            # Backtrack in case there are any comments we want to parse
            @scanner.pos = pos
            @line = line
            node
          end
      end

      def else_block(node)
        start_pos = source_position
        return unless tok(/@else/)
        ss
        else_node = block(
          node(Sass::Tree::IfNode.new((sass_script(:parse) if tok(/if/))), start_pos),
          :directive)
        node.add_else(else_node)
        pos = @scanner.pos
        line = @line
        ss

        else_block(node) ||
          begin
            # Backtrack in case there are any comments we want to parse
            @scanner.pos = pos
            @line = line
            node
          end
      end

      def else_directive(start_pos)
        err("Invalid CSS: @else must come after @if")
      end

      def extend_directive(start_pos)
        selector, selector_range = expr!(:selector_sequence)
        optional = tok(OPTIONAL)
        ss
        node(Sass::Tree::ExtendNode.new(selector, !!optional, selector_range), start_pos)
      end

      def import_directive(start_pos)
        values = []

        loop do
          values << expr!(:import_arg)
          break if use_css_import?
          break unless tok(/,/)
          ss
        end

        values
      end

      def import_arg
        start_pos = source_position
        return unless (str = tok(STRING)) || (uri = tok?(/url\(/i))
        if uri
          str = sass_script(:parse_string)
          ss
          media = media_query_list
          ss
          return node(Tree::CssImportNode.new(str, media.to_a), start_pos)
        end

        path = @scanner[1] || @scanner[2]
        ss

        media = media_query_list
        if path =~ %r{^(https?:)?//} || media || use_css_import?
          return node(Sass::Tree::CssImportNode.new(str, media.to_a), start_pos)
        end

        node(Sass::Tree::ImportNode.new(path.strip), start_pos)
      end

      def use_css_import?; false; end

      def media_directive(start_pos)
        block(node(Sass::Tree::MediaNode.new(expr!(:media_query_list).to_a), start_pos), :directive)
      end

      # http://www.w3.org/TR/css3-mediaqueries/#syntax
      def media_query_list
        query = media_query
        return unless query
        queries = [query]

        ss
        while tok(/,/)
          ss; queries << expr!(:media_query)
        end
        ss

        Sass::Media::QueryList.new(queries)
      end

      def media_query
        if (ident1 = interp_ident)
          ss
          ident2 = interp_ident
          ss
          if ident2 && ident2.length == 1 && ident2[0].is_a?(String) && ident2[0].downcase == 'and'
            query = Sass::Media::Query.new([], ident1, [])
          else
            if ident2
              query = Sass::Media::Query.new(ident1, ident2, [])
            else
              query = Sass::Media::Query.new([], ident1, [])
            end
            return query unless tok(/and/i)
            ss
          end
        end

        if query
          expr = expr!(:media_expr)
        else
          expr = media_expr
          return unless expr
        end
        query ||= Sass::Media::Query.new([], [], [])
        query.expressions << expr

        ss
        while tok(/and/i)
          ss; query.expressions << expr!(:media_expr)
        end

        query
      end

      def query_expr
        interp = interpolation
        return interp if interp
        return unless tok(/\(/)
        res = ['(']
        ss
        res << sass_script(:parse)

        if tok(/:/)
          res << ': '
          ss
          res << sass_script(:parse)
        end
        res << tok!(/\)/)
        ss
        res
      end

      # Aliases allow us to use different descriptions if the same
      # expression fails in different contexts.
      alias_method :media_expr, :query_expr
      alias_method :at_root_query, :query_expr

      def charset_directive(start_pos)
        tok! STRING
        name = @scanner[1] || @scanner[2]
        ss
        node(Sass::Tree::CharsetNode.new(name), start_pos)
      end

      # The document directive is specified in
      # http://www.w3.org/TR/css3-conditional/, but Gecko allows the
      # `url-prefix` and `domain` functions to omit quotation marks, contrary to
      # the standard.
      #
      # We could parse all document directives according to Mozilla's syntax,
      # but if someone's using e.g. @-webkit-document we don't want them to
      # think WebKit works sans quotes.
      def _moz_document_directive(start_pos)
        res = ["@-moz-document "]
        loop do
          res << str {ss} << expr!(:moz_document_function)
          if (c = tok(/,/))
            res << c
          else
            break
          end
        end
        directive_body(res.flatten, start_pos)
      end

      def moz_document_function
        val = interp_uri || _interp_string(:url_prefix) ||
          _interp_string(:domain) || function(!:allow_var) || interpolation
        return unless val
        ss
        val
      end

      def at_root_directive(start_pos)
        if tok?(/\(/) && (expr = at_root_query)
          return block(node(Sass::Tree::AtRootNode.new(expr), start_pos), :directive)
        end

        at_root_node = node(Sass::Tree::AtRootNode.new, start_pos)
        rule_node = ruleset
        return block(at_root_node, :stylesheet) unless rule_node
        at_root_node << rule_node
        at_root_node
      end

      def at_root_directive_list
        return unless (first = tok(IDENT))
        arr = [first]
        ss
        while (e = tok(IDENT))
          arr << e
          ss
        end
        arr
      end

      # http://www.w3.org/TR/css3-conditional/
      def supports_directive(name, start_pos)
        condition = expr!(:supports_condition)
        node = Sass::Tree::SupportsNode.new(name, condition)

        tok!(/\{/)
        node.has_children = true
        block_contents(node, :directive)
        tok!(/\}/)

        node(node, start_pos)
      end

      def supports_condition
        supports_negation || supports_operator || supports_interpolation
      end

      def supports_negation
        return unless tok(/not/i)
        ss
        Sass::Supports::Negation.new(expr!(:supports_condition_in_parens))
      end

      def supports_operator
        cond = supports_condition_in_parens
        return unless cond
        while (op = tok(/and|or/i))
          ss
          cond = Sass::Supports::Operator.new(
            cond, expr!(:supports_condition_in_parens), op)
        end
        cond
      end

      def supports_condition_in_parens
        interp = supports_interpolation
        return interp if interp
        return unless tok(/\(/); ss
        if (cond = supports_condition)
          tok!(/\)/); ss
          cond
        else
          name = sass_script(:parse)
          tok!(/:/); ss
          value = sass_script(:parse)
          tok!(/\)/); ss
          Sass::Supports::Declaration.new(name, value)
        end
      end

      def supports_declaration_condition
        return unless tok(/\(/); ss
        supports_declaration_body
      end

      def supports_interpolation
        interp = interpolation
        return unless interp
        ss
        Sass::Supports::Interpolation.new(interp)
      end

      def variable
        return unless tok(/\$/)
        start_pos = source_position
        name = tok!(IDENT)
        ss; tok!(/:/); ss

        expr = sass_script(:parse)
        while tok(/!/)
          flag_name = tok!(IDENT)
          if flag_name == 'default'
            guarded ||= true
          elsif flag_name == 'global'
            global ||= true
          else
            raise Sass::SyntaxError.new("Invalid flag \"!#{flag_name}\".", :line => @line)
          end
          ss
        end

        result = Sass::Tree::VariableNode.new(name, expr, guarded, global)
        node(result, start_pos)
      end

      def operator
        # Many of these operators (all except / and ,)
        # are disallowed by the CSS spec,
        # but they're included here for compatibility
        # with some proprietary MS properties
        str {ss if tok(/[\/,:.=]/)}
      end

      def ruleset
        start_pos = source_position
        rules, source_range = selector_sequence
        return unless rules
        block(node(
          Sass::Tree::RuleNode.new(rules.flatten.compact, source_range), start_pos), :ruleset)
      end

      def block(node, context)
        node.has_children = true
        tok!(/\{/)
        block_contents(node, context)
        tok!(/\}/)
        node
      end

      # A block may contain declarations and/or rulesets
      def block_contents(node, context)
        block_given? ? yield : ss_comments(node)
        node << (child = block_child(context))
        while tok(/;/) || has_children?(child)
          block_given? ? yield : ss_comments(node)
          node << (child = block_child(context))
        end
        node
      end

      def block_child(context)
        return variable || directive if context == :function
        return variable || directive || ruleset if context == :stylesheet
        variable || directive || declaration_or_ruleset
      end

      def has_children?(child_or_array)
        return false unless child_or_array
        return child_or_array.last.has_children if child_or_array.is_a?(Array)
        child_or_array.has_children
      end

      # This is a nasty hack, and the only place in the parser
      # that requires a large amount of backtracking.
      # The reason is that we can't figure out if certain strings
      # are declarations or rulesets with fixed finite lookahead.
      # For example, "foo:bar baz baz baz..." could be either a property
      # or a selector.
      #
      # To handle this, we simply check if it works as a property
      # (which is the most common case)
      # and, if it doesn't, try it as a ruleset.
      #
      # We could eke some more efficiency out of this
      # by handling some easy cases (first token isn't an identifier,
      # no colon after the identifier, whitespace after the colon),
      # but I'm not sure the gains would be worth the added complexity.
      def declaration_or_ruleset
        old_use_property_exception, @use_property_exception =
          @use_property_exception, false
        decl_err = catch_error do
          decl = declaration
          unless decl && decl.has_children
            # We want an exception if it's not there,
            # but we don't want to consume if it is
            tok!(/[;}]/) unless tok?(/[;}]/)
          end
          return decl
        end

        ruleset_err = catch_error {return ruleset}
        rethrow(@use_property_exception ? decl_err : ruleset_err)
      ensure
        @use_property_exception = old_use_property_exception
      end

      def selector_sequence
        start_pos = source_position
        if (sel = tok(STATIC_SELECTOR, true))
          return [sel], range(start_pos)
        end

        rules = []
        v = selector
        return unless v
        rules.concat v

        ws = ''
        while tok(/,/)
          ws << str {ss}
          if (v = selector)
            rules << ',' << ws
            rules.concat v
            ws = ''
          end
        end
        return rules, range(start_pos)
      end

      def selector
        sel = _selector
        return unless sel
        sel.to_a
      end

      def selector_comma_sequence
        sel = _selector
        return unless sel
        selectors = [sel]
        ws = ''
        while tok(/,/)
          ws << str {ss}
          if (sel = _selector)
            selectors << sel
            if ws.include?("\n")
              selectors[-1] = Selector::Sequence.new(["\n"] + selectors.last.members)
            end
            ws = ''
          end
        end
        Selector::CommaSequence.new(selectors)
      end

      def _selector
        # The combinator here allows the "> E" hack
        val = combinator || simple_selector_sequence
        return unless val
        nl = str {ss}.include?("\n")
        res = []
        res << val
        res << "\n" if nl

        while (val = combinator || simple_selector_sequence)
          res << val
          res << "\n" if str {ss}.include?("\n")
        end
        Selector::Sequence.new(res.compact)
      end

      def combinator
        tok(PLUS) || tok(GREATER) || tok(TILDE) || reference_combinator
      end

      def reference_combinator
        return unless tok(/\//)
        res = ['/']
        ns, name = expr!(:qualified_name)
        res << ns << '|' if ns
        res << name << tok!(/\//)
        res = res.flatten
        res = res.join '' if res.all? {|e| e.is_a?(String)}
        res
      end

      def simple_selector_sequence
        # Returning expr by default allows for stuff like
        # http://www.w3.org/TR/css3-animations/#keyframes-

        start_pos = source_position
        e = element_name || id_selector ||
          class_selector || placeholder_selector || attrib || pseudo ||
          parent_selector || interpolation_selector
        return expr(!:allow_var) unless e
        res = [e]

        # The tok(/\*/) allows the "E*" hack
        while (v = id_selector || class_selector || placeholder_selector ||
                   attrib || pseudo || interpolation_selector ||
                   (tok(/\*/) && Selector::Universal.new(nil)))
          res << v
        end

        pos = @scanner.pos
        line = @line
        if (sel = str? {simple_selector_sequence})
          @scanner.pos = pos
          @line = line
          begin
            # If we see "*E", don't force a throw because this could be the
            # "*prop: val" hack.
            expected('"{"') if res.length == 1 && res[0].is_a?(Selector::Universal)
            throw_error {expected('"{"')}
          rescue Sass::SyntaxError => e
            e.message << "\n\n\"#{sel}\" may only be used at the beginning of a compound selector."
            raise e
          end
        end

        Selector::SimpleSequence.new(res, tok(/!/), range(start_pos))
      end

      def parent_selector
        return unless tok(/&/)
        Selector::Parent.new(interp_ident(NAME) || [])
      end

      def class_selector
        return unless tok(/\./)
        @expected = "class name"
        Selector::Class.new(merge(expr!(:interp_ident)))
      end

      def id_selector
        return unless tok(/#(?!\{)/)
        @expected = "id name"
        Selector::Id.new(merge(expr!(:interp_name)))
      end

      def placeholder_selector
        return unless tok(/%/)
        @expected = "placeholder name"
        Selector::Placeholder.new(merge(expr!(:interp_ident)))
      end

      def element_name
        ns, name = Sass::Util.destructure(qualified_name(:allow_star_name))
        return unless ns || name

        if name == '*'
          Selector::Universal.new(merge(ns))
        else
          Selector::Element.new(merge(name), merge(ns))
        end
      end

      def qualified_name(allow_star_name = false)
        name = interp_ident || tok(/\*/) || (tok?(/\|/) && "")
        return unless name
        return nil, name unless tok(/\|/)

        return name, expr!(:interp_ident) unless allow_star_name
        @expected = "identifier or *"
        return name, interp_ident || tok!(/\*/)
      end

      def interpolation_selector
        if (script = interpolation)
          Selector::Interpolation.new(script)
        end
      end

      def attrib
        return unless tok(/\[/)
        ss
        ns, name = attrib_name!
        ss

        op = tok(/=/) ||
             tok(INCLUDES) ||
             tok(DASHMATCH) ||
             tok(PREFIXMATCH) ||
             tok(SUFFIXMATCH) ||
             tok(SUBSTRINGMATCH)
        if op
          @expected = "identifier or string"
          ss
          val = interp_ident || expr!(:interp_string)
          ss
        end
        flags = interp_ident || interp_string
        tok!(/\]/)

        Selector::Attribute.new(merge(name), merge(ns), op, merge(val), merge(flags))
      end

      def attrib_name!
        if (name_or_ns = interp_ident)
          # E, E|E
          if tok(/\|(?!=)/)
            ns = name_or_ns
            name = interp_ident
          else
            name = name_or_ns
          end
        else
          # *|E or |E
          ns = [tok(/\*/) || ""]
          tok!(/\|/)
          name = expr!(:interp_ident)
        end
        return ns, name
      end

      def pseudo
        s = tok(/::?/)
        return unless s
        @expected = "pseudoclass or pseudoelement"
        name = expr!(:interp_ident)
        if tok(/\(/)
          ss
          arg = expr!(:pseudo_arg)
          while tok(/,/)
            arg << ',' << str {ss}
            arg.concat expr!(:pseudo_arg)
          end
          tok!(/\)/)
        end
        Selector::Pseudo.new(s == ':' ? :class : :element, merge(name), merge(arg))
      end

      def pseudo_arg
        # In the CSS spec, every pseudo-class/element either takes a pseudo
        # expression or a selector comma sequence as an argument. However, we
        # don't want to have to know which takes which, so we handle both at
        # once.
        #
        # However, there are some ambiguities between the two. For instance, "n"
        # could start a pseudo expression like "n+1", or it could start a
        # selector like "n|m". In order to handle this, we must regrettably
        # backtrack.
        expr, sel = nil, nil
        pseudo_err = catch_error do
          expr = pseudo_expr
          next if tok?(/[,)]/)
          expr = nil
          expected '")"'
        end

        return expr if expr
        sel_err = catch_error {sel = selector}
        return sel if sel
        rethrow pseudo_err if pseudo_err
        rethrow sel_err if sel_err
        nil
      end

      def pseudo_expr_token
        tok(PLUS) || tok(/[-*]/) || tok(NUMBER) || interp_string || tok(IDENT) || interpolation
      end

      def pseudo_expr
        e = pseudo_expr_token
        return unless e
        res = [e, str {ss}]
        while (e = pseudo_expr_token)
          res << e << str {ss}
        end
        res
      end

      def declaration
        # This allows the "*prop: val", ":prop: val", and ".prop: val" hacks
        name_start_pos = source_position
        if (s = tok(/[:\*\.]|\#(?!\{)/))
          @use_property_exception = s !~ /[\.\#]/
          name = [s, str {ss}, *expr!(:interp_ident)]
        else
          name = interp_ident
          return unless name
          name = [name] if name.is_a?(String)
        end
        if (comment = tok(COMMENT))
          name << comment
        end
        name_end_pos = source_position
        ss

        tok!(/:/)
        value_start_pos, space, value = value!
        value_end_pos = source_position
        ss
        require_block = tok?(/\{/)

        node = node(Sass::Tree::PropNode.new(name.flatten.compact, value, :new),
                    name_start_pos, value_end_pos)
        node.name_source_range = range(name_start_pos, name_end_pos)
        node.value_source_range = range(value_start_pos, value_end_pos)

        return node unless require_block
        nested_properties! node, space
      end

      def value!
        space = !str {ss}.empty?
        value_start_pos = source_position
        @use_property_exception ||= space || !tok?(IDENT)

        if tok?(/\{/)
          str = Sass::Script::Tree::Literal.new(Sass::Script::Value::String.new(""))
          str.line = source_position.line
          str.source_range = range(source_position)
          return value_start_pos, true, str
        end

        start_pos = source_position
        # This is a bit of a dirty trick:
        # if the value is completely static,
        # we don't parse it at all, and instead return a plain old string
        # containing the value.
        # This results in a dramatic speed increase.
        if (val = tok(STATIC_VALUE, true))
          str = Sass::Script::Tree::Literal.new(Sass::Script::Value::String.new(val.strip))
          str.line = start_pos.line
          str.source_range = range(start_pos)
          return value_start_pos, space, str
        end
        return value_start_pos, space, sass_script(:parse)
      end

      def nested_properties!(node, space)
        err(<<MESSAGE) unless space
Invalid CSS: a space is required between a property and its definition
when it has other properties nested beneath it.
MESSAGE

        @use_property_exception = true
        @expected = 'expression (e.g. 1px, bold) or "{"'
        block(node, :property)
      end

      def expr(allow_var = true)
        t = term(allow_var)
        return unless t
        res = [t, str {ss}]

        while (o = operator) && (t = term(allow_var))
          res << o << t << str {ss}
        end

        res.flatten
      end

      def term(allow_var)
        e = tok(NUMBER) ||
            interp_uri ||
            function(allow_var) ||
            interp_string ||
            tok(UNICODERANGE) ||
            interp_ident ||
            tok(HEXCOLOR) ||
            (allow_var && var_expr)
        return e if e

        op = tok(/[+-]/)
        return unless op
        @expected = "number or function"
        [op,
         tok(NUMBER) || function(allow_var) || (allow_var && var_expr) || expr!(:interpolation)]
      end

      def function(allow_var)
        name = tok(FUNCTION)
        return unless name
        if name == "expression(" || name == "calc("
          str, _ = Sass::Shared.balance(@scanner, ?(, ?), 1)
          [name, str]
        else
          [name, str {ss}, expr(allow_var), tok!(/\)/)]
        end
      end

      def var_expr
        return unless tok(/\$/)
        line = @line
        var = Sass::Script::Tree::Variable.new(tok!(IDENT))
        var.line = line
        var
      end

      def interpolation
        return unless tok(INTERP_START)
        sass_script(:parse_interpolated)
      end

      def interp_string
        _interp_string(:double) || _interp_string(:single)
      end

      def interp_uri
        _interp_string(:uri)
      end

      def _interp_string(type)
        start = tok(Sass::Script::Lexer::STRING_REGULAR_EXPRESSIONS[type][false])
        return unless start
        res = [start]

        mid_re = Sass::Script::Lexer::STRING_REGULAR_EXPRESSIONS[type][true]
        # @scanner[2].empty? means we've started an interpolated section
        while @scanner[2] == '#{'
          @scanner.pos -= 2 # Don't consume the #{
          res.last.slice!(-2..-1)
          res << expr!(:interpolation) << tok(mid_re)
        end
        res
      end

      def interp_ident(start = IDENT)
        val = tok(start) || interpolation || tok(IDENT_HYPHEN_INTERP, true)
        return unless val
        res = [val]
        while (val = tok(NAME) || interpolation)
          res << val
        end
        res
      end

      def interp_ident_or_var
        id = interp_ident
        return id if id
        var = var_expr
        return [var] if var
      end

      def interp_name
        interp_ident NAME
      end

      def str
        @strs.push ""
        yield
        @strs.last
      ensure
        @strs.pop
      end

      def str?
        pos = @scanner.pos
        line = @line
        offset = @offset
        @strs.push ""
        throw_error {yield} && @strs.last
      rescue Sass::SyntaxError
        @scanner.pos = pos
        @line = line
        @offset = offset
        nil
      ensure
        @strs.pop
      end

      def node(node, start_pos, end_pos = source_position)
        node.line = start_pos.line
        node.source_range = range(start_pos, end_pos)
        node
      end

      @sass_script_parser = Class.new(Sass::Script::Parser)
      @sass_script_parser.send(:include, ScriptParser)

      class << self
        # @private
        attr_accessor :sass_script_parser
      end

      def sass_script(*args)
        parser = self.class.sass_script_parser.new(@scanner, @line, @offset,
                                                   :filename => @filename, :importer => @importer)
        result = parser.send(*args)
        unless @strs.empty?
          # Convert to CSS manually so that comments are ignored.
          src = result.to_sass
          @strs.each {|s| s << src}
        end
        @line = parser.line
        @offset = parser.offset
        result
      rescue Sass::SyntaxError => e
        throw(:_sass_parser_error, true) if @throw_error
        raise e
      end

      def merge(arr)
        arr && Sass::Util.merge_adjacent_strings([arr].flatten)
      end

      EXPR_NAMES = {
        :media_query => "media query (e.g. print, screen, print and screen)",
        :media_query_list => "media query (e.g. print, screen, print and screen)",
        :media_expr => "media expression (e.g. (min-device-width: 800px))",
        :at_root_query => "@at-root query (e.g. (without: media))",
        :at_root_directive_list => '* or identifier',
        :pseudo_arg => "expression (e.g. fr, 2n+1)",
        :interp_ident => "identifier",
        :interp_name => "identifier",
        :qualified_name => "identifier",
        :expr => "expression (e.g. 1px, bold)",
        :_selector => "selector",
        :selector_comma_sequence => "selector",
        :simple_selector_sequence => "selector",
        :import_arg => "file to import (string or url())",
        :moz_document_function => "matching function (e.g. url-prefix(), domain())",
        :supports_condition => "@supports condition (e.g. (display: flexbox))",
        :supports_condition_in_parens => "@supports condition (e.g. (display: flexbox))",
      }

      TOK_NAMES = Sass::Util.to_hash(Sass::SCSS::RX.constants.map do |c|
        [Sass::SCSS::RX.const_get(c), c.downcase]
      end).merge(
        IDENT => "identifier",
        /[;}]/ => '";"',
        /\b(without|with)\b/ => '"with" or "without"'
      )

      def tok?(rx)
        @scanner.match?(rx)
      end

      def expr!(name)
        e = send(name)
        return e if e
        expected(EXPR_NAMES[name] || name.to_s)
      end

      def tok!(rx)
        t = tok(rx)
        return t if t
        name = TOK_NAMES[rx]

        unless name
          # Display basic regexps as plain old strings
          source = rx.source.gsub(/\\\//, '/')
          string = rx.source.gsub(/\\(.)/, '\1')
          name = source == Regexp.escape(string) ? string.inspect : rx.inspect
        end

        expected(name)
      end

      def expected(name)
        throw(:_sass_parser_error, true) if @throw_error
        self.class.expected(@scanner, @expected || name, @line)
      end

      def err(msg)
        throw(:_sass_parser_error, true) if @throw_error
        raise Sass::SyntaxError.new(msg, :line => @line)
      end

      def throw_error
        old_throw_error, @throw_error = @throw_error, false
        yield
      ensure
        @throw_error = old_throw_error
      end

      def catch_error(&block)
        old_throw_error, @throw_error = @throw_error, true
        pos = @scanner.pos
        line = @line
        offset = @offset
        expected = @expected
        if catch(:_sass_parser_error) {yield; false}
          @scanner.pos = pos
          @line = line
          @offset = offset
          @expected = expected
          {:pos => pos, :line => line, :expected => @expected, :block => block}
        end
      ensure
        @throw_error = old_throw_error
      end

      def rethrow(err)
        if @throw_error
          throw :_sass_parser_error, err
        else
          @scanner = Sass::Util::MultibyteStringScanner.new(@scanner.string)
          @scanner.pos = err[:pos]
          @line = err[:line]
          @expected = err[:expected]
          err[:block].call
        end
      end

      # @private
      def self.expected(scanner, expected, line)
        pos = scanner.pos

        after = scanner.string[0...pos]
        # Get rid of whitespace between pos and the last token,
        # but only if there's a newline in there
        after.gsub!(/\s*\n\s*$/, '')
        # Also get rid of stuff before the last newline
        after.gsub!(/.*\n/, '')
        after = "..." + after[-15..-1] if after.size > 18

        was = scanner.rest.dup
        # Get rid of whitespace between pos and the next token,
        # but only if there's a newline in there
        was.gsub!(/^\s*\n\s*/, '')
        # Also get rid of stuff after the next newline
        was.gsub!(/\n.*/, '')
        was = was[0...15] + "..." if was.size > 18

        raise Sass::SyntaxError.new(
          "Invalid CSS after \"#{after}\": expected #{expected}, was \"#{was}\"",
          :line => line)
      end

      # Avoid allocating lots of new strings for `#tok`.
      # This is important because `#tok` is called all the time.
      NEWLINE = "\n"

      def tok(rx, last_group_lookahead = false)
        res = @scanner.scan(rx)
        if res
          # This fixes https://github.com/nex3/sass/issues/104, which affects
          # Ruby 1.8.7 and REE. This fix is to replace the ?= zero-width
          # positive lookahead operator in the Regexp (which matches without
          # consuming the matched group), with a match that does consume the
          # group, but then rewinds the scanner and removes the group from the
          # end of the matched string. This fix makes the assumption that the
          # matched group will always occur at the end of the match.
          if last_group_lookahead && @scanner[-1]
            @scanner.pos -= @scanner[-1].length
            res.slice!(-@scanner[-1].length..-1)
          end

          newline_count = res.count(NEWLINE)
          if newline_count > 0
            @line += newline_count
            @offset = res[res.rindex(NEWLINE)..-1].size
          else
            @offset += res.size
          end

          @expected = nil
          if !@strs.empty? && rx != COMMENT && rx != SINGLE_LINE_COMMENT
            @strs.each {|s| s << res}
          end
          res
        end
      end
    end
  end
end
