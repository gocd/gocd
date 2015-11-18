require "helper"
require "thor/parser"

describe Thor::Options do
  def create(opts, defaults = {}, stop_on_unknown = false)
    opts.each do |key, value|
      opts[key] = Thor::Option.parse(key, value) unless value.is_a?(Thor::Option)
    end

    @opt = Thor::Options.new(opts, defaults, stop_on_unknown)
  end

  def parse(*args)
    @opt.parse(args.flatten)
  end

  def check_unknown!
    @opt.check_unknown!
  end

  def remaining
    @opt.remaining
  end

  describe "#to_switches" do
    it "turns true values into a flag" do
      expect(Thor::Options.to_switches(:color => true)).to eq("--color")
    end

    it "ignores nil" do
      expect(Thor::Options.to_switches(:color => nil)).to eq("")
    end

    it "ignores false" do
      expect(Thor::Options.to_switches(:color => false)).to eq("")
    end

    it "writes --name value for anything else" do
      expect(Thor::Options.to_switches(:format => "specdoc")).to eq('--format "specdoc"')
    end

    it "joins several values" do
      switches = Thor::Options.to_switches(:color => true, :foo => "bar").split(" ").sort
      expect(switches).to eq(%w["bar" --color --foo])
    end

    it "accepts arrays" do
      expect(Thor::Options.to_switches(:count => [1, 2, 3])).to eq("--count 1 2 3")
    end

    it "accepts hashes" do
      expect(Thor::Options.to_switches(:count => {:a => :b})).to eq("--count a:b")
    end

    it "accepts underscored options" do
      expect(Thor::Options.to_switches(:under_score_option => "foo bar")).to eq('--under_score_option "foo bar"')
    end

  end

  describe "#parse" do
    it "allows multiple aliases for a given switch" do
      create %w[--foo --bar --baz] => :string
      expect(parse("--foo", "12")["foo"]).to eq("12")
      expect(parse("--bar", "12")["foo"]).to eq("12")
      expect(parse("--baz", "12")["foo"]).to eq("12")
    end

    it "allows custom short names" do
      create "-f" => :string
      expect(parse("-f", "12")).to eq("f" => "12")
    end

    it "allows custom short-name aliases" do
      create %w[--bar -f] => :string
      expect(parse("-f", "12")).to eq("bar" => "12")
    end

    it "accepts conjoined short switches" do
      create %w[--foo -f] => true, %w[--bar -b] => true, %w[--app -a] => true
      opts = parse("-fba")
      expect(opts["foo"]).to be true
      expect(opts["bar"]).to be true
      expect(opts["app"]).to be true
    end

    it "accepts conjoined short switches with input" do
      create %w[--foo -f] => true, %w[--bar -b] => true, %w[--app -a] => :required
      opts = parse "-fba", "12"
      expect(opts["foo"]).to be true
      expect(opts["bar"]).to be true
      expect(opts["app"]).to eq("12")
    end

    it "returns the default value if none is provided" do
      create :foo => "baz", :bar => :required
      expect(parse("--bar", "boom")["foo"]).to eq("baz")
    end

    it "returns the default value from defaults hash to required arguments" do
      create Hash[:bar => :required], Hash[:bar => "baz"]
      expect(parse["bar"]).to eq("baz")
    end

    it "gives higher priority to defaults given in the hash" do
      create Hash[:bar => true], Hash[:bar => false]
      expect(parse["bar"]).to eq(false)
    end

    it "raises an error for unknown switches" do
      create :foo => "baz", :bar => :required
      parse("--bar", "baz", "--baz", "unknown")
      expect { check_unknown! }.to raise_error(Thor::UnknownArgumentError, "Unknown switches '--baz'")
    end

    it "skips leading non-switches" do
      create(:foo => "baz")

      expect(parse("asdf", "--foo", "bar")).to eq("foo" => "bar")
    end

    it "correctly recognizes things that look kind of like options, but aren't, as not options" do
      create(:foo => "baz")
      expect(parse("--asdf---asdf", "baz", "--foo", "--asdf---dsf--asdf")).to eq("foo" => "--asdf---dsf--asdf")
      check_unknown!
    end

    it "accepts underscores in commandline args hash for boolean" do
      create :foo_bar => :boolean
      expect(parse("--foo_bar")["foo_bar"]).to eq(true)
      expect(parse("--no_foo_bar")["foo_bar"]).to eq(false)
    end

    it "accepts underscores in commandline args hash for strings" do
      create :foo_bar => :string, :baz_foo => :string
      expect(parse("--foo_bar", "baz")["foo_bar"]).to eq("baz")
      expect(parse("--baz_foo", "foo bar")["baz_foo"]).to eq("foo bar")
    end

    it "interprets everything after -- as args instead of options" do
      create(:foo => :string, :bar => :required)
      expect(parse(%w[--bar abc moo -- --foo def -a])).to eq("bar" => "abc")
      expect(remaining).to eq(%w[moo --foo def -a])
    end

    it "ignores -- when looking for single option values" do
      create(:foo => :string, :bar => :required)
      expect(parse(%w[--bar -- --foo def -a])).to eq("bar" => "--foo")
      expect(remaining).to eq(%w[def -a])
    end

    it "ignores -- when looking for array option values" do
      create(:foo => :array)
      expect(parse(%w[--foo a b -- c d -e])).to eq("foo" => %w[a b c d -e])
      expect(remaining).to eq([])
    end

    it "ignores -- when looking for hash option values" do
      create(:foo => :hash)
      expect(parse(%w[--foo a:b -- c:d -e])).to eq("foo" => {"a" => "b", "c" => "d"})
      expect(remaining).to eq(%w[-e])
    end

    it "ignores trailing --" do
      create(:foo => :string)
      expect(parse(%w[--foo --])).to eq("foo" => nil)
      expect(remaining).to eq([])
    end

    describe "with no input" do
      it "and no switches returns an empty hash" do
        create({})
        expect(parse).to eq({})
      end

      it "and several switches returns an empty hash" do
        create "--foo" => :boolean, "--bar" => :string
        expect(parse).to eq({})
      end

      it "and a required switch raises an error" do
        create "--foo" => :required
        expect { parse }.to raise_error(Thor::RequiredArgumentMissingError, "No value provided for required options '--foo'")
      end
    end

    describe "with one required and one optional switch" do
      before do
        create "--foo" => :required, "--bar" => :boolean
      end

      it "raises an error if the required switch has no argument" do
        expect { parse("--foo") }.to raise_error(Thor::MalformattedArgumentError)
      end

      it "raises an error if the required switch isn't given" do
        expect { parse("--bar") }.to raise_error(Thor::RequiredArgumentMissingError)
      end

      it "raises an error if the required switch is set to nil" do
        expect { parse("--no-foo") }.to raise_error(Thor::RequiredArgumentMissingError)
      end

      it "does not raises an error if the required option has a default value" do
        options = {:required => true, :type => :string, :default => "baz"}
        create :foo => Thor::Option.new("foo", options), :bar => :boolean
        expect { parse("--bar") }.not_to raise_error
      end
    end

    context "when stop_on_unknown is true" do
      before do
        create({:foo => :string, :verbose => :boolean}, {}, true)
      end

      it "stops parsing on first non-option" do
        expect(parse(%w[foo --verbose])).to eq({})
        expect(remaining).to eq(%w[foo --verbose])
      end

      it "stops parsing on unknown option" do
        expect(parse(%w[--bar --verbose])).to eq({})
        expect(remaining).to eq(%w[--bar --verbose])
      end

      it "retains -- after it has stopped parsing" do
        expect(parse(%w[--bar -- whatever])).to eq({})
        expect(remaining).to eq(%w[--bar -- whatever])
      end

      it "still accepts options that are given before non-options" do
        expect(parse(%w[--verbose foo])).to eq("verbose" => true)
        expect(remaining).to eq(%w[foo])
      end

      it "still accepts options that require a value" do
        expect(parse(%w[--foo bar baz])).to eq("foo" => "bar")
        expect(remaining).to eq(%w[baz])
      end

      it "still interprets everything after -- as args instead of options" do
        expect(parse(%w[-- --verbose])).to eq({})
        expect(remaining).to eq(%w[--verbose])
      end
    end

    describe "with :string type" do
      before do
        create %w[--foo -f] => :required
      end

      it "accepts a switch <value> assignment" do
        expect(parse("--foo", "12")["foo"]).to eq("12")
      end

      it "accepts a switch=<value> assignment" do
        expect(parse("-f=12")["foo"]).to eq("12")
        expect(parse("--foo=12")["foo"]).to eq("12")
        expect(parse("--foo=bar=baz")["foo"]).to eq("bar=baz")
      end

      it "must accept underscores switch=value assignment" do
        create :foo_bar => :required
        expect(parse("--foo_bar=http://example.com/under_score/")["foo_bar"]).to eq("http://example.com/under_score/")
      end

      it "accepts a --no-switch format" do
        create "--foo" => "bar"
        expect(parse("--no-foo")["foo"]).to be nil
      end

      it "does not consume an argument for --no-switch format" do
        create "--cheese" => :string
        expect(parse("burger", "--no-cheese", "fries")["cheese"]).to be nil
      end

      it "accepts a --switch format on non required types" do
        create "--foo" => :string
        expect(parse("--foo")["foo"]).to eq("foo")
      end

      it "accepts a --switch format on non required types with default values" do
        create "--baz" => :string, "--foo" => "bar"
        expect(parse("--baz", "bang", "--foo")["foo"]).to eq("bar")
      end

      it "overwrites earlier values with later values" do
        expect(parse("--foo=bar", "--foo", "12")["foo"]).to eq("12")
        expect(parse("--foo", "12", "--foo", "13")["foo"]).to eq("13")
      end

      it "raises error when value isn't in enum" do
        enum = %w[apple banana]
        create :fruit => Thor::Option.new("fruit", :type => :string, :enum => enum)
        expect { parse("--fruit", "orange") }.to raise_error(Thor::MalformattedArgumentError,
                                                             "Expected '--fruit' to be one of #{enum.join(', ')}; got orange")
      end
    end

    describe "with :boolean type" do
      before do
        create "--foo" => false
      end

      it "accepts --opt assignment" do
        expect(parse("--foo")["foo"]).to eq(true)
        expect(parse("--foo", "--bar")["foo"]).to eq(true)
      end

      it "uses the default value if no switch is given" do
        expect(parse("")["foo"]).to eq(false)
      end

      it "accepts --opt=value assignment" do
        expect(parse("--foo=true")["foo"]).to eq(true)
        expect(parse("--foo=false")["foo"]).to eq(false)
      end

      it "accepts --[no-]opt variant, setting false for value" do
        expect(parse("--no-foo")["foo"]).to eq(false)
      end

      it "accepts --[skip-]opt variant, setting false for value" do
        expect(parse("--skip-foo")["foo"]).to eq(false)
      end

      it "will prefer 'no-opt' variant over inverting 'opt' if explicitly set" do
        create "--no-foo" => true
        expect(parse("--no-foo")["no-foo"]).to eq(true)
      end

      it "will prefer 'skip-opt' variant over inverting 'opt' if explicitly set" do
        create "--skip-foo" => true
        expect(parse("--skip-foo")["skip-foo"]).to eq(true)
      end

      it "accepts inputs in the human name format" do
        create :foo_bar => :boolean
        expect(parse("--foo-bar")["foo_bar"]).to eq(true)
        expect(parse("--no-foo-bar")["foo_bar"]).to eq(false)
        expect(parse("--skip-foo-bar")["foo_bar"]).to eq(false)
      end

      it "doesn't eat the next part of the param" do
        create :foo => :boolean
        expect(parse("--foo", "bar")).to eq("foo" => true)
        expect(@opt.remaining).to eq(%w[bar])
      end
    end

    describe "with :hash type" do
      before do
        create "--attributes" => :hash
      end

      it "accepts a switch=<value> assignment" do
        expect(parse("--attributes=name:string", "age:integer")["attributes"]).to eq("name" => "string", "age" => "integer")
      end

      it "accepts a switch <value> assignment" do
        expect(parse("--attributes", "name:string", "age:integer")["attributes"]).to eq("name" => "string", "age" => "integer")
      end

      it "must not mix values with other switches" do
        expect(parse("--attributes", "name:string", "age:integer", "--baz", "cool")["attributes"]).to eq("name" => "string", "age" => "integer")
      end
    end

    describe "with :array type" do
      before do
        create "--attributes" => :array
      end

      it "accepts a switch=<value> assignment" do
        expect(parse("--attributes=a", "b", "c")["attributes"]).to eq(%w[a b c])
      end

      it "accepts a switch <value> assignment" do
        expect(parse("--attributes", "a", "b", "c")["attributes"]).to eq(%w[a b c])
      end

      it "must not mix values with other switches" do
        expect(parse("--attributes", "a", "b", "c", "--baz", "cool")["attributes"]).to eq(%w[a b c])
      end
    end

    describe "with :numeric type" do
      before do
        create "n" => :numeric, "m" => 5
      end

      it "accepts a -nXY assignment" do
        expect(parse("-n12")["n"]).to eq(12)
      end

      it "converts values to numeric types" do
        expect(parse("-n", "3", "-m", ".5")).to eq("n" => 3, "m" => 0.5)
      end

      it "raises error when value isn't numeric" do
        expect { parse("-n", "foo") }.to raise_error(Thor::MalformattedArgumentError,
                                                     "Expected numeric value for '-n'; got \"foo\"")
      end

      it "raises error when value isn't in enum" do
        enum = [1, 2]
        create :limit => Thor::Option.new("limit", :type => :numeric, :enum => enum)
        expect { parse("--limit", "3") }.to raise_error(Thor::MalformattedArgumentError,
                                                        "Expected '--limit' to be one of #{enum.join(', ')}; got 3")
      end
    end

  end
end
