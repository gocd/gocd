#--
# = COPYRIGHT:
#
#   Copyright (c) 1998-2003 Minero Aoki <aamine@loveruby.net>
#
#   Permission is hereby granted, free of charge, to any person obtaining
#   a copy of this software and associated documentation files (the
#   "Software"), to deal in the Software without restriction, including
#   without limitation the rights to use, copy, modify, merge, publish,
#   distribute, sublicense, and/or sell copies of the Software, and to
#   permit persons to whom the Software is furnished to do so, subject to
#   the following conditions:
#
#   The above copyright notice and this permission notice shall be
#   included in all copies or substantial portions of the Software.
#
#   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
#   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
#   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
#   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
#   LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
#   OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
#   WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
#   Note: Originally licensed under LGPL v2+. Using MIT license for Rails
#   with permission of Minero Aoki.
#++
#:stopdoc:
require 'nkf'
require 'tmail/base64'
require 'tmail/stringio'
require 'tmail/utils'
#:startdoc:


module TMail
  
  #:stopdoc:
  class << self
    attr_accessor :KCODE
  end
  self.KCODE = 'NONE'

  module StrategyInterface

    def create_dest( obj )
      case obj
      when nil
        StringOutput.new
      when String
        StringOutput.new(obj)
      when IO, StringOutput
        obj
      else
        raise TypeError, 'cannot handle this type of object for dest'
      end
    end
    module_function :create_dest

    #:startdoc:
    # Returns the TMail object encoded and ready to be sent via SMTP etc.
    # You should call this before you are packaging up your  email to
    # correctly escape all the values that need escaping in the email, line
    # wrap the email etc.
    # 
    # It is also a good idea to call this before you marshal or serialize
    # a TMail object.
    # 
    # For Example:
    # 
    #  email = TMail::Load(my_email_file)
    #  email_to_send = email.encoded
    def encoded( eol = "\r\n", charset = 'j', dest = nil )
      accept_strategy Encoder, eol, charset, dest
    end

    # Returns the TMail object decoded and ready to be used by you, your
    # program etc.
    # 
    # You should call this before you are packaging up your  email to
    # correctly escape all the values that need escaping in the email, line
    # wrap the email etc.
    # 
    # For Example:
    # 
    #  email = TMail::Load(my_email_file)
    #  email_to_send = email.encoded
    def decoded( eol = "\n", charset = 'e', dest = nil )
      # Turn the E-Mail into a string and return it with all
      # encoded characters decoded.  alias for to_s
      accept_strategy Decoder, eol, charset, dest
    end

    alias to_s decoded

    def accept_strategy( klass, eol, charset, dest = nil ) #:nodoc:
      dest ||= ''
      accept klass.new( create_dest(dest), charset, eol )
      dest
    end

  end

  #:stopdoc:

  ###
  ### MIME B encoding decoder
  ###

  class Decoder

    include TextUtils

    encoded = '=\?(?:iso-2022-jp|euc-jp|shift_jis)\?[QB]\?[a-z0-9+/=]+\?='
    ENCODED_WORDS = /#{encoded}(?:\s+#{encoded})*/i
    SPACER       = "\t"

    OUTPUT_ENCODING = {
      'EUC'  => 'e',
      'SJIS' => 's',
    }

    def self.decode( str, encoding = nil )
      encoding ||= (OUTPUT_ENCODING[TMail.KCODE] || 'j')
      opt = '-mS' + encoding
      str.gsub(ENCODED_WORDS) {|s| NKF.nkf(opt, s) }
    end

    def initialize( dest, encoding = nil, eol = "\n" )
      @f = StrategyInterface.create_dest(dest)
      @encoding = (/\A[ejs]/ === encoding) ? encoding[0,1] : nil
      @eol = eol
    end

    def decode( str )
      self.class.decode(str, @encoding)
    end
    private :decode

    def terminate
    end

    def header_line( str )
      @f << decode(str)
    end

    def header_name( nm )
      @f << nm << ': '
    end

    def header_body( str )
      @f << decode(str)
    end

    def space
      @f << ' '
    end

    alias spc space

    def lwsp( str )
      @f << str
    end

    def meta( str )
      @f << str
    end

    def puts_meta( str )
      @f << str
    end

    def text( str )
      @f << decode(str)
    end

    def phrase( str )
      @f << quote_phrase(decode(str))
    end

    def kv_pair( k, v )
      v = dquote(v) unless token_safe?(v)
      @f << k << '=' << v
    end

    def puts( str = nil )
      @f << str if str
      @f << @eol
    end

    def write( str )
      @f << str
    end

  end


  ###
  ### MIME B-encoding encoder
  ###

  #
  # FIXME: This class can handle only (euc-jp/shift_jis -> iso-2022-jp).
  #
  class Encoder

    include TextUtils

    BENCODE_DEBUG = false unless defined?(BENCODE_DEBUG)

    def Encoder.encode( str )
      e = new()
      e.header_body str
      e.terminate
      e.dest.string
    end

    SPACER       = "\t"
    MAX_LINE_LEN = 78
    RFC_2822_MAX_LENGTH = 998

    OPTIONS = {
      'EUC'  => '-Ej -m0',
      'SJIS' => '-Sj -m0',
      'UTF8' => nil,      # FIXME
      'NONE' => nil
    }

    def initialize( dest = nil, encoding = nil, eol = "\r\n", limit = nil )
      @f = StrategyInterface.create_dest(dest)
      @opt = OPTIONS[TMail.KCODE]
      @eol = eol
      @folded = false
      @preserve_quotes = true
      reset
    end

    def preserve_quotes=( bool )
      @preserve_quotes
    end

    def preserve_quotes
      @preserve_quotes
    end

    def normalize_encoding( str )
      if @opt
      then NKF.nkf(@opt, str)
      else str
      end
    end

    def reset
      @text = ''
      @lwsp = ''
      @curlen = 0
    end

    def terminate
      add_lwsp ''
      reset
    end

    def dest
      @f
    end

    def puts( str = nil )
      @f << str if str
      @f << @eol
    end

    def write( str )
      @f << str
    end

    #
    # add
    #

    def header_line( line )
      scanadd line
    end

    def header_name( name )
      add_text name.split(/-/).map {|i| i.capitalize }.join('-')
      add_text ':'
      add_lwsp ' '
    end

    def header_body( str )
      scanadd normalize_encoding(str)
    end

    def space
      add_lwsp ' '
    end

    alias spc space

    def lwsp( str )
      add_lwsp str.sub(/[\r\n]+[^\r\n]*\z/, '')
    end

    def meta( str )
      add_text str
    end

    def puts_meta( str )
      add_text str + @eol + SPACER
    end

    def text( str )
      scanadd normalize_encoding(str)
    end

    def phrase( str )
      str = normalize_encoding(str)
      if CONTROL_CHAR === str
        scanadd str
      else
        add_text quote_phrase(str)
      end
    end

    # FIXME: implement line folding
    #
    def kv_pair( k, v )
      return if v.nil?
      v = normalize_encoding(v)
      if token_safe?(v)
        add_text k + '=' + v
      elsif not CONTROL_CHAR === v
        add_text k + '=' + quote_token(v)
      else
        # apply RFC2231 encoding
        kv = k + '*=' + "iso-2022-jp'ja'" + encode_value(v)
        add_text kv
      end
    end

    def encode_value( str )
      str.gsub(TOKEN_UNSAFE) {|s| '%%%02x' % s[0] }
    end

    private

    def scanadd( str, force = false )
      types = ''
      strs = []
      if str.respond_to?(:encoding)
        enc = str.encoding 
        str.force_encoding(Encoding::ASCII_8BIT)
      end
      until str.empty?
        if m = /\A[^\e\t\r\n ]+/.match(str)
          types << (force ? 'j' : 'a')
          if str.respond_to?(:encoding)
            strs.push m[0].force_encoding(enc)
          else
            strs.push m[0]
          end
        elsif m = /\A[\t\r\n ]+/.match(str)
          types << 's'
          if str.respond_to?(:encoding)
            strs.push m[0].force_encoding(enc)
          else
            strs.push m[0]
          end

        elsif m = /\A\e../.match(str)
          esc = m[0]
          str = m.post_match
          if esc != "\e(B" and m = /\A[^\e]+/.match(str)
            types << 'j'
            if str.respond_to?(:encoding)
              strs.push m[0].force_encoding(enc)
            else
              strs.push m[0]
            end
          end

        else
          raise 'TMail FATAL: encoder scan fail'
        end
        (str = m.post_match) unless m.nil?
      end

      do_encode types, strs
    end

    def do_encode( types, strs )
      #
      # result  : (A|E)(S(A|E))*
      # E       : W(SW)*
      # W       : (J|A)+ but must contain J  # (J|A)*J(J|A)*
      # A       : <<A character string not to be encoded>>
      # J       : <<A character string to be encoded>>
      # S       : <<LWSP>>
      #
      # An encoding unit is `E'.
      # Input (parameter `types') is  (J|A)(J|A|S)*(J|A)
      #
      if BENCODE_DEBUG
        puts
        puts '-- do_encode ------------'
        puts types.split(//).join(' ')
        p strs
      end

      e = /[ja]*j[ja]*(?:s[ja]*j[ja]*)*/

      while m = e.match(types)
        pre = m.pre_match
        concat_A_S pre, strs[0, pre.size] unless pre.empty?
        concat_E m[0], strs[m.begin(0) ... m.end(0)]
        types = m.post_match
        strs.slice! 0, m.end(0)
      end
      concat_A_S types, strs
    end

    def concat_A_S( types, strs )
      if RUBY_VERSION < '1.9'
        a = ?a; s = ?s
      else
        a = 'a'.ord; s = 's'.ord
      end
      i = 0
      types.each_byte do |t|
        case t
        when a then add_text strs[i]
        when s then add_lwsp strs[i]
        else
          raise "TMail FATAL: unknown flag: #{t.chr}"
        end
        i += 1
      end
    end

    METHOD_ID = {
      ?j => :extract_J,
      ?e => :extract_E,
      ?a => :extract_A,
      ?s => :extract_S
    }

    def concat_E( types, strs )
      if BENCODE_DEBUG
        puts '---- concat_E'
        puts "types=#{types.split(//).join(' ')}"
        puts "strs =#{strs.inspect}"
      end

      flush() unless @text.empty?

      chunk = ''
      strs.each_with_index do |s,i|
        mid = METHOD_ID[types[i]]
        until s.empty?
          unless c = __send__(mid, chunk.size, s)
            add_with_encode chunk unless chunk.empty?
            flush
            chunk = ''
            fold
            c = __send__(mid, 0, s)
            raise 'TMail FATAL: extract fail' unless c
          end
          chunk << c
        end
      end
      add_with_encode chunk unless chunk.empty?
    end

    def extract_J( chunksize, str )
      size = max_bytes(chunksize, str.size) - 6
      size = (size % 2 == 0) ? (size) : (size - 1)
      return nil if size <= 0
      if str.respond_to?(:encoding)
        enc = str.encoding
        str.force_encoding(Encoding::ASCII_8BIT)
        "\e$B#{str.slice!(0, size)}\e(B".force_encoding(enc)
      else
        "\e$B#{str.slice!(0, size)}\e(B"
      end
    end

    def extract_A( chunksize, str )
      size = max_bytes(chunksize, str.size)
      return nil if size <= 0
      str.slice!(0, size)
    end

    alias extract_S extract_A

    def max_bytes( chunksize, ssize )
      (restsize() - '=?iso-2022-jp?B??='.size) / 4 * 3 - chunksize
    end

    #
    # free length buffer
    #

    def add_text( str )
      @text << str
      # puts '---- text -------------------------------------'
      # puts "+ #{str.inspect}"
      # puts "txt >>>#{@text.inspect}<<<"
    end

    def add_with_encode( str )
      @text << "=?iso-2022-jp?B?#{Base64.encode(str)}?="
    end

    def add_lwsp( lwsp )
      # puts '---- lwsp -------------------------------------'
      # puts "+ #{lwsp.inspect}"
      fold if restsize() <= 0
      flush(@folded)
      @lwsp = lwsp
    end

    def flush(folded = false)
      # puts '---- flush ----'
      # puts "spc >>>#{@lwsp.inspect}<<<"
      # puts "txt >>>#{@text.inspect}<<<"
      @f << @lwsp << @text
      if folded
        @curlen = 0
      else
        @curlen += (@lwsp.size + @text.size)
      end
      @text = ''
      @lwsp = ''
    end

    def fold
      # puts '---- fold ----'
      unless @f.string =~ /^.*?:$/
        @f << @eol
        @lwsp = SPACER
      else
        fold_header
        @folded = true
      end
      @curlen = 0
    end

    def fold_header
      # Called because line is too long - so we need to wrap.
      # First look for whitespace in the text
      # if it has text, fold there
      # check the remaining text, if too long, fold again
      # if it doesn't, then don't fold unless the line goes beyond 998 chars

      # Check the text to see if there is whitespace, or if not
      @wrapped_text = []
      until @text.blank?
        fold_the_string
      end
      @text = @wrapped_text.join("#{@eol}#{SPACER}")
    end

    def fold_the_string
      whitespace_location = @text =~ /\s/ || @text.length
      # Is the location of the whitespace shorter than the RCF_2822_MAX_LENGTH?
      # if there is no whitespace in the string, then this
      unless mazsize(whitespace_location) <= 0
        @text.strip!
        @wrapped_text << @text.slice!(0...whitespace_location)
      # If it is not less, we have to wrap it destructively
      else
        slice_point = RFC_2822_MAX_LENGTH - @curlen - @lwsp.length
        @text.strip!
        @wrapped_text << @text.slice!(0...slice_point)
      end
    end

    def restsize
      MAX_LINE_LEN - (@curlen + @lwsp.size + @text.size)
    end

    def mazsize(whitespace_location)
      # Per RFC2822, the maximum length of a line is 998 chars
      RFC_2822_MAX_LENGTH - (@curlen + @lwsp.size + whitespace_location)
    end

  end
  #:startdoc:
end    # module TMail
