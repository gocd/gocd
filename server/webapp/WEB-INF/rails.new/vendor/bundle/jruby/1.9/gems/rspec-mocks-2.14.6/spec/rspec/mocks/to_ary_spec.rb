require "spec_helper"

describe "a double receiving to_ary" do
  shared_examples "to_ary" do
    it "can be overridden with a stub" do
      obj.stub(:to_ary) { :non_nil_value }
      expect(obj.to_ary).to be(:non_nil_value)
    end

    it "responds when overriden" do
      obj.stub(:to_ary) { :non_nil_value }
      expect(obj).to respond_to(:to_ary)
    end

    it "supports Array#flatten" do
      obj = double('foo')
      expect([obj].flatten).to eq([obj])
    end
  end

  context "double as_null_object" do
    let(:obj) { double('obj').as_null_object }
    include_examples "to_ary"

    it "does respond to to_ary" do
      expect(obj).to respond_to(:to_ary)
    end

    it "does respond to to_a" do
      expect(obj).to respond_to(:to_a)
    end

    it "returns nil" do
      expect(obj.to_ary).to eq nil
    end
  end

  context "double without as_null_object" do
    let(:obj) { double('obj') }
    include_examples "to_ary"

    it "doesn't respond to to_ary" do
      expect(obj).not_to respond_to(:to_ary)
    end

    it "doesn't respond to to_a", :if => ( RUBY_VERSION.to_f > 1.8 ) do
      expect(obj).not_to respond_to(:to_a)
    end

    it "raises " do
      expect { obj.to_ary }.to raise_error(NoMethodError)
    end
  end
end
