#!/bin/bash -eu

pathSeparator=":"
classpath="../fbase/bin${pathSeparator}../fajax/bin${pathSeparator}../fiwb/bin${pathSeparator}bin"
for jar in ../fbase/lib/*/*.jar ../fiwb/lib/*/*.jar ../fiwb/lib/*.jar
do
	[[ "${jar}" != *webdav*.jar ]] && classpath="$classpath${pathSeparator}$jar"
done

java \
	-Dcom.fluidops.api.Bootstrap=com.fluidops.iwb.api.EndpointImpl \
	-Dcom.fluidops.api.Parse=com.fluidops.iwb.api.CliParser \
	-cp "$classpath" \
	com.fluidops.api.Cli2 "$@"