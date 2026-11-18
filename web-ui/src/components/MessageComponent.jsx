import React from 'react';

const MessageComponent = ({ message }) => {
  const renderDocumentGrid = (fileAttachment) => {
    // Check if this is a mixed attachment (contains both images and documents)
    if (fileAttachment.includes('data-file-id') && fileAttachment.includes('<img')) {
      // Mixed content - render as-is (already formatted as grid)
      return <div dangerouslySetInnerHTML={{ __html: fileAttachment }} />;
    }
    
    // Check if this is a document attachment (contains file info)
    if (fileAttachment.includes('data-file-id') && fileAttachment.includes('data-file-name')) {
      // Extract file information from the HTML
      const parser = new DOMParser();
      const doc = parser.parseFromString(fileAttachment, 'text/html');
      const fileElements = doc.querySelectorAll('[data-file-id]');
      
      if (fileElements.length > 1) {
        // Multiple files - create grid
        return (
          <div className="flex justify-end mb-2">
            <div className="grid grid-cols-2 gap-2 max-w-xs">
              {Array.from(fileElements).map((element, index) => {
                const fileName = element.getAttribute('data-file-name');
                const fileSize = element.getAttribute('data-file-size');
                const fileId = element.getAttribute('data-file-id');
                const isPdf = fileName.toLowerCase().endsWith('.pdf');
                const FileIcon = isPdf ? '📄' : '📄';
                
                return (
                  <div key={index} className="bg-gray-800 border border-gray-700 rounded-lg p-2 hover:bg-gray-750 transition-colors">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{FileIcon}</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-xs text-gray-300 truncate font-medium" title={fileName}>
                          {fileName.length > 15 ? fileName.substring(0, 15) + '...' : fileName}
                        </div>
                        <div className="text-xs text-gray-500">{fileSize}</div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      } else if (fileElements.length === 1) {
        // Single file - create single card
        const element = fileElements[0];
        const fileName = element.getAttribute('data-file-name');
        const fileSize = element.getAttribute('data-file-size');
        const fileId = element.getAttribute('data-file-id');
        const isPdf = fileName.toLowerCase().endsWith('.pdf');
        const FileIcon = isPdf ? '📄' : '📄';
        
        return (
          <div className="flex justify-end mb-2">
            <div className="bg-gray-800 border border-gray-700 rounded-lg p-3 hover:bg-gray-750 transition-colors max-w-xs">
              <div className="flex items-center space-x-3">
                <span className="text-xl">{FileIcon}</span>
                <div className="flex-1 min-w-0">
                  <div className="text-sm text-gray-300 truncate font-medium" title={fileName}>
                    {fileName.length > 20 ? fileName.substring(0, 20) + '...' : fileName}
                  </div>
                  <div className="text-xs text-gray-500">{fileSize}</div>
                </div>
              </div>
            </div>
          </div>
        );
      }
    }
    
    // Single file or image - use original rendering
    return <div dangerouslySetInnerHTML={{ __html: fileAttachment }} />;
  };

  if (message.role === 'user') {
    return (
      <div className="flex justify-end mb-4 message-bubble">
        <div className="max-w-xs lg:max-w-lg">
          {/* Document/Image Grid Above Message */}
          {message.fileAttachment && renderDocumentGrid(message.fileAttachment)}
          {/* Message Bubble */}
          <div style={{backgroundColor: '#374151'}} className="text-white rounded-2xl rounded-br-sm px-5 py-3">
            <div className="text-base message-content text-right">{message.content}</div>
          </div>
          <div className="text-sm text-gray-400 mt-1 text-right">You</div>
        </div>
      </div>
    );
  } else if (message.role === 'assistant') {
    return (
      <div className="flex justify-center mb-4 message-bubble">
        <div className="max-w-4xl">
          <div className="text-white rounded-2xl rounded-bl-sm px-6 py-4">
            <div className="text-base message-content leading-relaxed">{message.content}</div>
          </div>
          <div className="text-sm text-gray-400 mt-2">Nova</div>
        </div>
      </div>
    );
  } else {
    return (
      <div className="flex justify-center mb-4">
        <div className="bg-red-900 bg-opacity-50 border border-red-600 text-red-200 rounded-lg px-4 py-3 text-sm max-w-md error-message">
          {message.content}
        </div>
      </div>
    );
  }
};

export default MessageComponent;
