#!/usr/bin/env bash

check_version() {
    if [[ "" == "$1" ]]; then
        echo "Expecting a version to be specified!"
        exit 2
    fi
}

check_configurations_volume() {
    if [ ! -d "$1" ]; then
        echo "Expecting a valid directory to be specified for mounting the configurations volume: $3"
        echo
        exit 3
    fi
}

build() {
    check_version $1
    TAG_VERSION="salesforce/pyplyn:$1"
    docker build -t $TAG_VERSION .
}

run() {
    check_version $1
    check_configurations_volume $2
    TAG_VERSION="salesforce/pyplyn:$1"
    docker run -v $2:/home/pyplyn/pyplyn/config -t $TAG_VERSION
}

case "$1" in
    build)
        build $2
    ;;

    run)
        run $2 $3
    ;;

    *)
        echo "Usage:"
        echo
        echo "Build the container: $0 build DOCKER_TAG_VERSION"
        echo "i.e.: $0 build 1.0.0"
        echo
        echo
        echo "Run Pyplyn in Docker: $0 run DOCKER_TAG_VERSION /path/to/configurations"
        echo "i.e.: $0 run 1.0.0 /tmp/configurations"
        echo
esac
exit 0