require 'abstract_unit'

class DocumentTest < Test::Unit::TestCase
  def test_handle_doctype
    doc = nil
    assert_nothing_raised do
      doc = HTML::Document.new <<-HTML.strip
        <!DOCTYPE "blah" "blah" "blah">
        <html>
        </html>
      HTML
    end
    assert_equal 3, doc.root.children.length
    assert_equal %{<!DOCTYPE "blah" "blah" "blah">}, doc.root.children[0].content
    assert_match %r{\s+}m, doc.root.children[1].content
    assert_equal "html", doc.root.children[2].name
  end
  
  def test_find_img
    doc = HTML::Document.new <<-HTML.strip
      <html>
        <body>
          <p><img src="hello.gif"></p>
        </body>
      </html>
    HTML
    assert doc.find(:tag=>"img", :attributes=>{"src"=>"hello.gif"})
  end

  def test_find_all
    doc = HTML::Document.new <<-HTML.strip
      <html>
        <body>
          <p class="test"><img src="hello.gif"></p>
          <div class="foo">
            <p class="test">something</p>
            <p>here is <em class="test">more</em></p>
          </div>
        </body>
      </html>
    HTML
    all = doc.find_all :attributes => { :class => "test" }
    assert_equal 3, all.length
    assert_equal [ "p", "p", "em" ], all.map { |n| n.name }
  end

  def test_find_with_text
    doc = HTML::Document.new <<-HTML.strip
      <html>
        <body>
          <p>Some text</p>
        </body>
      </html>
    HTML
    assert doc.find(:content => "Some text")
    assert doc.find(:tag => "p", :child => { :content => "Some text" })
    assert doc.find(:tag => "p", :child => "Some text")
    assert doc.find(:tag => "p", :content => "Some text")
  end

  def test_parse_xml
    assert_nothing_raised { HTML::Document.new("<tags><tag/></tags>", true, true) }
    assert_nothing_raised { HTML::Document.new("<outer><link>something</link></outer>", true, true) }
  end

  def test_parse_document
    doc = HTML::Document.new(<<-HTML)
      <div>
        <h2>blah</h2>
        <table>
        </table>
      </div>
    HTML
    assert_not_nil doc.find(:tag => "div", :children => { :count => 1, :only => { :tag => "table" } })
  end

  def test_tag_nesting_nothing_to_s
    doc = HTML::Document.new("<tag></tag>")
    assert_equal "<tag></tag>", doc.root.to_s
  end

  def test_tag_nesting_space_to_s
    doc = HTML::Document.new("<tag> </tag>")
    assert_equal "<tag> </tag>", doc.root.to_s
  end

  def test_tag_nesting_text_to_s
    doc = HTML::Document.new("<tag>text</tag>")
    assert_equal "<tag>text</tag>", doc.root.to_s
  end

  def test_tag_nesting_tag_to_s
    doc = HTML::Document.new("<tag><nested /></tag>")
    assert_equal "<tag><nested /></tag>", doc.root.to_s
  end

  def test_parse_cdata
    doc = HTML::Document.new(<<-HTML)
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">
  <head>
    <title><![CDATA[<br>]]></title>
   </head>
  <body>
    <p>this document has &lt;br&gt; for a title</p>
  </body>
</html>
HTML

    assert_nil doc.find(:tag => "title", :descendant => { :tag => "br" })
    assert doc.find(:tag => "title", :child => "<br>")
  end

  def test_find_empty_tag
    doc = HTML::Document.new("<div id='map'></div>")
    assert_nil doc.find(:tag => "div", :attributes => { :id => "map" }, :content => /./)
    assert doc.find(:tag => "div", :attributes => { :id => "map" }, :content => /\A\Z/)
    assert doc.find(:tag => "div", :attributes => { :id => "map" }, :content => /^$/)
    assert doc.find(:tag => "div", :attributes => { :id => "map" }, :content => "")
    assert doc.find(:tag => "div", :attributes => { :id => "map" }, :content => nil)
  end

  def test_parse_invalid_document
    assert_nothing_raised do
      doc = HTML::Document.new("<html>
        <table>
          <tr>
            <td style=\"color: #FFFFFF; height: 17px; onclick=\"window.location.href='http://www.rmeinc.com/about_rme.aspx'\" style=\"cursor:pointer; height: 17px;\"; nowrap onclick=\"window.location.href='http://www.rmeinc.com/about_rme.aspx'\" onmouseout=\"this.bgColor='#0066cc'; this.style.color='#FFFFFF'\" onmouseover=\"this.bgColor='#ffffff'; this.style.color='#0033cc'\">About Us</td>
          </tr>
        </table>
      </html>")
    end
  end

  def test_invalid_document_raises_exception_when_strict
    assert_raise RuntimeError do
      doc = HTML::Document.new("<html>
        <table>
          <tr>
            <td style=\"color: #FFFFFF; height: 17px; onclick=\"window.location.href='http://www.rmeinc.com/about_rme.aspx'\" style=\"cursor:pointer; height: 17px;\"; nowrap onclick=\"window.location.href='http://www.rmeinc.com/about_rme.aspx'\" onmouseout=\"this.bgColor='#0066cc'; this.style.color='#FFFFFF'\" onmouseover=\"this.bgColor='#ffffff'; this.style.color='#0033cc'\">About Us</td>
          </tr>
        </table>
      </html>", true)
    end
  end

end
