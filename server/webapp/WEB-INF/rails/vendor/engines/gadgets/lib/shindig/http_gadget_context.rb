module Shindig
  class HttpGadgetContext < GadgetContext
    def initialize(request)
      @request = request
      super()
    end


    def getParameter(name)
      @request.params[name]
    end

    def getUrl
      url = getParameter("url")
      return unless url
      Uri.parse(url)
    end

    def getContainter
      @request.params["container"] || @request.params["synd"]
    end

    def getHost
      @request.host
    end

    def getRenderingContext
      getParameter('c') == '1' ? org.apache.shindig.gadgets.RenderingContext::CONTAINER :
          org.apache.shindig.gadgets.RenderingContext::GADGET
    end

    def getModuleId
      getParameter('mid').to_i
    end

    def getUserIp
      @request.remote_ip
    end

    def getDebug
      return false if @request.params["debug"].blank?
      @request.params["debug"] != "0"
    end

    def getIgnoreCache
      return false if @request.params["nocache"].blank?
      @request.params["nocache"] != "0"
    end

    def getView
      getParameter('view')
    end

  end
end