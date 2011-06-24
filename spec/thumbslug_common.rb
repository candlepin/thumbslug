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
      trap('INT') { server.stop }
      server.start
    }
    #give the webrick a few seconds to start up
    sleep 3
    return pid
  end

  def create_thumbslug
    pid = fork {
      exec("java -jar " + Dir.pwd + "/target/thumbslug-1.0.0.jar")
    }
    #avoid a race condition
    sleep 1
    Process.detach(pid)
    return pid
  end
end
