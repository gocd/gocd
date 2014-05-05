=begin rdoc

= Quoting methods

=end
module TMail
  class Mail
    def subject(to_charset = 'utf-8')
      Unquoter.unquote_and_convert_to(quoted_subject, to_charset)
    end

    def unquoted_body(to_charset = 'utf-8')
      from_charset = charset
      case (content_transfer_encoding || "7bit").downcase
        when "quoted-printable"
          # the default charset is set to iso-8859-1 instead of 'us-ascii'.
          # This is needed as many mailer do not set the charset but send in ISO. This is only used if no charset is set.
          if !from_charset.blank? && from_charset.downcase == 'us-ascii'
            from_charset = 'iso-8859-1'
          end

          Unquoter.unquote_quoted_printable_and_convert_to(quoted_body,
            to_charset, from_charset, true)
        when "base64"
          Unquoter.unquote_base64_and_convert_to(quoted_body, to_charset,
            from_charset)
        when "7bit", "8bit"
          Unquoter.convert_to(quoted_body, to_charset, from_charset)
        when "binary"
          quoted_body
        else
          quoted_body
      end
    end

    def body(to_charset = 'utf-8', &block)
      attachment_presenter = block || Proc.new { |file_name| "Attachment: #{file_name}\n" }

      if multipart?
        parts.collect { |part|
          header = part["content-type"]

          if part.multipart?
            part.body(to_charset, &attachment_presenter)
          elsif header.nil?
            ""
          elsif !attachment?(part)
            part.unquoted_body(to_charset)
          else
            attachment_presenter.call(header["name"] || "(unnamed)")
          end
        }.join
      else
        unquoted_body(to_charset)
      end
    end
  end

  class Attachment
    
    include TextUtils
    
    def quoted?(string)
      !!((string =~ /.+'\w\w'.+/) || (string =~ /=\?.+\?.\?.+\?=/))
    end
    
    # Only unquote if quoted
    def original_filename(to_charset = 'utf-8')
      if quoted?(quoted_filename)
        Unquoter.unquote_and_convert_to(quoted_filename, to_charset).chomp
      else
        quoted_filename
      end
    end
  end

  class Unquoter
    class << self
      def unquote_and_convert_to(text, to_charset, from_charset = "iso-8859-1", preserve_underscores=false)
        return "" if text.nil?
        text.gsub!(/\?=(\s*)=\?/, '?==?') # Remove whitespaces between 'encoded-word's
        text.gsub(/(.*?)(?:(?:=\?(.*?)\?(.)\?(.*?)\?=)|$)/) do
          before = $1
          from_charset = $2
          quoting_method = $3
          text = $4

          before = convert_to(before, to_charset, from_charset) if before.length > 0
          before + case quoting_method
              when "q", "Q" then
                unquote_quoted_printable_and_convert_to(text, to_charset, from_charset, preserve_underscores)
              when "b", "B" then
                unquote_base64_and_convert_to(text, to_charset, from_charset)
              when nil then
                # will be nil at the end of the string, due to the nature of
                # the regex used.
                ""
              else
                raise "unknown quoting method #{quoting_method.inspect}"
            end
        end
      end

      def convert_to_with_fallback_on_iso_8859_1(text, to, from)
        return text if to == 'utf-8' and text.isutf8

        if from.blank? and !text.is_binary_data?
          from = CharDet.detect(text)['encoding']

          # Chardet ususally detects iso-8859-2 (aka windows-1250), but the text is
          # iso-8859-1 (aka windows-1252 and Latin1). http://en.wikipedia.org/wiki/ISO/IEC_8859-2
          # This can cause unwanted characters, like ŕ instead of à.
          # (I know, could be a very bad decision...)
          from = 'iso-8859-1' if from =~ /iso-8859-2/i
        end

        begin
          convert_to_without_fallback_on_iso_8859_1(text, to, from)
        rescue Iconv::InvalidCharacter
          unless from == 'iso-8859-1'
            from = 'iso-8859-1'
            retry
          end
        end
      end

      def unquote_quoted_printable_and_convert_to(text, to, from, preserve_underscores=false)
        text = text.gsub(/_/, " ") unless preserve_underscores
        text = text.gsub(/\r\n|\r/, "\n") # normalize newlines
        convert_to(text.unpack("M*").first, to, from)
      end

      def unquote_base64_and_convert_to(text, to, from)
        convert_to(Base64.decode(text), to, from)
      end

      begin
        require 'iconv'
        def convert_to(text, to, from)
          return text unless to && from
          text ? Iconv.iconv(to, from, text).first : ""
        rescue Iconv::IllegalSequence, Iconv::InvalidEncoding, Errno::EINVAL
          # the 'from' parameter specifies a charset other than what the text
          # actually is...not much we can do in this case but just return the
          # unconverted text.
          #
          # Ditto if either parameter represents an unknown charset, like
          # X-UNKNOWN.
          text
        end
      rescue LoadError
        # Not providing quoting support
        def convert_to(text, to, from)
          warn "Action Mailer: iconv not loaded; ignoring conversion from #{from} to #{to} (#{__FILE__}:#{__LINE__})"
          text
        end
      end
      
      alias_method :convert_to_without_fallback_on_iso_8859_1, :convert_to
      alias_method :convert_to, :convert_to_with_fallback_on_iso_8859_1
      
    end
  end
end
