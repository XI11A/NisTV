import React from 'react';
import { Download, ArrowUp } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';

interface FooterProps {
  onOpenDownloadModal: () => void;
}

export const Footer: React.FC<FooterProps> = ({ onOpenDownloadModal }) => {
  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <footer className="border-t border-white/10 bg-[#05070a] pt-16 pb-12 text-slate-400 text-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Main Footer Row */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10 pb-12 border-b border-white/5">
          
          {/* Brand Info */}
          <div className="md:col-span-2 space-y-4">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-brand-600 to-accent-cyan p-0.5">
                <img src="./images/logo.svg" alt="NisTV" className="w-full h-full object-contain rounded-[10px] bg-[#0c1017] p-1.5" />
              </div>
              <span className="font-extrabold text-lg text-white tracking-tight">NisTV Revamped</span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30">
                v{CURRENT_RELEASE.version}
              </span>
            </div>

            <p className="text-slate-300 max-w-md text-xs sm:text-sm leading-relaxed">
              A modern, high-performance Android IPTV player and TMDB media browser engineered with Kotlin, Jetpack Compose, ExoPlayer, and Room SQLite.
            </p>

            <div className="flex items-center space-x-3 pt-2">
              <button
                onClick={onOpenDownloadModal}
                className="px-4 py-2 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-semibold text-xs flex items-center space-x-2 transition-colors shadow-lg shadow-brand-600/30"
              >
                <Download className="w-3.5 h-3.5" />
                <span>Download APK ({CURRENT_RELEASE.fileSize})</span>
              </button>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="space-y-3">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider">Navigation</h4>
            <ul className="space-y-2">
              <li>
                <a href="#features" className="hover:text-white transition-colors">Features & Architecture</a>
              </li>
              <li>
                <a href="#preview" className="hover:text-white transition-colors">App Screens Preview</a>
              </li>
              <li>
                <a href="#download" className="hover:text-white transition-colors">Download Center</a>
              </li>
              <li>
                <a href="#install-guide" className="hover:text-white transition-colors">Install Guide (Mobile/TV)</a>
              </li>
              <li>
                <a href="#specs" className="hover:text-white transition-colors">System Requirements</a>
              </li>
              <li>
                <a href="#faq" className="hover:text-white transition-colors">FAQ & Sideloading</a>
              </li>
            </ul>
          </div>

          {/* Package & Verification Info */}
          <div className="space-y-3">
            <h4 className="font-bold text-white text-xs uppercase tracking-wider">Build Details</h4>
            <ul className="space-y-1.5 font-mono text-[11px] text-slate-400">
              <li>Package: <span className="text-slate-200">{CURRENT_RELEASE.packageName}</span></li>
              <li>Version: <span className="text-slate-200">v{CURRENT_RELEASE.version} ({CURRENT_RELEASE.versionCode})</span></li>
              <li>Size: <span className="text-slate-200">{CURRENT_RELEASE.fileSize}</span></li>
              <li>Target SDK: <span className="text-slate-200">{CURRENT_RELEASE.targetAndroid}</span></li>
              <li>License: <span className="text-slate-200">MIT Open Source</span></li>
            </ul>
          </div>

        </div>

        {/* Disclaimer & Bottom Bar */}
        <div className="pt-8 flex flex-col md:flex-row items-center justify-between gap-4 text-[11px] text-slate-500">
          <p className="max-w-2xl leading-relaxed">
            <strong>Disclaimer:</strong> NisTV Revamped does not host, store, or stream media directly. It is an open-source media client and search indexer for user-provided IPTV playlists and TMDB community metadata.
          </p>

          <button
            onClick={scrollToTop}
            className="flex items-center space-x-1.5 text-slate-400 hover:text-white transition-colors px-3 py-1.5 rounded-lg bg-white/5 border border-white/5 flex-shrink-0"
            aria-label="Back to top"
          >
            <span>Back to Top</span>
            <ArrowUp className="w-3.5 h-3.5" />
          </button>
        </div>

      </div>
    </footer>
  );
};
