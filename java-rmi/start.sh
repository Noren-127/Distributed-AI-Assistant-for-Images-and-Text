#!/bin/bash

# Nova Chatbot Startup Script with Hadoop Integration

# Set environment variables
export HADOOP_HOME=/usr/local/hadoop
export NOVA_PROJECT_ROOT=/home/shuraaa/Desktop/DistributedSystem/Project
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

echo "Starting Nova Chatbot with Hadoop integration..."
echo "HADOOP_HOME: $HADOOP_HOME"
echo "NOVA_PROJECT_ROOT: $NOVA_PROJECT_ROOT"

# Check if JAR exists
JAR_FILE="target/chatbot-rmi-server-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found: $JAR_FILE"
    echo "Please run 'mvn package' first"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

# Start Nova Chatbot
echo "Starting Nova Chatbot server..."
java -jar "$JAR_FILE" > logs/nova.log 2>&1 &

# Get process ID
NOVA_PID=$!
echo "Nova Chatbot started with PID: $NOVA_PID"
echo "Log file: logs/nova.log"
echo "Web interface: http://localhost:8082"

# Wait a moment and check if it started
sleep 3

if ps -p $NOVA_PID > /dev/null; then
    echo "✅ Nova Chatbot started successfully!"
    echo "Check logs with: tail -f logs/nova.log"
else
    echo "❌ Failed to start Nova Chatbot"
    echo "Check logs for errors: cat logs/nova.log"
    exit 1
fi
