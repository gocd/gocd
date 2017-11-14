require "helper"

class IntegrationTestAdHoc < Loofah::TestCase

  context "blank input string" do
    context "fragment" do
      it "return a blank string" do
        assert_equal "", Loofah.scrub_fragment("", :prune).to_s
      end
    end

    context "document" do
      it "return a blank string" do
        assert_equal "", Loofah.scrub_document("", :prune).root.to_s
      end
    end
  end

  def test_removal_of_illegal_tag
    html = <<-HTML
      following this there should be no jim tag
      <jim>jim</jim>
      was there?
    HTML
    sane = Nokogiri::HTML(Loofah.scrub_fragment(html, :escape).to_xml)
    assert sane.xpath("//jim").empty?
  end

  def test_removal_of_illegal_attribute
    html = "<p class=bar foo=bar abbr=bar />"
    sane = Nokogiri::HTML(Loofah.scrub_fragment(html, :escape).to_xml)
    node = sane.xpath("//p").first
    assert node.attributes['class']
    assert node.attributes['abbr']
    assert_nil node.attributes['foo']
  end

  def test_removal_of_illegal_url_in_href
    html = <<-HTML
      <a href='jimbo://jim.jim/'>this link should have its href removed because of illegal url</a>
      <a href='http://jim.jim/'>this link should be fine</a>
    HTML
    sane = Nokogiri::HTML(Loofah.scrub_fragment(html, :escape).to_xml)
    nodes = sane.xpath("//a")
    assert_nil nodes.first.attributes['href']
    assert nodes.last.attributes['href']
  end

  def test_css_sanitization
    html = "<p style='background-color: url(\"http://foo.com/\") ; background-color: #000 ;' />"
    sane = Nokogiri::HTML(Loofah.scrub_fragment(html, :escape).to_xml)
    assert_match %r/#000/,    sane.inner_html
    refute_match %r/foo\.com/, sane.inner_html
  end

  def test_fragment_with_no_tags
    assert_equal "This fragment has no tags.", Loofah.scrub_fragment("This fragment has no tags.", :escape).to_xml
  end

  def test_fragment_in_p_tag
    assert_equal "<p>This fragment is in a p.</p>", Loofah.scrub_fragment("<p>This fragment is in a p.</p>", :escape).to_xml
  end

  def test_fragment_in_p_tag_plus_stuff
    assert_equal "<p>This fragment is in a p.</p>foo<strong>bar</strong>", Loofah.scrub_fragment("<p>This fragment is in a p.</p>foo<strong>bar</strong>", :escape).to_xml
  end

  def test_fragment_with_text_nodes_leading_and_trailing
    assert_equal "text<p>fragment</p>text", Loofah.scrub_fragment("text<p>fragment</p>text", :escape).to_xml
  end

  def test_whitewash_on_fragment
    html = "safe<frameset rows=\"*\"><frame src=\"http://example.com\"></frameset> <b>description</b>"
    whitewashed = Loofah.scrub_document(html, :whitewash).xpath("/html/body/*").to_s
    assert_equal "<p>safe</p><b>description</b>", whitewashed.gsub("\n","")
  end

  MSWORD_HTML = <<-EOHTML
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"><meta name="ProgId" content="Word.Document"><meta name="Generator" content="Microsoft Word 11"><meta name="Originator" content="Microsoft Word 11"><link rel="File-List" href="file:///C:%5CDOCUME%7E1%5CNICOLE%7E1%5CLOCALS%7E1%5CTemp%5Cmsohtml1%5C01%5Cclip_filelist.xml"><!--[if gte mso 9]><xml>
<w:WordDocument>
 <w:View>Normal</w:View>
 <w:Zoom>0</w:Zoom>
 <w:PunctuationKerning/>
 <w:ValidateAgainstSchemas/>
 <w:SaveIfXMLInvalid>false</w:SaveIfXMLInvalid>
 <w:IgnoreMixedContent>false</w:IgnoreMixedContent>
 <w:AlwaysShowPlaceholderText>false</w:AlwaysShowPlaceholderText>
 <w:Compatibility>
  <w:BreakWrappedTables/>
  <w:SnapToGridInCell/>
  <w:WrapTextWithPunct/>
  <w:UseAsianBreakRules/>
  <w:DontGrowAutofit/>
 </w:Compatibility>
 <w:BrowserLevel>MicrosoftInternetExplorer4</w:BrowserLevel>
</w:WordDocument>
</xml><![endif]--><!--[if gte mso 9]><xml>
<w:LatentStyles DefLockedState="false" LatentStyleCount="156">
</w:LatentStyles>
</xml><![endif]--><style>
<!--
/* Style Definitions */
p.MsoNormal, li.MsoNormal, div.MsoNormal
{mso-style-parent:"";
margin:0in;
margin-bottom:.0001pt;
mso-pagination:widow-orphan;
font-size:12.0pt;
font-family:"Times New Roman";
mso-fareast-font-family:"Times New Roman";}
@page Section1
{size:8.5in 11.0in;
margin:1.0in 1.25in 1.0in 1.25in;
mso-header-margin:.5in;
mso-footer-margin:.5in;
mso-paper-source:0;}
div.Section1
{page:Section1;}
-->
</style><!--[if gte mso 10]>
<style>
/* Style Definitions */
table.MsoNormalTable
{mso-style-name:"Table Normal";
mso-tstyle-rowband-size:0;
mso-tstyle-colband-size:0;
mso-style-noshow:yes;
mso-style-parent:"";
mso-padding-alt:0in 5.4pt 0in 5.4pt;
mso-para-margin:0in;
mso-para-margin-bottom:.0001pt;
mso-pagination:widow-orphan;
font-size:10.0pt;
font-family:"Times New Roman";
mso-ansi-language:#0400;
mso-fareast-language:#0400;
mso-bidi-language:#0400;}
</style>
<![endif]-->

<p class="MsoNormal">Foo <b style="">BOLD<o:p></o:p></b></p>
  EOHTML

  def test_fragment_whitewash_on_microsofty_markup
    whitewashed = Loofah.fragment(MSWORD_HTML).scrub!(:whitewash)
    assert_equal "<p>Foo <b>BOLD</b></p>", whitewashed.to_s.strip
  end

  def test_document_whitewash_on_microsofty_markup
    whitewashed = Loofah.document(MSWORD_HTML).scrub!(:whitewash)
    assert_match %r(<p>Foo <b>BOLD</b></p>), whitewashed.to_s
    assert_equal "<p>Foo <b>BOLD</b></p>",   whitewashed.xpath("/html/body/*").to_s
  end

  def test_return_empty_string_when_nothing_left
    assert_equal "", Loofah.scrub_document('<script>test</script>', :prune).text
  end

  def test_removal_of_all_tags
    html = <<-HTML
      What's up <strong>doc</strong>?
    HTML
    stripped = Loofah.scrub_document(html, :prune).text
    assert_equal %Q(What\'s up doc?).strip, stripped.strip
  end

  def test_dont_remove_whitespace
    html = "Foo\nBar"
    assert_equal html, Loofah.scrub_document(html, :prune).text
  end

  def test_dont_remove_whitespace_between_tags
    html = "<p>Foo</p>\n<p>Bar</p>"
    assert_equal "Foo\nBar", Loofah.scrub_document(html, :prune).text
  end
end
