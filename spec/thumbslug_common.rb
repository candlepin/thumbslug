module ThumbslugMethods

  def create_secure_httpd
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
    
    pid = fork {
      $stderr = File.open('/dev/null', 'w')
      #the dots and pluses are outputted direct to stderr by webrick's ssl
      #routines.
      server = HTTPServer.new(config)
      trap('INT') { server.stop }
      server.start
    }
    #give the webrick a few seconds to start up
    sleep 3
    return pid
  end


  def create_httpd
    config = {
      :Port => 9090,
      :BindAddress => '127.0.0.1',
      :DocumentRoot => Dir.pwd + '/spec/data/',
      #comment out these two lines to enable webrick logging
      :Logger => WEBrick::Log.new("/dev/null"),
      :AccessLog => [nil, nil],
    }
    pid = fork {
      server = HTTPServer.new(config)
      server.mount "/this_will_500", FiveHundred 
      trap('INT') { server.stop }
      server.start
    }
    #give the webrick a few seconds to start up
    sleep 3
    return pid
  end

  def create_thumbslug
    #these need to all be strings
    config = {
     :port => '8088',
     :ssl => 'false',
     :cdn_port => '9090',
     :cdn_host => 'localhost',
     :cdn_ssl => 'false'
    }
    tslug_exec_string = "java -jar" + 
                 " -Dport=" + config[:port] +
                 " -Dssl=" + config[:ssl] +
                 " -Dcdn_port=" + config[:cdn_port] +
                 " -Dcdn_host=" + config[:cdn_host] +
                 " -Dcdn_ssl=" + config[:cdn_ssl] +
                 " " +  Dir.pwd + "/target/thumbslug-1.0.0.jar"
    pid = fork {
      exec(tslug_exec_string)
    }
    #avoid a race condition
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
