require "helper"
require "thor/actions"

describe Thor::Actions::EmptyDirectory do
  before do
    ::FileUtils.rm_rf(destination_root)
  end

  def empty_directory(destination, options = {})
    @action = Thor::Actions::EmptyDirectory.new(base, destination)
  end

  def invoke!
    capture(:stdout) { @action.invoke! }
  end

  def revoke!
    capture(:stdout) { @action.revoke! }
  end

  def base
    @base ||= MyCounter.new([1, 2], {}, :destination_root => destination_root)
  end

  describe "#destination" do
    it "returns the full destination with the destination_root" do
      expect(empty_directory("doc").destination).to eq(File.join(destination_root, "doc"))
    end

    it "takes relative root into account" do
      base.inside("doc") do
        expect(empty_directory("contents").destination).to eq(File.join(destination_root, "doc", "contents"))
      end
    end
  end

  describe "#relative_destination" do
    it "returns the relative destination to the original destination root" do
      base.inside("doc") do
        expect(empty_directory("contents").relative_destination).to eq("doc/contents")
      end
    end
  end

  describe "#given_destination" do
    it "returns the destination supplied by the user" do
      base.inside("doc") do
        expect(empty_directory("contents").given_destination).to eq("contents")
      end
    end
  end

  describe "#invoke!" do
    it "copies the file to the specified destination" do
      empty_directory("doc")
      invoke!
      expect(File.exist?(File.join(destination_root, "doc"))).to be true
    end

    it "shows created status to the user" do
      empty_directory("doc")
      expect(invoke!).to eq("      create  doc\n")
    end

    it "does not create a directory if pretending" do
      base.inside("foo", :pretend => true) do
        empty_directory("ghost")
      end
      expect(File.exist?(File.join(base.destination_root, "ghost"))).to be false
    end

    describe "when directory exists" do
      it "shows exist status" do
        empty_directory("doc")
        invoke!
        expect(invoke!).to eq("       exist  doc\n")
      end
    end
  end

  describe "#revoke!" do
    it "removes the destination file" do
      empty_directory("doc")
      invoke!
      revoke!
      expect(File.exist?(@action.destination)).to be false
    end
  end

  describe "#exists?" do
    it "returns true if the destination file exists" do
      empty_directory("doc")
      expect(@action.exists?).to be false
      invoke!
      expect(@action.exists?).to be true
    end
  end

  context "protected methods" do
    describe "#convert_encoded_instructions" do
      before do
        empty_directory("test_dir")
        allow(@action.base).to receive(:file_name).and_return("expected")
      end

      it "accepts and executes a 'legal' %\w+% encoded instruction" do
        expect(@action.send(:convert_encoded_instructions, "%file_name%.txt")).to eq("expected.txt")
      end

      it "accepts and executes a private %\w+% encoded instruction" do
        @action.base.extend Module.new {
          def private_file_name
            "expected"
          end
          private :private_file_name
        }
        expect(@action.send(:convert_encoded_instructions, "%private_file_name%.txt")).to eq("expected.txt")
      end

      it "ignores an 'illegal' %\w+% encoded instruction" do
        expect(@action.send(:convert_encoded_instructions, "%some_name%.txt")).to eq("%some_name%.txt")
      end

      it "ignores incorrectly encoded instruction" do
        expect(@action.send(:convert_encoded_instructions, "%some.name%.txt")).to eq("%some.name%.txt")
      end
    end
  end
end
