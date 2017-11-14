require "webpacker_test_helper"

class HelperTest < ActionView::TestCase
  def setup
    @view = ActionView::Base.new
    @view.extend Webpacker::Helper
  end

  def test_asset_pack_path
    assert_equal @view.asset_pack_path("bootstrap.js"), "/packs/bootstrap-300631c4f0e0f9c865bc.js"
    assert_equal @view.asset_pack_path("bootstrap.css"), "/packs/bootstrap-c38deda30895059837cf.css"
  end

  def test_javascript_pack_tag
    assert_equal \
      %(<script src="/packs/bootstrap-300631c4f0e0f9c865bc.js"></script>),
      @view.javascript_pack_tag("bootstrap.js")
  end

  def test_javascript_pack_tag_splat
    assert_equal \
    %(<script src="/packs/bootstrap-300631c4f0e0f9c865bc.js" defer="defer"></script>\n) +
        %(<script src="/packs/application-k344a6d59eef8632c9d1.js" defer="defer"></script>),
      @view.javascript_pack_tag("bootstrap.js", "application.js", defer: true)
  end

  def test_stylesheet_pack_tag
    assert_equal \
      %(<link rel="stylesheet" media="screen" href="/packs/bootstrap-c38deda30895059837cf.css" />),
      @view.stylesheet_pack_tag("bootstrap.css")
  end

  def test_stylesheet_pack_tag_splat
    assert_equal \
      %(<link rel="stylesheet" media="all" href="/packs/bootstrap-c38deda30895059837cf.css" />\n) +
        %(<link rel="stylesheet" media="all" href="/packs/application-dd6b1cd38bfa093df600.css" />),
      @view.stylesheet_pack_tag("bootstrap.css", "application.css", media: "all")
  end
end
