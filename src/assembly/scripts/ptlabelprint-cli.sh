#!/usr/bin/env bash
DIR="$(cd "$(dirname "$0")" && pwd)"
CP="$DIR/ptlabelprint-cli.jar:$DIR/lib/*"
# Git Bash/MSYS/Cygwin run a Windows java, which wants Windows paths separated by ';'
if command -v cygpath >/dev/null 2>&1; then
	CP="$(cygpath -w "$DIR/ptlabelprint-cli.jar");$(cygpath -w "$DIR/lib")\*"
fi
exec java -cp "$CP" cz.bliksoft.ptlabelprint.Cli "$@"
