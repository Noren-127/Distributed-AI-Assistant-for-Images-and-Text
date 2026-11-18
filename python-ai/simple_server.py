#!/usr/bin/env python3
"""
Simple Python AI Server - Enhanced with Mistral API
"""

import json
import logging
import time
import os
from datetime import datetime
from dotenv import load_dotenv
from mistral_engine import MistralAIChatbot
import uuid

# Load environment variables
load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class SimpleAIServer:
    """Enhanced AI Server with Mistral API"""
    
    def __init__(self):
        self.chatbot = MistralAIChatbot()
        self.uploaded_files = {}  # Store uploaded files
        _root = os.getenv('NOVA_PROJECT_ROOT')
        _custom = os.getenv('NOVA_UPLOAD_DIR')
        if _custom:
            self.upload_dir = os.path.abspath(_custom)
        elif _root:
            self.upload_dir = os.path.abspath(os.path.join(_root, 'uploads'))
        else:
            self.upload_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'uploads'))
        os.makedirs(self.upload_dir, exist_ok=True)
        logger.info("Enhanced AI Server with Mistral API initialized")
    
    def create_session(self):
        """Create new chat session"""
        return self.chatbot.create_session()
    
    def send_message(self, session_id, message, file_content=None, image_data=None):
        """Send message and get response with optional file content and image support"""
        
        # Handle mixed content - both files and images
        all_responses = []
        
        # Process files first
        if file_content and file_content.startswith('['):
            try:
                import json
                files = json.loads(file_content)
                for file in files:
                    response_chunks = []
                    # Process file without image data to avoid duplication
                    for chunk in self.chatbot.chat(session_id, message, file['content'], None):
                        if chunk["type"] == "chunk":
                            response_chunks.append(chunk["content"])
                        elif chunk["type"] == "error":
                            return chunk["content"]
                    all_responses.append(f"--- File: {file['name']} ---\n{''.join(response_chunks)}")
            except json.JSONDecodeError:
                # Fallback to single file processing
                response_chunks = []
                for chunk in self.chatbot.chat(session_id, message, file_content, None):
                    if chunk["type"] == "chunk":
                        response_chunks.append(chunk["content"])
                    elif chunk["type"] == "error":
                        return chunk["content"]
                all_responses.append(f"--- Document ---\n{''.join(response_chunks)}")
        elif file_content:
            # Single file
            response_chunks = []
            for chunk in self.chatbot.chat(session_id, message, file_content, None):
                if chunk["type"] == "chunk":
                    response_chunks.append(chunk["content"])
                elif chunk["type"] == "error":
                    return chunk["content"]
            all_responses.append(f"--- Document ---\n{''.join(response_chunks)}")
        
        # Process images separately
        if image_data and image_data.startswith('['):
            try:
                import json
                images = json.loads(image_data)
                for img in images:
                    response_chunks = []
                    # Process image without file data to avoid duplication
                    for chunk in self.chatbot.chat(session_id, message, None, img['data']):
                        if chunk["type"] == "chunk":
                            response_chunks.append(chunk["content"])
                        elif chunk["type"] == "error":
                            return chunk["content"]
                    all_responses.append(f"--- Image: {img['name']} ---\n{''.join(response_chunks)}")
            except json.JSONDecodeError:
                # Fallback to single image processing
                response_chunks = []
                for chunk in self.chatbot.chat(session_id, message, None, image_data):
                    if chunk["type"] == "chunk":
                        response_chunks.append(chunk["content"])
                    elif chunk["type"] == "error":
                        return chunk["content"]
                all_responses.append(f"--- Image ---\n{''.join(response_chunks)}")
        elif image_data:
            # Single image
            response_chunks = []
            for chunk in self.chatbot.chat(session_id, message, None, image_data):
                if chunk["type"] == "chunk":
                    response_chunks.append(chunk["content"])
                elif chunk["type"] == "error":
                    return chunk["content"]
            all_responses.append(f"--- Image ---\n{''.join(response_chunks)}")
        
        # If no files or images, process normally
        if not all_responses:
            response_chunks = []
            for chunk in self.chatbot.chat(session_id, message, file_content, image_data):
                if chunk["type"] == "chunk":
                    response_chunks.append(chunk["content"])
                elif chunk["type"] == "error":
                    return chunk["content"]
            return "".join(response_chunks)
        
        return "\n\n".join(all_responses)
    
    def get_session_info(self, session_id):
        """Get session information"""
        return self.chatbot.get_session_info(session_id)
    
    def health_check(self):
        """Health check"""
        return self.chatbot.health_check()
    
    def shutdown(self):
        """Shutdown server"""
        self.chatbot.shutdown()
    
    def upload_file(self, session_id, file_data, filename):
        """Handle file upload"""
        try:
            file_id = str(uuid.uuid4())
            file_path = os.path.join(self.upload_dir, file_id)
            
            with open(file_path, 'wb') as f:
                f.write(file_data)
            
            self.uploaded_files[file_id] = {
                'session_id': session_id,
                'filename': filename,
                'path': file_path,
                'uploaded_at': datetime.now()
            }
            
            return {
                "status": "success",
                "file_id": file_id,
                "filename": filename
            }
        except Exception as e:
            logger.error(f"File upload failed: {e}")
            return {
                "status": "error",
                "message": str(e)
            }
    
    def get_file_content(self, file_id):
        """Get uploaded file content"""
        if file_id not in self.uploaded_files:
            return {"status": "error", "message": "File not found"}
        
        file_info = self.uploaded_files[file_id]
        file_path = file_info['path']
        
        try:
            # Try to read as text first
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            return {"status": "success", "content": content}
        except UnicodeDecodeError:
            # If it's a binary file (like PDF), try to extract text
            try:
                import PyPDF2
                with open(file_path, 'rb') as f:
                    pdf_reader = PyPDF2.PdfReader(f)
                    text_content = ""
                    for page in pdf_reader.pages:
                        text_content += page.extract_text() + "\n"
                
                if text_content.strip():
                    return {"status": "success", "content": text_content[:10000]}
                else:
                    return {"status": "success", "content": "[PDF file uploaded but text extraction failed. The file may be scanned images.]"}
            except Exception as pdf_ex:
                logger.warning(f"PDF extraction failed: {pdf_ex}")
                return {"status": "success", "content": "[Binary file content - PDF or other format. File uploaded successfully for processing.]"}

