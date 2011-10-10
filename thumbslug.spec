%global _binary_filedigest_algorithm 1
%global _source_filedigest_algorithm 1
%global _binary_payload w9.gzdio
%global _source_payload w9.gzdio

Name: thumbslug
Summary: Thumbslug CDN proxy
Group: Internet/Applications
License: GPLv2
Version: 0.0.10
Release: 1%{?dist}
URL: http://fedorahosted.org/thumbslug
# Source0: 
Source: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Vendor: Red Hat, Inc
BuildArch: noarch

BuildRequires: ant >= 0:1.7.0
BuildRequires: thumbslug-deps >= 0:0.0.1

%define __jar_repack %{nil}

%description
fill me in

%prep
%setup -q 

%build
ant -Dlibdir=/usr/share/thumbslug/lib/ clean package

%install
install -d -m 755 $RPM_BUILD_ROOT/%{_datadir}/%{name}/
install -m 644 target/%{name}.jar $RPM_BUILD_ROOT/%{_datadir}/%{name}

%clean
rm -rf $RPM_BUILD_ROOT

%files
#%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf
%{_datadir}/%{name}/thumbslug.jar

%changelog
* Mon Oct 10 2011 jesus m. rodriguez <jesusr@redhat.com> 0.0.10-1
- update gitignore (jbowes@redhat.com)
- checkstyle fixup (jbowes@redhat.com)

* Wed Jul 13 2011 Chris Duryee (beav) <cduryee@redhat.com>
- ant jar stuff (cduryee@redhat.com)
- Merge commit 'ed410d78475813f9bd8e28530408c60280c7a000' (cduryee@redhat.com)
- first cut at a thumbslug init file (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- pull the jar into the rpm (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- make the jar file have a consistent name (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- dont generate schema (we dont have any) (cduryee@redhat.com)

* Tue Jul 12 2011 Chris Duryee (beav) <cduryee@redhat.com>
- include ant (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- try to make an rpm (cduryee@redhat.com)

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- bump the version 

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- new package built with tito

* Mon Jul 11 2011 Chris Duryee <cduryee@redhat.com> 0.0.1-1
- first cut (cduryee@redhat.com)
