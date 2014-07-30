require 'test_helper'
require 'rails/generators/coffee/assets/assets_generator'

class AssetGeneratorTest < Rails::Generators::TestCase
  tests Coffee::Generators::AssetsGenerator

  destination File.expand_path("../tmp", __FILE__)
  setup :prepare_destination

  def test_assets
    run_generator %w(posts)
    assert_no_file "app/assets/javascripts/posts.js"
    assert_file "app/assets/javascripts/posts.js.coffee"
  end
end
