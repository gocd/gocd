require 'spec_helper'

describe XPath::Union do
  let(:template) { File.read(File.expand_path('fixtures/simple.html', File.dirname(__FILE__))) }
  let(:doc) { Nokogiri::HTML(template) }

  describe '#expressions' do
    it "should return the expressions" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = XPath::Union.new(@expr1, @expr2)
      @collection.expressions.should == [@expr1, @expr2]
    end
  end

  describe '#each' do
    it "should iterate through the expressions" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = XPath::Union.new(@expr1, @expr2)
      exprs = []
      @collection.each { |expr| exprs << expr }
      exprs.should == [@expr1, @expr2]
    end
  end

  describe '#map' do
    it "should map the expressions" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = XPath::Union.new(@expr1, @expr2)
      @collection.map { |expr| expr.expression }.should == [:descendant, :descendant]
    end
  end

  describe '#to_xpath' do
    it "should create a valid xpath expression" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div).where(x.attr(:id) == 'foo') }
      @collection = XPath::Union.new(@expr1, @expr2)
      @results = doc.xpath(@collection.to_xpath)
      @results[0][:title].should == 'fooDiv'
      @results[1].text.should == 'Blah'
      @results[2].text.should == 'Bax'
    end
  end


  describe '#where and others' do
    it "should be delegated to the individual expressions" do
      @expr1 = XPath.generate { |x| x.descendant(:p) }
      @expr2 = XPath.generate { |x| x.descendant(:div) }
      @collection = XPath::Union.new(@expr1, @expr2)
      @xpath1 = @collection.where(XPath.attr(:id) == 'foo').to_xpath
      @xpath2 = @collection.where(XPath.attr(:id) == 'fooDiv').to_xpath
      @results = doc.xpath(@xpath1)
      @results[0][:title].should == 'fooDiv'
      @results = doc.xpath(@xpath2)
      @results[0][:id].should == 'fooDiv'
    end
  end

end

