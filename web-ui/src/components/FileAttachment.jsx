import React from 'react';
import { FileText, File, Trash2, Image as ImageIcon } from 'lucide-react';

const FileAttachment = ({ uploadedFiles, uploadedImages, formatFileSize, removeFile }) => {
  return (
    <>
      {(uploadedFiles.length > 0 || uploadedImages.length > 0) && (
        <div className="flex flex-wrap gap-2 mb-2">
          {uploadedFiles.map(file => (
            <div key={file.id} className="bg-gray-600 rounded-lg px-3 py-2 flex items-center space-x-2">
              {file.name.toLowerCase().endsWith('.pdf') ? (
                <FileText className="w-4 h-4 text-red-400 flex-shrink-0" />
              ) : (
                <File className="w-4 h-4 text-blue-400 flex-shrink-0" />
              )}
              <div className="flex flex-col">
                <span className="text-white text-sm truncate max-w-[120px]">{file.name}</span>
                <span className="text-gray-400 text-xs">{formatFileSize(file.size)}</span>
              </div>
              <button
                onClick={() => removeFile(file.id)}
                className="text-gray-400 hover:text-white"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
          ))}
          {uploadedImages.map(image => (
            <div key={image.id} className="bg-gray-600 rounded-lg px-3 py-2 flex items-center space-x-2">
              <ImageIcon className="w-4 h-4 text-green-400 flex-shrink-0" />
              <div className="flex flex-col">
                <span className="text-white text-sm truncate max-w-[120px]">{image.name}</span>
                <span className="text-gray-400 text-xs">{formatFileSize(image.size)}</span>
              </div>
              <button
                onClick={() => removeFile(image.id)}
                className="text-gray-400 hover:text-white"
              >
                <Trash2 className="w-3 h-3" />
              </button>
            </div>
          ))}
        </div>
      )}
    </>
  );
};

export default FileAttachment;
