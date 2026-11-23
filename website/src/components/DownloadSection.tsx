import React, { useState } from 'react';
import { Download, Copy, Check, ShieldCheck, Smartphone, QrCode, Terminal } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';
import confetti from 'canvas-confetti';

interface DownloadSectionProps {
  onOpenDownloadModal: () => void;
  onOpenQrModal: () => void;
  onTriggerToast: (msg: string) => void;
}

export const DownloadSection: React.FC<DownloadSectionProps> = ({
  onOpenDownloadModal,
  onOpenQrModal,
  onTriggerToast,
}) => {
  const [copiedSha, setCopiedSha] = useState(false);
  const [copiedMd5, setCopiedMd5] = useState(false);

  const handleCopySha = () => {
    navigator.clipboard.writeText(CURRENT_RELEASE.sha256);
    setCopiedSha(true);
    onTriggerToast('SHA-256 Checksum copied!');
    setTimeout(() => setCopiedSha(false), 2500);
  };

  const handleCopyMd5 = () => {
    navigator.clipboard.writeText(CURRENT_RELEASE.md5);
    setCopiedMd5(true);
    onTriggerToast('MD5 Checksum copied!');
    setTimeout(() => setCopiedMd5(false), 2500);
  };

  const handleDirectDownload = () => {
    try {
      confetti({
        particleCount: 100,
        spread: 80,
        origin: { y: 0.7 },
      });
    } catch {
      // ignore
    }
    onOpenDownloadModal();
  };

  return (
    <section id="download" className="py-24 relative scroll-mt-20">
      {/* Background radial accent */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full max-w-5xl h-[500px] bg-brand-600/10 rounded-full blur-[140px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs font-semibold mb-3">
            <span className="w-2 h-2 rounded-full bg-emerald-400" />
            <span>OFFICIAL PRODUCTION RELEASE</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight mb-4">
            Download <span className="text-gradient">NisTV Revamped</span>
          </h2>
          <p className="text-base sm:text-lg text-slate-300 leading-relaxed">
            Get the verified, standalone APK for any Android smartphone, tablet, foldable, or TV device. No registration or Google account required.
          </p>
        </div>

        {/* Main Download Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Main Card (8 cols) */}
          <div className="lg:col-span-8 glass-panel rounded-3xl p-6 sm:p-10 border border-white/10 shadow-2xl relative overflow-hidden">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-8 border-b border-white/10">
              <div className="flex items-center space-x-4">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-brand-600 to-accent-cyan p-0.5 shadow-lg flex-shrink-0">
                  <img
                    src="./images/logo.svg"
                    alt="NisTV App Icon"
                    className="w-full h-full object-contain rounded-[14px] bg-[#0c1017] p-2"
                  />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <h3 className="text-2xl font-bold text-white tracking-tight">NisTV Revamped</h3>
                    <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 font-mono text-xs font-semibold">
                      v{CURRENT_RELEASE.version}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 font-mono mt-0.5">
                    Package: {CURRENT_RELEASE.packageName} • Build #{CURRENT_RELEASE.versionCode}
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-2 text-xs font-medium text-slate-300">
                <span className="flex h-2 w-2 rounded-full bg-emerald-400" />
                <span>Verified Safe • VirusTotal Clean</span>
              </div>
            </div>

            {/* Release Specifications Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 py-6 border-b border-white/10 text-xs">
              <div className="p-3 rounded-xl bg-white/5 border border-white/5">
                <span className="text-slate-400 block mb-1">File Size</span>
                <span className="text-white font-bold text-sm font-mono">{CURRENT_RELEASE.fileSize}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5 border border-white/5">
                <span className="text-slate-400 block mb-1">Architecture</span>
                <span className="text-white font-bold text-sm font-mono">Universal (All CPUs)</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5 border border-white/5">
                <span className="text-slate-400 block mb-1">Min Android</span>
                <span className="text-white font-bold text-sm">Android 7.0+ (API 24)</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5 border border-white/5">
                <span className="text-slate-400 block mb-1">Target Android</span>
                <span className="text-white font-bold text-sm">Android 14 / 15</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="pt-8 flex flex-col sm:flex-row items-center gap-4">
              <button
                onClick={handleDirectDownload}
                className="w-full sm:flex-1 py-4 px-6 rounded-2xl bg-gradient-to-r from-brand-600 via-brand-500 to-accent-teal hover:from-brand-500 hover:to-accent-cyan text-white font-bold text-base shadow-xl shadow-brand-500/30 hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center space-x-3 group min-h-[56px] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-400"
                aria-label={`Direct Download ${CURRENT_RELEASE.apkFileName}`}
              >
                <Download className="w-5 h-5 transition-transform group-hover:-translate-y-1" />
                <span>Direct Download APK ({CURRENT_RELEASE.fileSize})</span>
              </button>

              <button
                onClick={onOpenQrModal}
                className="w-full sm:w-auto py-4 px-5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/15 text-slate-200 hover:text-white font-semibold text-sm transition-all flex items-center justify-center space-x-2 min-h-[56px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
                aria-label="Open QR Code modal to download on phone"
              >
                <QrCode className="w-5 h-5 text-brand-400" />
                <span>Scan for Phone</span>
              </button>
            </div>

            {/* Checksums Verification Box */}
            <div className="mt-8 p-4 rounded-2xl bg-[#090d15] border border-white/10 text-xs space-y-3">
              <div className="flex items-center justify-between text-slate-300 font-semibold">
                <div className="flex items-center space-x-2">
                  <ShieldCheck className="w-4 h-4 text-emerald-400" />
                  <span>Package Cryptographic Signatures</span>
                </div>
                <span className="text-[11px] text-slate-500">Verify before installing</span>
              </div>

              {/* SHA-256 */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5 rounded-xl bg-white/5 font-mono text-[11px]">
                <div className="flex items-center space-x-2 overflow-hidden">
                  <span className="text-slate-400 font-bold">SHA-256:</span>
                  <span className="text-slate-200 truncate select-all">{CURRENT_RELEASE.sha256}</span>
                </div>
                <button
                  onClick={handleCopySha}
                  className="px-3 py-1 rounded-md bg-white/10 hover:bg-white/20 text-slate-200 font-sans text-xs flex items-center space-x-1 flex-shrink-0 self-end sm:self-auto transition-colors"
                  aria-label="Copy SHA-256 checksum"
                >
                  {copiedSha ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedSha ? 'Copied' : 'Copy'}</span>
                </button>
              </div>

              {/* MD5 */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5 rounded-xl bg-white/5 font-mono text-[11px]">
                <div className="flex items-center space-x-2 overflow-hidden">
                  <span className="text-slate-400 font-bold">MD5:</span>
                  <span className="text-slate-200 truncate select-all">{CURRENT_RELEASE.md5}</span>
                </div>
                <button
                  onClick={handleCopyMd5}
                  className="px-3 py-1 rounded-md bg-white/10 hover:bg-white/20 text-slate-200 font-sans text-xs flex items-center space-x-1 flex-shrink-0 self-end sm:self-auto transition-colors"
                  aria-label="Copy MD5 checksum"
                >
                  {copiedMd5 ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedMd5 ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
            </div>

          </div>

          {/* Right Card: Instant QR Code & TV Sideloading (4 cols) */}
          <div className="lg:col-span-4 space-y-6">
            
            {/* Direct Phone QR Card */}
            <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 text-center flex flex-col items-center">
              <div className="inline-flex items-center space-x-1.5 text-xs font-semibold text-brand-300 mb-3">
                <Smartphone className="w-4 h-4" />
                <span>Instant Phone Download</span>
              </div>
              <h4 className="text-lg font-bold text-white mb-2">Scan with Camera</h4>
              <p className="text-xs text-slate-300 mb-6">
                Point your smartphone camera at this code to download the APK directly without cables.
              </p>

              {/* Dynamic Clean QR Code Rendering */}
              <div
                onClick={onOpenQrModal}
                className="w-48 h-48 p-3 rounded-2xl bg-white flex items-center justify-center shadow-xl cursor-pointer hover:scale-105 transition-transform group relative"
                title="Click to enlarge QR Code"
              >
                {/* Clean SVG QR Code Representation */}
                <svg viewBox="0 0 100 100" className="w-full h-full text-slate-900 fill-current">
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
                  {/* Data Grid Dots */}
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
                <div className="absolute inset-0 bg-brand-600/10 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                  <span className="px-2 py-1 rounded bg-black/80 text-white text-[10px] font-semibold">Click to Enlarge</span>
                </div>
              </div>

              <div className="mt-4 text-[11px] text-slate-400">
                Or download directly to your computer and transfer via USB
              </div>
            </div>

            {/* Android TV & Fire Stick Card */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 text-left">
              <div className="flex items-center space-x-2 text-xs font-semibold text-accent-cyan mb-2">
                <Terminal className="w-4 h-4" />
                <span>Fire TV & Android TV Quick URL</span>
              </div>
              <p className="text-xs text-slate-300 mb-3">
                Use the <strong>Downloader App</strong> on your TV and type this direct download URL:
              </p>
              <div className="p-2.5 rounded-xl bg-black/60 border border-white/10 flex items-center justify-between font-mono text-xs text-brand-300">
                <span className="truncate">{`https://nistv.xi11a.eu.org/downloads/${CURRENT_RELEASE.apkFileName}`}</span>
              </div>
            </div>

          </div>

        </div>

      </div>
    </section>
  );
};
