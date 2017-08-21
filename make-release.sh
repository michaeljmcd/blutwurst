#!/usr/bin/sh

VERSION="0.3.0"

lein uberjar

SCRATCH_SPACE="/tmp/blutwurst-${VERSION}/"

mkdir ${SCRATCH_SPACE}

mkdir ${SCRATCH_SPACE}/lib
cp target/uberjar/*-standalone.jar ${SCRATCH_SPACE}/lib
cp -r bin ${SCRATCH_SPACE}/

zip -r target/blutwurst.zip ${SCRATCH_SPACE}

rm -rf ${SCRATCH_SPACE}
