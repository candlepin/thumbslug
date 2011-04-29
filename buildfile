# Generated by Buildr 1.4.5, change to your liking


# Version number for this release
VERSION_NUMBER = "1.0.0"
# Group identifier for your projects
GROUP = "sam-proxy"
COPYRIGHT = ""

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"
repositories.remote << "https://repository.jboss.org/nexus/content/repositories/releases/"

NETTY = transitive 'org.jboss.netty:netty:jar:3.2.4.Final'

desc "The Sam-proxy project"
define "sam-proxy" do

  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT
  manifest["Main-Class"] = "com.redhat.katello.sam.proxy.Main"
  compile.with NETTY
  test.compile.with NETTY

  package(:jar).merge NETTY
end

task :serve do
    sh "java -jar target/#{GROUP}-#{VERSION_NUMBER}.jar"
end
task :serve => :package
