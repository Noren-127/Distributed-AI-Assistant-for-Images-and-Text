# Hadoop Setup Guide for Nova Chatbot

## System Requirements
- **Java**: OpenJDK 21.0.10 ✅ (Already installed)
- **OS**: Linux (Debian/Ubuntu)
- **Memory**: Minimum 4GB RAM
- **Disk**: 10GB+ free space

## Step 1: Install Hadoop

### 1.1 Create Hadoop User
```bash
# Create dedicated user for Hadoop
sudo useradd -m hadoop
sudo passwd hadoop
# Add to sudo group (optional)
sudo usermod -aG sudo hadoop

# Switch to hadoop user
su - hadoop
```

### 1.2 Download Hadoop
```bash
# Download Hadoop 3.4.1 (stable version)
cd ~
wget https://downloads.apache.org/hadoop/common/hadoop-3.4.1/hadoop-3.4.1.tar.gz

# Extract
tar -xzf hadoop-3.4.1.tar.gz
sudo mv hadoop-3.4.1 /usr/local/hadoop
sudo chown -R hadoop:hadoop /usr/local/hadoop
```

### 1.3 Set Environment Variables
```bash
# Edit ~/.bashrc
nano ~/.bashrc

# Add these lines at the end:
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export HADOOP_MAPRED_HOME=$HADOOP_HOME
export HADOOP_COMMON_HOME=$HADOOP_HOME
export HADOOP_HDFS_HOME=$HADOOP_HOME
export YARN_HOME=$HADOOP_HOME
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin

# Apply changes
source ~/.bashrc
```

## Step 2: Configure Hadoop

### 2.1 Java Home Configuration
```bash
# Find Java path
readlink -f $(which java)
# Output: /usr/lib/jvm/java-21-openjdk-amd64/bin/java

# Edit hadoop-env.sh
nano $HADOOP_HOME/etc/hadoop/hadoop-env.sh

# Add/modify this line:
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### 2.2 Core Configuration
```bash
# Edit core-site.xml
nano $HADOOP_HOME/etc/hadoop/core-site.xml

<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/usr/local/hadoop/tmp</value>
    </property>
</configuration>
```

### 2.3 HDFS Configuration
```bash
# Edit hdfs-site.xml
nano $HADOOP_HOME/etc/hadoop/hdfs-site.xml

<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///usr/local/hadoop/hdfs/namenode</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///usr/local/hadoop/hdfs/datanode</value>
    </property>
    <property>
        <name>dfs.permissions.enabled</name>
        <value>false</value>
    </property>
</configuration>
```

### 2.4 MapReduce Configuration
```bash
# Edit mapred-site.xml
nano $HADOOP_HOME/etc/hadoop/mapred-site.xml

<configuration>
    <property>
        <name>mapreduce.framework.name</name>
        <value>yarn</value>
    </property>
    <property>
        <name>mapreduce.jobtracker.address</name>
        <value>localhost:9001</value>
    </property>
</configuration>
```

### 2.5 YARN Configuration
```bash
# Edit yarn-site.xml
nano $HADOOP_HOME/etc/hadoop/yarn-site.xml

<configuration>
    <property>
        <name>yarn.nodemanager.aux-services</name>
        <value>mapreduce_shuffle</value>
    </property>
    <property>
        <name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name>
        <value>org.apache.hadoop.mapred.ShuffleHandler</value>
    </property>
    <property>
        <name>yarn.resourcemanager.hostname</name>
        <value>localhost</value>
    </property>
</configuration>
```

## Step 3: Initialize Hadoop

### 3.1 Create Directories
```bash
sudo mkdir -p /usr/local/hadoop/tmp
sudo mkdir -p /usr/local/hadoop/hdfs/namenode
sudo mkdir -p /usr/local/hadoop/hdfs/datanode
sudo chown -R hadoop:hadoop /usr/local/hadoop/tmp
sudo chown -R hadoop:hadoop /usr/local/hadoop/hdfs
```

### 3.2 Format NameNode
```bash
# Format HDFS (only do this once!)
hdfs namenode -format
```

### 3.3 Setup SSH (Required for Hadoop)
```bash
# Generate SSH key
ssh-keygen -t rsa -P ""

