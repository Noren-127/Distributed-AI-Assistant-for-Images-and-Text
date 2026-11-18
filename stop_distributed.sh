#!/bin/bash

echo "🛑 Stopping Nova AI Distributed System..."

# Stop servers using saved PIDs
if [ -f /tmp/nova_ai.pid ]; then
    AI_PID=$(cat /tmp/nova_ai.pid)
    kill $AI_PID 2>/dev/null && echo "✅ Python AI Server stopped"
    rm -f /tmp/nova_ai.pid
fi

if [ -f /tmp/nova_java.pid ]; then
    JAVA_PID=$(cat /tmp/nova_java.pid)
    kill $JAVA_PID 2>/dev/null && echo "✅ Java RMI Server stopped"
    rm -f /tmp/nova_java.pid
fi

if [ -f /tmp/nova_web.pid ]; then
    WEB_PID=$(cat /tmp/nova_web.pid)
    kill $WEB_PID 2>/dev/null && echo "✅ Web Server stopped"
    rm -f /tmp/nova_web.pid
fi

# Also kill any processes on ports
lsof -ti:8081 | xargs -r kill -9 2>/dev/null
lsof -ti:8082 | xargs -r kill -9 2>/dev/null
lsof -ti:8400 | xargs -r kill -9 2>/dev/null

echo "✅ All servers stopped!"
