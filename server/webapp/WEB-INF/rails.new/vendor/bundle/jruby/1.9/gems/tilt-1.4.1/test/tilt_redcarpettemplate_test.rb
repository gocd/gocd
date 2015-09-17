require 'contest'
require 'tilt'

begin
  require 'redcarpet'

  class RedcarpetTemplateTest < Test::Unit::TestCase
    test "registered for '.md' files" do
      assert Tilt.mappings['md'].include?(Tilt::RedcarpetTemplate)
    end

    test "registered for '.mkd' files" do
      assert Tilt.mappings['mkd'].include?(Tilt::RedcarpetTemplate)
    end

    test "registered for '.markdown' files" do
      assert Tilt.mappings['markdown'].include?(Tilt::RedcarpetTemplate)
    end

    test "registered above BlueCloth" do
      %w[md mkd markdown].each do |ext|
        mappings = Tilt.mappings[ext]
        blue_idx = mappings.index(Tilt::BlueClothTemplate)
        redc_idx = mappings.index(Tilt::RedcarpetTemplate)
        assert redc_idx < blue_idx,
          "#{redc_idx} should be lower than #{blue_idx}"
      end
    end

    test "registered above RDiscount" do
      %w[md mkd markdown].each do |ext|
        mappings = Tilt.mappings[ext]
        rdis_idx = mappings.index(Tilt::RDiscountTemplate)
        redc_idx = mappings.index(Tilt::RedcarpetTemplate)
        assert redc_idx < rdis_idx,
          "#{redc_idx} should be lower than #{rdis_idx}"
      end
    end

    test "redcarpet2 is our default choice" do
      template = Tilt::RedcarpetTemplate.new {}
      assert_equal Tilt::RedcarpetTemplate::Redcarpet2, template.prepare.class
    end

    test "preparing and evaluating templates on #render" do
      template = Tilt::RedcarpetTemplate.new { |t| "# Hello World!" }
      assert_equal "<h1>Hello World!</h1>\n", template.render
    end

    test "can be rendered more than once" do
      template = Tilt::RedcarpetTemplate.new { |t| "# Hello World!" }
      3.times { assert_equal "<h1>Hello World!</h1>\n", template.render }
    end

    test "smartypants when :smart is set" do
      template = Tilt::RedcarpetTemplate.new(:smartypants => true) { |t|
        "OKAY -- 'Smarty Pants'" }
      assert_match /<p>OKAY &ndash; &#39;Smarty Pants&#39;<\/p>/,
        template.render
    end

    test "smartypants with a rendererer instance" do
      template = Tilt::RedcarpetTemplate.new(:renderer => Redcarpet::Render::HTML.new(:hard_wrap => true), :smartypants => true) { |t|
        "OKAY -- 'Smarty Pants'" }
      assert_match /<p>OKAY &ndash; &#39;Smarty Pants&#39;<\/p>/,
        template.render
    end
  end
rescue LoadError => boom
  warn "Tilt::RedcarpetTemplate (disabled)"
end
