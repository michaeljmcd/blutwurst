#!/usr/bin/sh

lein uberjar

VERSION=`lein print :version | sed 's/"//g' | tr -d '[:space:]'`

SCRATCH_SPACE="target/blutwurst-${VERSION}/"

mkdir ${SCRATCH_SPACE}

mkdir ${SCRATCH_SPACE}/lib
cp target/uberjar/*-standalone.jar ${SCRATCH_SPACE}/lib
cp -r bin ${SCRATCH_SPACE}/
cp -r samples ${SCRATCH_SPACE}/
cp -r doc ${SCRATCH_SPACE}
cp CHANGELOG.md LICENSE README.md TODO.md ${SCRATCH_SPACE}/

cd target

zip -r blutwurst-${VERSION}.zip blutwurst-${VERSION}