# Add key to authorized_keys
cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

# Test SSH connection
ssh localhost
# Type 'exit' to return
```

## Step 4: Start Hadoop Services

### 4.1 Start HDFS
```bash
# Start NameNode and DataNode
start-dfs.sh

# Check if services are running
jps
# Should see: NameNode, DataNode, SecondaryNameNode
```

# Switch back to your user for Nova testing
```bash
exit
```

# Set environment for Nova
```bash
export HADOOP_HOME=/usr/local/hadoop
export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
export NOVA_PROJECT_ROOT=/home/shuraaa/Desktop/DistributedSystem/Project (Your project root folder)
```

### 4.2 Verify Hadoop is Working 
```bash
# Check HDFS status
hdfs dfsadmin -report

# Create test directory
hdfs dfs -mkdir -p /chatbot/Documents
hdfs dfs -mkdir -p /chatbot/Images
hdfs dfs -mkdir -p /chatbot/ChatHistory

# Verify directories exist
hdfs dfs -ls /chatbot

# Test file upload to HDFS
echo "Nova Chatbot HDFS Test - $(date)" > nova_hdfs_test.txt
hdfs dfs -put nova_hdfs_test.txt /chatbot/Documents/
hdfs dfs -ls /chatbot/Documents/
hdfs dfs -cat /chatbot/Documents/nova_hdfs_test.txt
```

## Step 5: Configure Nova Chatbot

### 5.1 Set Environment Variables
```bash
# Add to ~/.bashrc (or your preferred shell config)
export HADOOP_HOME=/usr/local/hadoop
export NOVA_PROJECT_ROOT=/home/shuraaa/Desktop/DistributedSystem/Project

# Apply changes
source ~/.bashrc
```

### 5.2 Restart Nova Services
```bash
# Stop any running Nova services
# Then restart with Hadoop support
cd /home/shuraaa/Desktop/DistributedSystem/Project/
./start_distributed.sh
```

## Step 6: Web Interfaces (Optional)

### 6.1 HDFS Web UI
- **URL**: http://localhost:9870
- **Shows**: NameNode status, file system browser

## Step 7: Common Commands

### HDFS File Operations
```bash
# List files
hdfs dfs -ls /

# Create directory
hdfs dfs -mkdir /path/to/directory

# Upload file
hdfs dfs -put localfile /hdfs/path

# Download file
hdfs dfs -get /hdfs/path localfile

# Delete file
hdfs dfs -rm /hdfs/path

# Check disk usage
hdfs dfs -du -h /
```

### Service Management
## For starting HDFS
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

## For stopping HDFS

```bash
# Switch to hadoop user
su - hadoop

# If you wanna Stop individual services
stop-dfs.sh
 
# Switch back to your user
exit
```

## Testing Nova Chatbot with Hadoop

1. **Upload a file** through the web interface
2. **Check logs** for Hadoop integration messages
3. **Verify HDFS storage**:
   ```
   hdfs dfs -ls /chatbot/Documents
   hdfs dfs -ls /chatbot/Images
   hdfs dfs -ls /chatbot/ChatHistory
   ```

## Success Indicators

✅ **Hadoop installed** in `/usr/local/hadoop`
✅ **Services running**: NameNode, DataNode, SecondaryNameNode
✅ **HDFS accessible**: `hdfs dfs -ls /` works
✅ **Dual storage**: Files appear in both `/uploads` and HDFS

## Next Steps

Once Hadoop is running, your Nova Chatbot will:
- Store files in both local filesystem and HDFS
- Provide redundancy and scalability
- Support distributed processing capabilities
- Maintain the organized folder structure in both storage systems
