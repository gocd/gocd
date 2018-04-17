# frozen_string_literal: true

include YARD::Parser::Ruby::Legacy
include YARD::Parser::Ruby::Legacy::RubyToken

RSpec.describe YARD::Parser::Ruby::Legacy::TokenList do
  describe "#initialize / #push" do
    it "accepts a tokenlist (via constructor or push)" do
      expect { TokenList.new(TokenList.new) }.not_to raise_error
      expect(TokenList.new.push(TokenList.new("x = 2")).size).to eq 6
    end

    it "accept a token (via constructor or push)" do
      expect { TokenList.new(Token.new(0, 0)) }.not_to raise_error
      expect(TokenList.new.push(Token.new(0, 0), Token.new(1, 1)).size).to eq 2
    end

    it "accepts a string and parse it as code (via constructor or push)" do
      expect { TokenList.new("x = 2") }.not_to raise_error
      x = TokenList.new
      x.push("x", "=", "2")
      expect(x.size).to eq 6
      expect(x.to_s).to eq "x\n=\n2\n"
    end

    it "does not accept any other input" do
      expect { TokenList.new(:notcode) }.to raise_error(ArgumentError)
    end

    it "does not interpolate string data" do
      x = TokenList.new('x = "hello #{world}"')
      expect(x.size).to eq 6
      expect(x[4].class).to eq TkDSTRING
      expect(x.to_s).to eq 'x = "hello #{world}"' + "\n"
    end

    it "handles label syntax" do
      x = TokenList.new('a:1,b:2')
      expect(x[0].class).to eq TkLABEL
      expect(x[0].text).to eq 'a:'
      expect(x[3].class).to eq TkLABEL
      expect(x[3].text).to eq 'b:'
    end
  end

  describe "#to_s" do
    before do
      @t = TokenList.new
      @t << TkDEF.new(1, 1, "def")
      @t << TkSPACE.new(1, 1)
      @t << TkIDENTIFIER.new(1, 1, "x")
      @t << TkStatementEnd.new(1, 1)
      @t << TkSEMICOLON.new(1, 1) << TkSPACE.new(1, 1)
      @t << TkBlockContents.new(1, 1)
      @t << TkSPACE.new(1, 1) << TkEND.new(1, 1, "end")
      @t[0].set_text "def"
      @t[1].set_text " "
      @t[2].set_text "x"
      @t[4].set_text ";"
      @t[5].set_text " "
      @t[7].set_text " "
      @t[8].set_text "end"
    end

    it "only shows the statement portion of the tokens by default" do
      expect(@t.to_s).to eq "def x"
    end

    it "shows ... for the block token if all of the tokens are shown" do
      expect(@t.to_s(true)).to eq "def x; ... end"
    end

    it "ignores ... if show_block = false" do
      expect(@t.to_s(true, false)).to eq "def x;  end"
    end
  end
end
