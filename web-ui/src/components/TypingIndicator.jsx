import React from 'react';

const TypingIndicator = () => {
  return (
    <div className="px-6 py-2 bg-[#141414]">
      <div className="max-w-4xl mx-auto">
        <div className="flex items-center space-x-2 text-gray-400 px-4">
          <div className="flex space-x-1">
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '0.1s'}}></div>
            <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{animationDelay: '0.2s'}}></div>
          </div>
          <span className="text-sm">Nova is typing...</span>
        </div>
      </div>
    </div>
  );
};

export default TypingIndicator;
