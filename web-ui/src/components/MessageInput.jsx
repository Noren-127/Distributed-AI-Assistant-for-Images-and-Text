import React, { useRef, useEffect } from 'react';
import { Send, Paperclip, Plus } from 'lucide-react';
import FileAttachment from './FileAttachment';

const MessageInput = ({ 
  inputValue, 
  setInputValue, 
  handleSubmit, 
  uploadedFiles, 
  uploadedImages, 
  formatFileSize, 
  removeFile, 
  canAddMoreFiles, 
  showAttachOptions, 
  setShowAttachOptions, 
  attachOptionsRef, 
  fileInputRef, 
  handleFileSelect 
}) => {
  // Auto-resize textarea
  const textareaRef = useRef(null);
  
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px';
    }
  }, [inputValue]);

  return (
    <div className="px-6 py-4 bg-[#141414]">
      <div className="max-w-4xl mx-auto">
        {/* Message Input Form */}
        <div className={`bg-gray-700 ${(uploadedFiles.length > 0 || uploadedImages.length > 0) ? 'rounded-2xl' : 'rounded-full'} px-4 ${(uploadedFiles.length > 0 || uploadedImages.length > 0) ? 'py-3' : 'py-2'} flex flex-col`}>
          <FileAttachment 
            uploadedFiles={uploadedFiles}
            uploadedImages={uploadedImages}
            formatFileSize={formatFileSize}
            removeFile={removeFile}
          />

          {/* Input Controls Row */}
          <div className="flex items-center">
            {/* Plus Button */}
            <div className="relative">
              <button
                type="button"
                onClick={() => canAddMoreFiles() && setShowAttachOptions(!showAttachOptions)}
                disabled={!canAddMoreFiles()}
                className={`text-gray-400 hover:text-white hover:bg-gray-600 transition-colors mr-2 rounded-full p-2 ${
                  !canAddMoreFiles() ? 'opacity-50 cursor-not-allowed' : ''
                }`}
                title={`${uploadedFiles.length + uploadedImages.length}/2 files used`}
              >
                <Plus className="w-6 h-6" />
              </button>
              
              {/* Attach Options Box */}
              {showAttachOptions && canAddMoreFiles() && (
                <div ref={attachOptionsRef} className="absolute bottom-full left-0 mb-2 bg-gray-700 rounded-lg shadow-lg border border-gray-600 p-2 z-50 w-48 -translate-x-4">
                  <button
                    type="button"
                    onClick={() => {
                      fileInputRef.current?.click();
                      setShowAttachOptions(false);
                    }}
                    className="flex items-center space-x-2 text-white hover:bg-gray-600 rounded p-2 w-full text-left"
                  >
                    <Paperclip className="w-4 h-4" />
                    <span className="text-sm">Upload File</span>
                  </button>
                </div>
              )}
            </div>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleFileSelect}
              multiple
              accept=".pdf,.txt,.csv,.md,.jpg,.jpeg,.png,.gif,.webp"
              className="hidden"
            />

            {/* Message Input */}
            <div className="flex-1">
              <textarea
                ref={textareaRef}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSubmit(e);
                  }
                }}
                placeholder="Message Nova..."
                className="w-full bg-transparent text-white px-3 py-2 resize-none focus:outline-none placeholder-gray-400"
                rows="1"
                style={{minHeight: '36px', maxHeight: '120px', overflow: 'hidden'}}
              />
            </div>

            {/* Send Button */}
            <button
              type="submit"
              disabled={!inputValue.trim()}
              className="bg-gradient-to-br from-purple-500 to-blue-600 hover:bg-green-700 text-white rounded-full p-2 flex items-center justify-center transition-colors disabled:opacity-50 disabled:cursor-not-allowed ml-2"
            >
              <Send className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MessageInput;
