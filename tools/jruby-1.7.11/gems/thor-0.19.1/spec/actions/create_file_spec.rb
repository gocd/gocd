require "helper"
require "thor/actions"

describe Thor::Actions::CreateFile do
  before do
    @silence = false
    ::FileUtils.rm_rf(destination_root)
  end

  def create_file(destination = nil, config = {}, options = {})
    @base = MyCounter.new([1, 2], options, :destination_root => destination_root)
    allow(@base).to receive(:file_name).and_return("rdoc")

    @action = Thor::Actions::CreateFile.new(@base, destination, "CONFIGURATION", {:verbose => !@silence}.merge(config))
  end

  def invoke!
    capture(:stdout) { @action.invoke! }
  end

  def revoke!
    capture(:stdout) { @action.revoke! }
  end

  def silence!
    @silence = true
  end

  describe "#invoke!" do
    it "creates a file" do
      create_file("doc/config.rb")
      invoke!
      expect(File.exist?(File.join(destination_root, "doc/config.rb"))).to be true
    end

    it "does not create a file if pretending" do
      create_file("doc/config.rb", {}, :pretend => true)
      invoke!
      expect(File.exist?(File.join(destination_root, "doc/config.rb"))).to be false
    end

    it "shows created status to the user" do
      create_file("doc/config.rb")
      expect(invoke!).to eq("      create  doc/config.rb\n")
    end

    it "does not show any information if log status is false" do
      silence!
      create_file("doc/config.rb")
      expect(invoke!).to be_empty
    end

    it "returns the given destination" do
      capture(:stdout) do
        expect(create_file("doc/config.rb").invoke!).to eq("doc/config.rb")
      end
    end

    it "converts encoded instructions" do
      create_file("doc/%file_name%.rb.tt")
      invoke!
      expect(File.exist?(File.join(destination_root, "doc/rdoc.rb.tt"))).to be true
    end

    describe "when file exists" do
      before do
        create_file("doc/config.rb")
        invoke!
      end

      describe "and is identical" do
        it "shows identical status" do
          create_file("doc/config.rb")
          invoke!
          expect(invoke!).to eq("   identical  doc/config.rb\n")
        end
      end

      describe "and is not identical" do
        before do
          File.open(File.join(destination_root, "doc/config.rb"), "w") { |f| f.write("FOO = 3") }
        end

        it "shows forced status to the user if force is given" do
          expect(create_file("doc/config.rb", {}, :force => true)).not_to be_identical
          expect(invoke!).to eq("       force  doc/config.rb\n")
        end

        it "shows skipped status to the user if skip is given" do
          expect(create_file("doc/config.rb", {}, :skip => true)).not_to be_identical
          expect(invoke!).to eq("        skip  doc/config.rb\n")
        end

        it "shows forced status to the user if force is configured" do
          expect(create_file("doc/config.rb", :force => true)).not_to be_identical
          expect(invoke!).to eq("       force  doc/config.rb\n")
        end

        it "shows skipped status to the user if skip is configured" do
          expect(create_file("doc/config.rb", :skip => true)).not_to be_identical
          expect(invoke!).to eq("        skip  doc/config.rb\n")
        end

        it "shows conflict status to the user" do
          file = File.join(destination_root, "doc/config.rb")
          expect(create_file("doc/config.rb")).not_to be_identical
          expect(Thor::LineEditor).to receive(:readline).with("Overwrite #{file}? (enter \"h\" for help) [Ynaqdh] ", anything).and_return("s")

          content = invoke!
          expect(content).to match(/conflict  doc\/config\.rb/)
          expect(content).to match(/skip  doc\/config\.rb/)
        end

        it "creates the file if the file collision menu returns true" do
          create_file("doc/config.rb")
          expect(Thor::LineEditor).to receive(:readline).and_return("y")
          expect(invoke!).to match(/force  doc\/config\.rb/)
        end

        it "skips the file if the file collision menu returns false" do
          create_file("doc/config.rb")
          expect(Thor::LineEditor).to receive(:readline).and_return("n")
          expect(invoke!).to match(/skip  doc\/config\.rb/)
        end

        it "executes the block given to show file content" do
          create_file("doc/config.rb")
          expect(Thor::LineEditor).to receive(:readline).and_return("d", "n")
          expect(@base.shell).to receive(:system).with(/diff -u/)
          invoke!
        end
      end
    end
  end

  describe "#revoke!" do
    it "removes the destination file" do
      create_file("doc/config.rb")
      invoke!
      revoke!
      expect(File.exist?(@action.destination)).to be false
    end

    it "does not raise an error if the file does not exist" do
      create_file("doc/config.rb")
      revoke!
      expect(File.exist?(@action.destination)).to be false
    end
  end

  describe "#exists?" do
    it "returns true if the destination file exists" do
      create_file("doc/config.rb")
      expect(@action.exists?).to be false
      invoke!
      expect(@action.exists?).to be true
    end
  end

  describe "#identical?" do
    it "returns true if the destination file exists and is identical" do
      create_file("doc/config.rb")
      expect(@action.identical?).to be false
      invoke!
      expect(@action.identical?).to be true
    end
  end
end
