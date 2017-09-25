require 'spec_helper'

require 'nokogiri'

class Thingy
  include XPath

  def foo_div
    descendant(:div).where(attr(:id) == 'foo')
  end
end

describe XPath do
  let(:template) { File.read(File.expand_path('fixtures/simple.html', File.dirname(__FILE__))) }
  let(:doc) { Nokogiri::HTML(template) }

  def xpath(type=nil, &block)
    doc.xpath XPath.generate(&block).to_xpath(type)
  end

  it "should work as a mixin" do
    xpath = Thingy.new.foo_div.to_xpath
    doc.xpath(xpath).first[:title].should == 'fooDiv'
  end

  describe '#descendant' do
    it "should find nodes that are nested below the current node" do
      @results = xpath { |x| x.descendant(:p) }
      @results[0].text.should == "Blah"
      @results[1].text.should == "Bax"
    end

    it "should not find nodes outside the context" do
      @results = xpath do |x|
        foo_div = x.descendant(:div).where(x.attr(:id) == 'foo')
        x.descendant(:p).where(x.attr(:id) == foo_div.attr(:title))
      end
      @results[0].should be_nil
    end

    it "should find multiple kinds of nodes" do
      @results = xpath { |x| x.descendant(:p, :ul) }
      @results[0].text.should == 'Blah'
      @results[3].text.should == 'A list'
    end

    it "should find all nodes when no arguments given" do
      @results = xpath { |x| x.descendant[x.attr(:id) == 'foo'].descendant }
      @results[0].text.should == 'Blah'
      @results[4].text.should == 'A list'
    end
  end

  describe '#child' do
    it "should find nodes that are nested directly below the current node" do
      @results = xpath { |x| x.descendant(:div).child(:p) }
      @results[0].text.should == "Blah"
      @results[1].text.should == "Bax"
    end

    it "should not find nodes that are nested further down below the current node" do
      @results = xpath { |x| x.child(:p) }
      @results[0].should be_nil
    end

    it "should find multiple kinds of nodes" do
      @results = xpath { |x| x.descendant(:div).child(:p, :ul) }
      @results[0].text.should == 'Blah'
      @results[3].text.should == 'A list'
    end

    it "should find all nodes when no arguments given" do
      @results = xpath { |x| x.descendant[x.attr(:id) == 'foo'].child }
      @results[0].text.should == 'Blah'
      @results[3].text.should == 'A list'
    end
  end

  describe '#axis' do
    it "should find nodes given the xpath axis" do
      @results = xpath { |x| x.axis(:descendant, :p) }
      @results[0].text.should == "Blah"
    end

    it "should find nodes given the xpath axis without a specific tag" do
      @results = xpath { |x| x.descendant(:div)[x.attr(:id) == 'foo'].axis(:descendant) }
      @results[0][:id].should == "fooDiv"
    end
  end

  describe '#next_sibling' do
    it "should find nodes which are immediate siblings of the current node" do
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'fooDiv'].next_sibling(:p) }.first.text.should == 'Bax'
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'fooDiv'].next_sibling(:ul, :p) }.first.text.should == 'Bax'
      xpath { |x| x.descendant(:p)[x.attr(:title) == 'monkey'].next_sibling(:ul, :p) }.first.text.should == 'A list'
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'fooDiv'].next_sibling(:ul, :li) }.first.should be_nil
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'fooDiv'].next_sibling }.first.text.should == 'Bax'
    end
  end

  describe '#previous_sibling' do
    it "should find nodes which are exactly preceding the current node" do
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'wooDiv'].previous_sibling(:p) }.first.text.should == 'Bax'
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'wooDiv'].previous_sibling(:ul, :p) }.first.text.should == 'Bax'
      xpath { |x| x.descendant(:p)[x.attr(:title) == 'gorilla'].previous_sibling(:ul, :p) }.first.text.should == 'A list'
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'wooDiv'].previous_sibling(:ul, :li) }.first.should be_nil
      xpath { |x| x.descendant(:p)[x.attr(:id) == 'wooDiv'].previous_sibling }.first.text.should == 'Bax'
    end
  end

  describe '#anywhere' do
    it "should find nodes regardless of the context" do
      @results = xpath do |x|
        foo_div = x.anywhere(:div).where(x.attr(:id) == 'foo')
        x.descendant(:p).where(x.attr(:id) == foo_div.attr(:title))
      end
      @results[0].text.should == "Blah"
    end

    it "should find multiple kinds of nodes regardless of the context" do
      @results = xpath do |x|
        context=x.descendant(:div).where(x.attr(:id)=='woo')
        context.anywhere(:p, :ul)
      end

      @results[0].text.should == 'Blah'
      @results[3].text.should == 'A list'
      @results[4].text.should == 'A list'
      @results[6].text.should == 'Bax'
    end

    it "should find all nodes when no arguments given regardless of the context" do
      @results = xpath do |x|
        context=x.descendant(:div).where(x.attr(:id)=='woo')
        context.anywhere
      end
      @results[0].name.should == 'html'
      @results[1].name.should == 'head'
      @results[2].name.should == 'body'
      @results[6].text.should == 'Blah'
      @results[10].text.should == 'A list'
      @results[13].text.should == 'A list'
      @results[15].text.should == 'Bax'
    end

  end

  describe '#contains' do
    it "should find nodes that contain the given string" do
      @results = xpath do |x|
        x.descendant(:div).where(x.attr(:title).contains('ooD'))
      end
      @results[0][:id].should == "foo"
    end

    it "should find nodes that contain the given expression" do
      @results = xpath do |x|
        expression = x.anywhere(:div).where(x.attr(:title) == 'fooDiv').attr(:id)
        x.descendant(:div).where(x.attr(:title).contains(expression))
      end
      @results[0][:id].should == "foo"
    end
  end

  describe '#starts_with' do
    it "should find nodes that begin with the given string" do
      @results = xpath do |x|
        x.descendant(:*).where(x.attr(:id).starts_with('foo'))
      end
      @results.size.should == 2
      @results[0][:id].should == "foo"
      @results[1][:id].should == "fooDiv"
    end

    it "should find nodes that contain the given expression" do
      @results = xpath do |x|
        expression = x.anywhere(:div).where(x.attr(:title) == 'fooDiv').attr(:id)
        x.descendant(:div).where(x.attr(:title).starts_with(expression))
      end
      @results[0][:id].should == "foo"
    end
  end

  describe '#text' do
    it "should select a node's text" do
      @results = xpath { |x| x.descendant(:p).where(x.text == 'Bax') }
      @results[0].text.should == 'Bax'
      @results[1][:title].should == 'monkey'
      @results = xpath { |x| x.descendant(:div).where(x.descendant(:p).text == 'Bax') }
      @results[0][:title].should == 'fooDiv'
    end
  end

  describe '#where' do
    it "should limit the expression to find only certain nodes" do
      xpath { |x| x.descendant(:div).where(:"@id = 'foo'") }.first[:title].should == "fooDiv"
    end

    it "should be aliased as []" do
      xpath { |x| x.descendant(:div)[:"@id = 'foo'"] }.first[:title].should == "fooDiv"
    end
  end

  describe '#inverse' do
    it "should invert the expression" do
      xpath { |x| x.descendant(:p).where(x.attr(:id).equals('fooDiv').inverse) }.first.text.should == 'Bax'
    end

    it "should be aliased as the unary tilde" do
      xpath { |x| x.descendant(:p).where(~x.attr(:id).equals('fooDiv')) }.first.text.should == 'Bax'
    end
  end

  describe '#equals' do
    it "should limit the expression to find only certain nodes" do
      xpath { |x| x.descendant(:div).where(x.attr(:id).equals('foo')) }.first[:title].should == "fooDiv"
    end

    it "should be aliased as ==" do
      xpath { |x| x.descendant(:div).where(x.attr(:id) == 'foo') }.first[:title].should == "fooDiv"
    end
  end

  describe '#is' do
    it "uses equality when :exact given" do
      xpath(:exact) { |x| x.descendant(:div).where(x.attr(:id).is('foo')) }.first[:title].should == "fooDiv"
      xpath(:exact) { |x| x.descendant(:div).where(x.attr(:id).is('oo')) }.first.should be_nil
    end

    it "uses substring matching otherwise" do
      xpath { |x| x.descendant(:div).where(x.attr(:id).is('foo')) }.first[:title].should == "fooDiv"
      xpath { |x| x.descendant(:div).where(x.attr(:id).is('oo')) }.first[:title].should == "fooDiv"
    end
  end

  describe '#one_of' do
    it "should return all nodes where the condition matches" do
      @results = xpath do |x|
        p = x.anywhere(:div).where(x.attr(:id) == 'foo').attr(:title)
        x.descendant(:*).where(x.attr(:id).one_of('foo', p, 'baz'))
      end
      @results[0][:title].should == "fooDiv"
      @results[1].text.should == "Blah"
      @results[2][:title].should == "bazDiv"
    end
  end

  describe '#and' do
    it "should find all nodes in both expression" do
      @results = xpath do |x|
        x.descendant(:*).where(x.contains('Bax').and(x.attr(:title).equals('monkey')))
      end
      @results[0][:title].should == "monkey"
    end

    it "should be aliased as ampersand (&)" do
      @results = xpath do |x|
        x.descendant(:*).where(x.contains('Bax') & x.attr(:title).equals('monkey'))
      end
      @results[0][:title].should == "monkey"
    end
  end

  describe '#or' do
    it "should find all nodes in either expression" do
      @results = xpath do |x|
        x.descendant(:*).where(x.attr(:id).equals('foo').or(x.attr(:id).equals('fooDiv')))
      end
      @results[0][:title].should == "fooDiv"
      @results[1].text.should == "Blah"
    end

    it "should be aliased as pipe (|)" do
      @results = xpath do |x|
        x.descendant(:*).where(x.attr(:id).equals('foo') | x.attr(:id).equals('fooDiv'))
      end
      @results[0][:title].should == "fooDiv"
      @results[1].text.should == "Blah"
    end
  end

  describe '#attr' do
    it "should be an attribute" do
      @results = xpath { |x| x.descendant(:div).where(x.attr(:id)) }
      @results[0][:title].should == "barDiv"
      @results[1][:title].should == "fooDiv"
    end
  end

  describe '#css' do
    it "should find nodes by the given CSS selector" do
      @results = xpath { |x| x.css('#preference p') }
      @results[0].text.should == 'allamas'
      @results[1].text.should == 'llama'
    end

    it "should respect previous expression" do
      @results = xpath { |x| x.descendant[x.attr(:id) == 'moar'].css('p') }
      @results[0].text.should == 'chimp'
      @results[1].text.should == 'flamingo'
    end

    it "should be composable" do
      @results = xpath { |x| x.css('#moar').descendant(:p) }
      @results[0].text.should == 'chimp'
      @results[1].text.should == 'flamingo'
    end

    it "should allow comma separated selectors" do
      @results = xpath { |x| x.descendant[x.attr(:id) == 'moar'].css('div, p') }
      @results[0].text.should == 'chimp'
      @results[1].text.should == 'elephant'
      @results[2].text.should == 'flamingo'
    end
  end

  describe '#name' do
    it "should match the node's name" do
      xpath { |x| x.descendant(:*).where(x.name == 'ul') }.first.text.should == "A list"
    end
  end

  describe '#union' do
    it "should create a union expression" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = @expr1.union(@expr2)
      @xpath1 = @collection.where(XPath.attr(:id) == 'foo').to_xpath
      @xpath2 = @collection.where(XPath.attr(:id) == 'fooDiv').to_xpath
      @results = doc.xpath(@xpath1)
      @results[0][:title].should == 'fooDiv'
      @results = doc.xpath(@xpath2)
      @results[0][:id].should == 'fooDiv'
    end

    it "should be aliased as +" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = @expr1 + @expr2
      @xpath1 = @collection.where(XPath.attr(:id) == 'foo').to_xpath
      @xpath2 = @collection.where(XPath.attr(:id) == 'fooDiv').to_xpath
      @results = doc.xpath(@xpath1)
      @results[0][:title].should == 'fooDiv'
      @results = doc.xpath(@xpath2)
      @results[0][:id].should == 'fooDiv'
    end
  end

end
