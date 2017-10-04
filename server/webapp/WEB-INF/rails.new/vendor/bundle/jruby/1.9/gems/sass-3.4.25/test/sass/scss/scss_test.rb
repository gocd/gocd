#!/usr/bin/env ruby
# -*- coding: utf-8 -*-
require File.dirname(__FILE__) + '/test_helper'

class ScssTest < MiniTest::Test
  include ScssTestHelper

  ## One-Line Comments

  def test_one_line_comments
    assert_equal <<CSS, render(<<SCSS)
.foo {
  baz: bang; }
CSS
.foo {// bar: baz;}
  baz: bang; //}
}
SCSS
    assert_equal <<CSS, render(<<SCSS)
.foo bar[val="//"] {
  baz: bang; }
CSS
.foo bar[val="//"] {
  baz: bang; //}
}
SCSS
  end

  ## Script

  def test_variables
    assert_equal <<CSS, render(<<SCSS)
blat {
  a: foo; }
CSS
$var: foo;

blat {a: $var}
SCSS

    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 2;
  b: 6; }
CSS
foo {
  $var: 2;
  $another-var: 4;
  a: $var;
  b: $var + $another-var;}
SCSS
  end

  def test_unicode_variables
    assert_equal <<CSS, render(<<SCSS)
blat {
  a: foo; }
CSS
$vär: foo;

blat {a: $vär}
SCSS
  end

  def test_guard_assign
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 1; }
CSS
$var: 1;
$var: 2 !default;

foo {a: $var}
SCSS

    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 2; }
CSS
$var: 2 !default;

foo {a: $var}
SCSS
  end

  def test_sass_script
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 3;
  b: -1;
  c: foobar;
  d: 12px; }
CSS
foo {
  a: 1 + 2;
  b: 1 - 2;
  c: foo + bar;
  d: floor(12.3px); }
SCSS
  end

  def test_debug_directive
    assert_warning "test_debug_directive_inline.scss:2 DEBUG: hello world!" do
      assert_equal <<CSS, render(<<SCSS)
foo {
  a: b; }

bar {
  c: d; }
CSS
foo {a: b}
@debug "hello world!";
bar {c: d}
SCSS
    end
  end

  def test_error_directive
    assert_raise_message(Sass::SyntaxError, "hello world!") {render(<<SCSS)}
foo {a: b}
@error "hello world!";
bar {c: d}
SCSS
  end

  def test_warn_directive
    expected_warning = <<EXPECTATION
WARNING: this is a warning
         on line 2 of test_warn_directive_inline.scss

