repl:
    clj

test:
    clj -M:test

uber:
    clj -T:build uber

release:
    #!/usr/bin/env sh

    clj -T:build uber

    VERSION=$(clj -T:build build-version)

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
