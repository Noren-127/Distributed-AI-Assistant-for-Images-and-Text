# Nova AI Chatbot System

A distributed AI chatbot system with conversational abilities and file upload capabilities, built with Python, Java RMI, Hadoop HDFS, and React.

## 🏗️ Architecture

- **Python AI Engine**: Handles LLM inference, conversational memory, and file processing
- **Java RMI Server**: Manages distributed communication and Hadoop integration
- **React Web UI**: Modern chat interface with streaming responses
- **Hadoop HDFS**: Scalable storage for files and chat history (dual storage)

## ✨ Features

- Human-like conversational AI with personality
- File upload and analysis (PDF, TXT, CSV, Images)
- **Dual storage**: Local filesystem + Hadoop HDFS
- Organized file storage: Documents, Images, ChatHistory
- Real-time streaming responses
- Assignment help and fact explanations
- Distributed processing with Hadoop
- Chat history persistence with Hadoop backup

## 🚀 Quick Start

### Prerequisites
- Java 21 (OpenJDK 21.0.10)
- Python 3.8+
- Node.js 16+
- Hadoop 3.4.1 (installed and configured)
*** If you haven't installed Hadoop, check HADOOP_SETUP.md ***

### 1. Start Hadoop HDFS (after reboot)
```bash
# Switch to hadoop user
su - hadoop

# Start HDFS services
start-dfs.sh

# Verify services are running
jps
# Should see: NameNode, DataNode, SecondaryNameNode

# Switch back to your user
exit
```

### 2. Start Nova Chatbot System
```bash
cd /home/shuraaa/Desktop/DistributedSystem/Project

# Start all services (Python AI + Java RMI + React UI)
./start_distributed.sh
```

### 3. Access the System
- **Web Interface**: http://localhost:8200
- **Java RMI Server**: http://localhost:8082
- **Python AI Server**: http://localhost:8400
- **HDFS Web UI**: http://localhost:9870

## 📁 Storage Structure

### Local Storage
```
uploads/
├── Documents/     # PDF, TXT, CSV files
├── Images/        # JPG, PNG, GIF files
└── ChatHistory/    # Chat session JSON files
```

### HDFS Storage
```
/chatbot/
├── Documents/      # Backup of document files
├── Images/         # Backup of image files
├── ChatHistory/    # Backup of chat history
└── sessions/       # Active session data
```

## 🔧 Configuration

### Environment Variables
```bash
export HADOOP_HOME=/usr/local/hadoop
export NOVA_PROJECT_ROOT=/home/shuraaa/Desktop/DistributedSystem/Project (Your project root path)
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
```

### Python AI Server
```bash
cd python-ai
# Edit .env with your API keys
source venv/bin/activate
pip install -r requirements.txt
```

### Java RMI Server
```bash
cd java-rmi
mvn compile package
# Automatically started by start_distributed.sh
```

### React Web UI
```bash
cd web-ui
npm install
npm run dev
# Automatically started by start_distributed.sh
```

## 🎯 Usage

### Upload and Analyze Files
1. Open http://localhost:8200 in your browser
2. Upload documents (PDF, TXT, CSV) or images
3. Files are stored in both local and HDFS storage
4. Ask questions about uploaded content

### Chat History Management
- **Save**: Automatically saved to both local and HDFS
- **Load**: Restore previous conversations
- **Delete**: Removes from both storage locations
- **Clear All**: Wipes all chat history from both systems

### Health Check
```bash
curl http://localhost:8082/api/health
```
Expected response:
```json
{
  "status": "healthy",
  "activeSessions": 0,
  "pythonConnected": true,
  "hadoopConnected": true,
  "timestamp": 1772625430114 or something
}
```

## 🔍 Verification Commands

### Check HDFS Storage
```bash
# As hadoop user
hdfs dfs -ls /chatbot/Documents
hdfs dfs -ls /chatbot/Images
hdfs dfs -ls /chatbot/ChatHistory
```

### Check Local Storage
```bash
ls -la uploads/Documents/
ls -la uploads/Images/
ls -la uploads/ChatHistory/
```

### Check Service Status
```bash
# Hadoop services (as hadoop user)
jps

# Nova services
ps aux | grep -E "(simple_server|ChatbotServer|npm)"
```

## 🛠️ Management

### Start Services
```bash
# Start HDFS (as hadoop user)
start-dfs.sh

# Start Nova system
./start_distributed.sh
```

### Stop Services
```bash
# Stop Nova system
./stop_distributed.sh

# Stop HDFS (as hadoop user)
stop-dfs.sh
```

### After System Reboot
1. Start HDFS: `su - hadoop && start-dfs.sh && exit`
2. Start Nova: `./start_distributed.sh`



## 🐛 Troubleshooting

### Common Issues

#### Hadoop Not Connected
```bash
# Check HDFS status
su - hadoop -c "hdfs dfsadmin -report"

# Restart HDFS
su - hadoop -c "stop-dfs.sh && start-dfs.sh"
```

#### Port Conflicts
```bash
# Check what's using ports
lsof -i :8082  # Java RMI
lsof -i :8400  # Python AI
lsof -i :8200  # React UI

# Kill conflicting processes
./stop_distributed.sh
```

### Log Files
- **Nova Java**: `java-rmi/java_server.log`
- **Python AI**: `python-ai/python_ai.log`
- **React UI**: `web-ui/web_server.log`
- **Hadoop**: `/usr/local/hadoop/logs/`

## 📊 System Requirements

- **RAM**: Minimum 4GB (8GB recommended)
- **Disk**: 10GB+ free space
- **OS**: Linux (Debian/Ubuntu tested)
- **Java**: OpenJDK 21.0.10
- **Python**: 3.8+
- **Node.js**: 16+

## 🔒 Security Notes

- Hadoop runs as dedicated `hadoop` user
- File uploads are validated and sandboxed
- Chat history is encrypted in storage
- No external API calls except configured AI services

## 📈 Performance

- **Dual Storage**: Redundancy + Performance
- **HDFS**: Scalable for large file collections
- **Local Storage**: Fast access for recent files
- **Memory Management**: Efficient session handling
