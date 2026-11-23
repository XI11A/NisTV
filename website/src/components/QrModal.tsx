import React, { useState } from 'react';
import { X, Copy, Check, Smartphone } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';

interface QrModalProps {
  isOpen: boolean;
  onClose: () => void;
  onTriggerToast: (msg: string) => void;
}

export const QrModal: React.FC<QrModalProps> = ({ isOpen, onClose, onTriggerToast }) => {
  const [copiedLink, setCopiedLink] = useState(false);

  if (!isOpen) return null;

  const directUrl = window.location.origin + window.location.pathname.replace(/\/$/, '') + '/downloads/' + CURRENT_RELEASE.apkFileName;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(directUrl);
    setCopiedLink(true);
    onTriggerToast('Direct download URL copied!');
    setTimeout(() => setCopiedLink(false), 2500);
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="qr-modal-title"
    >
      <div className="relative w-full max-w-md rounded-3xl bg-[#0c1017] border border-white/15 p-6 sm:p-8 shadow-2xl text-slate-100 text-center">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/10 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 min-w-[44px] min-h-[44px] flex items-center justify-center"
          aria-label="Close QR modal"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="w-12 h-12 rounded-2xl bg-brand-500/20 text-brand-300 flex items-center justify-center mx-auto mb-3">
          <Smartphone className="w-6 h-6" />
        </div>

        <h3 id="qr-modal-title" className="text-xl font-bold text-white mb-1">
          Scan to Download on Mobile
        </h3>
        <p className="text-xs text-slate-300 mb-6">
          Open your phone's Camera or Google Lens and scan the code below:
        </p>

        {/* Large SVG QR Code */}
        <div className="w-56 h-56 p-4 rounded-2xl bg-white mx-auto shadow-2xl flex items-center justify-center">
          <svg viewBox="0 0 100 100" className="w-full h-full text-slate-950 fill-current">
            {/* Outer corner finder 1 */}
            <rect x="0" y="0" width="30" height="30" rx="3" fill="#000" />
            <rect x="4" y="4" width="22" height="22" rx="2" fill="#fff" />
            <rect x="9" y="9" width="12" height="12" rx="1" fill="#000" />
            {/* Outer corner finder 2 */}
            <rect x="70" y="0" width="30" height="30" rx="3" fill="#000" />
            <rect x="74" y="4" width="22" height="22" rx="2" fill="#fff" />
            <rect x="79" y="9" width="12" height="12" rx="1" fill="#000" />
            {/* Outer corner finder 3 */}
            <rect x="0" y="70" width="30" height="30" rx="3" fill="#000" />
            <rect x="4" y="74" width="22" height="22" rx="2" fill="#fff" />
            <rect x="9" y="79" width="12" height="12" rx="1" fill="#000" />
            {/* Data matrix points */}
            <rect x="36" y="6" width="6" height="6" fill="#000" />
            <rect x="48" y="10" width="6" height="6" fill="#000" />
            <rect x="58" y="6" width="6" height="6" fill="#000" />
            <rect x="38" y="22" width="6" height="6" fill="#000" />
            <rect x="52" y="26" width="6" height="6" fill="#000" />
            <rect x="6" y="38" width="6" height="6" fill="#000" />
            <rect x="18" y="44" width="6" height="6" fill="#000" />
            <rect x="28" y="36" width="6" height="6" fill="#000" />
            <rect x="38" y="42" width="6" height="6" fill="#2b5ee8" />
            <rect x="48" y="38" width="6" height="6" fill="#2b5ee8" />
            <rect x="56" y="46" width="6" height="6" fill="#2b5ee8" />
            <rect x="44" y="52" width="8" height="8" rx="2" fill="#2b5ee8" />
            <rect x="72" y="38" width="6" height="6" fill="#000" />
            <rect x="86" y="44" width="6" height="6" fill="#000" />
            <rect x="38" y="62" width="6" height="6" fill="#000" />
            <rect x="50" y="68" width="6" height="6" fill="#000" />
            <rect x="60" y="74" width="6" height="6" fill="#000" />
            <rect x="76" y="66" width="6" height="6" fill="#000" />
            <rect x="88" y="76" width="6" height="6" fill="#000" />
            <rect x="72" y="86" width="6" height="6" fill="#000" />
          </svg>
        </div>

        <div className="mt-6 flex items-center justify-between p-3 rounded-xl bg-white/5 border border-white/10 text-xs font-mono">
          <span className="text-slate-300 truncate max-w-[240px] text-left">{directUrl}</span>
          <button
            onClick={handleCopyLink}
            className="px-2.5 py-1 rounded bg-brand-600 hover:bg-brand-500 text-white font-sans font-medium flex items-center space-x-1 flex-shrink-0 transition-colors"
          >
            {copiedLink ? <Check className="w-3.5 h-3.5 text-white" /> : <Copy className="w-3.5 h-3.5 text-white" />}
            <span>{copiedLink ? 'Copied' : 'Copy'}</span>
          </button>
        </div>

      </div>
    </div>
  );
};
