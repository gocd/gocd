require 'test_helper'
require 'uber/options'

class UberOptionTest < MiniTest::Spec
  Value = Uber::Options::Value
  let (:object) { Object.new }

  class Callable
    include Uber::Callable
    def call(*); 999 end
  end

  describe "#dynamic?" do
    it { Value.new(1).dynamic?.must_equal false }
    it { Value.new(true).dynamic?.must_equal false }
    it { Value.new("loud").dynamic?.must_equal false }
    it { Value.new(:loud, :dynamic => false).dynamic?.must_equal false }
    it { Value.new("loud", :dynamic => true).dynamic?.must_equal true }

    it { Value.new(lambda {}).dynamic?.must_equal true }
    it { Value.new(Proc.new{}).dynamic?.must_equal true }
    it { Value.new(:method).dynamic?.must_equal true }

    # Uber::Callable
    it { Value.new(Callable.new).dynamic?.must_equal true }
  end

  describe "#call" do
    let (:version) { Module.new { def version; 999 end } }

    it { Value.new(nil).(Object.new).must_equal nil }
    # it { Value.new(nil, :dynamic => true).(Object.new).must_equal nil } # DISCUSS: should we handle that?

    it { Value.new(true).(Object.new).must_equal true }

    it { Value.new(:version).(object.extend(version)).must_equal 999 }
    it { Value.new("version", :dynamic => true).(object.extend(version)).must_equal 999 }
    it { Value.new(:version, :dynamic => false).(object.extend(version)).must_equal :version }
    it { Value.new(lambda { self }).(object).must_equal object }
    it { Value.new(lambda { self }).(nil).must_equal self }

    it { Value.new(lambda { :loud }, :dynamic => true).(object).must_equal :loud }

    # Uber::Callable
    it { Value.new(Callable.new).(nil).must_equal 999 }
  end

  it "#call is aliased to evaluate" do
    Value.new(Callable.new).(nil).must_equal 999
  end

  describe "passing options" do
    let (:version) { Module.new { def version(*args); args.inspect end } }
    let (:block) { Proc.new { |*args| args.inspect } }
    let (:callable) { (Class.new { include Uber::Callable; def call(*args); args.inspect; end }).new }

    it { Value.new(:version).(object.extend(version), 1, 2, 3).must_equal "[1, 2, 3]" }
    it { Value.new(block).(object, 1, 2, 3).must_equal "[1, 2, 3]" }
    it { Value.new(callable).(Object, 1, 2, 3).must_equal "[Object, 1, 2, 3]" }
  end

  # it "speed" do
  #   require "benchmark"

  #   options = 1000000.times.collect do
  #     Uber::Options.new(expires: false)
  #   end

  #   time = Benchmark.measure do
  #     options.each do |opt|
  #       opt.evaluate(nil)
  #     end
  #   end

  #   puts "good results"
  #   puts time
  # end
end

# TODO: test passing arguments to block and method optionally.

class UberOptionsTest < MiniTest::Spec
  Options = Uber::Options

  let (:dynamic) { Options.new(:volume =>1, :style => "Punkrock", :track => Proc.new { |i| i.to_s }) }

  describe "#dynamic?" do
    it { Options.new(:volume =>1, :style => "Punkrock").send(:dynamic?).must_equal false }
    it { Options.new(:style => Proc.new{}, :volume =>1).send(:dynamic?).must_equal true }
  end

  describe "#evaluate" do

    it { dynamic.evaluate(Object.new, 999).must_equal({:volume =>1, :style => "Punkrock", :track => "999"}) }

    describe "static" do
      let (:static) { Options.new(:volume =>1, :style => "Punkrock") }

      it { static.evaluate(nil).must_equal({:volume =>1, :style => "Punkrock"}) }

      it "doesn't evaluate internally" do
        static.instance_eval do
          def evaluate_for(*)
            raise "i shouldn't be called!"
          end
        end
        static.evaluate(nil).must_equal({:volume =>1, :style => "Punkrock"})
      end
    end
  end

  describe "#eval" do
    it { dynamic.eval(:volume, 999).must_equal 1 }
    it { dynamic.eval(:style, 999).must_equal "Punkrock" }
    it { dynamic.eval(:track, Object.new, 999).must_equal "999" }
  end
end