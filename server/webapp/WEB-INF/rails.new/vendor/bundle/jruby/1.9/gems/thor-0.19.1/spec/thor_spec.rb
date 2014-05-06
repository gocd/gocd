require "helper"

describe Thor do
  describe "#method_option" do
    it "sets options to the next method to be invoked" do
      args = %w[foo bar --force]
      _, options = MyScript.start(args)
      expect(options).to eq("force" => true)
    end

    describe ":lazy_default" do
      it "is absent when option is not specified" do
        _, options = MyScript.start(%w[with_optional])
        expect(options).to eq({})
      end

      it "sets a default that can be overridden for strings" do
        _, options = MyScript.start(%w[with_optional --lazy])
        expect(options).to eq("lazy" => "yes")

        _, options = MyScript.start(%w[with_optional --lazy yesyes!])
        expect(options).to eq("lazy" => "yesyes!")
      end

      it "sets a default that can be overridden for numerics" do
        _, options = MyScript.start(%w[with_optional --lazy-numeric])
        expect(options).to eq("lazy_numeric" => 42)

        _, options = MyScript.start(%w[with_optional --lazy-numeric 20000])
        expect(options).to eq("lazy_numeric" => 20_000)
      end

      it "sets a default that can be overridden for arrays" do
        _, options = MyScript.start(%w[with_optional --lazy-array])
        expect(options).to eq("lazy_array" => %w[eat at joes])

        _, options = MyScript.start(%w[with_optional --lazy-array hello there])
        expect(options).to eq("lazy_array" => %w[hello there])
      end

      it "sets a default that can be overridden for hashes" do
        _, options = MyScript.start(%w[with_optional --lazy-hash])
        expect(options).to eq("lazy_hash" => {"swedish" => "meatballs"})

        _, options = MyScript.start(%w[with_optional --lazy-hash polish:sausage])
        expect(options).to eq("lazy_hash" => {"polish" => "sausage"})
      end
    end

    describe "when :for is supplied" do
      it "updates an already defined command" do
        _, options = MyChildScript.start(%w[animal horse --other=fish])
        expect(options[:other]).to eq("fish")
      end

      describe "and the target is on the parent class" do
        it "updates an already defined command" do
          args = %w[example_default_command my_param --new-option=verified]
          options = Scripts::MyScript.start(args)
          expect(options[:new_option]).to eq("verified")
        end

        it "adds a command to the command list if the updated command is on the parent class" do
          expect(Scripts::MyScript.commands["example_default_command"]).to be
        end

        it "clones the parent command" do
          expect(Scripts::MyScript.commands["example_default_command"]).not_to eq(MyChildScript.commands["example_default_command"])
        end
      end
    end
  end

  describe "#default_command" do
    it "sets a default command" do
      expect(MyScript.default_command).to eq("example_default_command")
    end

    it "invokes the default command if no command is specified" do
      expect(MyScript.start([])).to eq("default command")
    end

    it "invokes the default command if no command is specified even if switches are given" do
      expect(MyScript.start(%w[--with option])).to eq("with" => "option")
    end

    it "inherits the default command from parent" do
      expect(MyChildScript.default_command).to eq("example_default_command")
    end
  end

  describe "#stop_on_unknown_option!" do
    my_script = Class.new(Thor) do
      class_option "verbose",   :type => :boolean
      class_option "mode",      :type => :string

      stop_on_unknown_option! :exec

      desc "exec", "Run a command"
      def exec(*args)
        [options, args]
      end

      desc "boring", "An ordinary command"
      def boring(*args)
        [options, args]
      end
    end

    it "passes remaining args to command when it encounters a non-option" do
      expect(my_script.start(%w[exec command --verbose])).to eq [{}, %w[command --verbose]]
    end

    it "passes remaining args to command when it encounters an unknown option" do
      expect(my_script.start(%w[exec --foo command --bar])).to eq [{}, %w[--foo command --bar]]
    end

    it "still accepts options that are given before non-options" do
      expect(my_script.start(%w[exec --verbose command --foo])).to eq [{"verbose" => true}, %w[command --foo]]
    end

    it "still accepts options that require a value" do
      expect(my_script.start(%w[exec --mode rashly command])).to eq [{"mode" => "rashly"}, %w[command]]
    end

    it "still passes everything after -- to command" do
      expect(my_script.start(%w[exec -- --verbose])).to eq [{}, %w[--verbose]]
    end

    it "does not affect ordinary commands"  do
      expect(my_script.start(%w[boring command --verbose])).to eq [{"verbose" => true}, %w[command]]
    end

    context "when provided with multiple command names" do
      klass = Class.new(Thor) do
        stop_on_unknown_option! :foo, :bar
      end
      it "affects all specified commands" do
        expect(klass.stop_on_unknown_option?(double(:name => "foo"))).to be true
        expect(klass.stop_on_unknown_option?(double(:name => "bar"))).to be true
        expect(klass.stop_on_unknown_option?(double(:name => "baz"))).to be false
      end
    end

    context "when invoked several times" do
      klass = Class.new(Thor) do
        stop_on_unknown_option! :foo
        stop_on_unknown_option! :bar
      end
      it "affects all specified commands" do
        expect(klass.stop_on_unknown_option?(double(:name => "foo"))).to be true
        expect(klass.stop_on_unknown_option?(double(:name => "bar"))).to be true
        expect(klass.stop_on_unknown_option?(double(:name => "baz"))).to be false
      end
    end

    it "doesn't break new" do
      expect(my_script.new).to be_a(Thor)
    end
  end

  describe "#map" do
    it "calls the alias of a method if one is provided" do
      expect(MyScript.start(%w[-T fish])).to eq(%w[fish])
    end

    it "calls the alias of a method if several are provided via #map" do
      expect(MyScript.start(%w[-f fish])).to eq(["fish", {}])
      expect(MyScript.start(%w[--foo fish])).to eq(["fish", {}])
    end

    it "inherits all mappings from parent" do
      expect(MyChildScript.default_command).to eq("example_default_command")
    end
  end

  describe "#package_name" do
    it "provides a proper description for a command when the package_name is assigned" do
      content = capture(:stdout) { PackageNameScript.start(%w[help]) }
      expect(content).to match(/Baboon commands:/m)
    end

    # TODO: remove this, might be redundant, just wanted to prove full coverage
    it "provides a proper description for a command when the package_name is NOT assigned" do
      content = capture(:stdout) { MyScript.start(%w[help]) }
      expect(content).to match(/Commands:/m)
    end
  end

  describe "#desc" do
    it "provides description for a command" do
      content = capture(:stdout) { MyScript.start(%w[help]) }
      expect(content).to match(/thor my_script:zoo\s+# zoo around/m)
    end

    it "provides no namespace if $thor_runner is false" do
      begin
        $thor_runner = false
        content = capture(:stdout) { MyScript.start(%w[help]) }
        expect(content).to match(/thor zoo\s+# zoo around/m)
      ensure
        $thor_runner = true
      end
    end

    describe "when :for is supplied" do
      it "overwrites a previous defined command" do
        expect(capture(:stdout) { MyChildScript.start(%w[help]) }).to match(/animal KIND \s+# fish around/m)
      end
    end

    describe "when :hide is supplied" do
      it "does not show the command in help" do
        expect(capture(:stdout) { MyScript.start(%w[help]) }).not_to match(/this is hidden/m)
      end

      it "but the command is still invokable, does not show the command in help" do
        expect(MyScript.start(%w[hidden yesyes])).to eq(%w[yesyes])
      end
    end
  end

  describe "#method_options" do
    it "sets default options if called before an initializer" do
      options = MyChildScript.class_options
      expect(options[:force].type).to eq(:boolean)
      expect(options[:param].type).to eq(:numeric)
    end

    it "overwrites default options if called on the method scope" do
      args = %w[zoo --force --param feathers]
      options = MyChildScript.start(args)
      expect(options).to eq("force" => true, "param" => "feathers")
    end

    it "allows default options to be merged with method options" do
      args = %w[animal bird --force --param 1.0 --other tweets]
      arg, options = MyChildScript.start(args)
      expect(arg).to eq("bird")
      expect(options).to eq("force" => true, "param" => 1.0, "other" => "tweets")
    end
  end

  describe "#start" do
    it "calls a no-param method when no params are passed" do
      expect(MyScript.start(%w[zoo])).to eq(true)
    end

    it "calls a single-param method when a single param is passed" do
      expect(MyScript.start(%w[animal fish])).to eq(%w[fish])
    end

    it "does not set options in attributes" do
      expect(MyScript.start(%w[with_optional --all])).to eq([nil, {"all" => true}, []])
    end

    it "raises an error if the wrong number of params are provided" do
      arity_asserter = lambda do |args, msg|
        stderr = capture(:stderr) { Scripts::Arities.start(args) }
        expect(stderr.strip).to eq(msg)
      end
      arity_asserter.call %w[zero_args one], %Q(ERROR: "thor zero_args" was called with arguments ["one"]
Usage: "thor scripts:arities:zero_args")
      arity_asserter.call %w[one_arg], %Q(ERROR: "thor one_arg" was called with no arguments
Usage: "thor scripts:arities:one_arg ARG")
      arity_asserter.call %w[one_arg one two], %Q(ERROR: "thor one_arg" was called with arguments ["one", "two"]
Usage: "thor scripts:arities:one_arg ARG")
      arity_asserter.call %w[one_arg one two], %Q(ERROR: "thor one_arg" was called with arguments ["one", "two"]
Usage: "thor scripts:arities:one_arg ARG")
      arity_asserter.call %w[two_args one], %Q(ERROR: "thor two_args" was called with arguments ["one"]
Usage: "thor scripts:arities:two_args ARG1 ARG2")
      arity_asserter.call %w[optional_arg one two], %Q(ERROR: "thor optional_arg" was called with arguments ["one", "two"]
Usage: "thor scripts:arities:optional_arg [ARG]")
    end

    it "raises an error if the invoked command does not exist" do
      expect(capture(:stderr) { Amazing.start(%w[animal]) }.strip).to eq('Could not find command "animal" in "amazing" namespace.')
    end

    it "calls method_missing if an unknown method is passed in" do
      expect(MyScript.start(%w[unk hello])).to eq([:unk, %w[hello]])
    end

    it "does not call a private method no matter what" do
      expect(capture(:stderr) { MyScript.start(%w[what]) }.strip).to eq('Could not find command "what" in "my_script" namespace.')
    end

    it "uses command default options" do
      options = MyChildScript.start(%w[animal fish]).last
      expect(options).to eq("other" => "method default")
    end

    it "raises when an exception happens within the command call" do
      expect { MyScript.start(%w[call_myself_with_wrong_arity]) }.to raise_error(ArgumentError)
    end

    context "when the user enters an unambiguous substring of a command" do
      it "invokes a command" do
        expect(MyScript.start(%w[z])).to eq(MyScript.start(%w[zoo]))
      end

      it "invokes a command, even when there's an alias it resolves to the same command" do
        expect(MyScript.start(%w[hi arg])).to eq(MyScript.start(%w[hidden arg]))
      end

      it "invokes an alias" do
        expect(MyScript.start(%w[animal_pri])).to eq(MyScript.start(%w[zoo]))
      end
    end

    context "when the user enters an ambiguous substring of a command" do
      it "raises an exception and displays a message that explains the ambiguity" do
        shell = Thor::Base.shell.new
        expect(shell).to receive(:error).with("Ambiguous command call matches [call_myself_with_wrong_arity, call_unexistent_method]")
        MyScript.start(%w[call], :shell => shell)
      end

      it "raises an exception when there is an alias" do
        shell = Thor::Base.shell.new
        expect(shell).to receive(:error).with("Ambiguous command f matches [foo, fu]")
        MyScript.start(%w[f], :shell => shell)
      end
    end

  end

  describe "#help" do
    def shell
      @shell ||= Thor::Base.shell.new
    end

    describe "on general" do
      before do
        @content = capture(:stdout) { MyScript.help(shell) }
      end

      it "provides useful help info for the help method itself" do
        expect(@content).to match(/help \[COMMAND\]\s+# Describe available commands/)
      end

      it "provides useful help info for a method with params" do
        expect(@content).to match(/animal TYPE\s+# horse around/)
      end

      it "uses the maximum terminal size to show commands" do
        expect(@shell).to receive(:terminal_width).and_return(80)
        content = capture(:stdout) { MyScript.help(shell) }
        expect(content).to match(/aaa\.\.\.$/)
      end

      it "provides description for commands from classes in the same namespace" do
        expect(@content).to match(/baz\s+# do some bazing/)
      end

      it "shows superclass commands" do
        content = capture(:stdout) { MyChildScript.help(shell) }
        expect(content).to match(/foo BAR \s+# do some fooing/)
      end

      it "shows class options information" do
        content = capture(:stdout) { MyChildScript.help(shell) }
        expect(content).to match(/Options\:/)
        expect(content).to match(/\[\-\-param=N\]/)
      end

      it "injects class arguments into default usage" do
        content = capture(:stdout) { Scripts::MyScript.help(shell) }
        expect(content).to match(/zoo ACCESSOR \-\-param\=PARAM/)
      end
    end

    describe "for a specific command" do
      it "provides full help info when talking about a specific command" do
        expect(capture(:stdout) { MyScript.command_help(shell, "foo") }).to eq(<<-END)
Usage:
  thor my_script:foo BAR

Options:
  [--force]  # Force to do some fooing

do some fooing
  This is more info!
  Everyone likes more info!
END
      end

      it "raises an error if the command can't be found" do
        expect do
          MyScript.command_help(shell, "unknown")
        end.to raise_error(Thor::UndefinedCommandError, 'Could not find command "unknown" in "my_script" namespace.')
      end

      it "normalizes names before claiming they don't exist" do
        expect(capture(:stdout) { MyScript.command_help(shell, "name-with-dashes") }).to match(/thor my_script:name-with-dashes/)
      end

      it "uses the long description if it exists" do
        expect(capture(:stdout) { MyScript.command_help(shell, "long_description") }).to eq(<<-HELP)
Usage:
  thor my_script:long_description

Description:
  This is a really really really long description. Here you go. So very long.

  It even has two paragraphs.
HELP
      end

      it "doesn't assign the long description to the next command without one" do
        expect(capture(:stdout) do
          MyScript.command_help(shell, "name_with_dashes")
        end).not_to match(/so very long/i)
      end
    end

    describe "instance method" do
      it "calls the class method" do
        expect(capture(:stdout) { MyScript.start(%w[help]) }).to match(/Commands:/)
      end

      it "calls the class method" do
        expect(capture(:stdout) { MyScript.start(%w[help foo]) }).to match(/Usage:/)
      end
    end
  end

  describe "when creating commands" do
    it "prints a warning if a public method is created without description or usage" do
      expect(capture(:stdout) do
        klass = Class.new(Thor)
        klass.class_eval "def hello_from_thor; end"
      end).to match(/\[WARNING\] Attempted to create command "hello_from_thor" without usage or description/)
    end

    it "does not print if overwriting a previous command" do
      expect(capture(:stdout) do
        klass = Class.new(Thor)
        klass.class_eval "def help; end"
      end).to be_empty
    end
  end

  describe "edge-cases" do
    it "can handle boolean options followed by arguments" do
      klass = Class.new(Thor) do
        method_option :loud, :type => :boolean
        desc "hi NAME", "say hi to name"
        def hi(name)
          name.upcase! if options[:loud]
          "Hi #{name}"
        end
      end

      expect(klass.start(%w[hi jose])).to eq("Hi jose")
      expect(klass.start(%w[hi jose --loud])).to eq("Hi JOSE")
      expect(klass.start(%w[hi --loud jose])).to eq("Hi JOSE")
    end

    it "passes through unknown options" do
      klass = Class.new(Thor) do
        desc "unknown", "passing unknown options"
        def unknown(*args)
          args
        end
      end

      expect(klass.start(%w[unknown foo --bar baz bat --bam])).to eq(%w[foo --bar baz bat --bam])
      expect(klass.start(%w[unknown --bar baz])).to eq(%w[--bar baz])
    end

    it "does not pass through unknown options with strict args" do
      klass = Class.new(Thor) do
        strict_args_position!

        desc "unknown", "passing unknown options"
        def unknown(*args)
          args
        end
      end

      expect(klass.start(%w[unknown --bar baz])).to eq([])
      expect(klass.start(%w[unknown foo --bar baz])).to eq(%w[foo])
    end

    it "strict args works in the inheritance chain" do
      parent = Class.new(Thor) do
        strict_args_position!
      end

      klass = Class.new(parent) do
        desc "unknown", "passing unknown options"
        def unknown(*args)
          args
        end
      end

      expect(klass.start(%w[unknown --bar baz])).to eq([])
      expect(klass.start(%w[unknown foo --bar baz])).to eq(%w[foo])
    end

    it "send as a command name" do
      expect(MyScript.start(%w[send])).to eq(true)
    end
  end
end
