require "helper"
require "thor/base"

describe Thor::Invocation do
  describe "#invoke" do
    it "invokes a command inside another command" do
      expect(capture(:stdout) { A.new.invoke(:two) }).to eq("2\n3\n")
    end

    it "invokes a command just once" do
      expect(capture(:stdout) { A.new.invoke(:one) }).to eq("1\n2\n3\n")
    end

    it "invokes a command just once even if they belongs to different classes" do
      expect(capture(:stdout) { Defined.new.invoke(:one) }).to eq("1\n2\n3\n4\n5\n")
    end

    it "invokes a command with arguments" do
      expect(A.new.invoke(:five, [5])).to be true
      expect(A.new.invoke(:five, [7])).to be false
    end

    it "invokes the default command if none is given to a Thor class" do
      content = capture(:stdout) { A.new.invoke("b") }
      expect(content).to match(/Commands/)
      expect(content).to match(/LAST_NAME/)
    end

    it "accepts a class as argument without a command to invoke" do
      content = capture(:stdout) { A.new.invoke(B) }
      expect(content).to match(/Commands/)
      expect(content).to match(/LAST_NAME/)
    end

    it "accepts a class as argument with a command to invoke" do
      base = A.new([], :last_name => "Valim")
      expect(base.invoke(B, :one, %w[Jose])).to eq("Valim, Jose")
    end

    it "allows customized options to be given" do
      base = A.new([], :last_name => "Wrong")
      expect(base.invoke(B, :one, %w[Jose], :last_name => "Valim")).to eq("Valim, Jose")
    end

    it "reparses options in the new class" do
      expect(A.start(%w[invoker --last-name Valim])).to eq("Valim, Jose")
    end

    it "shares initialize options with invoked class" do
      expect(A.new([], :foo => :bar).invoke("b:two")).to eq("foo" => :bar)
    end

    it "uses default options from invoked class if no matching arguments are given" do
      expect(A.new([]).invoke("b:four")).to eq("default")
    end

    it "overrides default options if options are passed to the invoker" do
      expect(A.new([], :defaulted_value => "not default").invoke("b:four")).to eq("not default")
    end

    it "returns the command chain" do
      expect(I.new.invoke("two")).to eq([:two])

      if RUBY_VERSION < "1.9.3"
        result = J.start(["one", "two" ])
        expect(result).to include(:one)
        expect(result).to include(:two)
      else
        expect(J.start(["one", "two" ])).to eq([:one, :two])
      end
    end

    it "dump configuration values to be used in the invoked class" do
      base = A.new
      expect(base.invoke("b:three").shell).to eq(base.shell)
    end

    it "allow extra configuration values to be given" do
      base, shell = A.new, Thor::Base.shell.new
      expect(base.invoke("b:three", [], {}, :shell => shell).shell).to eq(shell)
    end

    it "invokes a Thor::Group and all of its commands" do
      expect(capture(:stdout) { A.new.invoke(:c) }).to eq("1\n2\n3\n")
    end

    it "does not invoke a Thor::Group twice" do
      base = A.new
      silence(:stdout) { base.invoke(:c) }
      expect(capture(:stdout) { base.invoke(:c) }).to be_empty
    end

    it "does not invoke any of Thor::Group commands twice" do
      base = A.new
      silence(:stdout) { base.invoke(:c) }
      expect(capture(:stdout) { base.invoke("c:one") }).to be_empty
    end

    it "raises Thor::UndefinedCommandError if the command can't be found" do
      expect do
        A.new.invoke("foo:bar")
      end.to raise_error(Thor::UndefinedCommandError)
    end

    it "raises Thor::UndefinedCommandError if the command can't be found even if all commands were already executed" do
      base = C.new
      silence(:stdout) { base.invoke_all }

      expect do
        base.invoke("foo:bar")
      end.to raise_error(Thor::UndefinedCommandError)
    end

    it "raises an error if a non Thor class is given" do
      expect do
        A.new.invoke(Object)
      end.to raise_error(RuntimeError, "Expected Thor class, got Object")
    end
  end
end
