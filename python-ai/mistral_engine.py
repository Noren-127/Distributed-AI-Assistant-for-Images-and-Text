#!/usr/bin/env python3
"""
Mistral AI Engine - Nova with Mistral API
"""

import json
import os
import time
import threading
from datetime import datetime
from typing import List, Dict, Any, Optional, Generator
import logging
import re
try:
    from mistralai import Mistral
except ImportError:
    Mistral = None

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ConversationalMemory:
    """Manages chat history and context"""
    
    def __init__(self, max_history: int = 10):
        self.max_history = max_history
        self.conversations: Dict[str, List[Dict[str, Any]]] = {}
    
    def add_message(self, session_id: str, role: str, content: str, timestamp: Optional[datetime] = None):
        """Add a message to conversation history"""
        if session_id not in self.conversations:
            self.conversations[session_id] = []
        
        message = {
            "role": role,
            "content": content,
            "timestamp": timestamp or datetime.now()
        }
        
        self.conversations[session_id].append(message)
        
        # Keep only last max_history messages
        if len(self.conversations[session_id]) > self.max_history:
            self.conversations[session_id] = self.conversations[session_id][-self.max_history:]
    
    def get_history(self, session_id: str) -> List[Dict[str, Any]]:
        """Get conversation history for a session"""
        return self.conversations.get(session_id, [])
    
    def clear_session(self, session_id: str):
        """Clear conversation history for a session"""
        if session_id in self.conversations:
            del self.conversations[session_id]

