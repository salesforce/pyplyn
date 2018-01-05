#!/usr/bin/env bash
### BEGIN INIT INFO
# Provides:          pyplyn
# Required-Start:    $local_fs $network $named $time
# Required-Stop:     $local_fs $network $named $time
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start the Pyplyn daemon at boot time
# Description:       Enable processing of Pyplyn ETL configurations at boot time
### END INIT INFO

# If you choose to deploy the startup script in a different directory than the JAR, update this parameter!
#   NOTE: Blank space (' ') characters are not currently supported in paths!
LOCATION="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

NAME="${project.parent.artifactId}-${project.parent.version}"
SCRIPT="java -Dname=$NAME -jar $LOCATION/$NAME.jar --config $LOCATION/config/pyplyn-config.json"
PIDFILE=$LOCATION/$NAME.pid
LOGFILE=$LOCATION/$NAME.log

is_service_running() {
    if [ ! -f "$PIDFILE" ]; then
        return 1

    elif kill -0 $(< "$PIDFILE") > /dev/null 2>&1; then
        return 0

    else
        return 1
    fi
}

start() {
    if is_service_running; then
        echo "$NAME is already running" 1>&2
        return 1
    fi

    echo "Starting $NAME" 1>&2
    bash -c "$SCRIPT >> \"$LOGFILE\" 2>&1 & echo \$!" > "$PIDFILE"
    echo "$NAME started" 1>&2
}

manual() {
    if is_service_running; then
        echo "$NAME is already running" 1>&2
        return 1
    fi

    bash -c "$SCRIPT"
}

nohup() {
    if is_service_running; then
        echo "$NAME is already running" 1>&2
        return 1
    fi

    nohup $SCRIPT >> "$LOGFILE" 2>&1 & echo $! > "$PIDFILE" 2>&1 < /dev/null &
}

stop() {
    if ! is_service_running; then
        echo "$NAME is not running" 1>&2
        return 1
    fi

    echo "Stopping $NAME" 1>&2
    kill -15 $(cat "$PIDFILE") && rm -f "$PIDFILE"
    echo "$NAME stopped" 1>&2
}

status() {
    if is_service_running; then
        echo "$NAME is running" 1>&2
    else
        echo "$NAME is not running" 1>&2
    fi
}

logs() {
    if ! is_service_running; then
        echo "$NAME is not running" 1>&2
        return 1
    fi

    tail -n 1000 -f "$LOGFILE"
}

case "$1" in
    start)
        start
    ;;

    stop)
        stop
    ;;

    restart)
        stop
        start
    ;;

    manual)
        manual
    ;;

    nohup)
        nohup
    ;;

    status)
        status
    ;;

    logs)
        logs
    ;;

    *)
        echo "Usage: $0 {start|stop|restart|manual|nohup|status|logs}"
        echo
esac
