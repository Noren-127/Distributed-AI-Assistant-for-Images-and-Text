import React from 'react';

const saveHistory = async (sessionId, messages, chatHistory, setChatHistory) => {
  console.log('saveHistory called with:', { sessionId, messageCount: messages.length, chatHistoryCount: chatHistory.length });
  
  if (!sessionId || messages.length === 0) {
    console.log('saveHistory: No sessionId or messages, returning early');
    return;
  }

  try {
    // Prepare messages for server
    const chatMessages = messages.map(msg => ({
      role: msg.role,
      content: msg.content,
      fileAttachment: msg.fileAttachment || null,
      timestamp: msg.timestamp
    }));

    // Generate title from first user message
    const firstUserMessage = messages.find(m => m.role === 'user');
    const chatTitle = firstUserMessage 
      ? firstUserMessage.content.substring(0, 50) + (firstUserMessage.content.length > 50 ? '...' : '')
      : 'New Chat';

    // Check if chat already exists
    const existingChatIndex = chatHistory.findIndex(c => c.id === sessionId);
    
    if (existingChatIndex >= 0) {
      // Update existing chat
      const updatedChat = {
        ...chatHistory[existingChatIndex],
        messages: chatMessages,
        messageCount: chatMessages.length,
        timestamp: new Date().toISOString()
      };

      // Update local state immediately
      const newHistory = [...chatHistory];
      newHistory[existingChatIndex] = updatedChat;
      newHistory.splice(existingChatIndex, 1);
      newHistory.unshift(updatedChat);
      setChatHistory(newHistory);

      // Save to server
      const response = await fetch('http://127.0.0.1:8082/api/history', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          chatId: sessionId,
          title: updatedChat.title,
          messages: JSON.stringify(chatMessages),
          timestamp: updatedChat.timestamp,
          messageCount: chatMessages.length
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      console.log('Chat updated on server');
      return true; // Return true to indicate it was an update
    } else {
      // Create new chat
      const newChat = {
        id: sessionId,
        title: chatTitle,
        messages: chatMessages,
        timestamp: new Date().toISOString(),
        messageCount: chatMessages.length
      };

      // Update local state immediately
      setChatHistory(prev => [newChat, ...prev]);

      // Save to server
      const response = await fetch('http://127.0.0.1:8082/api/history', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          chatId: sessionId,
          title: chatTitle,
          messages: JSON.stringify(chatMessages),
          timestamp: new Date().toISOString(),
          messageCount: chatMessages.length
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      console.log('New chat saved to server');
      return false; // Return false to indicate it was a new chat
    }
  } catch (error) {
    console.error('Failed to save chat history:', error);
  }
};

export default saveHistory;
