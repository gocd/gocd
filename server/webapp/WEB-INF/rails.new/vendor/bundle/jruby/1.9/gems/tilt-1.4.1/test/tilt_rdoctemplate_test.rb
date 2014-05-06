require 'contest'
require 'tilt'

begin
  require 'rdoc'
  require 'rdoc/markup'
  require 'rdoc/markup/to_html'
  class RDocTemplateTest < Test::Unit::TestCase
    test "is registered for '.rdoc' files" do
      assert_equal Tilt::RDocTemplate, Tilt['test.rdoc']
    end

    test "preparing and evaluating the template with #render" do
      template = Tilt::RDocTemplate.new { |t| "= Hello World!" }
      result = template.render.strip
      assert_match /<h1/, result
      assert_match />Hello World!</, result
    end

    test "can be rendered more than once" do
      template = Tilt::RDocTemplate.new { |t| "= Hello World!" }
      3.times do
        result = template.render.strip
        assert_match /<h1/, result
        assert_match />Hello World!</, result
      end
    end
  end
rescue LoadError => boom
  warn "Tilt::RDocTemplate (disabled) [#{boom}]"
end
