import React, { useState, useEffect } from 'react';
import { Download, Menu, X, Tv, ExternalLink } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';

interface NavbarProps {
  onOpenDownloadModal: () => void;
  onOpenQrModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onOpenDownloadModal, onOpenQrModal }) => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const navLinks = [
    { name: 'Features', href: '#features' },
    { name: 'Preview', href: '#preview' },
    { name: 'Download', href: '#download' },
    { name: 'Install Guide', href: '#install-guide' },
    { name: 'Specs', href: '#specs' },
    { name: 'FAQ', href: '#faq' },
  ];

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        isScrolled
          ? 'bg-[#07090e]/90 backdrop-blur-md border-b border-white/10 shadow-xl py-3'
          : 'bg-transparent py-5'
      }`}
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between">
          {/* Logo & Brand */}
          <a
            href="#"
            className="flex items-center space-x-3 group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 rounded-lg p-1"
            aria-label="NisTV Revamped Home"
          >
            <div className="relative w-10 h-10 rounded-xl overflow-hidden bg-gradient-to-tr from-brand-600 via-accent-purple to-accent-cyan p-0.5 shadow-lg shadow-brand-500/20 group-hover:shadow-brand-500/40 transition-shadow">
              <img
                src="./images/logo.svg"
                alt="NisTV Revamped Logo"
                className="w-full h-full object-contain rounded-[10px] bg-[#0c1017] p-1.5"
                onError={(e) => {
                  // Fallback to icon if svg has any load issue
                  e.currentTarget.style.display = 'none';
                  e.currentTarget.parentElement?.querySelector('.fallback-icon')?.removeAttribute('style');
                }}
              />
              <Tv className="fallback-icon hidden w-full h-full p-2 text-white" />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-extrabold text-xl tracking-tight text-white group-hover:text-brand-300 transition-colors">
                  NisTV
                </span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30">
                  REVAMPED
                </span>
              </div>
              <span className="block text-[10px] tracking-wider uppercase text-slate-400 font-medium">
                Live TV & Media Hub
              </span>
            </div>
          </a>

          {/* Desktop Nav Links */}
          <nav className="hidden md:flex items-center space-x-1 lg:space-x-2" aria-label="Main navigation">
            {navLinks.map((link) => (
              <a
                key={link.name}
                href={link.href}
                className="px-3 py-2 rounded-lg text-sm font-medium text-slate-300 hover:text-white hover:bg-white/5 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Action CTAs */}
          <div className="hidden sm:flex items-center space-x-3">
            <button
              onClick={onOpenQrModal}
              className="px-3 py-2 text-xs font-medium text-slate-300 hover:text-white rounded-lg hover:bg-white/5 border border-white/10 transition-colors flex items-center space-x-1.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
              title="Scan QR to download on phone"
              aria-label="Scan QR Code to download on mobile"
            >
              <ExternalLink className="w-3.5 h-3.5 text-brand-400" />
              <span>Scan QR</span>
            </button>

            <button
              onClick={onOpenDownloadModal}
              className="relative group overflow-hidden px-4 py-2.5 rounded-xl bg-gradient-to-r from-brand-600 to-brand-500 hover:from-brand-500 hover:to-accent-teal text-white font-semibold text-sm shadow-lg shadow-brand-600/30 hover:shadow-brand-500/50 transition-all flex items-center space-x-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:ring-brand-400 active:scale-95"
              aria-label={`Download NisTV APK version ${CURRENT_RELEASE.version}`}
            >
              <Download className="w-4 h-4 transition-transform group-hover:-translate-y-0.5" />
              <span>Download APK</span>
              <span className="text-xs bg-black/25 px-1.5 py-0.5 rounded-md font-mono text-brand-100">
                v{CURRENT_RELEASE.version}
              </span>
            </button>
          </div>

          {/* Mobile Menu Hamburger */}
          <div className="flex md:hidden items-center space-x-2">
            <button
              onClick={onOpenDownloadModal}
              className="px-3 py-2 rounded-lg bg-brand-600 text-white font-medium text-xs flex items-center space-x-1.5 shadow-md shadow-brand-600/30"
              aria-label="Download APK"
            >
              <Download className="w-3.5 h-3.5" />
              <span>APK</span>
            </button>

            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2.5 rounded-lg text-slate-300 hover:text-white hover:bg-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 min-w-[44px] min-h-[44px] flex items-center justify-center"
              aria-label={mobileMenuOpen ? 'Close menu' : 'Open menu'}
              aria-expanded={mobileMenuOpen}
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Nav Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-[#0c1017]/95 border-b border-white/10 backdrop-blur-xl px-4 pt-3 pb-6 animate-in slide-in-from-top-4 duration-200">
          <nav className="flex flex-col space-y-1">
            {navLinks.map((link) => (
              <a
                key={link.name}
                href={link.href}
                onClick={() => setMobileMenuOpen(false)}
                className="px-4 py-3 rounded-lg text-base font-medium text-slate-200 hover:text-white hover:bg-white/5 transition-colors flex items-center justify-between"
              >
                <span>{link.name}</span>
              </a>
            ))}
            <div className="pt-4 border-t border-white/10 flex flex-col space-y-2">
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  onOpenQrModal();
                }}
                className="w-full py-3 px-4 rounded-xl border border-white/15 bg-white/5 text-slate-200 text-center font-medium text-sm flex items-center justify-center space-x-2"
              >
                <ExternalLink className="w-4 h-4 text-brand-400" />
                <span>Show Mobile QR Code</span>
              </button>

              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  onOpenDownloadModal();
                }}
                className="w-full py-3 px-4 rounded-xl bg-gradient-to-r from-brand-600 to-accent-teal text-white text-center font-semibold text-base shadow-lg shadow-brand-500/30 flex items-center justify-center space-x-2"
              >
                <Download className="w-5 h-5" />
                <span>Download APK ({CURRENT_RELEASE.fileSize})</span>
              </button>
            </div>
          </nav>
        </div>
      )}
    </header>
  );
};