class MistralAIChatbot:
    """AI Chatbot using Mistral API"""
    
    def __init__(self):
        self.memory = ConversationalMemory()
        self.sessions: Dict[str, Dict[str, Any]] = {}
        self.session_counter = 0
        
        # Initialize Mistral
        api_key = os.getenv('MISTRAL_API_KEY')
        if not api_key:
            logger.warning("MISTRAL_API_KEY not found in environment. Using fallback mode.")
            self.mistral = None
            self.model = None
        else:
            if Mistral is None:
                logger.error("Mistral library not installed. Install with: pip install mistralai")
                self.mistral = None
                self.model = None
            else:
                self.mistral = Mistral(api_key=api_key)
                self.agent_id = os.getenv("MISTRAL_AGENT_ID", "ag_019c996595ba7585a8f710e5dc81cd06")
                self.model = os.getenv("MISTRAL_MODEL", "mistral-large-2512")  # Vision-capable model
                logger.info("Mistral agent initialized: %s", self.agent_id)
    
    def create_session(self) -> str:
        """Create new chat session"""
        self.session_counter += 1
        session_id = f"mistral_session_{self.session_counter}"
        
        self.sessions[session_id] = {
            "created_at": datetime.now(),
            "message_count": 0,
            "last_activity": datetime.now()
        }
        
        logger.info(f"Created new session: {session_id}")
        return session_id
    
    def chat(self, session_id: str, message: str, file_content: Optional[str] = None, image_data: Optional[str] = None) -> Generator[Dict[str, Any], None, None]:
        """Process chat message using Mistral Agent API with optional file content and image support"""
        try:
            logger.info(f"Chat request - Session: {session_id}, Message: {message[:50]}..., File content: {file_content[:50] if file_content else 'None'}, Image: {'Yes' if image_data else 'None'}")
            
            # Add user message to memory
            self.memory.add_message(session_id, "user", message)
            
            # Update session activity
            if session_id in self.sessions:
                self.sessions[session_id]["message_count"] += 1
                self.sessions[session_id]["last_activity"] = datetime.now()
            
            # Start response
            yield {"type": "start", "session_id": session_id}
            
            if self.mistral:
                # Use Mistral Agent API
                response = self._get_agent_response(session_id, message, file_content, image_data)
                yield {"type": "chunk", "content": response}
            else:
                # Fallback to rule-based responses
                response = self._get_fallback_response(message, file_content)
                yield {"type": "chunk", "content": response}
            
            # Add assistant response to memory
            if self.mistral:
                self.memory.add_message(session_id, "assistant", response)
            else:
                self.memory.add_message(session_id, "assistant", response)
            
            yield {"type": "end", "session_id": session_id}
            
        except Exception as e:
            logger.error(f"Chat error for session {session_id}: {e}")
            yield {"type": "error", "content": "Sorry, I encountered an error. Please try again."}
    
    def _get_agent_response(self, session_id: str, message: str, file_content: Optional[str] = None, image_data: Optional[str] = None) -> str:
        """Get response from Mistral Agent API with context, file content, and image support"""
        try:
            # Build conversation context
            history = self.memory.get_history(session_id)
            messages = []
            
            # Add conversation history (last 5 messages, excluding current)
            for msg in history[-5:]:
                if msg["role"] in ["user", "assistant"] and msg["content"] != message:
                    messages.append({
                        "role": msg["role"],
                        "content": msg["content"]
                    })
            
            # Build user message with file and image support
            user_content = []
            
            # Add text message
            if file_content and file_content.strip():
                user_content.append({"type": "text", "text": f"Question about uploaded file: {message}"})
            else:
                user_content.append({"type": "text", "text": message})
            
            # Add image if provided
            if image_data:
                user_content.append({
                    "type": "image_url", 
                    "image_url": {
                        "url": image_data
                    }
                })
            
            # Add file content as context if provided
            if file_content and file_content.strip():
                user_content.append({
                    "type": "text", 
                    "text": f"\n\nFile content for context:\n{file_content[:5000]}"
                })
            
            messages.append({
                "role": "user",
                "content": user_content
            })
            
            # Debug logging
            logger.info(f"Sending to agent: {json.dumps(messages, indent=2)}")
            
            # Get response from Mistral Agent
            # Temporarily use chat completion for testing vision
            logger.info(f"Using chat completion with vision model: {self.model}")
            chat_response = self.mistral.chat.complete(
                model=self.model,
                messages=messages
            )
            return chat_response.choices[0].message.content
            
        except Exception as e:
            error_text = str(e)
            logger.error(f"Mistral Agent API error: {error_text}")
            logger.error(f"Session: {session_id}, Message: {message[:100]}, File content length: {len(file_content) if file_content else 0}, Image: {'Yes' if image_data else 'None'}")
            if "quota" in error_text.lower() or "rate limit" in error_text.lower():
                return (
                    "Mistral API quota is exhausted for this project. "
                    "Please check your plan and billing, or wait before retrying."
                )
            return self._get_fallback_response(message, file_content)
    
    def _get_fallback_response(self, message: str, file_content: Optional[str] = None) -> str:
        """Fallback rule-based responses when Mistral is unavailable"""
        message_lower = message.lower().strip()
        
        # If file content is provided, give basic file analysis
        if file_content and file_content.strip():
            if "analyze" in message_lower or "explain" in message_lower or "what" in message_lower:
                return "I can see you've uploaded a file. While I'm in fallback mode, I can tell you that the file contains text content. For detailed analysis, please check your API connection or try again later."
            else:
                return "I've received your file, but I'm currently in fallback mode. I can see the file content, but for proper analysis, please ensure the API is available."
        
        # Greeting patterns
        if any(greeting in message_lower for greeting in ['hello', 'hi', 'hey', 'good morning', 'good afternoon']):
            return "Hello! I'm Nova, your AI assistant. How can I help you today?"
        
        # Help patterns
        if any(help_word in message_lower for help_word in ['help', 'assist', 'support']):
            return "I'm here to help! I can assist with questions, analyze uploaded files, help with assignments, and provide explanations. What would you like help with?"
        
        # Default response
        return "I'm here to help! Could you tell me more about what you need assistance with?"
    
    def get_session_info(self, session_id: str) -> Dict[str, Any]:
        """Get session information"""
        if session_id not in self.sessions:
            return {"status": "error", "message": "Session not found"}
        
        session = self.sessions[session_id]
        history = self.memory.get_history(session_id)
        
        return {
            "status": "success",
            "session_id": session_id,
            "message_count": session["message_count"],
            "created_at": session["created_at"].isoformat(),
            "last_activity": session["last_activity"].isoformat(),
            "history_length": len(history)
        }
    
    def health_check(self) -> Dict[str, Any]:
        """Health check for the AI engine"""
        return {
            "status": "healthy",
            "mistral_connected": self.mistral is not None,
            "active_sessions": len(self.sessions),
            "total_conversations": len(self.memory.conversations),
            "timestamp": datetime.now().isoformat()
        }
    
    def shutdown(self):
        """Cleanup resources"""
        logger.info("Mistral AI Engine shutting down...")
        self.memory.conversations.clear()
        self.sessions.clear()

# Test function
if __name__ == "__main__":
    chatbot = MistralAIChatbot()
    session_id = chatbot.create_session()
    
    print("Testing Mistral AI Engine...")
    for chunk in chatbot.chat(session_id, "Hello, what can you do?"):
        print(f"Chunk: {chunk}")
    
    print(f"Session info: {chatbot.get_session_info(session_id)}")
