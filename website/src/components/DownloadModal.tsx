import React, { useEffect, useState } from 'react';
import { X, Download, ShieldCheck, Copy, Check, RefreshCw } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';

interface DownloadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTriggerToast: (msg: string) => void;
}

export const DownloadModal: React.FC<DownloadModalProps> = ({ isOpen, onClose, onTriggerToast }) => {
  const [copiedSha, setCopiedSha] = useState(false);

  useEffect(() => {
    if (isOpen) {
      // Trigger actual download link
      const link = document.createElement('a');
      link.href = CURRENT_RELEASE.downloadUrl;
      link.download = CURRENT_RELEASE.apkFileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleCopySha = () => {
    navigator.clipboard.writeText(CURRENT_RELEASE.sha256);
    setCopiedSha(true);
    onTriggerToast('SHA-256 copied to clipboard!');
    setTimeout(() => setCopiedSha(false), 2500);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="download-modal-title"
    >
      <div className="relative w-full max-w-lg rounded-3xl bg-[#0c1017] border border-white/15 p-6 sm:p-8 shadow-2xl text-slate-100 overflow-hidden">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 min-w-[44px] min-h-[44px] flex items-center justify-center"
          aria-label="Close download modal"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="text-center mb-6">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center mx-auto mb-4 border border-emerald-500/30">
            <Download className="w-8 h-8 animate-bounce" />
          </div>
          <h3 id="download-modal-title" className="text-2xl font-bold text-white tracking-tight">
            Your Download Has Started!
          </h3>
          <p className="text-xs sm:text-sm text-slate-300 mt-1">
            Downloading <strong>{CURRENT_RELEASE.apkFileName}</strong> ({CURRENT_RELEASE.fileSize})
          </p>
        </div>

        {/* Next Steps Reminder */}
        <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-3 mb-6 text-xs text-slate-300">
          <div className="font-semibold text-white flex items-center space-x-2">
            <span className="w-2 h-2 rounded-full bg-brand-400" />
            <span>Next steps to complete installation:</span>
          </div>

          <div className="flex items-start space-x-2.5">
            <span className="w-5 h-5 rounded-full bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
              1
            </span>
            <span>If browser warns <em>"File might be harmful"</em>, tap <strong>Download anyway</strong>.</span>
          </div>

          <div className="flex items-start space-x-2.5">
            <span className="w-5 h-5 rounded-full bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
              2
            </span>
            <span>Enable <strong>Install unknown apps</strong> in Settings if prompted.</span>
          </div>

          <div className="flex items-start space-x-2.5">
            <span className="w-5 h-5 rounded-full bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
              3
            </span>
            <span>Tap the downloaded file in your notification bar and press <strong>Install</strong>!</span>
          </div>
        </div>

        {/* Checksum quick verify */}
        <div className="p-3 rounded-xl bg-black/50 border border-white/5 flex items-center justify-between text-[11px] font-mono text-slate-400 mb-6">
          <div className="flex items-center space-x-2 overflow-hidden mr-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
            <span className="text-slate-300 truncate">SHA: {CURRENT_RELEASE.sha256}</span>
          </div>
          <button
            onClick={handleCopySha}
            className="px-2 py-1 rounded bg-white/10 hover:bg-white/20 text-slate-200 flex items-center space-x-1 flex-shrink-0"
          >
            {copiedSha ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copiedSha ? 'Copied' : 'Copy'}</span>
          </button>
        </div>

        {/* Fallback download button */}
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <a
            href={CURRENT_RELEASE.downloadUrl}
            download={CURRENT_RELEASE.apkFileName}
            className="w-full py-3 px-4 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-semibold text-xs sm:text-sm text-center flex items-center justify-center space-x-2 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Didn't start? Click here to retry</span>
          </a>

          <button
            onClick={onClose}
            className="w-full sm:w-auto py-3 px-5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white font-semibold text-xs sm:text-sm transition-colors border border-white/10"
          >
            Got it
          </button>
        </div>

      </div>
    </div>
  );
};
