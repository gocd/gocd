# encoding: utf-8
module Mail
  class EnvelopeFromElement
    
    include Mail::Utilities
    
    def initialize( string )
      parser = Mail::EnvelopeFromParser.new
      if @tree = parser.parse(string)
        @address = tree.addr_spec.text_value.strip
        @date_time = ::DateTime.parse("#{tree.ctime_date.text_value}")
      else
        raise Mail::Field::ParseError.new(EnvelopeFromElement, string, parser.failure_reason)
      end
    end
    
    def tree
      @tree
    end
    
    def date_time
      @date_time
    end
    
    def address
      @address
    end
    
    # RFC 4155:
    #   a timestamp indicating the UTC date and time when the message
    #   was originally received, conformant with the syntax of the
    #   traditional UNIX 'ctime' output sans timezone (note that the
    #   use of UTC precludes the need for a timezone indicator);
    def formatted_date_time
      if @date_time.respond_to?(:ctime)
        @date_time.ctime
      else
        @date_time.strftime '%a %b %e %T %Y'
      end
    end

    def to_s
      "#{@address} #{formatted_date_time}"
    end
    
  end
end
