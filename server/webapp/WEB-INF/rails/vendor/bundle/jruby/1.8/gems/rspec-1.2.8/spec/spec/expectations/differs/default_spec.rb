require File.dirname(__FILE__) + '/../../../spec_helper.rb'

module Spec
  module Fixtures
    class Animal
      def initialize(name,species)
        @name,@species = name,species
      end

      def inspect
        <<-EOA
<Animal
  name=#{@name},
  species=#{@species}
>
        EOA
      end
    end
  end
end

describe "Diff" do
  before(:each) do
    @options = ::Spec::Runner::Options.new(StringIO.new, StringIO.new)
    @differ = Spec::Expectations::Differs::Default.new(@options)
  end

  it "should output unified diff of two strings" do
    expected="foo\nbar\nzap\nthis\nis\nsoo\nvery\nvery\nequal\ninsert\na\nline\n"
    actual="foo\nzap\nbar\nthis\nis\nsoo\nvery\nvery\nequal\ninsert\na\nanother\nline\n"
    expected_diff= <<'EOD'


@@ -1,6 +1,6 @@
 foo
-zap
 bar
+zap
 this
 is
 soo
@@ -9,6 +9,5 @@
 equal
 insert
 a
-another
 line
EOD

    diff = @differ.diff_as_string(expected, actual)
    diff.should eql(expected_diff)
  end

  it "should output unified diff message of two arrays" do
    expected = [ :foo, 'bar', :baz, 'quux', :metasyntactic, 'variable', :delta, 'charlie', :width, 'quite wide' ]
    actual   = [ :foo, 'bar', :baz, 'quux', :metasyntactic, 'variable', :delta, 'tango'  , :width, 'very wide'  ]

    expected_diff = <<'EOD'


@@ -5,7 +5,7 @@
  :metasyntactic,
  "variable",
  :delta,
- "tango",
+ "charlie",
  :width,
- "very wide"]
+ "quite wide"]
EOD


    diff = @differ.diff_as_object(expected,actual)
    diff.should == expected_diff
  end
  
  it "should output a friendly message if comparing simple hashes" do
    expected = { "foo" => "bar" }
    actual   = { "foo" => "baz" }
    
    expected_diff = <<'EOD'

Expected the key "foo" to be "bar", but was "baz"

EOD


    diff = @differ.diff_as_hash(actual, expected)
    diff.should == expected_diff
  end
  
  
  it "should output a friendly message if comparing simple hashes that contain different keys" do
    expected = { "bar" => "foo" }
    actual   = { "foo" => "baz" }

    expected_diff = <<'EOD'

Expected hash contains keys that target hash does not: ["bar"]
Target hash contains keys that expected hash does not: ["foo"]
Expected the key "bar" to be "foo", but was nil

EOD


    diff = @differ.diff_as_hash(actual, expected)
    diff.should == expected_diff
  end
  
  it "should output diff message if the hash is complex (containing Array or Hash)" do
    expected = { "foo" => "bar", "fizz" => [1, 2, 3] }
    actual   = { "foo" => "baz", "fizz" => [1, 2] }

    expected_diff = <<'EOD'

Expected the key "fizz" to be [1, 2, 3], but was [1, 2]
Expected the key "foo" to be "bar", but was "baz"


@@ -1,2 +1,2 @@
-{"foo"=>"bar", "fizz"=>[1, 2, 3]}
+{"foo"=>"baz", "fizz"=>[1, 2]}
EOD


    diff = @differ.diff_as_hash(actual, expected)
    diff.should == expected_diff
  end
  

  it "should output unified diff message of two objects" do
    expected = Spec::Fixtures::Animal.new "bob", "giraffe"
    actual   = Spec::Fixtures::Animal.new "bob", "tortoise"

    expected_diff = <<'EOD'

@@ -1,5 +1,5 @@
 <Animal
   name=bob,
-  species=tortoise
+  species=giraffe
 >
EOD

    diff = @differ.diff_as_object(expected,actual)
    diff.should == expected_diff
  end

end


describe "Diff in context format" do
  before(:each) do
    @options = Spec::Runner::Options.new(StringIO.new, StringIO.new)
    @options.diff_format = :context
    @differ = Spec::Expectations::Differs::Default.new(@options)
  end

  it "should output unified diff message of two objects" do
    expected = Spec::Fixtures::Animal.new "bob", "giraffe"
    actual   = Spec::Fixtures::Animal.new "bob", "tortoise"

    expected_diff = <<'EOD'

***************
*** 1,5 ****
  <Animal
    name=bob,
!   species=tortoise
  >
--- 1,5 ----
  <Animal
    name=bob,
!   species=giraffe
  >
EOD

    diff = @differ.diff_as_object(expected,actual)
    diff.should == expected_diff
  end
end
