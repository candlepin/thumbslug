# This script is used to create a given number of consumers in threads and
# have them all attempt to consume from the pools for a particular product.
#
# TO RUN:
# in terminal 1: touch logout
# in terminal 1: tail -f logout
# in terminal 2: ruby concurrency-test.rb 40 2> logout
# watch the progress bar in terminal 2 (green dot for consumed ent, red N for not)
# watch the details in terminal 1

require 'pp'
require 'optparse'
require 'net/https'

TS_SERVER = "localhost"
TS_PORT = 8088
TS_FILE = "/"

def debug(msg)
  STDERR.write Thread.current[:name]
  STDERR.write " :: "
  STDERR.write msg
  STDERR.write "\n"
  STDERR.flush
end

def grab_file(server, port)
  headers = nil
  client = Net::HTTP.new server, port
  client.use_ssl = true
  client.verify_mode = OpenSSL::SSL::VERIFY_NONE #TODO: verify server
  client.ca_file = "CA/cacert.pem"
  client.key = OpenSSL::PKey::RSA.new(File.read("spec/data/spec/spec-client_keypair.pem"))
  client.cert = OpenSSL::X509::Certificate.new(File.read("spec/data/spec/cert_spec-client.pem"))
  
  client.request_get(TS_FILE) do |response|
    response.read_body do |segment|
      # just iterate through it all, but do nothing
    end
  end
end

def an_iteration(num_threads)
  
  #queue = Queue.new

  threads = []
  for i in 0..num_threads - 1
    threads[i] = Thread.new do
      Thread.current[:name] = "Thread #{i}"
      begin
        grab_file(TS_SERVER, TS_PORT)
        #queue << (ent.nil? ? no_ent : an_ent)
      rescue Exception => e
        debug "Exception caught"
        debug e
        #queue << no_ent
      end
    end
  end

#  collector = Thread.new do
#    res_string = ""
#    for i in 0..num_threads - 1
#      res_string << queue.pop
#      STDOUT.print "\r" + res_string
#      STDOUT.flush
#    end
#    STDOUT.print "\n"
#  end

#  collector.join
  threads.each { |thread| thread.join }
end

num_threads = ARGV[1].to_i
if num_threads == 0
  num_threads = 1
end

num_iterations = ARGV[0].to_i
if num_iterations == 0
  num_iterations = 1
end

p "running #{num_threads} requests #{num_iterations} times"

num_iterations.times do |i|
  p "iteration #{i}"
  an_iteration num_threads
end
