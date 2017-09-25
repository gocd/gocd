require "helper"

describe Thor::Shell::Color do
  def shell
    @shell ||= Thor::Shell::Color.new
  end

  before do
    allow($stdout).to receive(:tty?).and_return(true)
    allow_any_instance_of(StringIO).to receive(:tty?).and_return(true)
  end

  describe "#ask" do
    it "sets the color if specified and tty?" do
      expect(Thor::LineEditor).to receive(:readline).with("\e[32mIs this green? \e[0m", anything).and_return("yes")
      shell.ask "Is this green?", :green

      expect(Thor::LineEditor).to receive(:readline).with("\e[32mIs this green? [Yes, No, Maybe] \e[0m", anything).and_return("Yes")
      shell.ask "Is this green?", :green, :limited_to => %w[Yes No Maybe]
    end
  end

  describe "#say" do
    it "set the color if specified and tty?" do
      out = capture(:stdout) do
        shell.say "Wow! Now we have colors!", :green
      end

      expect(out.chomp).to eq("\e[32mWow! Now we have colors!\e[0m")
    end

    it "does not set the color if output is not a tty" do
      out = capture(:stdout) do
        expect($stdout).to receive(:tty?).and_return(false)
        shell.say "Wow! Now we have colors!", :green
      end

      expect(out.chomp).to eq("Wow! Now we have colors!")
    end

    it "does not use a new line even with colors" do
      out = capture(:stdout) do
        shell.say "Wow! Now we have colors! ", :green
      end

      expect(out.chomp).to eq("\e[32mWow! Now we have colors! \e[0m")
    end

    it "handles an Array of colors" do
      out = capture(:stdout) do
        shell.say "Wow! Now we have colors *and* background colors", [:green, :on_red, :bold]
      end

      expect(out.chomp).to eq("\e[32m\e[41m\e[1mWow! Now we have colors *and* background colors\e[0m")
    end
  end

  describe "#say_status" do
    it "uses color to say status" do
      out = capture(:stdout) do
        shell.say_status :conflict, "README", :red
      end

      expect(out.chomp).to eq("\e[1m\e[31m    conflict\e[0m  README")
    end
  end

  describe "#set_color" do
    it "colors a string with a foreground color" do
      red = shell.set_color "hi!", :red
      expect(red).to eq("\e[31mhi!\e[0m")
    end

    it "colors a string with a background color" do
      on_red = shell.set_color "hi!", :white, :on_red
      expect(on_red).to eq("\e[37m\e[41mhi!\e[0m")
    end

    it "colors a string with a bold color" do
      bold = shell.set_color "hi!", :white, true
      expect(bold).to eq("\e[1m\e[37mhi!\e[0m")

      bold = shell.set_color "hi!", :white, :bold
      expect(bold).to eq("\e[37m\e[1mhi!\e[0m")

      bold = shell.set_color "hi!", :white, :on_red, :bold
      expect(bold).to eq("\e[37m\e[41m\e[1mhi!\e[0m")
    end

    it "does nothing when there are no colors" do
      colorless = shell.set_color "hi!", nil
      expect(colorless).to eq("hi!")

      colorless = shell.set_color "hi!"
      expect(colorless).to eq("hi!")
    end

    it "does nothing when the terminal does not support color" do
      allow($stdout).to receive(:tty?).and_return(false)
      colorless = shell.set_color "hi!", :white
      expect(colorless).to eq("hi!")
    end
  end

  describe "#file_collision" do
    describe "when a block is given" do
      it "invokes the diff command" do
        allow($stdout).to receive(:print)
        allow($stdout).to receive(:tty?).and_return(true)
        expect(Thor::LineEditor).to receive(:readline).and_return("d", "n")

        output = capture(:stdout) { shell.file_collision("spec/fixtures/doc/README") { "README\nEND\n" } }
        expect(output).to match(/\e\[31m\- __start__\e\[0m/)
        expect(output).to match(/^  README/)
        expect(output).to match(/\e\[32m\+ END\e\[0m/)
      end
    end
  end
end
