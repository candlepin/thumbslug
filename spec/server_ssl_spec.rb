require 'thumbslug_common'
require 'openssl'
require 'socket'

describe 'Server SSL' do
  include ThumbslugMethods

# I was trying to read the stderr message for these two, and verify that
# the server exited with -1. but darn, this is hard!

#  it 'provides a friendly error message if the keystore cannot be read' do
    # XXX add me
#    fail
#  end

#  it 'provides a friendly error message if the keystore password is wrong' do
    # XXX add me
#    fail
#  end

  it 'uses the configured ssl certificate for its server certificate' do
    fail #this isn't cleaning up properly for some reason, need to investigate
    httpd = create_httpd
    tslug = create_thumbslug({
      :ssl_keystore => 'spec/data/keystore.p12',
      :ssl_keystore_password => 'password'
    })

    socket = TCPSocket.new('127.0.0.1', 8088)
    ssl_context = OpenSSL::SSL::SSLContext.new()
    ssl_socket = OpenSSL::SSL::SSLSocket.new(socket, ssl_context)
    ssl_socket.sync_close = true

    ssl_socket.connect

    serial = ssl_socket.peer_cert.serial

    # shut everything down before asserting
    ssl_socket.close

    Process.kill('KILL', httpd)
    Process.kill('KILL', tslug.pid)
    Process.waitall()

    # this is the magic number from our cert
    serial.should == 18235744395953304678
  end
end
