require "helper"

class Application; end

describe Thor::Actions do
  def runner(options = {})
    @runner ||= MyCounter.new([1], options, :destination_root => destination_root)
  end

  def action(*args, &block)
    capture(:stdout) { runner.send(*args, &block) }
  end

  def exists_and_identical?(source, destination)
    destination = File.join(destination_root, destination)
    expect(File.exist?(destination)).to be true

    source = File.join(source_root, source)
    expect(FileUtils).to be_identical(source, destination)
  end

  def file
    File.join(destination_root, "foo")
  end

  before do
    ::FileUtils.rm_rf(destination_root)
  end

  describe "#chmod" do
    it "executes the command given" do
      expect(FileUtils).to receive(:chmod_R).with(0755, file) # rubocop:disable SymbolName
      action :chmod, "foo", 0755
    end

    it "does not execute the command if pretending" do
      expect(FileUtils).not_to receive(:chmod_R) # rubocop:disable SymbolName
      runner(:pretend => true)
      action :chmod, "foo", 0755
    end

    it "logs status" do
      expect(FileUtils).to receive(:chmod_R).with(0755, file) # rubocop:disable SymbolName
      expect(action(:chmod, "foo", 0755)).to eq("       chmod  foo\n")
    end

    it "does not log status if required" do
      expect(FileUtils).to receive(:chmod_R).with(0755, file) # rubocop:disable SymbolName
      expect(action(:chmod, "foo", 0755, :verbose => false)).to be_empty
    end
  end

  describe "#copy_file" do
    it "copies file from source to default destination" do
      action :copy_file, "command.thor"
      exists_and_identical?("command.thor", "command.thor")
    end

    it "copies file from source to the specified destination" do
      action :copy_file, "command.thor", "foo.thor"
      exists_and_identical?("command.thor", "foo.thor")
    end

    it "copies file from the source relative to the current path" do
      runner.inside("doc") do
        action :copy_file, "README"
      end
      exists_and_identical?("doc/README", "doc/README")
    end

    it "copies file from source to default destination and preserves file mode" do
      action :copy_file, "preserve/script.sh", :mode => :preserve
      original = File.join(source_root, "preserve/script.sh")
      copy = File.join(destination_root, "preserve/script.sh")
      expect(File.stat(original).mode).to eq(File.stat(copy).mode)
    end

    it "logs status" do
      expect(action(:copy_file, "command.thor")).to eq("      create  command.thor\n")
    end

    it "accepts a block to change output" do
      action :copy_file, "command.thor" do |content|
        "OMG" + content
      end
      expect(File.read(File.join(destination_root, "command.thor"))).to match(/^OMG/)
    end
  end

  describe "#link_file" do
    it "links file from source to default destination" do
      action :link_file, "command.thor"
      exists_and_identical?("command.thor", "command.thor")
    end

    it "links file from source to the specified destination" do
      action :link_file, "command.thor", "foo.thor"
      exists_and_identical?("command.thor", "foo.thor")
    end

    it "links file from the source relative to the current path" do
      runner.inside("doc") do
        action :link_file, "README"
      end
      exists_and_identical?("doc/README", "doc/README")
    end

    it "logs status" do
      expect(action(:link_file, "command.thor")).to eq("      create  command.thor\n")
    end
  end

  describe "#get" do
    it "copies file from source to the specified destination" do
      action :get, "doc/README", "docs/README"
      exists_and_identical?("doc/README", "docs/README")
    end

    it "uses just the source basename as destination if none is specified" do
      action :get, "doc/README"
      exists_and_identical?("doc/README", "README")
    end

    it "allows the destination to be set as a block result" do
      action(:get, "doc/README") { |c| "docs/README" }
      exists_and_identical?("doc/README", "docs/README")
    end

    it "yields file content to a block" do
      action :get, "doc/README" do |content|
        expect(content).to eq("__start__\nREADME\n__end__\n")
      end
    end

    it "logs status" do
      expect(action(:get, "doc/README", "docs/README")).to eq("      create  docs/README\n")
    end

    it "accepts http remote sources" do
      body = "__start__\nHTTPFILE\n__end__\n"
      FakeWeb.register_uri(:get, "http://example.com/file.txt", :body => body)
      action :get, "http://example.com/file.txt" do |content|
        expect(content).to eq(body)
      end
      FakeWeb.clean_registry
    end

    it "accepts https remote sources" do
      body = "__start__\nHTTPSFILE\n__end__\n"
      FakeWeb.register_uri(:get, "https://example.com/file.txt", :body => body)
      action :get, "https://example.com/file.txt" do |content|
        expect(content).to eq(body)
      end
      FakeWeb.clean_registry
    end
  end

  describe "#template" do
    it "allows using block helpers in the template" do
      action :template, "doc/block_helper.rb"

      file = File.join(destination_root, "doc/block_helper.rb")
      expect(File.read(file)).to eq("Hello world!")
    end

    it "evaluates the template given as source" do
      runner.instance_variable_set("@klass", "Config")
      action :template, "doc/config.rb"

      file = File.join(destination_root, "doc/config.rb")
      expect(File.read(file)).to eq("class Config; end\n")
    end

    it "copies the template to the specified destination" do
      runner.instance_variable_set("@klass", "Config")
      action :template, "doc/config.rb", "doc/configuration.rb"
      file = File.join(destination_root, "doc/configuration.rb")
      expect(File.exist?(file)).to be true
    end

    it "converts encoded instructions" do
      expect(runner).to receive(:file_name).and_return("rdoc")
      action :template, "doc/%file_name%.rb.tt"
      file = File.join(destination_root, "doc/rdoc.rb")
      expect(File.exist?(file)).to be true
    end

    it "accepts filename without .tt for template method" do
      expect(runner).to receive(:file_name).and_return("rdoc")
      action :template, "doc/%file_name%.rb"
      file = File.join(destination_root, "doc/rdoc.rb")
      expect(File.exist?(file)).to be true
    end

    it "logs status" do
      runner.instance_variable_set("@klass", "Config")
      expect(capture(:stdout) { runner.template("doc/config.rb") }).to eq("      create  doc/config.rb\n")
    end

    it "accepts a block to change output" do
      runner.instance_variable_set("@klass", "Config")
      action :template, "doc/config.rb" do |content|
        "OMG" + content
      end
      expect(File.read(File.join(destination_root, "doc/config.rb"))).to match(/^OMG/)
    end

    it "guesses the destination name when given only a source" do
      action :template, "doc/config.yaml.tt"

      file = File.join(destination_root, "doc/config.yaml")
      expect(File.exist?(file)).to be true
    end
  end

  describe "when changing existent files" do
    before do
      ::FileUtils.cp_r(source_root, destination_root)
    end

    def file
      File.join(destination_root, "doc", "README")
    end

    describe "#remove_file" do
      it "removes the file given" do
        action :remove_file, "doc/README"
        expect(File.exist?(file)).to be false
      end

      it "removes directories too" do
        action :remove_dir, "doc"
        expect(File.exist?(File.join(destination_root, "doc"))).to be false
      end

      it "does not remove if pretending" do
        runner(:pretend => true)
        action :remove_file, "doc/README"
        expect(File.exist?(file)).to be true
      end

      it "logs status" do
        expect(action(:remove_file, "doc/README")).to eq("      remove  doc/README\n")
      end

      it "does not log status if required" do
        expect(action(:remove_file, "doc/README", :verbose => false)).to be_empty
      end
    end

    describe "#gsub_file" do
      it "replaces the content in the file" do
        action :gsub_file, "doc/README", "__start__", "START"
        expect(File.binread(file)).to eq("START\nREADME\n__end__\n")
      end

      it "does not replace if pretending" do
        runner(:pretend => true)
        action :gsub_file, "doc/README", "__start__", "START"
        expect(File.binread(file)).to eq("__start__\nREADME\n__end__\n")
      end

      it "accepts a block" do
        action(:gsub_file, "doc/README", "__start__") { |match| match.gsub("__", "").upcase  }
        expect(File.binread(file)).to eq("START\nREADME\n__end__\n")
      end

      it "logs status" do
        expect(action(:gsub_file, "doc/README", "__start__", "START")).to eq("        gsub  doc/README\n")
      end

      it "does not log status if required" do
        expect(action(:gsub_file, file, "__", :verbose => false) { |match| match * 2 }).to be_empty
      end
    end

    describe "#append_to_file" do
      it "appends content to the file" do
        action :append_to_file, "doc/README", "END\n"
        expect(File.binread(file)).to eq("__start__\nREADME\n__end__\nEND\n")
      end

      it "accepts a block" do
        action(:append_to_file, "doc/README") { "END\n" }
        expect(File.binread(file)).to eq("__start__\nREADME\n__end__\nEND\n")
      end

      it "logs status" do
        expect(action(:append_to_file, "doc/README", "END")).to eq("      append  doc/README\n")
      end
    end

    describe "#prepend_to_file" do
      it "prepends content to the file" do
        action :prepend_to_file, "doc/README", "START\n"
        expect(File.binread(file)).to eq("START\n__start__\nREADME\n__end__\n")
      end

      it "accepts a block" do
        action(:prepend_to_file, "doc/README") { "START\n" }
        expect(File.binread(file)).to eq("START\n__start__\nREADME\n__end__\n")
      end

      it "logs status" do
        expect(action(:prepend_to_file, "doc/README", "START")).to eq("     prepend  doc/README\n")
      end
    end

    describe "#inject_into_class" do
      def file
        File.join(destination_root, "application.rb")
      end

      it "appends content to a class" do
        action :inject_into_class, "application.rb", Application, "  filter_parameters :password\n"
        expect(File.binread(file)).to eq("class Application < Base\n  filter_parameters :password\nend\n")
      end

      it "accepts a block" do
        action(:inject_into_class, "application.rb", Application) { "  filter_parameters :password\n" }
        expect(File.binread(file)).to eq("class Application < Base\n  filter_parameters :password\nend\n")
      end

      it "logs status" do
        expect(action(:inject_into_class, "application.rb", Application, "  filter_parameters :password\n")).to eq("      insert  application.rb\n")
      end

      it "does not append if class name does not match" do
        action :inject_into_class, "application.rb", "App", "  filter_parameters :password\n"
        expect(File.binread(file)).to eq("class Application < Base\nend\n")
      end
    end
  end

  describe "when adjusting comments" do
    before do
      ::FileUtils.cp_r(source_root, destination_root)
    end

    def file
      File.join(destination_root, "doc", "COMMENTER")
    end

    unmodified_comments_file = /__start__\n # greenblue\n#\n# yellowblue\n#yellowred\n #greenred\norange\n    purple\n  ind#igo\n  # ind#igo\n__end__/

    describe "#uncomment_lines" do
      it "uncomments all matching lines in the file" do
        action :uncomment_lines, "doc/COMMENTER", "green"
        expect(File.binread(file)).to match(/__start__\n greenblue\n#\n# yellowblue\n#yellowred\n greenred\norange\n    purple\n  ind#igo\n  # ind#igo\n__end__/)

        action :uncomment_lines, "doc/COMMENTER", "red"
        expect(File.binread(file)).to match(/__start__\n greenblue\n#\n# yellowblue\nyellowred\n greenred\norange\n    purple\n  ind#igo\n  # ind#igo\n__end__/)
      end

      it "correctly uncomments lines with hashes in them" do
        action :uncomment_lines, "doc/COMMENTER", "ind#igo"
        expect(File.binread(file)).to match(/__start__\n # greenblue\n#\n# yellowblue\n#yellowred\n #greenred\norange\n    purple\n  ind#igo\n  ind#igo\n__end__/)
      end

      it "does not modify already uncommented lines in the file" do
        action :uncomment_lines, "doc/COMMENTER", "orange"
        action :uncomment_lines, "doc/COMMENTER", "purple"
        expect(File.binread(file)).to match(unmodified_comments_file)
      end

      it "does not uncomment the wrong line when uncommenting lines preceded by blank commented line" do
        action :uncomment_lines, "doc/COMMENTER", "yellow"
        expect(File.binread(file)).to match(/__start__\n # greenblue\n#\nyellowblue\nyellowred\n #greenred\norange\n    purple\n  ind#igo\n  # ind#igo\n__end__/)
      end
    end

    describe "#comment_lines" do
      it "comments lines which are not commented" do
        action :comment_lines, "doc/COMMENTER", "orange"
        expect(File.binread(file)).to match(/__start__\n # greenblue\n#\n# yellowblue\n#yellowred\n #greenred\n# orange\n    purple\n  ind#igo\n  # ind#igo\n__end__/)

        action :comment_lines, "doc/COMMENTER", "purple"
        expect(File.binread(file)).to match(/__start__\n # greenblue\n#\n# yellowblue\n#yellowred\n #greenred\n# orange\n    # purple\n  ind#igo\n  # ind#igo\n__end__/)
      end

      it "correctly comments lines with hashes in them" do
        action :comment_lines, "doc/COMMENTER", "ind#igo"
        expect(File.binread(file)).to match(/__start__\n # greenblue\n#\n# yellowblue\n#yellowred\n #greenred\norange\n    purple\n  # ind#igo\n  # ind#igo\n__end__/)
      end

      it "does not modify already commented lines" do
        action :comment_lines, "doc/COMMENTER", "green"
        expect(File.binread(file)).to match(unmodified_comments_file)
      end
    end
  end
end
