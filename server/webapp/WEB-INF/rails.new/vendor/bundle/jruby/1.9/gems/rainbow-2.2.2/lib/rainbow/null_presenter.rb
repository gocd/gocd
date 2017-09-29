module Rainbow

  class NullPresenter < ::String

    def color(*values); self; end
    def background(*values); self; end
    def reset; self; end
    def bright; self; end
    def faint; self; end
    def italic; self; end
    def underline; self; end
    def blink; self; end
    def inverse; self; end
    def hide; self; end

    def black; self; end
    def red; self; end
    def green; self; end
    def yellow; self; end
    def blue; self; end
    def magenta; self; end
    def cyan; self; end
    def white; self; end

    def method_missing(method_name,*args)
      if Color::X11Named.color_names.include? method_name and args.empty? then
        self
      else
        super
      end
    end

    alias_method :foreground, :color
    alias_method :fg, :color
    alias_method :bg, :background
    alias_method :bold, :bright
    alias_method :dark, :faint

  end

end