WARNING: this is a mixin
         on line 1 of test_warn_directive_inline.scss, in `foo'
         from line 3 of test_warn_directive_inline.scss
EXPECTATION
    assert_warning expected_warning do
      assert_equal <<CSS, render(<<SCSS)
bar {
  c: d; }
CSS
@mixin foo { @warn "this is a mixin";}
@warn "this is a warning";
bar {c: d; @include foo;}
SCSS
    end
  end

  def test_for_directive
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  a: 2;
  a: 3;
  a: 4; }
CSS
.foo {
  @for $var from 1 to 5 {a: $var;}
}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  a: 2;
  a: 3;
  a: 4;
  a: 5; }
CSS
.foo {
  @for $var from 1 through 5 {a: $var;}
}
SCSS
  end

  def test_for_directive_with_same_start_and_end
    assert_equal <<CSS, render(<<SCSS)
CSS
.foo {
  @for $var from 1 to 1 {a: $var;}
}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1; }
CSS
.foo {
  @for $var from 1 through 1 {a: $var;}
}
SCSS
  end

  def test_decrementing_estfor_directive
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 5;
  a: 4;
  a: 3;
  a: 2;
  a: 1; }
CSS
.foo {
  @for $var from 5 through 1 {a: $var;}
}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 5;
  a: 4;
  a: 3;
  a: 2; }
CSS
.foo {
  @for $var from 5 to 1 {a: $var;}
}
SCSS
  end

  def test_if_directive
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b; }
CSS
@if "foo" == "foo" {foo {a: b}}
@if "foo" != "foo" {bar {a: b}}
SCSS

    assert_equal <<CSS, render(<<SCSS)
bar {
  a: b; }
CSS
@if "foo" != "foo" {foo {a: b}}
@else if "foo" == "foo" {bar {a: b}}
@else if true {baz {a: b}}
SCSS

    assert_equal <<CSS, render(<<SCSS)
bar {
  a: b; }
CSS
@if "foo" != "foo" {foo {a: b}}
@else {bar {a: b}}
SCSS
  end

  def test_comment_after_if_directive
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b;
  /* This is a comment */
  c: d; }
CSS
foo {
  @if true {a: b}
  /* This is a comment */
  c: d }
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b;
  /* This is a comment */
  c: d; }
CSS
foo {
  @if true {a: b}
  @else {x: y}
  /* This is a comment */
  c: d }
SCSS
  end

  def test_while_directive
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  a: 2;
  a: 3;
  a: 4; }
CSS
$i: 1;

.foo {
  @while $i != 5 {
    a: $i;
    $i: $i + 1 !global;
  }
}
SCSS
  end

  def test_each_directive
    assert_equal <<CSS, render(<<SCSS)
a {
  b: 1px;
  b: 2px;
  b: 3px;
  b: 4px; }

c {
  d: foo;
  d: bar;
  d: baz;
  d: bang; }
CSS
a {
  @each $number in 1px 2px 3px 4px {
    b: $number;
  }
}
c {
  @each $str in foo, bar, baz, bang {
    d: $str;
  }
}
SCSS
  end

  def test_destructuring_each_directive
    assert_equal <<CSS, render(<<SCSS)
a {
  foo: 1px;
  bar: 2px;
  baz: 3px; }

c {
  foo: "Value is bar";
  bar: "Value is baz";
  bang: "Value is "; }
CSS
a {
  @each $name, $number in (foo: 1px, bar: 2px, baz: 3px) {
    \#{$name}: $number;
  }
}
c {
  @each $key, $value in (foo bar) (bar, baz) bang {
    \#{$key}: "Value is \#{$value}";
  }
}
SCSS
  end

  def test_css_import_directive
    assert_equal "@import url(foo.css);\n", render('@import "foo.css";')
    assert_equal "@import url(foo.css);\n", render("@import 'foo.css';")
    assert_equal "@import url(\"foo.css\");\n", render('@import url("foo.css");')
    assert_equal "@import url(\"foo.css\");\n", render('@import url("foo.css");')
    assert_equal "@import url(foo.css);\n", render('@import url(foo.css);')
  end

  def test_css_string_import_directive_with_media
    assert_parses '@import "foo.css" screen;'
    assert_parses '@import "foo.css" screen, print;'
    assert_parses '@import "foo.css" screen, print and (foo: 0);'
    assert_parses '@import "foo.css" screen, only print, screen and (foo: 0);'
  end

  def test_css_url_import_directive_with_media
    assert_parses '@import url("foo.css") screen;'
    assert_parses '@import url("foo.css") screen, print;'
    assert_parses '@import url("foo.css") screen, print and (foo: 0);'
    assert_parses '@import url("foo.css") screen, only print, screen and (foo: 0);'
  end

  def test_media_import
    assert_equal("@import \"./fonts.sass\" all;\n", render("@import \"./fonts.sass\" all;"))
  end

  def test_dynamic_media_import
    assert_equal(<<CSS, render(<<SCSS))
@import "foo" print and (-webkit-min-device-pixel-ratio-foo: 25);
CSS
$media: print;
$key: -webkit-min-device-pixel-ratio;
$value: 20;
@import "foo" \#{$media} and ($key + "-foo": $value + 5);
SCSS
  end

  def test_http_import
    assert_equal("@import \"http://fonts.googleapis.com/css?family=Droid+Sans\";\n",
      render("@import \"http://fonts.googleapis.com/css?family=Droid+Sans\";"))
  end

  def test_protocol_relative_import
    assert_equal("@import \"//fonts.googleapis.com/css?family=Droid+Sans\";\n",
      render("@import \"//fonts.googleapis.com/css?family=Droid+Sans\";"))
  end

  def test_import_with_interpolation
    assert_equal <<CSS, render(<<SCSS)
@import url("http://fonts.googleapis.com/css?family=Droid+Sans");
CSS
$family: unquote("Droid+Sans");
@import url("http://fonts.googleapis.com/css?family=\#{$family}");
SCSS
  end

  def test_url_import
    assert_equal("@import url(fonts.sass);\n", render("@import url(fonts.sass);"))
  end

  def test_css_import_doesnt_move_through_comments
    assert_equal <<CSS, render(<<SCSS)
/* Comment 1 */
@import url("foo.css");
/* Comment 2 */
@import url("bar.css");
CSS
/* Comment 1 */
@import url("foo.css");

/* Comment 2 */
@import url("bar.css");
SCSS
  end

  def test_css_import_movement_stops_at_comments
    assert_equal <<CSS, render(<<SCSS)
/* Comment 1 */
@import url("foo.css");
/* Comment 2 */
@import url("bar.css");
.foo {
  a: b; }

/* Comment 3 */
CSS
/* Comment 1 */
@import url("foo.css");

/* Comment 2 */

.foo {a: b}

/* Comment 3 */
@import url("bar.css");
SCSS
  end

  def test_block_comment_in_script
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 1bar; }
CSS
foo {a: 1 + /* flang */ bar}
SCSS
  end

  def test_line_comment_in_script
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 1blang; }
CSS
foo {a: 1 + // flang }
  blang }
SCSS
  end

  def test_static_hyphenated_unit
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 0px; }
CSS
foo {a: 10px-10px }
SCSS
  end

  ## Nested Rules

  def test_nested_rules
    assert_equal <<CSS, render(<<SCSS)
foo bar {
  a: b; }
CSS
foo {bar {a: b}}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo bar {
  a: b; }
foo baz {
  b: c; }
CSS
foo {
  bar {a: b}
  baz {b: c}}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo bar baz {
  a: b; }
foo bang bip {
  a: b; }
CSS
foo {
  bar {baz {a: b}}
  bang {bip {a: b}}}
SCSS
  end

  def test_nested_rules_with_declarations
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b; }
  foo bar {
    c: d; }
CSS
foo {
  a: b;
  bar {c: d}}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b; }
  foo bar {
    c: d; }
CSS
foo {
  bar {c: d}
  a: b}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo {
  ump: nump;
  grump: clump; }
  foo bar {
    blat: bang;
    habit: rabbit; }
    foo bar baz {
      a: b; }
    foo bar bip {
      c: d; }
  foo bibble bap {
    e: f; }
CSS
foo {
  ump: nump;
  grump: clump;
  bar {
    blat: bang;
    habit: rabbit;
    baz {a: b}
    bip {c: d}}
  bibble {
    bap {e: f}}}
SCSS
  end

  def test_nested_rules_with_fancy_selectors
    assert_equal <<CSS, render(<<SCSS)
foo .bar {
  a: b; }
foo :baz {
  c: d; }
foo bang:bop {
  e: f; }
foo ::qux {
  g: h; }
foo zap::fblthp {
  i: j; }
CSS
foo {
  .bar {a: b}
  :baz {c: d}
  bang:bop {e: f}
  ::qux {g: h}
  zap::fblthp {i: j}}
SCSS
  end

  def test_almost_ambiguous_nested_rules_and_declarations
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: baz bang bop biddle woo look at all these elems; }
  foo bar:baz:bang:bop:biddle:woo:look:at:all:these:pseudoclasses {
    a: b; }
  foo bar:baz bang bop biddle woo look at all these elems {
    a: b; }
CSS
foo {
  bar:baz:bang:bop:biddle:woo:look:at:all:these:pseudoclasses {a: b};
  bar:baz bang bop biddle woo look at all these elems {a: b};
  bar:baz bang bop biddle woo look at all these elems; }
SCSS
  end

  def test_newlines_in_selectors
    assert_equal <<CSS, render(<<SCSS)
foo
bar {
  a: b; }
CSS
foo
bar {a: b}
SCSS

    assert_equal <<CSS, render(<<SCSS)
foo baz,
foo bang,
bar baz,
bar bang {
  a: b; }
CSS
foo,
bar {
  baz,
  bang {a: b}}
SCSS

    assert_equal <<CSS, render(<<SCSS)
foo
bar baz
bang {
  a: b; }
foo
bar bip bop {
  c: d; }
CSS
foo
bar {
  baz
  bang {a: b}

  bip bop {c: d}}
SCSS

    assert_equal <<CSS, render(<<SCSS)
foo bang, foo bip
bop, bar
baz bang, bar
baz bip
bop {
  a: b; }
CSS
foo, bar
baz {
  bang, bip
  bop {a: b}}
SCSS
  end

  def test_trailing_comma_in_selector
    assert_equal <<CSS, render(<<SCSS)
#foo #bar,
#baz #boom {
  a: b; }

#bip #bop {
  c: d; }
CSS
#foo #bar,,
,#baz #boom, {a: b}

#bip #bop, ,, {c: d}
SCSS
  end

  def test_parent_selectors
    assert_equal <<CSS, render(<<SCSS)
foo:hover {
  a: b; }
bar foo.baz {
  c: d; }
CSS
foo {
  &:hover {a: b}
  bar &.baz {c: d}}
SCSS
  end

  def test_parent_selector_with_subject
    silence_warnings {assert_equal <<CSS, render(<<SCSS)}
bar foo.baz! .bip {
  a: b; }

bar foo bar.baz! .bip {
  c: d; }
CSS
foo {
  bar &.baz! .bip {a: b}}

foo bar {
  bar &.baz! .bip {c: d}}
SCSS
  end

  def test_parent_selector_with_suffix
    assert_equal <<CSS, render(<<SCSS)
.foo-bar {
  a: b; }
.foo_bar {
  c: d; }
.foobar {
  e: f; }
.foo123 {
  e: f; }

:hover-suffix {
  g: h; }
CSS
.foo {
  &-bar {a: b}
  &_bar {c: d}
  &bar {e: f}
  &123 {e: f}
}

:hover {
  &-suffix {g: h}
}
SCSS
  end

  def test_unknown_directive_bubbling
    assert_equal(<<CSS, render(<<SCSS, :style => :nested))
@fblthp {
  .foo .bar {
    a: b; } }
CSS
.foo {
  @fblthp {
    .bar {a: b}
  }
}
SCSS
  end

  def test_keyframe_bubbling
    assert_equal(<<CSS, render(<<SCSS, :style => :nested))
@keyframes spin {
  0% {
    transform: rotate(0deg); } }
@-webkit-keyframes spin {
  0% {
    transform: rotate(0deg); } }
CSS
.foo {
  @keyframes spin {
    0% {transform: rotate(0deg)}
  }
  @-webkit-keyframes spin {
    0% {transform: rotate(0deg)}
  }
}
SCSS
  end

  ## Namespace Properties

  def test_namespace_properties
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: baz;
  bang-bip: 1px;
  bang-bop: bar; }
CSS
foo {
  bar: baz;
  bang: {
    bip: 1px;
    bop: bar;}}
SCSS
  end

  def test_several_namespace_properties
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: baz;
  bang-bip: 1px;
  bang-bop: bar;
  buzz-fram: "foo";
  buzz-frum: moo; }
CSS
foo {
  bar: baz;
  bang: {
    bip: 1px;
    bop: bar;}
  buzz: {
    fram: "foo";
    frum: moo;
  }
}
SCSS
  end

  def test_nested_namespace_properties
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: baz;
  bang-bip: 1px;
  bang-bop: bar;
  bang-blat-baf: bort; }
CSS
foo {
  bar: baz;
  bang: {
    bip: 1px;
    bop: bar;
    blat:{baf:bort}}}
SCSS
  end

  def test_namespace_properties_with_value
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: baz;
    bar-bip: bop;
    bar-bing: bop; }
CSS
foo {
  bar: baz {
    bip: bop;
    bing: bop; }}
SCSS
  end

  def test_namespace_properties_with_script_value
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: bazbang;
    bar-bip: bop;
    bar-bing: bop; }
CSS
foo {
  bar: baz + bang {
    bip: bop;
    bing: bop; }}
SCSS
  end

  def test_no_namespace_properties_without_space
    assert_equal <<CSS, render(<<SCSS)
foo bar:baz {
  bip: bop; }
CSS
foo {
  bar:baz {
    bip: bop }}
SCSS
  end

  def test_no_namespace_properties_without_space_even_when_its_unambiguous
    render(<<SCSS)
foo {
  bar:baz calc(1 + 2) {
    bip: bop }}
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "bar:baz calc": expected selector, was "(1 + 2)"', e.message
    assert_equal 2, e.sass_line
  end

  def test_namespace_properties_without_space_allowed_for_non_identifier
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: 1px;
    bar-bip: bop; }
CSS
foo {
  bar:1px {
    bip: bop }}
SCSS
  end

  ## Mixins

  def test_basic_mixins
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
@mixin foo {
  .foo {a: b}}

@include foo;
SCSS

    assert_equal <<CSS, render(<<SCSS)
bar {
  c: d; }
  bar .foo {
    a: b; }
CSS
@mixin foo {
  .foo {a: b}}

bar {
  @include foo;
  c: d; }
SCSS

    assert_equal <<CSS, render(<<SCSS)
bar {
  a: b;
  c: d; }
CSS
@mixin foo {a: b}

bar {
  @include foo;
  c: d; }
SCSS
  end

  def test_mixins_with_empty_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
@mixin foo() {a: b}

.foo {@include foo();}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
@mixin foo() {a: b}

.foo {@include foo;}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
@mixin foo {a: b}

.foo {@include foo();}
SCSS
  end

  def test_mixins_with_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: bar; }
CSS
@mixin foo($a) {a: $a}

.foo {@include foo(bar)}
SCSS

    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: bar;
  b: 12px; }
CSS
@mixin foo($a, $b) {
  a: $a;
  b: $b; }

.foo {@include foo(bar, 12px)}
SCSS
  end

  def test_keyframes_rules_in_content
    assert_equal <<CSS, render(<<SCSS)
@keyframes identifier {
  0% {
    top: 0;
    left: 0; }
  30% {
    top: 50px; }
  68%, 72% {
    left: 50px; }
  100% {
    top: 100px;
    left: 100%; } }
CSS
@mixin keyframes {
  @keyframes identifier { @content }
}

@include keyframes {
  0% {top: 0; left: 0}
  \#{"30%"} {top: 50px}
  68%, 72% {left: 50px}
  100% {top: 100px; left: 100%}
}
SCSS
  end

  ## Functions

  def test_basic_function
    assert_equal(<<CSS, render(<<SASS))
bar {
  a: 3; }
CSS
@function foo() {
  @return 1 + 2;
}

bar {
  a: foo();
}
SASS
  end

  def test_function_args
    assert_equal(<<CSS, render(<<SASS))
bar {
  a: 3; }
CSS
@function plus($var1, $var2) {
  @return $var1 + $var2;
}

bar {
  a: plus(1, 2);
}
SASS
  end

  def test_disallowed_function_names
    Sass::Deprecation.allow_double_warnings do
      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "calc" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function calc() {}
SCSS

      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "-my-calc" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function -my-calc() {}
SCSS

      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "element" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function element() {}
SCSS

      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "-my-element" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function -my-element() {}
SCSS

      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "expression" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function expression() {}
SCSS

      assert_warning(<<WARNING) {render(<<SCSS)}
DEPRECATION WARNING on line 1 of test_disallowed_function_names_inline.scss:
Naming a function "url" is disallowed and will be an error in future versions of Sass.
This name conflicts with an existing CSS function with special parse rules.
WARNING
@function url() {}
SCSS
    end
  end

  def test_allowed_function_names
    assert_no_warning {assert_equal(<<CSS, render(<<SCSS))}
.a {
  b: c; }
CSS
@function -my-expression() {@return c}

.a {b: -my-expression()}
SCSS

    assert_no_warning {assert_equal(<<CSS, render(<<SCSS))}
.a {
  b: c; }
CSS
@function -my-url() {@return c}

.a {b: -my-url()}
SCSS
  end

  ## Var Args

  def test_mixin_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2, 3, 4; }
CSS
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

.foo {@include foo(1, 2, 3, 4)}
SCSS
  end

  def test_mixin_empty_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 0; }
CSS
@mixin foo($a, $b...) {
  a: $a;
  b: length($b);
}

.foo {@include foo(1)}
SCSS
  end

  def test_mixin_var_args_act_like_list
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 3;
  b: 3; }
CSS
@mixin foo($a, $b...) {
  a: length($b);
  b: nth($b, 2);
}

.foo {@include foo(1, 2, 3, 4)}
SCSS
  end

  def test_mixin_splat_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3;
  d: 4; }
CSS
@mixin foo($a, $b, $c, $d) {
  a: $a;
  b: $b;
  c: $c;
  d: $d;
}

$list: 2, 3, 4;
.foo {@include foo(1, $list...)}
SCSS
  end

  def test_mixin_splat_expression
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3;
  d: 4; }
CSS
@mixin foo($a, $b, $c, $d) {
  a: $a;
  b: $b;
  c: $c;
  d: $d;
}

.foo {@include foo(1, (2, 3, 4)...)}
SCSS
  end

  def test_mixin_splat_args_with_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2, 3, 4; }
CSS
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

$list: 2, 3, 4;
.foo {@include foo(1, $list...)}
SCSS
  end

  def test_mixin_splat_args_with_var_args_and_normal_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3, 4; }
CSS
@mixin foo($a, $b, $c...) {
  a: $a;
  b: $b;
  c: $c;
}

$list: 2, 3, 4;
.foo {@include foo(1, $list...)}
SCSS
  end

  def test_mixin_splat_args_with_var_args_preserves_separator
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2 3 4 5; }
CSS
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

$list: 3 4 5;
.foo {@include foo(1, 2, $list...)}
SCSS
  end

  def test_mixin_var_and_splat_args_pass_through_keywords
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 3;
  b: 1;
  c: 2; }
CSS
@mixin foo($a...) {
  @include bar($a...);
}

@mixin bar($b, $c, $a) {
  a: $a;
  b: $b;
  c: $c;
}

.foo {@include foo(1, $c: 2, $a: 3)}
SCSS
  end

  def test_mixin_var_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3; }
CSS
@mixin foo($args...) {
  a: map-get(keywords($args), a);
  b: map-get(keywords($args), b);
  c: map-get(keywords($args), c);
}

.foo {@include foo($a: 1, $b: 2, $c: 3)}
SCSS
  end

  def test_mixin_empty_var_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  length: 0; }
CSS
@mixin foo($args...) {
  length: length(keywords($args));
}

.foo {@include foo}
SCSS
  end

  def test_mixin_map_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3; }
CSS
@mixin foo($a, $b, $c) {
  a: $a;
  b: $b;
  c: $c;
}

.foo {
  $map: (a: 1, b: 2, c: 3);
  @include foo($map...);
}
SCSS
  end

  def test_mixin_map_and_list_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: x;
  b: y;
  c: z;
  d: 1;
  e: 2;
  f: 3; }
CSS
@mixin foo($a, $b, $c, $d, $e, $f) {
  a: $a;
  b: $b;
  c: $c;
  d: $d;
  e: $e;
  f: $f;
}

.foo {
  $list: x y z;
  $map: (d: 1, e: 2, f: 3);
  @include foo($list..., $map...);
}
SCSS
  end

  def test_mixin_map_splat_takes_precedence_over_pass_through
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: z; }
CSS
@mixin foo($args...) {
  $map: (c: z);
  @include bar($args..., $map...);
}

@mixin bar($a, $b, $c) {
  a: $a;
  b: $b;
  c: $c;
}

.foo {
  @include foo(1, $b: 2, $c: 3);
}
SCSS
  end

  def test_mixin_list_of_pairs_splat_treated_as_list
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: a 1;
  b: b 2;
  c: c 3; }
CSS
@mixin foo($a, $b, $c) {
  a: $a;
  b: $b;
  c: $c;
}

.foo {
  @include foo((a 1, b 2, c 3)...);
}
SCSS
  end

  def test_mixin_splat_after_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3; }
CSS
@mixin foo($a, $b, $c) {
  a: 1;
  b: 2;
  c: 3;
}

.foo {
  @include foo(1, $c: 3, 2...);
}
SCSS
  end

  def test_mixin_keyword_args_after_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3; }
CSS
@mixin foo($a, $b, $c) {
  a: 1;
  b: 2;
  c: 3;
}

.foo {
  @include foo(1, 2..., $c: 3);
}
SCSS
  end

  def test_mixin_keyword_splat_after_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3; }
CSS
@mixin foo($a, $b, $c) {
  a: 1;
  b: 2;
  c: 3;
}

.foo {
  @include foo(1, $b: 2, (c: 3)...);
}
SCSS
  end

  def test_mixin_triple_keyword_splat_merge
    assert_equal <<CSS, render(<<SCSS)
.foo {
  foo: 1;
  bar: 2;
  kwarg: 3;
  a: 3;
  b: 2;
  c: 3; }
CSS
@mixin foo($foo, $bar, $kwarg, $a, $b, $c) {
  foo: $foo;
  bar: $bar;
  kwarg: $kwarg;
  a: $a;
  b: $b;
  c: $c;
}

@mixin bar($args...) {
  @include foo($args..., $bar: 2, $a: 2, $b: 2, (kwarg: 3, a: 3, c: 3)...);
}

.foo {
  @include bar($foo: 1, $a: 1, $b: 1, $c: 1);
}
SCSS
  end

  def test_mixin_map_splat_converts_hyphens_and_underscores_for_real_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: 1;
  b: 2;
  c: 3;
  d: 4; }
CSS
@mixin foo($a-1, $b-2, $c_3, $d_4) {
  a: $a-1;
  b: $b-2;
  c: $c_3;
  d: $d_4;
}

.foo {
  $map: (a-1: 1, b_2: 2, c-3: 3, d_4: 4);
  @include foo($map...);
}
SCSS
  end

  def test_mixin_map_splat_doesnt_convert_hyphens_and_underscores_for_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a-1: 1;
  b_2: 2;
  c-3: 3;
  d_4: 4; }
CSS
@mixin foo($args...) {
  @each $key, $value in keywords($args) {
    \#{$key}: $value;
  }
}

.foo {
  $map: (a-1: 1, b_2: 2, c-3: 3, d_4: 4);
  @include foo($map...);
}
SCSS
  end

  def test_mixin_conflicting_splat_after_keyword_args
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Mixin foo was passed argument $b both by position and by name.
MESSAGE
@mixin foo($a, $b, $c) {
  a: 1;
  b: 2;
  c: 3;
}

.foo {
  @include foo(1, $b: 2, 3...);
}
SCSS
  end

  def test_mixin_keyword_splat_must_have_string_keys
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Variable keyword argument map must have string keys.
12 is not a string in (12: 1).
MESSAGE
@mixin foo($a) {
  a: $a;
}

.foo {@include foo((12: 1)...)}
SCSS
  end

  def test_mixin_positional_arg_after_splat
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Only keyword arguments may follow variable arguments (...).
MESSAGE
@mixin foo($a, $b, $c) {
  a: 1;
  b: 2;
  c: 3;
}

.foo {
  @include foo(1, 2..., 3);
}
SCSS
  end

  def test_mixin_var_args_with_keyword
    assert_raise_message(Sass::SyntaxError, "Positional arguments must come before keyword arguments.") {render <<SCSS}
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

.foo {@include foo($a: 1, 2, 3, 4)}
SCSS
  end

  def test_mixin_keyword_for_var_arg
    assert_raise_message(Sass::SyntaxError, "Argument $b of mixin foo cannot be used as a named argument.") {render <<SCSS}
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

.foo {@include foo(1, $b: 2 3 4)}
SCSS
  end

  def test_mixin_keyword_for_unknown_arg_with_var_args
    assert_raise_message(Sass::SyntaxError, "Mixin foo doesn't have an argument named $c.") {render <<SCSS}
@mixin foo($a, $b...) {
  a: $a;
  b: $b;
}

.foo {@include foo(1, $c: 2 3 4)}
SCSS
  end

  def test_mixin_map_splat_before_list_splat
    assert_raise_message(Sass::SyntaxError, "Variable keyword arguments must be a map (was (2 3)).") {render <<SCSS}
@mixin foo($a, $b, $c) {
  a: $a;
  b: $b;
  c: $c;
}

.foo {
  @include foo((a: 1)..., (2 3)...);
}
SCSS
  end

  def test_mixin_map_splat_with_unknown_keyword
    assert_raise_message(Sass::SyntaxError, "Mixin foo doesn't have an argument named $c.") {render <<SCSS}
@mixin foo($a, $b) {
  a: $a;
  b: $b;
}

.foo {
  @include foo(1, 2, (c: 1)...);
}
SCSS
  end

  def test_mixin_map_splat_with_wrong_type
    assert_raise_message(Sass::SyntaxError, "Variable keyword arguments must be a map (was 12).") {render <<SCSS}
@mixin foo($a, $b) {
  a: $a;
  b: $b;
}

.foo {
  @include foo((1, 2)..., 12...);
}
SCSS
  end

  def test_mixin_splat_too_many_args
    assert_warning(<<WARNING) {render <<SCSS}
WARNING: Mixin foo takes 2 arguments but 4 were passed.
        on line 2 of #{filename_for_test(:scss)}
This will be an error in future versions of Sass.
WARNING
@mixin foo($a, $b) {}
@include foo((1, 2, 3, 4)...);
SCSS
  end

  def test_function_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, 3, 4"; }
CSS
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{$b}";
}

.foo {val: foo(1, 2, 3, 4)}
SCSS
  end

  def test_function_empty_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 0"; }
CSS
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{length($b)}";
}

.foo {val: foo(1)}
SCSS
  end

  def test_function_var_args_act_like_list
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 3, b: 3"; }
CSS
@function foo($a, $b...) {
  @return "a: \#{length($b)}, b: \#{nth($b, 2)}";
}

.foo {val: foo(1, 2, 3, 4)}
SCSS
  end

  def test_function_splat_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3, d: 4"; }
CSS
@function foo($a, $b, $c, $d) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}, d: \#{$d}";
}

$list: 2, 3, 4;
.foo {val: foo(1, $list...)}
SCSS
  end

  def test_function_splat_expression
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3, d: 4"; }
CSS
@function foo($a, $b, $c, $d) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}, d: \#{$d}";
}

.foo {val: foo(1, (2, 3, 4)...)}
SCSS
  end

  def test_function_splat_args_with_var_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, 3, 4"; }
CSS
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{$b}";
}

$list: 2, 3, 4;
.foo {val: foo(1, $list...)}
SCSS
  end

  def test_function_splat_args_with_var_args_and_normal_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3, 4"; }
CSS
@function foo($a, $b, $c...) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

$list: 2, 3, 4;
.foo {val: foo(1, $list...)}
SCSS
  end

  def test_function_splat_args_with_var_args_preserves_separator
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2 3 4 5"; }
CSS
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{$b}";
}

$list: 3 4 5;
.foo {val: foo(1, 2, $list...)}
SCSS
  end

  def test_function_var_and_splat_args_pass_through_keywords
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 3, b: 1, c: 2"; }
CSS
@function foo($a...) {
  @return bar($a...);
}

@function bar($b, $c, $a) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {val: foo(1, $c: 2, $a: 3)}
SCSS
  end

  def test_function_var_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3"; }
CSS
@function foo($args...) {
  @return "a: \#{map-get(keywords($args), a)}, " +
    "b: \#{map-get(keywords($args), b)}, " +
    "c: \#{map-get(keywords($args), c)}";
}

.foo {val: foo($a: 1, $b: 2, $c: 3)}
SCSS
  end

  def test_function_empty_var_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  length: 0; }
CSS
@function foo($args...) {
  @return length(keywords($args));
}

.foo {length: foo()}
SCSS
  end

  def test_function_map_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3"; }
CSS
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  $map: (a: 1, b: 2, c: 3);
  val: foo($map...);
}
SCSS
  end

  def test_function_map_and_list_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: x, b: y, c: z, d: 1, e: 2, f: 3"; }
CSS
@function foo($a, $b, $c, $d, $e, $f) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}, d: \#{$d}, e: \#{$e}, f: \#{$f}";
}

.foo {
  $list: x y z;
  $map: (d: 1, e: 2, f: 3);
  val: foo($list..., $map...);
}
SCSS
  end

  def test_function_map_splat_takes_precedence_over_pass_through
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: z"; }
CSS
@function foo($args...) {
  $map: (c: z);
  @return bar($args..., $map...);
}

@function bar($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, $b: 2, $c: 3);
}
SCSS
  end

  def test_ruby_function_map_splat_takes_precedence_over_pass_through
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: 1 2 3 z; }
CSS
@function foo($args...) {
  $map: (val: z);
  @return append($args..., $map...);
}

.foo {
  val: foo(1 2 3, $val: 4)
}
SCSS
  end

  def test_function_list_of_pairs_splat_treated_as_list
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: a 1, b: b 2, c: c 3"; }
CSS
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo((a 1, b 2, c 3)...);
}
SCSS
  end

  def test_function_splat_after_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3"; }
CSS
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, $c: 3, 2...);
}
SCSS
  end

  def test_function_keyword_args_after_splat
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3"; }
CSS
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, 2..., $c: 3);
}
SCSS
  end

  def test_function_keyword_splat_after_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "a: 1, b: 2, c: 3"; }
CSS
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, $b: 2, (c: 3)...);
}
SCSS
  end

  def test_function_triple_keyword_splat_merge
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: "foo: 1, bar: 2, kwarg: 3, a: 3, b: 2, c: 3"; }
CSS
@function foo($foo, $bar, $kwarg, $a, $b, $c) {
  @return "foo: \#{$foo}, bar: \#{$bar}, kwarg: \#{$kwarg}, a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

@function bar($args...) {
  @return foo($args..., $bar: 2, $a: 2, $b: 2, (kwarg: 3, a: 3, c: 3)...);
}

.foo {
  val: bar($foo: 1, $a: 1, $b: 1, $c: 1);
}
SCSS
  end

  def test_function_conflicting_splat_after_keyword_args
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Function foo was passed argument $b both by position and by name.
MESSAGE
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, $b: 2, 3...);
}
SCSS
  end

  def test_function_positional_arg_after_splat
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Only keyword arguments may follow variable arguments (...).
MESSAGE
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo(1, 2..., 3);
}
SCSS
  end

  def test_function_var_args_with_keyword
    assert_raise_message(Sass::SyntaxError, "Positional arguments must come before keyword arguments.") {render <<SCSS}
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{$b}";
}

.foo {val: foo($a: 1, 2, 3, 4)}
SCSS
  end

  def test_function_keyword_for_var_arg
    assert_raise_message(Sass::SyntaxError, "Argument $b of function foo cannot be used as a named argument.") {render <<SCSS}
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{$b}";
}

.foo {val: foo(1, $b: 2 3 4)}
SCSS
  end

  def test_function_keyword_for_unknown_arg_with_var_args
    assert_raise_message(Sass::SyntaxError, "Function foo doesn't have an argument named $c.") {render <<SCSS}
@function foo($a, $b...) {
  @return "a: \#{$a}, b: \#{length($b)}";
}

.foo {val: foo(1, $c: 2 3 4)}
SCSS
  end

  def test_function_var_args_passed_to_native
    assert_equal <<CSS, render(<<SCSS)
.foo {
  val: #102035; }
CSS
@function foo($args...) {
  @return adjust-color($args...);
}

.foo {val: foo(#102030, $blue: 5)}
SCSS
  end

  def test_function_map_splat_before_list_splat
    assert_raise_message(Sass::SyntaxError, "Variable keyword arguments must be a map (was (2 3)).") {render <<SCSS}
@function foo($a, $b, $c) {
  @return "a: \#{$a}, b: \#{$b}, c: \#{$c}";
}

.foo {
  val: foo((a: 1)..., (2 3)...);
}
SCSS
  end

  def test_function_map_splat_with_unknown_keyword
    assert_raise_message(Sass::SyntaxError, "Function foo doesn't have an argument named $c.") {render <<SCSS}
@function foo($a, $b) {
  @return "a: \#{$a}, b: \#{$b}";
}

.foo {
  val: foo(1, 2, (c: 1)...);
}
SCSS
  end

  def test_function_map_splat_with_wrong_type
    assert_raise_message(Sass::SyntaxError, "Variable keyword arguments must be a map (was 12).") {render <<SCSS}
@function foo($a, $b) {
  @return "a: \#{$a}, b: \#{$b}";
}

.foo {
  val: foo((1, 2)..., 12...);
}
SCSS
  end

  def test_function_keyword_splat_must_have_string_keys
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Variable keyword argument map must have string keys.
12 is not a string in (12: 1).
MESSAGE
@function foo($a) {
  @return $a;
}

.foo {val: foo((12: 1)...)}
SCSS
  end

  def test_function_splat_too_many_args
    assert_warning(<<WARNING) {render <<SCSS}
WARNING: Function foo takes 2 arguments but 4 were passed.
        on line 2 of #{filename_for_test(:scss)}
This will be an error in future versions of Sass.
WARNING
@function foo($a, $b) {@return null}
$var: foo((1, 2, 3, 4)...);
SCSS
  end

  ## Interpolation

  def test_basic_selector_interpolation
    assert_equal <<CSS, render(<<SCSS)
foo ab baz {
  a: b; }
CSS
foo \#{'a' + 'b'} baz {a: b}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo.bar baz {
  a: b; }
CSS
foo\#{".bar"} baz {a: b}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo.bar baz {
  a: b; }
CSS
\#{"foo"}.bar baz {a: b}
SCSS
  end

  def test_selector_only_interpolation
    assert_equal <<CSS, render(<<SCSS)
foo bar {
  a: b; }
CSS
\#{"foo" + " bar"} {a: b}
SCSS
  end

  def test_selector_interpolation_before_element_name
    assert_equal <<CSS, render(<<SCSS)
foo barbaz {
  a: b; }
CSS
\#{"foo" + " bar"}baz {a: b}
SCSS
  end

  def test_selector_interpolation_in_string
    assert_equal <<CSS, render(<<SCSS)
foo[val="bar foo bar baz"] {
  a: b; }
CSS
foo[val="bar \#{"foo" + " bar"} baz"] {a: b}
SCSS
  end

  def test_selector_interpolation_in_pseudoclass
    assert_equal <<CSS, render(<<SCSS)
foo:nth-child(5n) {
  a: b; }
CSS
foo:nth-child(\#{5 + "n"}) {a: b}
SCSS
  end

  def test_selector_interpolation_at_class_begininng
    assert_equal <<CSS, render(<<SCSS)
.zzz {
  a: b; }
CSS
$zzz: zzz;
.\#{$zzz} { a: b; }
SCSS
  end

  def test_selector_interpolation_at_id_begininng
    assert_equal <<CSS, render(<<SCSS)
#zzz {
  a: b; }
CSS
$zzz: zzz;
#\#{$zzz} { a: b; }
SCSS
  end

  def test_selector_interpolation_at_pseudo_begininng
    assert_equal <<CSS, render(<<SCSS)
:zzz::zzz {
  a: b; }
CSS
$zzz: zzz;
:\#{$zzz}::\#{$zzz} { a: b; }
SCSS
  end

  def test_selector_interpolation_at_attr_beginning
    assert_equal <<CSS, render(<<SCSS)
[zzz=foo] {
  a: b; }
CSS
$zzz: zzz;
[\#{$zzz}=foo] { a: b; }
SCSS
  end

  def test_selector_interpolation_at_attr_end
    assert_equal <<CSS, render(<<SCSS)
[foo=zzz] {
  a: b; }
CSS
$zzz: zzz;
[foo=\#{$zzz}] { a: b; }
SCSS
  end

  def test_selector_interpolation_at_dashes
    assert_equal <<CSS, render(<<SCSS)
div {
  -foo-a-b-foo: foo; }
CSS
$a : a;
$b : b;
div { -foo-\#{$a}-\#{$b}-foo: foo }
SCSS
  end

  def test_selector_interpolation_in_reference_combinator
    silence_warnings {assert_equal <<CSS, render(<<SCSS)}
.foo /a/ .bar /b|c/ .baz {
  a: b; }
CSS
$a: a;
$b: b;
$c: c;
.foo /\#{$a}/ .bar /\#{$b}|\#{$c}/ .baz {a: b}
SCSS
  end

  def test_parent_selector_with_parent_and_subject
    silence_warnings {assert_equal <<CSS, render(<<SCSS)}
bar foo.baz! .bip {
  c: d; }
CSS
$subject: "!";
foo {
  bar &.baz\#{$subject} .bip {c: d}}
SCSS
  end

  def test_basic_prop_name_interpolation
    assert_equal <<CSS, render(<<SCSS)
foo {
  barbazbang: blip; }
CSS
foo {bar\#{"baz" + "bang"}: blip}
SCSS
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar3: blip; }
CSS
foo {bar\#{1 + 2}: blip}
SCSS
  end

  def test_prop_name_only_interpolation
    assert_equal <<CSS, render(<<SCSS)
foo {
  bazbang: blip; }
CSS
foo {\#{"baz" + "bang"}: blip}
SCSS
  end

  def test_directive_interpolation
    assert_equal <<CSS, render(<<SCSS)
@foo bar12 qux {
  a: b; }
CSS
$baz: 12;
@foo bar\#{$baz} qux {a: b}
SCSS
  end

  def test_media_interpolation
    assert_equal <<CSS, render(<<SCSS)
@media bar12 {
  a: b; }
CSS
$baz: 12;
@media bar\#{$baz} {a: b}
SCSS
  end

  def test_script_in_media
    assert_equal <<CSS, render(<<SCSS)
@media screen and (-webkit-min-device-pixel-ratio: 20), only print {
  a: b; }
CSS
$media1: screen;
$media2: print;
$var: -webkit-min-device-pixel-ratio;
$val: 20;
@media \#{$media1} and ($var: $val), only \#{$media2} {a: b}
SCSS

    assert_equal <<CSS, render(<<SCSS)
@media screen and (-webkit-min-device-pixel-ratio: 13) {
  a: b; }
CSS
$vals: 1 2 3;
@media screen and (-webkit-min-device-pixel-ratio: 5 + 6 + nth($vals, 2)) {a: b}
SCSS
  end

  def test_media_interpolation_with_reparse
    assert_equal <<CSS, render(<<SCSS)
@media screen and (max-width: 300px) {
  a: b; }
@media screen and (max-width: 300px) {
  a: b; }
@media screen and (max-width: 300px) {
  a: b; }
@media screen and (max-width: 300px), print and (max-width: 300px) {
  a: b; }
CSS
$constraint: "(max-width: 300px)";
$fragment: "nd \#{$constraint}";
$comma: "een, pri";
@media screen and \#{$constraint} {a: b}
@media screen {
  @media \#{$constraint} {a: b}
}
@media screen a\#{$fragment} {a: b}
@media scr\#{$comma}nt {
  @media \#{$constraint} {a: b}
}
SCSS
  end

  def test_moz_document_interpolation
    assert_equal <<CSS, render(<<SCSS)
@-moz-document url(http://sass-lang.com/),
               url-prefix(http://sass-lang.com/docs),
               domain(sass-lang.com),
               domain("sass-lang.com") {
  .foo {
    a: b; } }
CSS
$domain: "sass-lang.com";
@-moz-document url(http://\#{$domain}/),
               url-prefix(http://\#{$domain}/docs),
               domain(\#{$domain}),
               \#{domain($domain)} {
  .foo {a: b}
}
SCSS
  end

  def test_supports_with_expressions
    assert_equal <<CSS, render(<<SCSS)
@supports ((feature1: val) and (feature2: val)) or (not (feature23: val4)) {
  foo {
    a: b; } }
CSS
$query: "(feature1: val)";
$feature: feature2;
$val: val;
@supports (\#{$query} and ($feature: $val)) or (not ($feature + 3: $val + 4)) {
  foo {a: b}
}
SCSS
  end

  def test_supports_bubbling
    assert_equal <<CSS, render(<<SCSS)
@supports (foo: bar) {
  a {
    b: c; }
    @supports (baz: bang) {
      a {
        d: e; } } }
CSS
a {
  @supports (foo: bar) {
    b: c;
    @supports (baz: bang) {
      d: e;
    }
  }
}
SCSS
  end

  def test_random_directive_interpolation
    assert_equal <<CSS, render(<<SCSS)
@foo url(http://sass-lang.com/),
     domain("sass-lang.com"),
     "foobarbaz",
     foobarbaz {
  .foo {
    a: b; } }
CSS
$domain: "sass-lang.com";
@foo url(http://\#{$domain}/),
     \#{domain($domain)},
     "foo\#{'ba' + 'r'}baz",
     foo\#{'ba' + 'r'}baz {
  .foo {a: b}
}
SCSS
  end

  def test_color_interpolation_warning_in_selector
    assert_warning(<<WARNING) {assert_equal <<CSS, render(<<SCSS)}
WARNING on line 1, column 4 of #{filename_for_test(:scss)}:
You probably don't mean to use the color value `blue' in interpolation here.
It may end up represented as #0000ff, which will likely produce invalid CSS.
Always quote color names when using them as strings (for example, "blue").
If you really want to use the color value here, use `"" + blue'.
WARNING
fooblue {
  a: b; }
CSS
foo\#{blue} {a: b}
SCSS
  end

  def test_color_interpolation_warning_in_directive
    assert_warning(<<WARNING) {assert_equal <<CSS, render(<<SCSS)}
WARNING on line 1, column 12 of #{filename_for_test(:scss)}:
You probably don't mean to use the color value `blue' in interpolation here.
It may end up represented as #0000ff, which will likely produce invalid CSS.
Always quote color names when using them as strings (for example, "blue").
If you really want to use the color value here, use `"" + blue'.
WARNING
@fblthp fooblue {
  a: b; }
CSS
@fblthp foo\#{blue} {a: b}
SCSS
  end

  def test_color_interpolation_warning_in_property_name
    assert_warning(<<WARNING) {assert_equal <<CSS, render(<<SCSS)}
WARNING on line 1, column 8 of #{filename_for_test(:scss)}:
You probably don't mean to use the color value `blue' in interpolation here.
It may end up represented as #0000ff, which will likely produce invalid CSS.
Always quote color names when using them as strings (for example, "blue").
If you really want to use the color value here, use `"" + blue'.
WARNING
foo {
  a-blue: b; }
CSS
foo {a-\#{blue}: b}
SCSS
  end

  def test_no_color_interpolation_warning_in_property_value
    assert_no_warning {assert_equal <<CSS, render(<<SCSS)}
foo {
  a: b-blue; }
CSS
foo {a: b-\#{blue}}
SCSS
  end

  def test_no_color_interpolation_warning_for_nameless_color
    assert_no_warning {assert_equal <<CSS, render(<<SCSS)}
foo-#abcdef {
  a: b; }
CSS
foo-\#{#abcdef} {a: b}
SCSS
  end

  def test_nested_mixin_def
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: b; }
CSS
foo {
  @mixin bar {a: b}
  @include bar; }
SCSS
  end

  def test_nested_mixin_shadow
    assert_equal <<CSS, render(<<SCSS)
foo {
  c: d; }

baz {
  a: b; }
CSS
@mixin bar {a: b}

foo {
  @mixin bar {c: d}
  @include bar;
}

baz {@include bar}
SCSS
  end

  def test_nested_function_def
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 1; }

bar {
  b: foo(); }
CSS
foo {
  @function foo() {@return 1}
  a: foo(); }

bar {b: foo()}
SCSS
  end

  def test_nested_function_shadow
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 2; }

baz {
  b: 1; }
CSS
@function foo() {@return 1}

foo {
  @function foo() {@return 2}
  a: foo();
}

baz {b: foo()}
SCSS
  end

  ## @at-root

  def test_simple_at_root
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
.foo {
  @at-root {
    .bar {a: b}
  }
}
SCSS
  end

  def test_at_root_with_selector
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
.foo {
  @at-root .bar {a: b}
}
SCSS
  end

  def test_at_root_in_mixin
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
@mixin bar {
  @at-root .bar {a: b}
}

.foo {
  @include bar;
}
SCSS
  end

  def test_at_root_in_media
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
@media screen {
  .foo {
    @at-root .bar {a: b}
  }
}
SCSS
  end

  def test_at_root_in_bubbled_media
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @at-root .bar {a: b}
  }
}
SCSS
  end

  def test_at_root_in_unknown_directive
    assert_equal <<CSS, render(<<SCSS)
@fblthp {
  .bar {
    a: b; } }
CSS
@fblthp {
  .foo {
    @at-root .bar {a: b}
  }
}
SCSS
  end

  def test_comments_in_at_root
    assert_equal <<CSS, render(<<SCSS)
/* foo */
.bar {
  a: b; }

/* baz */
CSS
.foo {
  @at-root {
    /* foo */
    .bar {a: b}
    /* baz */
  }
}
SCSS
  end

  def test_comments_in_at_root_in_media
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  /* foo */
  .bar {
    a: b; }

  /* baz */ }
CSS
@media screen {
  .foo {
    @at-root {
      /* foo */
      .bar {a: b}
      /* baz */
    }
  }
}
SCSS
  end

  def test_comments_in_at_root_in_unknown_directive
    assert_equal <<CSS, render(<<SCSS)
@fblthp {
  /* foo */
  .bar {
    a: b; }

  /* baz */ }
CSS
@fblthp {
  .foo {
    @at-root {
      /* foo */
      .bar {a: b}
      /* baz */
    }
  }
}
SCSS
  end

  def test_media_directive_in_at_root
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
.foo {
  @at-root {
    @media screen {.bar {a: b}}
  }
}
SCSS
  end

  def test_bubbled_media_directive_in_at_root
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar .baz {
    a: b; } }
CSS
.foo {
  @at-root {
    .bar {
      @media screen {.baz {a: b}}
    }
  }
}
SCSS
  end

  def test_unknown_directive_in_at_root
    assert_equal <<CSS, render(<<SCSS)
@fblthp {
  .bar {
    a: b; } }
CSS
.foo {
  @at-root {
    @fblthp {.bar {a: b}}
  }
}
SCSS
  end

  def test_at_root_in_at_root
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
.foo {
  @at-root {
    @at-root .bar {a: b}
  }
}
SCSS
  end

  def test_at_root_with_parent_ref
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
.foo {
  @at-root & {
    a: b;
  }
}
SCSS
  end

  def test_multi_level_at_root_with_parent_ref
    assert_equal <<CSS, render(<<SCSS)
.foo .bar {
  a: b; }
CSS
.foo {
  @at-root & {
    .bar {
      @at-root & {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_multi_level_at_root_with_inner_parent_ref
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
.foo {
  @at-root .bar {
    @at-root & {
      a: b;
    }
  }
}
SCSS
  end

  def test_at_root_beneath_comma_selector
    assert_equal(<<CSS, render(<<SCSS))
.baz {
  a: b; }
CSS
.foo, .bar {
  @at-root .baz {
    a: b;
  }
}
SCSS
  end

  def test_at_root_with_parent_ref_and_class
    assert_equal(<<CSS, render(<<SCSS))
.foo.bar {
  a: b; }
CSS
.foo {
  @at-root &.bar {
    a: b;
  }
}
SCSS
  end

  def test_at_root_beneath_comma_selector_with_parent_ref
    assert_equal(<<CSS, render(<<SCSS))
.foo.baz, .bar.baz {
  a: b; }
CSS
.foo, .bar {
  @at-root &.baz {
    a: b;
  }
}
SCSS
  end

  ## @at-root (...)

  def test_at_root_without_media
    assert_equal <<CSS, render(<<SCSS)
.foo .bar {
  a: b; }
CSS
.foo {
  @media screen {
    @at-root (without: media) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_supports
    assert_equal <<CSS, render(<<SCSS)
.foo .bar {
  a: b; }
CSS
.foo {
  @supports (foo: bar) {
    @at-root (without: supports) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_rule
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @at-root (without: rule) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_unknown_directive
    assert_equal <<CSS, render(<<SCSS)
@fblthp {}
.foo .bar {
  a: b; }
CSS
.foo {
  @fblthp {
    @at-root (without: fblthp) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_multiple
    assert_equal <<CSS, render(<<SCSS)
@supports (foo: bar) {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @supports (foo: bar) {
      @at-root (without: media rule) {
        .bar {
          a: b;
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_all
    assert_equal <<CSS, render(<<SCSS)
@supports (foo: bar) {
  @fblthp {} }
.bar {
  a: b; }
CSS
.foo {
  @supports (foo: bar) {
    @fblthp {
      @at-root (without: all) {
        .bar {
          a: b;
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_media
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {}
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: media) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_rule
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {} }
.foo .bar {
  a: b; }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: rule) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_supports
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {} }
@supports (foo: bar) {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: supports) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_unknown_directive
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {} }
@fblthp {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: fblthp) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_multiple
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {}
  .foo .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: media rule) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_with_all
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  @fblthp {
    @supports (foo: bar) {
      .foo .bar {
        a: b; } } } }
CSS
.foo {
  @media screen {
    @fblthp {
      @supports (foo: bar) {
        @at-root (with: all) {
          .bar {
            a: b;
          }
        }
      }
    }
  }
}
SCSS
  end

  def test_at_root_dynamic_values
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
$key: with;
$value: media;
.foo {
  @media screen {
    @at-root ($key: $value) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_interpolated_query
    assert_equal <<CSS, render(<<SCSS)
@media screen {
  .bar {
    a: b; } }
CSS
.foo {
  @media screen {
    @at-root (\#{"with: media"}) {
      .bar {
        a: b;
      }
    }
  }
}
SCSS
  end

  def test_at_root_plus_extend
    assert_equal <<CSS, render(<<SCSS)
.foo .bar {
  a: b; }
CSS
%base {
  a: b;
}

.foo {
  @media screen {
    @at-root (without: media) {
      .bar {
        @extend %base;
      }
    }
  }
}
SCSS
  end

  def test_at_root_without_keyframes_in_keyframe_rule
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }
CSS
@keyframes identifier {
  0% {
    @at-root (without: keyframes) {
      .foo {a: b}
    }
  }
}
SCSS
  end

  def test_at_root_without_rule_in_keyframe_rule
    assert_equal <<CSS, render(<<SCSS)
@keyframes identifier {
  0% {
    a: b; } }
CSS
@keyframes identifier {
  0% {
    @at-root (without: rule) {a: b}
  }
}
SCSS
  end

  ## Selector Script

  def test_selector_script
    assert_equal(<<CSS, render(<<SCSS))
.foo .bar {
  content: ".foo .bar"; }
CSS
.foo .bar {
  content: "\#{&}";
}
SCSS
  end

  def test_nested_selector_script
    assert_equal(<<CSS, render(<<SCSS))
.foo .bar {
  content: ".foo .bar"; }
CSS
.foo {
  .bar {
    content: "\#{&}";
  }
}
SCSS
  end

  def test_nested_selector_script_with_outer_comma_selector
    assert_equal(<<CSS, render(<<SCSS))
.foo .baz, .bar .baz {
  content: ".foo .baz, .bar .baz"; }
CSS
.foo, .bar {
  .baz {
    content: "\#{&}";
  }
}
SCSS
  end

  def test_nested_selector_script_with_inner_comma_selector
    assert_equal(<<CSS, render(<<SCSS))
.foo .bar, .foo .baz {
  content: ".foo .bar, .foo .baz"; }
CSS
.foo {
  .bar, .baz {
    content: "\#{&}";
  }
}
SCSS
  end

  def test_selector_script_through_mixin
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  content: ".foo"; }
CSS
@mixin mixin {
  content: "\#{&}";
}

.foo {
  @include mixin;
}
SCSS
  end

  def test_selector_script_through_content
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  content: ".foo"; }
CSS
@mixin mixin {
  @content;
}

.foo {
  @include mixin {
    content: "\#{&}";
  }
}
SCSS
  end

  def test_selector_script_through_function
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  content: ".foo"; }
CSS
@function fn() {
  @return "\#{&}";
}

.foo {
  content: fn();
}
SCSS
  end

  def test_selector_script_through_media
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  content: "outer"; }
  @media screen {
    .foo .bar {
      content: ".foo .bar"; } }
CSS
.foo {
  content: "outer";
  @media screen {
    .bar {
      content: "\#{&}";
    }
  }
}
SCSS
  end

  def test_selector_script_save_and_reuse
    assert_equal(<<CSS, render(<<SCSS))
.bar {
  content: ".foo"; }
CSS
$var: null;
.foo {
  $var: & !global;
}

.bar {
  content: "\#{$var}";
}
SCSS
  end

  def test_selector_script_with_at_root
    assert_equal(<<CSS, render(<<SCSS))
.foo-bar {
  a: b; }
CSS
.foo {
  @at-root \#{&}-bar {
    a: b;
  }
}
SCSS
  end

  def test_multi_level_at_root_with_inner_selector_script
    assert_equal <<CSS, render(<<SCSS)
.bar {
  a: b; }
CSS
.foo {
  @at-root .bar {
    @at-root \#{&} {
      a: b;
    }
  }
}
SCSS
  end

  def test_at_root_with_at_root_through_mixin
    assert_equal(<<CSS, render(<<SCSS))
.bar-baz {
  a: b; }
CSS
@mixin foo {
  .bar {
    @at-root \#{&}-baz {
      a: b;
    }
  }
}

@include foo;
SCSS
  end

  # See https://github.com/sass/sass/issues/1294
  def test_extend_top_leveled_by_at_root
    render(<<SCSS)
.span-10 {
  @at-root (without: all) {
    @extend %column;
  }
}
SCSS

    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal "Extend directives may only be used within rules.", e.message
    assert_equal 3, e.sass_line
  end

  def test_at_root_doesnt_always_break_blocks
    assert_equal <<CSS, render(<<SCSS)
.foo {
  a: b; }

@media screen {
  .foo {
    c: d; }
  .bar {
    e: f; } }
CSS
%base {
  a: b;
}

@media screen {
  .foo {
    c: d;
    @at-root (without: media) {
      @extend %base;
    }
  }

  .bar {e: f}
}
SCSS
  end

  ## Errors

  def test_keyframes_rule_outside_of_keyframes
    render <<SCSS
0% {
  top: 0; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "": expected selector, was "0%"', e.message
    assert_equal 1, e.sass_line
  end

  def test_selector_rule_in_keyframes
    render <<SCSS
@keyframes identifier {
  .foo {
    top: 0; } }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "": expected keyframes selector (e.g. 10%), was ".foo"', e.message
    assert_equal 2, e.sass_line
  end

  def test_nested_mixin_def_is_scoped
    render <<SCSS
foo {
  @mixin bar {a: b}}
bar {@include bar}
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal "Undefined mixin 'bar'.", e.message
    assert_equal 3, e.sass_line
  end

  def test_rules_beneath_properties
    render <<SCSS
foo {
  bar: {
    baz {
      bang: bop }}}
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Illegal nesting: Only properties may be nested beneath properties.', e.message
    assert_equal 3, e.sass_line
  end

  def test_uses_property_exception_with_star_hack
    render <<SCSS
foo {
  *bar:baz [fail]; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  *bar:baz ": expected ";", was "[fail]; }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_uses_property_exception_with_colon_hack
    render <<SCSS
foo {
  :bar:baz [fail]; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  :bar:baz ": expected ";", was "[fail]; }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_uses_rule_exception_with_dot_hack
    render <<SCSS
foo {
  .bar:baz <fail>; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  .bar:baz <fail>": expected expression (e.g. 1px, bold), was "; }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_uses_property_exception_with_space_after_name
    render <<SCSS
foo {
  bar: baz [fail]; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  bar: baz ": expected ";", was "[fail]; }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_uses_property_exception_with_non_identifier_after_name
    render <<SCSS
foo {
  bar:1px [fail]; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  bar:1px ": expected ";", was "[fail]; }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_uses_property_exception_when_followed_by_open_bracket
    render <<SCSS
foo {
  bar:{baz: .fail} }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  bar:{baz: ": expected expression (e.g. 1px, bold), was ".fail} }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_script_error
    render <<SCSS
foo {
  bar: "baz" * * }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "  bar: "baz" * ": expected expression (e.g. 1px, bold), was "* }"', e.message
    assert_equal 2, e.sass_line
  end

  def test_multiline_script_syntax_error
    render <<SCSS
foo {
  bar:
    "baz" * * }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "    "baz" * ": expected expression (e.g. 1px, bold), was "* }"', e.message
    assert_equal 3, e.sass_line
  end

  def test_multiline_script_runtime_error
    render <<SCSS
foo {
  bar: "baz" +
    "bar" +
    $bang }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal "Undefined variable: \"$bang\".", e.message
    assert_equal 4, e.sass_line
  end

  def test_post_multiline_script_runtime_error
    render <<SCSS
foo {
  bar: "baz" +
    "bar" +
    "baz";
  bip: $bop; }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal "Undefined variable: \"$bop\".", e.message
    assert_equal 5, e.sass_line
  end

  def test_multiline_property_runtime_error
    render <<SCSS
foo {
  bar: baz
    bar
    \#{$bang} }
SCSS
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal "Undefined variable: \"$bang\".", e.message
    assert_equal 4, e.sass_line
  end

  def test_post_resolution_selector_error
    render "\n\nfoo \#{\") bar\"} {a: b}"
    assert(false, "Expected syntax error")
  rescue Sass::SyntaxError => e
    assert_equal 'Invalid CSS after "foo ": expected selector, was ") bar"', e.message
    assert_equal 3, e.sass_line
  end

  def test_parent_in_mid_selector_error
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Invalid CSS after ".foo": expected "{", was "&.bar"

"&.bar" may only be used at the beginning of a compound selector.
MESSAGE
flim {
  .foo&.bar {a: b}
}
SCSS
  end

  def test_parent_after_selector_error
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Invalid CSS after ".foo.bar": expected "{", was "&"

"&" may only be used at the beginning of a compound selector.
MESSAGE
flim {
  .foo.bar& {a: b}
}
SCSS
  end

  def test_double_parent_selector_error
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Invalid CSS after "&": expected "{", was "&"

"&" may only be used at the beginning of a compound selector.
MESSAGE
flim {
  && {a: b}
}
SCSS
  end

  def test_no_lonely_else
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Invalid CSS: @else must come after @if
MESSAGE
@else {foo: bar}
SCSS
  end

  def test_failed_parent_selector_with_suffix
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid parent selector for "&-bar": "*"
MESSAGE
* {
  &-bar {a: b}
}
SCSS

    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid parent selector for "&-bar": "[foo=bar]"
MESSAGE
[foo=bar] {
  &-bar {a: b}
}
SCSS

    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid parent selector for "&-bar": "::nth-child(2n+1)"
MESSAGE
::nth-child(2n+1) {
  &-bar {a: b}
}
SCSS

    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid parent selector for "&-bar": ":not(.foo)"
MESSAGE
:not(.foo) {
  &-bar {a: b}
}
SCSS

    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid parent selector for "&-bar": ".foo +"
MESSAGE
.foo + {
  &-bar {a: b}
}
SCSS
  end

  def test_empty_media_query_error
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render(<<SCSS)}
Invalid CSS after "": expected media query list, was ""
MESSAGE
@media \#{""} {
  foo {a: b}
}
SCSS
  end

  def test_newline_in_property_value
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  bar: "bazbang"; }
CSS
.foo {
  $var: "baz\\
bang";
  bar: $var;
}
SCSS
  end

  def test_raw_newline_warning
    assert_warning(<<MESSAGE.rstrip) {assert_equal(<<CSS, render(<<SCSS))}
DEPRECATION WARNING on line 2, column 9 of #{filename_for_test :scss}:
Unescaped multiline strings are deprecated and will be removed in a future version of Sass.
To include a newline in a string, use "\\a" or "\\a " as in CSS.
MESSAGE
.foo {
  bar: "baz\\a bang"; }
CSS
.foo {
  $var: "baz
bang";
  bar: $var;
}
SCSS
  end

  # Regression

  # Regression test for #2031.
  def test_no_interpolation_warning_in_nested_selector
    assert_no_warning {assert_equal(<<CSS, render(<<SCSS))}
z a:b(n+1) {
  x: y; }
CSS
z {
  a:b(n+\#{1}) {
    x: y;
  }
}
SCSS
  end

  # Ensures that the fix for #2031 doesn't hide legitimate warnings.
  def test_interpolation_warning_in_selector_like_property
    assert_warning(<<WARNING) {assert_equal(<<CSS, render(<<SCSS))}
DEPRECATION WARNING on line 2 of #{filename_for_test :scss}:
\#{} interpolation near operators will be simplified in a future version of Sass.
To preserve the current behavior, use quotes:

  unquote("n+1")

You can use the sass-convert command to automatically fix most cases.
WARNING
z {
  a: b(n+1); }
CSS
z {
  a:b(n+\#{1});
}
SCSS
  end

  def test_escape_in_selector
    assert_equal(<<CSS, render(".\\!foo {a: b}"))
.\\!foo {
  a: b; }
CSS
  end

  def test_for_directive_with_float_bounds
    assert_equal(<<CSS, render(<<SCSS))
.a {
  b: 0;
  b: 1;
  b: 2;
  b: 3;
  b: 4;
  b: 5; }
CSS
.a {
  @for $i from 0.0 through 5.0 {b: $i}
}
SCSS

    assert_raise_message(Sass::SyntaxError, "0.5 is not an integer.") {render(<<SCSS)}
.a {
  @for $i from 0.5 through 5.0 {b: $i}
}
SCSS

    assert_raise_message(Sass::SyntaxError, "5.5 is not an integer.") {render(<<SCSS)}
.a {
  @for $i from 0.0 through 5.5 {b: $i}
}
SCSS
  end

  def test_parent_selector_in_function_pseudo_selector
    assert_equal <<CSS, render(<<SCSS)
.bar:not(.foo) {
  a: b; }

.qux:nth-child(2n of .baz .bang) {
  c: d; }
CSS
.foo {
  .bar:not(&) {a: b}
}

.baz .bang {
  .qux:nth-child(2n of &) {c: d}
}
SCSS
  end

  def test_parent_selector_in_and_out_of_function_pseudo_selector
    # Regression test for https://github.com/sass/sass/issues/1464#issuecomment-70352288
    assert_equal(<<CSS, render(<<SCSS))
.a:not(.a-b) {
  x: y; }
CSS
.a {
  &:not(&-b) {
    x: y;
  }
}
SCSS

    assert_equal(<<CSS, render(<<SCSS))
.a:nth-child(2n of .a-b) {
  x: y; }
CSS
.a {
  &:nth-child(2n of &-b) {
    x: y;
  }
}
SCSS
  end

  def test_attribute_selector_in_selector_pseudoclass
    # Even though this is plain CSS, it only failed when given to the SCSS
    # parser.
    assert_equal(<<CSS, render(<<SCSS))
[href^='http://'] {
  color: red; }
CSS
[href^='http://'] {
  color: red;
}
SCSS
  end

  def test_top_level_unknown_directive_in_at_root
    assert_equal(<<CSS, render(<<SCSS))
@fblthp {
  a: b; }
CSS
@at-root {
  @fblthp {a: b}
}
SCSS
  end

  def test_parent_ref_with_newline
    assert_equal(<<CSS, render(<<SCSS))
a.c
, b.c {
  x: y; }
CSS
a
, b {&.c {x: y}}
SCSS
  end

  def test_parent_ref_in_nested_at_root
    assert_equal(<<CSS, render(<<SCSS))
#test {
  border: 0; }
  #test:hover {
    display: none; }
CSS
a {
  @at-root #test {
    border: 0;
    &:hover{
      display: none;
    }
  }
}
SCSS
  end

  def test_loud_comment_in_compressed_mode
    assert_equal(<<CSS, render(<<SCSS))
/*! foo */
CSS
/*! foo */
SCSS
  end

  def test_parsing_decimals_followed_by_comments_doesnt_take_forever
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  padding: 4.21053% 4.21053% 5.63158%; }
CSS
.foo {
  padding: 4.21052631578947% 4.21052631578947% 5.631578947368421% /**/
}
SCSS
  end

  def test_parsing_many_numbers_doesnt_take_forever
    values = ["80% 90%"] * 1000
    assert_equal(<<CSS, render(<<SCSS))
.foo {
  padding: #{values.join(', ')}; }
CSS
.foo {
  padding: #{values.join(', ')};
}
SCSS
  end

  def test_import_comments_in_imports
    assert_equal(<<CSS, render(<<SCSS))
@import url(foo.css);
@import url(bar.css);
@import url(baz.css);
CSS
@import "foo.css", // this is a comment
        "bar.css", /* this is another comment */
        "baz.css"; // this is a third comment
SCSS
  end

  def test_reference_combinator_with_parent_ref
    silence_warnings {assert_equal <<CSS, render(<<SCSS)}
a /foo/ b {
  c: d; }
CSS
a {& /foo/ b {c: d}}
SCSS
  end

  def test_reference_combinator_warning
    assert_warning(<<WARNING) {assert_equal <<CSS, render(<<SCSS)}
DEPRECATION WARNING on line 1, column 8 of test_reference_combinator_warning_inline.scss:
The reference combinator /foo/ is deprecated and will be removed in a future release.
WARNING
a /foo/ b {
  c: d; }
CSS
a {& /foo/ b {c: d}}
SCSS
  end

  def test_newline_selector_rendered_multiple_times
    assert_equal <<CSS, render(<<SCSS)
form input,
form select {
  color: white; }

form input,
form select {
  color: white; }
CSS
@for $i from 1 through 2 {
  form {
    input,
    select {
      color: white;
    }
  }
}
SCSS
  end

  def test_prop_name_interpolation_after_hyphen
    assert_equal <<CSS, render(<<SCSS)
a {
  -foo-bar: b; }
CSS
a { -\#{"foo"}-bar: b; }
SCSS
  end

  def test_star_plus_and_parent
    assert_equal <<CSS, render(<<SCSS)
* + html foo {
  a: b; }
CSS
foo {*+html & {a: b}}
SCSS
  end

  def test_weird_added_space
    assert_equal <<CSS, render(<<SCSS)
foo {
  bar: -moz-bip; }
CSS
$value : bip;

foo {
  bar: -moz-\#{$value};
}
SCSS
  end

  def test_interpolation_with_bracket_on_next_line
    assert_equal <<CSS, render(<<SCSS)
a.foo b {
  color: red; }
CSS
a.\#{"foo"} b
{color: red}
SCSS
  end

  def test_extra_comma_in_mixin_arglist_error
    assert_raise_message(Sass::SyntaxError, <<MESSAGE.rstrip) {render <<SCSS}
Invalid CSS after "...clude foo(bar, ": expected mixin argument, was ");"
MESSAGE
@mixin foo($a1, $a2) {
  baz: $a1 $a2;
}

.bar {
  @include foo(bar, );
}
SCSS
  end

  def test_interpolation
    assert_equal <<CSS, render(<<SCSS)
ul li#foo a span.label {
  foo: bar; }
CSS
$bar : "#foo";
ul li\#{$bar} a span.label { foo: bar; }
SCSS
  end

  def test_mixin_with_keyword_args
    assert_equal <<CSS, render(<<SCSS)
.mixed {
  required: foo;
  arg1: default-val1;
  arg2: non-default-val2; }
CSS
@mixin a-mixin($required, $arg1: default-val1, $arg2: default-val2) {
  required: $required;
  arg1: $arg1;
  arg2: $arg2;
}
.mixed { @include a-mixin(foo, $arg2: non-default-val2); }
SCSS
  end

  def test_keyword_arg_scope
    assert_equal <<CSS, render(<<SCSS)
.mixed {
  arg1: default;
  arg2: non-default; }
CSS
$arg1: default;
$arg2: default;
@mixin a-mixin($arg1: $arg1, $arg2: $arg2) {
  arg1: $arg1;
  arg2: $arg2;
}
.mixed { @include a-mixin($arg2: non-default); }
SCSS
  end

  def test_passing_required_args_as_a_keyword_arg
    assert_equal <<CSS, render(<<SCSS)
.mixed {
  required: foo;
  arg1: default-val1;
  arg2: default-val2; }
CSS
@mixin a-mixin($required, $arg1: default-val1, $arg2: default-val2) {
  required: $required;
  arg1: $arg1;
  arg2: $arg2; }
.mixed { @include a-mixin($required: foo); }
SCSS
  end

  def test_passing_all_as_keyword_args_in_opposite_order
    assert_equal <<CSS, render(<<SCSS)
.mixed {
  required: foo;
  arg1: non-default-val1;
  arg2: non-default-val2; }
CSS
@mixin a-mixin($required, $arg1: default-val1, $arg2: default-val2) {
  required: $required;
  arg1: $arg1;
  arg2: $arg2; }
.mixed { @include a-mixin($arg2: non-default-val2, $arg1: non-default-val1, $required: foo); }
SCSS
  end

  def test_keyword_args_in_functions
    assert_equal <<CSS, render(<<SCSS)
.keyed {
  color: rgba(170, 119, 204, 0.4); }
CSS
.keyed { color: rgba($color: #a7c, $alpha: 0.4) }
SCSS
  end

  def test_unknown_keyword_arg_raises_error
    assert_raise_message(Sass::SyntaxError, "Mixin a doesn't have an argument named $c.") {render <<SCSS}
@mixin a($b: 1) { a: $b; }
div { @include a(1, $c: 3); }
SCSS
  end


  def test_newlines_removed_from_selectors_when_compressed
    assert_equal <<CSS, render(<<SCSS, :style => :compressed)
z a,z b{display:block}
CSS
a
, b {
  z & {
    display: block;
  }
}
SCSS
  end

  def test_if_error_line
    assert_raise_line(2) {render(<<SCSS)}
@if true {foo: bar}
}
SCSS
  end

  def test_multiline_var
    assert_equal <<CSS, render(<<SCSS)
foo {
  a: 3;
  b: false;
  c: a b c; }
CSS
foo {
  $var1: 1 +
    2;
  $var2: true and
    false;
  $var3: a b
    c;
  a: $var1;
  b: $var2;
  c: $var3; }
SCSS
  end

  def test_mixin_content
    assert_equal <<CSS, render(<<SASS)
.parent {
  background-color: red;
  border-color: red; }
  .parent .child {
    background-color: yellow;
    color: blue;
    border-color: yellow; }
CSS
$color: blue;
@mixin context($class, $color: red) {
  .\#{$class} {
    background-color: $color;
    @content;
    border-color: $color;
  }
}
@include context(parent) {
  @include context(child, $color: yellow) {
    color: $color;
  }
}
SASS
  end

  def test_empty_content
    assert_equal <<CSS, render(<<SCSS)
a {
  b: c; }
CSS
@mixin foo { @content }
a { b: c; @include foo {} }
SCSS
  end

  def test_options_passed_to_script
    assert_equal <<CSS, render(<<SCSS, :style => :compressed)
foo{color:#000}
CSS
foo {color: darken(black, 10%)}
SCSS
  end

  # ref: https://github.com/nex3/sass/issues/104
  def test_no_buffer_overflow
    template = render <<SCSS
.aaa {
  background-color: white;
}
.aaa .aaa .aaa {
  background-color: black;
}
.bbb {
  @extend .aaa;
}
.xxx {
  @extend .bbb;
}
.yyy {
  @extend .bbb;
}
.zzz {
  @extend .bbb;
}
SCSS
    Sass::SCSS::Parser.new(template, "test.scss", nil).parse
  end

  def test_extend_in_media_in_rule
    assert_equal(<<CSS, render(<<SCSS))
@media screen {
  .foo {
    a: b; } }
CSS
.foo {
  @media screen {
    @extend %bar;
  }
}

@media screen {
  %bar {
    a: b;
  }
}
SCSS
  end

  def test_import_with_supports_clause_interp
    assert_equal(<<CSS, render(<<'SASS', :style => :compressed))
@import url("fallback-layout.css") supports(not (display: flex))
CSS
$display-type: flex;
@import url("fallback-layout.css") supports(not (display: #{$display-type}));
SASS
  end

  def test_import_with_supports_clause
    assert_equal(<<CSS, render(<<SASS, :style => :compressed))
@import url("fallback-layout.css") supports(not (display: flex));.foo{bar:baz}
CSS
@import url("fallback-layout.css") supports(not (display: flex));
.foo { bar: baz; }
SASS
  end

  def test_crlf
    # Attempt to reproduce https://github.com/sass/sass/issues/1985
    assert_equal(<<CSS, render(<<SCSS))
p {
  margin: 0; }
CSS
p {\r\n   margin: 0;\r\n}
SCSS
  end

end
