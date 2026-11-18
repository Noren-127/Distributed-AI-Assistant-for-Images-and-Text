import React from 'react';
import { Sparkles } from 'lucide-react';

const WelcomeScreen = ({ onPromptClick }) => {
  const examplePrompts = [
    "Help me write a professional email",
    "Explain quantum computing simply", 
    "Create a workout plan for beginners",
    "Help me debug this code error"
  ];

  return (
    <div className="flex flex-col items-center justify-center h-full px-8">
      {/* Nova Logo */}
      <div className="mb-8">
        <div className="w-24 h-24 bg-gradient-to-br from-purple-500 to-blue-600 rounded-3xl flex items-center justify-center shadow-lg">
          <Sparkles className="w-12 h-12 text-white" />
        </div>
      </div>

      {/* Welcome Text */}
      <div className="text-center mb-12">
        <h1 className="text-3xl font-bold text-white mb-2">Welcome to Nova</h1>
        <p className="text-lg text-gray-400">AI Assistant</p>
      </div>

      {/* Example Prompts */}
      <div className="w-full max-w-2xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {examplePrompts.map((prompt, index) => (
            <div 
              key={index}
              className="bg-[#1a1a1a] border border-gray-700 rounded-lg px-4 py-3 text-gray-300 text-sm hover:bg-[#2a2a2a] transition-colors cursor-pointer"
              onClick={() => onPromptClick(prompt)}
            >
              "{prompt}"
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default WelcomeScreen;