# HTTP Server implementation
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
import threading

class AIRequestHandler(BaseHTTPRequestHandler):
    """HTTP request handler for AI server"""
    
    def __init__(self, *args, ai_server=None, **kwargs):
        self.ai_server = ai_server
        super().__init__(*args, **kwargs)
    
    def do_GET(self):
        """Handle GET requests"""
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query_params = parse_qs(parsed_path.query)
        
        if path == '/health':
            self.send_json_response(200, self.ai_server.health_check())
        elif path == '/session':
            session_id = query_params.get('sessionId', [None])[0]
            if session_id:
                self.send_json_response(200, self.ai_server.get_session_info(session_id))
            else:
                self.send_json_response(400, {"status": "error", "message": "Missing sessionId"})
        else:
            self.send_json_response(404, {"status": "error", "message": "Not found"})
    
    def do_POST(self):
        """Handle POST requests"""
        parsed_path = urlparse(self.path)
        path = parsed_path.path
        query_params = parse_qs(parsed_path.query)
        
        if path == '/session':
            # Create new session
            session_id = self.ai_server.create_session()
            response = {
                "status": "success",
                "sessionId": session_id,
                "timestamp": int(time.time() * 1000)
            }
            self.send_json_response(200, response)
        elif path == '/chat':
            # Handle chat message
            session_id = query_params.get('sessionId', [None])[0]
            message = query_params.get('message', [None])[0]
            file_content = query_params.get('fileContent', [None])[0]
            image_data = query_params.get('imageData', [None])[0]
            
            # Try to read JSON request body for file content and image data
            if self.headers.get('Content-Type', '').startswith('application/json'):
                content_length = int(self.headers.get('Content-Length', 0))
                if content_length > 0:
                    post_data = self.rfile.read(content_length)
                    try:
                        import json
                        request_data = json.loads(post_data.decode('utf-8'))
                        if request_data.get('fileContent'):
                            file_content = request_data.get('fileContent')
                        if request_data.get('imageData'):
                            image_data = request_data.get('imageData')
                    except json.JSONDecodeError:
                        pass  # Fall back to URL parameters
            
            if not session_id or not message:
                self.send_json_response(400, {"status": "error", "message": "Missing sessionId or message"})
                return
            
            response = self.ai_server.send_message(session_id, message, file_content, image_data)
            chat_response = {
                "status": "success",
                "response": response,
                "timestamp": int(time.time() * 1000)
            }
            self.send_json_response(200, chat_response)
        elif path == '/extract_pdf':
            # PDF text extraction endpoint
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            
            try:
                import json
                request_data = json.loads(post_data.decode('utf-8'))
                file_path = request_data.get('file_path')
                
                if not file_path:
                    self.send_json_response(400, {"status": "error", "message": "Missing file_path"})
                    return
                
                # Extract text from PDF
                import PyPDF2
                with open(file_path, 'rb') as f:
                    pdf_reader = PyPDF2.PdfReader(f)
                    text_content = ""
                    for page in pdf_reader.pages:
                        text_content += page.extract_text() + "\n"
                
                if text_content.strip():
                    self.send_json_response(200, {
                        "status": "success", 
                        "content": text_content[:10000]  # Limit to 10k chars
                    })
                else:
                    self.send_json_response(200, {
                        "status": "success", 
                        "content": "[PDF text extraction failed - document may contain only images]"
                    })
                    
            except Exception as e:
                logger.error(f"PDF extraction error: {e}")
                self.send_json_response(500, {"status": "error", "message": str(e)})
        elif path == '/uploads':
            # Handle file upload
            session_id = query_params.get('sessionId', [None])[0]
            if not session_id:
                self.send_json_response(400, {"status": "error", "message": "Missing sessionId"})
                return
            
            try:
                content_type = self.headers.get('Content-Type', '')
                if not content_type.startswith('multipart/form-data'):
                    self.send_json_response(400, {"status": "error", "message": "Expected multipart/form-data"})
                    return
                
                # Read multipart data manually
                content_length = int(self.headers.get('Content-Length', 0))
                post_data = self.rfile.read(content_length)
                
                # Simple multipart parsing for files (strip quotes/whitespace from boundary)
                boundary_raw = content_type.split('boundary=')[1].strip().strip('"').strip()
                boundary = boundary_raw.encode()
                parts = post_data.split(b'--' + boundary)
                
                file_data = None
                filename = None
                
                for part in parts:
                    if b'Content-Disposition' in part and b'filename=' in part:
                        # Extract filename
                        filename_start = part.find(b'filename="') + 10
                        filename_end = part.find(b'"', filename_start)
                        if filename_start > 9 and filename_end > filename_start:
                            filename = part[filename_start:filename_end].decode('utf-8')
                        
                        # Extract file data
                        data_start = part.find(b'\r\n\r\n') + 4
                        if data_start < len(part):
                            file_data = part[data_start:-2]  # Remove trailing \r\n
                        break
                
                if file_data and filename:
                    result = self.ai_server.upload_file(session_id, file_data, filename)
                    self.send_json_response(200, result)
                else:
                    self.send_json_response(400, {"status": "error", "message": "No file provided"})
                
            except Exception as e:
                logger.error(f"Upload error: {e}")
                self.send_json_response(500, {"status": "error", "message": str(e)})
        elif path.startswith('/uploads/'):
            # Serve uploaded files for frontend access
            file_id = path.split('/uploads/')[-1]
            result = self.ai_server.get_file_content(file_id)
            self.send_json_response(200 if result["status"] == "success" else 404, result)
        else:
            self.send_json_response(404, {"status": "error", "message": "Not found"})
    
    def do_OPTIONS(self):
        """Handle OPTIONS requests for CORS"""
        self.send_response_headers()
        self.end_headers()
    
    def send_response_headers(self):
        """Send CORS headers (for GET/OPTIONS preflight)"""
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
    
    def send_json_response(self, code, data):
        """Send JSON response with proper headers"""
        response_data = json.dumps(data)
        self.send_response(code)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()
        self.wfile.write(response_data.encode('utf-8'))
    
    def log_message(self, format, *args):
        """Override to prevent default logging"""
        pass

import argparse

def run_server(port=8400):
    """Run AI server on specified port"""
    ai_server = SimpleAIServer()
    
    def handler(*args, **kwargs):
        return AIRequestHandler(*args, ai_server=ai_server, **kwargs)
    
    try:
        with HTTPServer(('0.0.0.0', port), handler) as httpd:
            httpd.protocol_version = "HTTP/1.1"  # Use HTTP/1.1
            logger.info(f"Enhanced AI Server with Mistral API started on port {port}")
            httpd.serve_forever()
    except Exception as e:
        logger.error(f"Failed to start server on port {port}: {e}")
        raise

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Enhanced AI Server with Mistral API')
    parser.add_argument('--port', type=int, default=8400, help='Port to run the server on')
    args = parser.parse_args()
    
    try:
        run_server(args.port)
    except KeyboardInterrupt:
        logger.info("Shutting down server...")
