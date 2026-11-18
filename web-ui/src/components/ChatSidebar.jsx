import React from 'react';
import { Plus, Trash2 } from 'lucide-react';

const ChatSidebar = ({ 
  chatHistory, 
  sessionId, 
  isConnected, 
  createNewChat, 
  loadChat, 
  deleteChat,
  clearAllHistory 
}) => {
  return (
    <div className="w-80 bg-black flex flex-col">
      {/* New Chat Button */}
      <div className="p-4 border-b border-gray-700">
        <button
          onClick={createNewChat}
          className="w-full bg-gradient-to-br from-purple-500 to-blue-600 hover:bg-gray-600 text-white rounded-lg px-4 py-3 flex items-center justify-center space-x-2 transition-colors"
        >
          <Plus className="w-5 h-5" />
          <span>New chat</span>
        </button>
      </div>

      {/* Chat History */}
      <div className="flex-1 overflow-y-auto p-4">
        {chatHistory.length === 0 ? (
          <div className="text-xs text-white text-center py-8">
            No previous conversations
          </div>
        ) : (
          chatHistory.map(chat => (
            <div
              key={chat.id}
              className={`p-3 border-b border-gray cursor-pointer hover:bg-gray-700 ${
                chat.id === sessionId ? 'bg-black' : ''
              }`}
              onClick={(e) => {
                // Only load chat if not clicking delete button
                if (!e.target.closest('.delete-btn')) {
                  loadChat(chat.id);
                }
              }}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-medium truncate">{chat.title}</div>
                  <div className="text-xs text-white">
                    {new Date(chat.timestamp).toLocaleDateString()}
                  </div>
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteChat(chat.id);
                  }}
                  className="delete-btn text-white hover:text-red-400 hover:bg-red-900/20 p-1 rounded transition-colors ml-2"
                  title="Delete chat"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Status */}
      <div className="p-4 border-t border-gray-700">
        <div className="flex items-center space-x-2 mb-3">
          <div className={`w-3 h-3 rounded-full bg-${isConnected ? 'green' : 'red'}-400`}></div>
          <span className="text-sm text-gray-300">{isConnected ? 'Connected' : 'Disconnected'}</span>
        </div>
        <button
          onClick={clearAllHistory}
          className="w-full bg-red-600 hover:bg-red-700 text-white rounded-lg px-3 py-2 text-sm flex items-center justify-center space-x-2 transition-colors"
        >
          <Trash2 className="w-4 h-4" />
          <span>Clear All History</span>
        </button>
      </div>
    </div>
  );
};

export default ChatSidebar;
