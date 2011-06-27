module ThumbslugMethods

  def get(url_str, headers = nil)
    uri = URI.parse(url_str)
    client = Net::HTTP.new uri.host, uri.port
    if uri.scheme == 'https':
      client.verify_mode = OpenSSL::SSL::VERIFY_NONE
      client.use_ssl = true
    end
    return client.request(Net::HTTP::Get.new(uri.path, headers))
  end

  def create_httpd(secure=false)
    if secure
      config = {
        :Port => 9443,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        :Debugger => true,
        :Verbose => true,
        :SSLEnable => true,
        :SSLVerifyClient => ::OpenSSL::SSL::VERIFY_NONE,
        :SSLCertName => [ [ "CN", WEBrick::Utils::getservername ] ],
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new("/dev/null"),
        :AccessLog => [nil, nil],
      }
    else
      config = {
        :Port => 9090,
        :BindAddress => '127.0.0.1',
        :DocumentRoot => Dir.pwd + '/spec/data/',
        #comment out these two lines to enable webrick logging
        :Logger => WEBrick::Log.new("/dev/null"),
        :AccessLog => [nil, nil],
      }
    end
    
    pid = fork {
      $stderr = File.open('/dev/null', 'w')
      #the dots and pluses are outputted direct to stderr by webrick's ssl
      #routines.
      server = HTTPServer.new(config)
      trap('INT') { server.stop }
      server.mount "/this_will_500", FiveHundred 
      server.mount "/trace", Trace
      server.start
    }
    #TODO: this should read the forked pid and wait for the "I am ready" message
    sleep 3
    return pid
  end

  def create_thumbslug(secure=false, sendTSheader=false)
    #these need to all be strings
    insecure_config = {
     :port => '8088',
     :ssl => 'false',
     :cdn_port => '9090',
     :cdn_host => 'localhost',
     :cdn_ssl => 'false',
     :sendTSHeader => 'false'
    }
    secure_config = {
     :port => '8443',
     :ssl => 'true',
     :cdn_port => '9443',
     :cdn_host => 'localhost',
     :cdn_ssl => 'true',
     :sendTSHeader => 'false'
    }
    config = secure ? secure_config : insecure_config
    if sendTSheader
      config[:sendTSHeader] = "true"
      config[:port] = config[:port].to_i + 1
      config[:port] = config[:port].to_s
    end
    tslug_exec_string = "java " + 
                 " -Dport=" + config[:port] +
                 " -Dssl=" + config[:ssl] +
                 " -Dcdn.port=" + config[:cdn_port] +
                 " -Dcdn.host=" + config[:cdn_host] +
                 " -Dcdn.ssl=" + config[:cdn_ssl] +
                 " -DsendTSheader=" + config[:sendTSHeader] +
                 " -jar " +  Dir.pwd + "/target/thumbslug-1.0.0.jar"
    pid = fork {
      exec(tslug_exec_string)
    }
    #TODO: this should read the forked pid and wait for the "I am ready" message
    sleep 1
    Process.detach(pid)
    return pid
  end
end


class FiveHundred < WEBrick::HTTPServlet::AbstractServlet

  def do_GET(request, response)
    response.status = 500 
    response['Content-Type'] = "text/plain"
    response.body = 'Error! a 500 error'
  end

end

class Trace < WEBrick::HTTPServlet::AbstractServlet
  #ersatz trace that implements GET, not TRACE verb

  def do_GET(request, response)
    response.status = 200
    response['Content-Type'] = "text/plain"
    headers = ""
    request.raw_header.each {|r| headers += r }
    response.body = headers
  end

end
