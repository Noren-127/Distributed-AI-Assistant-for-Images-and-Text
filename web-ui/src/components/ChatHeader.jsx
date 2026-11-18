import React from 'react';
import { Sparkles } from 'lucide-react';

const ChatHeader = () => {
  return (
    <div className="px-6 py-4 bg-[#141414]">
      <div className="flex items-center space-x-3">
        <div className="w-8 h-8 bg-gradient-to-br from-purple-500 to-blue-600 rounded-full flex items-center justify-center">
          <Sparkles className="w-4 h-4 text-white" />
        </div>
        <div>
          <h1 className="text-lg font-semibold">Nova AI Assistant</h1>
          <p className="text-sm text-gray-400">Always here to help</p>
        </div>
      </div>
    </div>
  );
};

export default ChatHeader;
