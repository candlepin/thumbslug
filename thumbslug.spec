%global _binary_filedigest_algorithm 1
%global _source_filedigest_algorithm 1
%global _binary_payload w9.gzdio
%global _source_payload w9.gzdio

Name: thumbslug
Summary: Thumbslug CDN proxy
Group: Internet/Applications
License: GPLv2
Version: 0.0.3
Release: 1%{?dist}
URL: http://fedorahosted.org/thumbslug
# Source0: 
Source: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-buildroot
Vendor: Red Hat, Inc
BuildArch: noarch

BuildRequires: thumbslug-deps >= 0:0.0.1
%define __jar_repack %{nil}

%description
fill me in

%prep
%setup -q 

%build
ant -Dlibdir=/usr/share/thumbslug/lib/ clean package genschema

%install

%clean
rm -rf $RPM_BUILD_ROOT

%files
#%config(noreplace) %{_sysconfdir}/%{name}/%{name}.conf
%{_datadir}/%{name}/*

%changelog
* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- bump the version 

* Mon Jul 11 2011 Chris Duryee (beav) <cduryee@redhat.com>
- new package built with tito

* Mon Jul 11 2011 Chris Duryee <cduryee@redhat.com> 0.0.1-1
- first cut (cduryee@redhat.com)
