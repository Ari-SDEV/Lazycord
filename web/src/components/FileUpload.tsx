import { useState, useCallback } from 'react'
import { useAuthStore } from '../stores/authStore'
import './FileUpload.css'

interface FileUploadProps {
  channelId?: string
  onUploadComplete?: (file: UploadedFile) => void
}

interface UploadedFile {
  id: string
  originalName: string
  mimeType: string
  size: number
  url: string
  downloadUrl: string
}

export default function FileUpload({ channelId, onUploadComplete }: FileUploadProps) {
  const { accessToken } = useAuthStore()
  const [isDragging, setIsDragging] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [error, setError] = useState('')

  const allowedTypes = [
    'image/jpeg', 'image/png', 'image/gif', 'image/webp',
    'application/pdf', 'text/plain', 'application/zip'
  ]

  const maxSize = 10 * 1024 * 1024 // 10MB

  const validateFile = (file: File): string | null => {
    if (!allowedTypes.includes(file.type)) {
      return `File type not allowed: ${file.type}. Allowed: images, PDF, TXT, ZIP`
    }
    if (file.size > maxSize) {
      return `File too large: ${(file.size / 1024 / 1024).toFixed(2)}MB. Max: 10MB`
    }
    return null
  }

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    
    const files = e.dataTransfer.files
    if (files.length > 0) {
      const file = files[0]
      const error = validateFile(file)
      if (error) {
        setError(error)
        return
      }
      setSelectedFile(file)
      setError('')
    }
  }, [])

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const error = validateFile(file)
      if (error) {
        setError(error)
        return
      }
      setSelectedFile(file)
      setError('')
    }
  }

  const uploadFile = async () => {
    if (!selectedFile) return

    setIsUploading(true)
    setUploadProgress(0)
    setError('')

    try {
      const formData = new FormData()
      formData.append('file', selectedFile)
      if (channelId) {
        formData.append('channelId', channelId)
      }

      const response = await fetch('/api/files/upload', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        body: formData,
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Upload failed')
      }

      const data: UploadedFile = await response.json()
      
      setSelectedFile(null)
      if (onUploadComplete) {
        onUploadComplete(data)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed')
    } finally {
      setIsUploading(false)
      setUploadProgress(0)
    }
  }

  const clearSelection = () => {
    setSelectedFile(null)
    setError('')
  }

  const getFileIcon = (type: string) => {
    if (type.startsWith('image/')) return '🖼️'
    if (type === 'application/pdf') return '📄'
    if (type === 'application/zip') return '📦'
    return '📎'
  }

  if (isUploading) {
    return (
      <div className="file-upload uploading">
        <div className="upload-progress">
          <div className="progress-bar">
            <div 
              className="progress-fill" 
              style={{ width: `${uploadProgress}%` }}
            />
          </div>
          <span>Uploading... {uploadProgress}%</span>
        </div>
      </div>
    )
  }

  return (
    <div className="file-upload">
      {selectedFile ? (
        <div className="file-preview">
          <div className="file-info">
            <span className="file-icon">{getFileIcon(selectedFile.type)}</span>
            <div className="file-details">
              <span className="file-name">{selectedFile.name}</span>
              <span className="file-size">
                {(selectedFile.size / 1024).toFixed(1)} KB
              </span>
            </div>
          </div>
          <div className="file-actions">
            <button className="upload-btn" onClick={uploadFile}>
              Upload
            </button>
            <button className="cancel-btn" onClick={clearSelection}>
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <div
          className={`drop-zone ${isDragging ? 'dragging' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <input
            type="file"
            id="file-input"
            onChange={handleFileSelect}
            accept="image/*,.pdf,.txt,.zip"
            style={{ display: 'none' }}
          />
          <label htmlFor="file-input" className="drop-label">
            <span className="drop-icon">📎</span>
            <p>Drag & drop a file here</p>
            <p className="or">or</p>
            <span className="browse-link">Click to browse</span>
          </label>
          
          <p className="file-hints">
            Max 10MB • Images, PDF, TXT, ZIP
          </p>
        </div>
      )}
      
      {error && <div className="upload-error">❌ {error}</div>}
    </div>
  )
}
