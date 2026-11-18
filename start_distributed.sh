#!/bin/bash

echo "🚀 Starting Nova AI Distributed System..."

# Project root (directory where this script lives)
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
export NOVA_PROJECT_ROOT="$PROJECT_ROOT"

# Load environment variables from .env file
if [ -f "$PROJECT_ROOT/python-ai/.env" ]; then
    echo "🔧 Loading environment variables from .env..."
    export $(cat "$PROJECT_ROOT/python-ai/.env" | grep -v '^#' | xargs)
    echo "✅ Environment variables loaded"
else
    echo "⚠️  .env file not found at $PROJECT_ROOT/python-ai/.env"
    echo "❌ Please create .env file with MISTRAL_API_KEY and MISTRAL_AGENT_ID"
    exit 1
fi

# Kill any existing servers
echo "🔄 Stopping existing servers..."
lsof -ti:8081 | xargs -r kill -9 2>/dev/null
lsof -ti:8082 | xargs -r kill -9 2>/dev/null
lsof -ti:8400 | xargs -r kill -9 2>/dev/null
pkill -f "simple_server.py" 2>/dev/null
pkill -f "ChatbotServer" 2>/dev/null

# Function to check if port is available
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo "❌ Port $port is already in use"
        return 1
    else
        echo "✅ Port $port is available"
        return 0
    fi
}

# Function to wait for service
wait_for_service() {
    local host=$1
    local port=$2
    local service_name=$3
    local max_attempts=30
    local attempt=1

    echo "⏳ Waiting for $service_name to be ready on $host:$port..."

    while [ $attempt -le $max_attempts ]; do
        if nc -z $host $port 2>/dev/null; then
            echo "✅ $service_name is ready on $host:$port"
            return 0
        fi
        echo "⏳ Attempt $attempt/$max_attempts - $service_name not ready yet..."
        sleep 2
        ((attempt++))
    done

    echo "❌ $service_name failed to start on $host:$port after $max_attempts attempts"
    return 1
}

# Start Python AI Server in background
echo "🤖 Starting Python AI Server..."
cd "$PROJECT_ROOT/python-ai"

# Check if venv exists
if [ ! -d "venv" ]; then
    echo "❌ Python virtual environment not found. Please run setup first."
    exit 1
fi

# Use exact working ports: React 8200, Java 8082, Python 8400
JAVA_PORT=${JAVA_PORT:-8082}
PYTHON_AI_PORT=${PYTHON_AI_PORT:-8400}
WEB_PORT=${WEB_PORT:-8200}

echo "🤖 Starting Python AI Server on port $PYTHON_AI_PORT..."
export PYTHONPATH="$PROJECT_ROOT/python-ai:$PYTHONPATH"
export MISTRAL_API_KEY="$MISTRAL_API_KEY"
export MISTRAL_AGENT_ID="$MISTRAL_AGENT_ID"
export MISTRAL_MODEL="$MISTRAL_MODEL"
echo "🔑 Using Agent ID: $MISTRAL_AGENT_ID"
echo "🧠 Using Model: $MISTRAL_MODEL"
nohup bash -c "source venv/bin/activate && python simple_server.py --port $PYTHON_AI_PORT" > python_ai.log 2>&1 &
AI_PID=$!
echo "Python AI Server PID: $AI_PID"

# Wait for AI server to start
if ! wait_for_service localhost $PYTHON_AI_PORT "Python AI Server"; then
    echo "❌ Failed to start Python AI Server"
    kill $AI_PID 2>/dev/null
    exit 1
fi

# Start Java RMI Server in background
echo "☕ Starting Java RMI Server..."
cd "$PROJECT_ROOT/java-rmi"

# Check if Java is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed or not in PATH"
    kill $AI_PID 2>/dev/null
    exit 1
fi

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed or not in PATH"
    kill $AI_PID 2>/dev/null
    exit 1
fi

# Set Java RMI properties
export JAVA_OPTS="-Djava.rmi.server.hostname=localhost -Djava.security.policy=$PROJECT_ROOT/java-rmi/java.policy"
export PYTHON_AI_PORT=$PYTHON_AI_PORT
export HADOOP_HOME=/usr/local/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

echo "☕ Starting Java RMI Server on port $JAVA_PORT..."
nohup mvn compile exec:java -Dexec.mainClass="com.mini.ai.chatbot.ChatbotServer" -Dexec.args="$PYTHON_AI_PORT" > java_server.log 2>&1 &
JAVA_PID=$!
echo "Java RMI Server PID: $JAVA_PID"

# Wait for Java server to start
if ! wait_for_service localhost $JAVA_PORT "Java RMI Server"; then
    echo "❌ Failed to start Java RMI Server"
    kill $AI_PID $JAVA_PID 2>/dev/null
    exit 1
fi

# Start React Web Server in background
echo "⚛️ Starting React Web Server..."
cd "$PROJECT_ROOT/web-ui"

# Check if Node.js is available
if ! command -v npm &> /dev/null; then
    echo "❌ Node.js/npm is not installed or not in PATH"
    kill $AI_PID $JAVA_PID 2>/dev/null
    exit 1
fi

echo "⚛️ Starting React Web Server on port $WEB_PORT..."
export PORT=$WEB_PORT
nohup npm run dev > web_server.log 2>&1 &
WEB_PID=$!
echo "React Web Server PID: $WEB_PID"

# Wait a bit for React server to start
sleep 5

# Save PIDs to file for later cleanup
echo "$AI_PID" > /tmp/nova_ai.pid
echo "$JAVA_PID" > /tmp/nova_java.pid
echo "$WEB_PID" > /tmp/nova_web.pid

echo ""
echo "✅ Nova AI Distributed System is ready!"
echo "🌐 React Web Interface: http://localhost:$WEB_PORT"
echo "☕ Java RMI Server: http://localhost:$JAVA_PORT"
echo "🤖 Python AI Server: http://localhost:$PYTHON_AI_PORT"
echo ""
echo "📋 Architecture:"
echo "   React UI ($WEB_PORT) → Java RMI ($JAVA_PORT) → Python AI ($PYTHON_AI_PORT)"
echo "   🗄️  Hadoop integration available via Java"
echo ""
echo "💡 To stop all servers, run: ./stop_distributed.sh"
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "🛑 Stopping servers..."
    kill $AI_PID $JAVA_PID $WEB_PID 2>/dev/null
    rm -f /tmp/nova_*.pid
    echo "✅ All servers stopped."
}

# Set trap for cleanup
trap cleanup EXIT INT TERM

# Keep script running
echo "Press Ctrl+C to stop all servers..."
wait
