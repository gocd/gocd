require 'test_helper'
require 'tilt'

begin
  require 'tilt/coffee'

  class CoffeeScriptTemplateTest < Minitest::Test

    test "is registered for '.coffee' files" do
      assert_equal Tilt::CoffeeScriptTemplate, Tilt['test.coffee']
    end

    test "bare is disabled by default" do
      assert_equal false, Tilt::CoffeeScriptTemplate.default_bare
    end

    test "compiles and evaluates the template on #render" do
      template = Tilt::CoffeeScriptTemplate.new { |t| "puts 'Hello, World!'\n" }
      assert_match "puts('Hello, World!');", template.render
    end

    test "can be rendered more than once" do
      template = Tilt::CoffeeScriptTemplate.new { |t| "puts 'Hello, World!'\n" }
      3.times { assert_match "puts('Hello, World!');", template.render }
    end

    test "disabling coffee-script wrapper" do
      str = 'name = "Josh"; puts "Hello #{name}"'

      template = Tilt::CoffeeScriptTemplate.new { str }
      assert_match "(function() {", template.render
      assert_match "puts(\"Hello \" + name);\n", template.render

      template = Tilt::CoffeeScriptTemplate.new(:bare => true) { str }
      refute_match "(function() {", template.render
      assert_equal "var name;\n\nname = \"Josh\";\n\nputs(\"Hello \" + name);\n", template.render

      template2 = Tilt::CoffeeScriptTemplate.new(:no_wrap => true) { str}
      refute_match "(function() {", template.render
      assert_equal "var name;\n\nname = \"Josh\";\n\nputs(\"Hello \" + name);\n", template.render
    end

    context "wrapper globally enabled" do
      setup do
        @bare = Tilt::CoffeeScriptTemplate.default_bare
        Tilt::CoffeeScriptTemplate.default_bare = false
      end

      teardown do
        Tilt::CoffeeScriptTemplate.default_bare = @bare
      end

      test "no options" do
        template = Tilt::CoffeeScriptTemplate.new { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        assert_match "(function() {", template.render
      end

      test "overridden by :bare" do
        template = Tilt::CoffeeScriptTemplate.new(:bare => true) { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        refute_match "(function() {", template.render
      end

      test "overridden by :no_wrap" do
        template = Tilt::CoffeeScriptTemplate.new(:no_wrap => true) { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        refute_match "(function() {", template.render
      end
    end

    context "wrapper globally disabled" do
      setup do
        @bare = Tilt::CoffeeScriptTemplate.default_bare
        Tilt::CoffeeScriptTemplate.default_bare = true
      end

      teardown do
        Tilt::CoffeeScriptTemplate.default_bare = @bare
      end

      test "no options" do
        template = Tilt::CoffeeScriptTemplate.new { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        refute_match "(function() {", template.render
      end

      test "overridden by :bare" do
        template = Tilt::CoffeeScriptTemplate.new(:bare => false) { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        assert_match "(function() {", template.render
      end

      test "overridden by :no_wrap" do
        template = Tilt::CoffeeScriptTemplate.new(:no_wrap => false) { |t| 'name = "Josh"; puts "Hello, #{name}"' }
        assert_match "puts(\"Hello, \" + name);", template.render
        assert_match "(function() {", template.render
      end
    end
  end

rescue LoadError => boom
  warn "Tilt::CoffeeScriptTemplate (disabled)"
end
