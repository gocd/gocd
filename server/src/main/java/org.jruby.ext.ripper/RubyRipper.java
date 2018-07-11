/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.ripper;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.lexer.ByteListLexerSource;
import org.jruby.lexer.GetsLexerSource;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.LexingCommon;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;

public class RubyRipper extends RubyObject {
    static {
        if (!Constants.VERSION.equals("9.2.0.0")) {
            throw new IllegalStateException("This patch is only for version 9.2.0.0, as a workaround for https://github.com/jruby/jruby/issues/5209. Please remove this file for a newer version of jruby");
        }
    }
    public static void initRipper(Ruby runtime) {
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyRipper(runtime, klazz);
            }
        });

        ripper.defineConstant("SCANNER_EVENT_TABLE", createScannerEventTable(runtime, ripper));
        ripper.defineConstant("PARSER_EVENT_TABLE", createParserEventTable(runtime, ripper));
        defineLexStateConstants(runtime, ripper);

        ripper.defineAnnotatedMethods(RubyRipper.class);
    }

    private static void defineLexStateConstants(Ruby runtime, RubyClass ripper) {
        for (int i = 0; i < lexStateNames.length; i++) {
            ripper.defineConstant(lexStateNames[i], runtime.newFixnum((1 << i)));
        }
    }

    // Creates mapping table of token to arity for on_* method calls for the scanner support
    private static IRubyObject createScannerEventTable(Ruby runtime, RubyClass ripper) {
        RubyHash hash = new RubyHash(runtime);

        hash.fastASet(runtime.newSymbol("CHAR"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("__end__"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("backref"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("backtick"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("comma"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("comment"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("const"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("cvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embdoc"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embdoc_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embdoc_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embexpr_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embexpr_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("embvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("float"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("gvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("heredoc_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("heredoc_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("ident"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("ignored_nl"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("imaginary"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("int"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("ivar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("kw"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("label"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("label_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("lbrace"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("lbracket"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("lparen"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("nl"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("op"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("period"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("qsymbols_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("qwords_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("rational"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("rbrace"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("rbracket"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("regexp_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("regexp_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("rparen"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("semicolon"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("sp"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("symbeg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("symbols_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("tlambda"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("tlambeg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("tstring_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("tstring_content"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("tstring_end"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("words_beg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("words_sep"), runtime.newFixnum(1));

        return hash;
    }

    // Creates mapping table of token to arity for on_* method calls for the parser support
    private static IRubyObject createParserEventTable(Ruby runtime, RubyClass ripper) {
        RubyHash hash = new RubyHash(runtime);

        hash.fastASet(runtime.newSymbol("BEGIN"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("END"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("alias"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("alias_error"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("aref"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("aref_field"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("arg_ambiguous"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("arg_paren"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("args_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("args_add_block"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("args_add_star"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("args_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("array"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("assign"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("assign_error"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("assoc_new"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("assoc_splat"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("assoclist_from_args"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("bare_assoc_hash"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("begin"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("binary"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("block_var"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("block_var_add_block"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("block_var_add_star"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("blockarg"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("bodystmt"), runtime.newFixnum(4));
        hash.fastASet(runtime.newSymbol("brace_block"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("break"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("call"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("case"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("class"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("class_name_error"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("command"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("command_call"), runtime.newFixnum(4));
        hash.fastASet(runtime.newSymbol("const_path_field"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("const_path_ref"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("const_ref"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("def"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("defined"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("defs"), runtime.newFixnum(5));
        hash.fastASet(runtime.newSymbol("do_block"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("dot2"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("dot3"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("dyna_symbol"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("else"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("elsif"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("ensure"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("excessed_comma"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("fcall"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("field"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("for"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("hash"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("heredoc_dedent"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("if"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("if_mod"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("ifop"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("kwrest_param"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("lambda"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("magic_comment"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("massign"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("method_add_arg"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("method_add_block"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mlhs_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mlhs_add_post"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mlhs_add_star"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mlhs_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("mlhs_paren"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("module"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mrhs_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mrhs_add_star"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("mrhs_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("mrhs_new_from_args"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("next"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("opassign"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("operator_ambiguous"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("param_error"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("params"), runtime.newFixnum(7));
        hash.fastASet(runtime.newSymbol("paren"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("parse_error"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("program"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("qsymbols_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("qsymbols_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("qwords_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("qwords_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("redo"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("regexp_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("regexp_literal"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("regexp_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("rescue"), runtime.newFixnum(4));
        hash.fastASet(runtime.newSymbol("rescue_mod"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("rest_param"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("retry"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("return"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("return0"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("sclass"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("stmts_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("stmts_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("string_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("string_concat"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("string_content"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("string_dvar"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("string_embexpr"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("string_literal"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("super"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("symbol"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("symbol_literal"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("symbols_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("symbols_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("top_const_field"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("top_const_ref"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("unary"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("undef"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("unless"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("unless_mod"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("until"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("until_mod"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("var_alias"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("var_field"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("var_ref"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("vcall"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("void_stmt"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("when"), runtime.newFixnum(3));
        hash.fastASet(runtime.newSymbol("while"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("while_mod"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("word_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("word_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("words_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("words_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("xstring_add"), runtime.newFixnum(2));
        hash.fastASet(runtime.newSymbol("xstring_literal"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("xstring_new"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("yield"), runtime.newFixnum(1));
        hash.fastASet(runtime.newSymbol("yield0"), runtime.newFixnum(0));
        hash.fastASet(runtime.newSymbol("zsuper"), runtime.newFixnum(0));

        return hash;
    }

    private RubyRipper(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src) {
        return initialize(context, src, null, null);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject file) {
        return initialize(context, src, file, null);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject src,IRubyObject file, IRubyObject line) {
        filename = filenameAsString(context, file).dup();
        parser = new RipperParser(context, this, source(context, src, filename.asJavaString(), lineAsInt(context, line)));

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject column(ThreadContext context) {
        if (!parser.hasStarted()) throw context.runtime.newArgumentError("method called for uninitialized object");

        if (!parseStarted) return context.nil;

        return context.runtime.newFixnum(parser.getColumn());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return context.runtime.getEncodingService().getEncoding(parser.encoding());
    }

    @JRubyMethod(name = "end_seen?")
    public IRubyObject end_seen_p(ThreadContext context) {
        return context.runtime.newBoolean(parser.isEndSeen());
    }

    @JRubyMethod(name = "error?")
    public IRubyObject error_p(ThreadContext context) {
        return context.runtime.newBoolean(parser.isError());
    }
    @JRubyMethod
    public IRubyObject filename(ThreadContext context) {
        return filename;
    }

    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        if (!parser.hasStarted()) throw context.runtime.newArgumentError("method called for uninitialized object");

        if (!parseStarted) return context.nil;

        return context.runtime.newFixnum(parser.getLineno());
    }

    @JRubyMethod
    public IRubyObject state(ThreadContext context) {
        int state = parser.getState();

        return state == 0 ? context.nil : context.runtime.newFixnum(parser.getState());
    }

    @JRubyMethod
    public IRubyObject parse(ThreadContext context) {
        parseStarted = true;

        try {
            return parser.parse(true);
        } catch (IOException e) {
            System.out.println("ERRROR: " + e);
        } catch (SyntaxException e) {

        }
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject yydebug(ThreadContext context) {
        return context.runtime.newBoolean(parser.getYYDebug());
    }

    @JRubyMethod(name = "yydebug=")
    public IRubyObject yydebug_set(ThreadContext context, IRubyObject arg) {
        parser.setYYDebug(arg.isTrue());
        return arg;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject dedent_string(ThreadContext context, IRubyObject self, IRubyObject _input, IRubyObject _width) {
        RubyString input = _input.convertToString();
        int wid = _width.convertToInteger().getIntValue();
        input.modify19();
        int col = LexingCommon.dedent_string(input.getByteList(), wid);
        return context.runtime.newFixnum(col);
    }

    @JRubyMethod
    public IRubyObject dedent_string(ThreadContext context, IRubyObject _input, IRubyObject _width) {
        return dedent_string(context, this, _input, _width);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject lex_state_name(ThreadContext context, IRubyObject self, IRubyObject lexStateParam) {
        int lexState = lexStateParam.convertToInteger().getIntValue();

        boolean needsSeparator = false;
        RubyString name = null;
        for (int i = 0; i < lexStateNames.length; i++) {
            if ((lexState & (1<<i)) != 0) {
                if (!needsSeparator) {
                    name = context.runtime.newString(lexStateNames[i]);
                    needsSeparator = true;
                } else {
                    name.cat('|');
                    name.catString(lexStateNames[i]);
                }
            }
        }

        if (name == null) name = context.runtime.newString("EXPR_NONE");

        return name;
    }

    private LexerSource source(ThreadContext context, IRubyObject src, String filename, int lineno) {
        // FIXME: respond_to? returns private methods
        DynamicMethod method = src.getMetaClass().searchMethod("gets");

        if (method.isUndefined() || method.getVisibility() == Visibility.PRIVATE) {
            return new ByteListLexerSource(filename, lineno, src.convertToString().getByteList(), null);
        }

        return new GetsLexerSource(filename, lineno, src, null);
    }

    private IRubyObject filenameAsString(ThreadContext context, IRubyObject filename) {
        if (filename == null || filename.isNil()) return context.runtime.newString("(ripper)");

        return filename.convertToString();
    }

    private int lineAsInt(ThreadContext context, IRubyObject line) {
        if (line == null || line.isNil()) return 0;

        return RubyNumeric.fix2int(line.convertToInteger()) - 1;
    }

    private RipperParserBase parser = null;
    private IRubyObject filename = null;
    private boolean parseStarted = false;

    // FIXME: Consider moving this to LexingCommon but it is very specific to ripper (perhaps I can make it more useful wit debuggin?).
    // These are ordered in same order as LexerCommon.  Any changes to lex_state should update this.
    private static String[] lexStateNames = new String[] {
            "EXPR_BEG", "EXPR_END", "EXPR_ENDARG", "EXPR_ENDFN", "EXPR_ARG", "EXPR_CMDARG",
            "EXPR_MID", "EXPR_FNAME", "EXPR_DOT", "EXPR_CLASS", "EXPR_LABEL", "EXPR_LABELED", "EXPR_FITEM"
    };
}
