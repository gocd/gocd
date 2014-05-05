require File.dirname(__FILE__) + '/../../spec_helper.rb'

describe "LineNumberQuery" do
  with_sandboxed_options do    
    attr_reader :parser, :file
    
    before do
      @parser = Spec::Runner::LineNumberQuery.new(options)
      @file = "#{File.dirname(__FILE__)}/line_number_query/line_number_query_fixture.rb"
      load file
    end

    it "should find spec name for 'specify' at same line" do
      parser.spec_name_for(file, 5).should == "c 1"
    end

    it "should find spec name for 'specify' at end of spec line" do
      parser.spec_name_for(file, 6).should == "c 1"
    end

    it "should find context for 'context' above all specs" do
      parser.spec_name_for(file, 4).should == "c"
    end

    it "should find spec name for 'it' at same line" do
      parser.spec_name_for(file, 15).should == "d 3"
    end

    it "should find spec name for 'it' at end of spec line" do
      parser.spec_name_for(file, 16).should == "d 3"
    end

    it "should find context for 'describe' above all specs" do
      parser.spec_name_for(file, 14).should == "d"
    end

    it "should find nearest example name between examples" do
      parser.spec_name_for(file, 7).should == "c 1"
    end

    it "should find nothing outside a context" do
      parser.spec_name_for(file, 2).should be_nil
    end

    it "should find context name for type" do
      parser.spec_name_for(file, 26).should == "LineNumberQuerySubject"
    end

    it "should find context and spec name for type" do
      parser.spec_name_for(file, 28).should == "LineNumberQuerySubject 5"
    end

    it "should find context and description for type" do
      parser.spec_name_for(file, 33).should == "LineNumberQuerySubject described"
    end

    it "should find context and description and example for type" do
      parser.spec_name_for(file, 36).should == "LineNumberQuerySubject described 6"
    end

    it "should find context and description for type with modifications" do
      parser.spec_name_for(file, 40).should == "LineNumberQuerySubject described"
    end

    it "should find context and described and example for type with modifications" do
      parser.spec_name_for(file, 43).should == "LineNumberQuerySubject described 7"
    end

    it "should find example group" do
      parser.spec_name_for(file, 47).should == "described"
    end

    it "should find example" do
      parser.spec_name_for(file, 50).should == "described 8"
    end

    it "should find nested example" do
      parser.spec_name_for(file, 63).should == "e f 11"
    end

    it "should handle paths which contain colons" do
      fixture =
         { "c:/somepath/somefile.rb:999:in 'method'" => "c:/somepath/somefile.rb",
           "./somepath/somefile:999"                 => "./somepath/somefile" }
      fixture.each_pair do |input, expected|
        parser.send(:parse_location, input ).should == [expected, 999]
      end
    end
    
    it "ignores example group base classes which have no location" do
      options = stub('options', :example_groups => [
        stub('example_group', :location => nil)
      ])
      parser = Spec::Runner::LineNumberQuery.new(options)
      parser.spec_name_for('foo',37).should == nil
    end

    describe "#example_line_for" do
      it "should find example declared on same line" do
        parser.example_line_for(file, 5).should == 5
      end

      it "should find example declared on the line above, while still inside the example" do
        parser.example_line_for(file, 6).should == 5
      end

      it "should find example declared from empty line below the example" do
        parser.example_line_for(file, 7).should == 5
      end

      it "should find the group declared on the same line" do
        parser.example_line_for(file, 3).should == 3
      end

      it "should find the group declared above the first example" do
        parser.example_line_for(file, 4).should == 3
      end
    end
  end
end
