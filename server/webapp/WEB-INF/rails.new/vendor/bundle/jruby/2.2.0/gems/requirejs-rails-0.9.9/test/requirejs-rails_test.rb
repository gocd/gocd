require 'test_helper'

require 'execjs'
require 'pathname'

class RequirejsRailsTest < ActiveSupport::TestCase
  test "truth" do
    assert_kind_of Module, Requirejs
    assert_kind_of Module, Requirejs::Rails
    assert_kind_of Class, Requirejs::Rails::Engine
  end

  test "require.js version" do
    require_js = Pathname.new(__FILE__+'/../../vendor/assets/javascripts/require.js').cleanpath.read
    context = ExecJS.compile(require_js)
    assert_equal Requirejs::Rails::LibVersion, context.eval("require.version")
  end

  test "CHANGELOG up to date" do
    changelog_match = (/^### v#{Requirejs::Rails::Version}/ =~ Pathname.new(__FILE__+'/../../CHANGELOG.md').cleanpath.read)
    assert changelog_match, "CHANGELOG has no section for v#{Requirejs::Rails::Version}"
  end
end

class RequirejsRailsConfigTest < ActiveSupport::TestCase
  def setup
    @cfg = Requirejs::Rails::Config.new(Rails.application)
  end

  def asset_allowed?(asset_path)
    !!@cfg.logical_path_patterns.find do |logical_path_pattern|
      logical_path_pattern.match(asset_path)
    end
  end

  test "config accepts known loaders" do
    @cfg.loader = :almond
    assert_equal :almond, @cfg.loader
  end

  test "config rejects bad loaders" do
    assert_raises Requirejs::ConfigError do
      @cfg.loader = :wombat
    end
  end

  test "matches configured logical assets" do
    assert_equal true, asset_allowed?("foo.js")
    assert_equal false, asset_allowed?("bar.frobnitz")
    @cfg.logical_path_patterns.push(Regexp.new("\\.frobnitz\\z"))
    assert_equal true, asset_allowed?("bar.frobnitz")
  end

  test "should have a default empty user_config" do
    assert_kind_of Hash, @cfg.user_config
  end

  test "user_config should reject baseUrl" do
    exc = assert_raises Requirejs::ConfigError do
      @cfg.user_config = {'baseUrl' => '/frobnitz'}
    end
    assert_match /baseUrl is not needed/, exc.message
  end

  test "run_config should inherit user_config settings" do
    @cfg.user_config = {'paths' => {'jquery' => 'lib/jquery-1.7.2.min'}}
    refute_nil @cfg.run_config['paths']
    assert_kind_of Hash, @cfg.run_config['paths']
    assert_equal 'lib/jquery-1.7.2.min', @cfg.run_config['paths']['jquery']
  end

  test "run_config should allow settings to be overridden" do
    @cfg.run_config['baseUrl'] = 'http://cdn.example.com/assets'
    assert_equal 'http://cdn.example.com/assets', @cfg.run_config['baseUrl']
  end

  test "build_config should inherit user_config settings" do
    @cfg.user_config = {'paths' => {'jquery' => 'lib/jquery-1.7.2.min'}}
    refute_nil @cfg.build_config['paths']
    assert_kind_of Hash, @cfg.build_config['paths']
    assert_equal 'lib/jquery-1.7.2.min', @cfg.build_config['paths']['jquery']
  end

  test "run_config should reject irrelevant settings" do
    @cfg.user_config = {'optimize' => 'none'}
    assert_nil @cfg.run_config['optimize']
  end

  test "build_config should reject irrelevant settings" do
    @cfg.user_config = {'priority' => %w{ foo bar baz }}
    assert_nil @cfg.build_config['priority']
  end

  ## Almond tests
  test "build_config with almond should accept one module" do
    @cfg.loader = :almond
    @cfg.user_config = {'modules' => [{'name' => 'foo'}]}
    assert_match 'almond', @cfg.build_config['modules'][0]['name']
  end

  test "build_config with almond must reject more than one module" do
    @cfg.loader = :almond
    @cfg.user_config = {'modules' => [{'name' => 'foo'}, {'name' => 'bar'}]}
    exc = assert_raises Requirejs::ConfigError do
      @cfg.build_config
    end
    assert_match /requires exactly one module/, exc.message
  end
end

class RequirejsHelperTest < ActionView::TestCase
  def setup
    controller.requirejs_included = false
    Rails.application.config.requirejs.user_config = {}
    Rails.application.config.requirejs.delete(:run_config)
    Rails.application.config.requirejs.delete(:build_config)
  end

  def with_cdn(protocol_relative = false)
    Rails.application.config.requirejs.user_config = {
        "paths" => {
            "jquery" => "#{ protocol_relative ? '' : 'http:' }//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"
        }
    }
  end

  def wrap(tag)
    "<html><head>#{tag}</head></html>"
  end

  test "requirejs_include_tag" do
    render text: wrap(requirejs_include_tag)

    assert_select "script:first-of-type[src=\"/javascripts/require.js\"]", count: 1
    assert_select "script:last-of-type", text: "require.config({\"baseUrl\":\"/assets\"});"
  end

  test "requirejs_include_tag_with_param" do
    render text: wrap(requirejs_include_tag("application"))

    assert_select "script:first-of-type[src=\"/javascripts/require.js\"]", count: 1
  end

  test "requirejs_include_tag_with_block" do
    result = wrap(
        requirejs_include_tag("application") do
          {"class" => controller.class.name.demodulize}
        end
    )

    render text: result

    assert_select "script:first-of-type[src=\"/javascripts/require.js\"]" \
      "[data-class=\"TestController\"]", count: 1
  end

  test "requirejs_include_tag can appear only once" do
    assert_raises Requirejs::MultipleIncludeError do
      requirejs_include_tag
      requirejs_include_tag
    end
  end

  test "requirejs_include_tag with digestified asset paths" do
    begin
      Rails.application.config.requirejs.user_config = {
          "modules" => [
              {"name" => "foo"}
          ]
      }

      saved_digest = Rails.application.config.assets.digest
      Rails.application.config.assets.digest = true

      render text: wrap(requirejs_include_tag)

      assert_select "script:last-of-type",
                    text: Regexp.new("\\Arequire\\.config\\({.*\"paths\":{.*\"foo\":\"/javascripts/foo\".*}.*}\\);\\z")
    ensure
      Rails.application.config.assets.digest = saved_digest
    end
  end

  test "requirejs_include_tag with CDN asset in paths" do
    with_cdn

    render text: wrap(requirejs_include_tag)

    assert_select "script:last-of-type",
                  text: Regexp.new("\\Arequire\\.config\\({.*\"paths\":{.*\"http://ajax\\..*\".*}.*}\\);\\z")
  end

  test "requirejs_include_tag with CDN asset and digestified asset paths" do
    begin
      with_cdn

      saved_digest = Rails.application.config.assets.digest
      Rails.application.config.assets.digest = true

      render text: wrap(requirejs_include_tag)

      assert_select "script:last-of-type",
                    text: Regexp.new("\\Arequire\\.config\\({.*\"paths\":{.*\"http://ajax\\..*\".*}.*}\\);\\z")
    ensure
      Rails.application.config.assets.digest = saved_digest
    end
  end

  test "requirejs_include_tag with protocol relative CDN asset in paths" do
    with_cdn(true)

    render text: wrap(requirejs_include_tag)

    assert_select "script:last-of-type",
                  text: Regexp.new("\\Arequire\\.config\\({.*\"paths\":{.*\"//ajax\\..*\".*}.*}\\);\\z")
  end
end
