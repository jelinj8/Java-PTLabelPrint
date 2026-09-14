#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -cp "$DIR/ptlabelprint-cli.jar:$DIR/lib/*" cz.bliksoft.ptlabelprint.Cli "$@"
