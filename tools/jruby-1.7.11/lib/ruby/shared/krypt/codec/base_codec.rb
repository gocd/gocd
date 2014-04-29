module Krypt::BaseCodec #:nodoc:
    
  def generic_read(len, read_len)
    data = @io.read(read_len)
    data = yield data if data
    if @buf
      data = data || ""
      data = @buf << data
    end
    return data unless len && data
    dlen = data.size
    remainder = dlen - len
    update_buffer(data, dlen, remainder)
    data
  end

  def generic_write(data, blk_size)
    return 0 unless data
    @write = true
    data = @buf ? @buf << data : data.dup
    dlen = data.size
    remainder = dlen % blk_size
    update_buffer(data, dlen, remainder)
    @io.write(yield data) if data.size > 0
  end

  def update_buffer(data, dlen, remainder)
    if remainder > 0
      @buf = data.slice!(dlen - remainder, remainder)
    else
      @buf = nil
    end
  end

end

