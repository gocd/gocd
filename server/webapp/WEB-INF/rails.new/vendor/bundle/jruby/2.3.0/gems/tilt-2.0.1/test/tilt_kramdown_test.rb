require 'test_helper'
require 'tilt'

begin
  require 'tilt/kramdown'

  class KramdownTemplateTest < Minitest::Test
    test "preparing and evaluating templates on #render" do
      template = Tilt::KramdownTemplate.new { |t| "# Hello World!" }
      assert_equal '<h1 id="hello-world">Hello World!</h1>', template.render.strip
    end

    test "can be rendered more than once" do
      template = Tilt::KramdownTemplate.new { |t| "# Hello World!" }
      3.times { assert_equal '<h1 id="hello-world">Hello World!</h1>', template.render.strip }
    end
  end
rescue LoadError => boom
  warn "Tilt::KramdownTemplate (disabled)"
end
