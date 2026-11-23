import React, { useState } from 'react';
import { Download, QrCode, ShieldCheck, Check, Copy, Sparkles, Play, Wifi, Cpu } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';
import confetti from 'canvas-confetti';

interface HeroProps {
  onOpenDownloadModal: () => void;
  onOpenQrModal: () => void;
  onTriggerToast: (msg: string) => void;
}

export const Hero: React.FC<HeroProps> = ({ onOpenDownloadModal, onOpenQrModal, onTriggerToast }) => {
  const [copiedSha, setCopiedSha] = useState(false);

  const handleCopySha = () => {
    navigator.clipboard.writeText(CURRENT_RELEASE.sha256);
    setCopiedSha(true);
    onTriggerToast('SHA-256 Checksum copied to clipboard!');
    setTimeout(() => setCopiedSha(false), 2500);
  };

  const handleDownloadClick = () => {
    // Trigger confetti celebration
    try {
      confetti({
        particleCount: 80,
        spread: 70,
        origin: { y: 0.6 },
        colors: ['#437df4', '#00f2fe', '#7928ca', '#10b981'],
      });
    } catch {
      // ignore in environments without canvas
    }
    onOpenDownloadModal();
  };

  return (
    <section className="relative pt-32 pb-20 md:pt-40 md:pb-28 overflow-hidden" aria-labelledby="hero-heading">
      {/* Dynamic Background Glows */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-[600px] pointer-events-none opacity-40">
        <div className="absolute top-10 left-1/4 w-96 h-96 bg-brand-600/30 rounded-full blur-[120px] animate-pulse-subtle" />
        <div className="absolute top-20 right-1/4 w-96 h-96 bg-accent-purple/25 rounded-full blur-[130px]" />
        <div className="absolute top-48 left-1/2 -translate-x-1/2 w-[600px] h-64 bg-accent-cyan/15 rounded-full blur-[140px]" />
      </div>

      <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          
          {/* Left Column: Headline & Action CTAs */}
          <div className="lg:col-span-7 text-center lg:text-left">
            {/* Version & Release Tag */}
            <div className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-full bg-white/5 border border-white/10 backdrop-blur-md mb-6 hover:border-brand-500/40 transition-colors">
              <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
              <span className="flex h-2 w-2 rounded-full bg-emerald-400 -ml-4" />
              <span className="text-xs font-semibold text-brand-300">
                NisTV Revamped v{CURRENT_RELEASE.version} Production Build
              </span>
              <span className="text-slate-500">•</span>
              <span className="text-xs text-slate-300">Android 7.0 to 15 Ready</span>
            </div>

            {/* Main Headline */}
            <h1 id="hero-heading" className="text-4xl sm:text-5xl lg:text-6xl font-extrabold tracking-tight leading-[1.1] mb-6">
              The Modern <br />
              <span className="text-gradient">Live IPTV & Media</span> <br />
              Hub for Android
            </h1>

            {/* Subtitle */}
            <p className="text-lg sm:text-xl text-slate-300 max-w-2xl mx-auto lg:mx-0 font-normal leading-relaxed mb-8">
              Stream <strong className="text-white font-semibold">1,000+ regional & global Live TV channels</strong> with zero-lag failover cascades, explore TMDB movies & series, and enjoy resilient <strong className="text-white font-semibold">chunked offline downloads</strong> — completely ad-free and tracker-free.
            </p>

            {/* Primary Actions */}
            <div className="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 mb-8">
              <button
                onClick={handleDownloadClick}
                className="w-full sm:w-auto px-8 py-4 rounded-2xl bg-gradient-to-r from-brand-600 via-brand-500 to-accent-teal text-white font-bold text-lg shadow-xl shadow-brand-600/35 hover:shadow-brand-500/50 hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center space-x-3 group min-h-[56px] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-400"
                aria-label={`Download NisTV APK v${CURRENT_RELEASE.version}`}
              >
                <Download className="w-6 h-6 transition-transform group-hover:-translate-y-1" />
                <div className="text-left">
                  <div className="leading-tight">Download APK Package</div>
                  <div className="text-xs font-mono font-normal text-white/80">
                    v{CURRENT_RELEASE.version} • {CURRENT_RELEASE.fileSize} • Universal
                  </div>
                </div>
              </button>

              <button
                onClick={onOpenQrModal}
                className="w-full sm:w-auto px-6 py-4 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/15 text-slate-200 hover:text-white font-semibold text-base transition-all flex items-center justify-center space-x-2.5 min-h-[56px] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
                aria-label="Scan QR Code to download directly to phone"
              >
                <QrCode className="w-5 h-5 text-brand-400" />
                <span>Scan for Phone</span>
              </button>
            </div>

            {/* Checksum & Trust verification pill */}
            <div className="p-3.5 rounded-xl bg-surface-card/80 border border-white/10 backdrop-blur-md max-w-xl mx-auto lg:mx-0 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
              <div className="flex items-center space-x-2 text-slate-300 overflow-hidden w-full sm:w-auto">
                <ShieldCheck className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                <span className="text-slate-400 font-medium">SHA-256:</span>
                <span className="font-mono text-slate-300 truncate max-w-[200px] sm:max-w-[240px]" title={CURRENT_RELEASE.sha256}>
                  {CURRENT_RELEASE.sha256.slice(0, 10)}...{CURRENT_RELEASE.sha256.slice(-10)}
                </span>
              </div>
              <button
                onClick={handleCopySha}
                className="px-2.5 py-1.5 rounded-lg bg-white/5 hover:bg-white/15 text-slate-300 hover:text-white border border-white/10 transition-colors flex items-center space-x-1.5 flex-shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
                aria-label="Copy SHA-256 checksum to clipboard"
              >
                {copiedSha ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="text-emerald-400 font-medium">Copied!</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5 text-slate-400" />
                    <span>Copy Hash</span>
                  </>
                )}
              </button>
            </div>

            {/* Quick feature tags */}
            <div className="mt-8 flex flex-wrap items-center justify-center lg:justify-start gap-y-2 gap-x-6 text-xs text-slate-400">
              <div className="flex items-center space-x-1.5">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                <span>No Ads & No In-App Purchases</span>
              </div>
              <div className="flex items-center space-x-1.5">
                <Cpu className="w-4 h-4 text-brand-400" />
                <span>On-Device Privacy Vector Engine</span>
              </div>
              <div className="flex items-center space-x-1.5">
                <Wifi className="w-4 h-4 text-accent-cyan" />
                <span>Resilient Multi-Thread Downloads</span>
              </div>
            </div>
          </div>

          {/* Right Column: High-Fidelity Interactive App Mockup */}
          <div className="lg:col-span-5 relative">
            <div className="relative mx-auto max-w-[340px] sm:max-w-[380px] rounded-[42px] p-3 bg-gradient-to-b from-slate-700 via-slate-800 to-slate-900 shadow-2xl shadow-brand-500/20 border border-slate-600/40">
              
              {/* Phone Speaker Notch */}
              <div className="absolute top-5 left-1/2 -translate-x-1/2 w-28 h-4 bg-slate-950 rounded-full z-20 flex items-center justify-center">
                <div className="w-3 h-3 rounded-full bg-slate-900/90 mr-2" />
                <div className="w-8 h-1 rounded-full bg-slate-800" />
              </div>

              {/* Screen Content Container */}
              <div className="relative rounded-[32px] overflow-hidden bg-[#0a0d14] border border-white/5 pt-8 pb-4 px-3 text-slate-100 select-none shadow-inner">
                
                {/* Simulated Top App Bar */}
                <div className="flex items-center justify-between px-2 pb-3 mb-2 border-b border-white/5">
                  <div className="flex items-center space-x-2">
                    <img src="./images/logo.svg" alt="NisTV" className="w-6 h-6 object-contain" />
                    <span className="font-bold text-sm tracking-tight text-white">NisTV</span>
                    <span className="text-[9px] px-1.5 py-0.2 rounded bg-brand-500/20 text-brand-300 font-semibold">
                      LIVE
                    </span>
                  </div>
                  <div className="flex items-center space-x-2 text-[10px] text-slate-400 font-mono">
                    <span className="px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-semibold">
                      1080p60
                    </span>
                    <span>19:30</span>
                  </div>
                </div>

                {/* Simulated ExoPlayer Active Stream Box */}
                <div className="relative rounded-2xl overflow-hidden aspect-video bg-gradient-to-tr from-slate-900 via-brand-950 to-slate-900 border border-white/10 mb-3 group">
                  <img
                    src="./images/splash.png"
                    alt="Player Screen Preview"
                    className="w-full h-full object-cover opacity-80"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent flex flex-col justify-between p-2.5">
                    <div className="flex items-center justify-between">
                      <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded bg-red-600 text-white font-bold text-[10px] tracking-wider uppercase animate-pulse">
                        <span className="w-1.5 h-1.5 rounded-full bg-white" />
                        <span>LIVE</span>
                      </span>
                      <span className="text-[10px] bg-black/60 backdrop-blur-sm px-1.5 py-0.5 rounded text-slate-300">
                        Auto-Failover: Active
                      </span>
                    </div>

                    <div>
                      <p className="text-xs font-semibold text-white drop-shadow">Star Sports 1 HD • Hindi</p>
                      <div className="flex items-center space-x-2 mt-1">
                        <div className="h-1 flex-1 bg-white/20 rounded-full overflow-hidden">
                          <div className="h-full bg-brand-500 w-3/4 rounded-full" />
                        </div>
                        <span className="text-[9px] text-slate-300 font-mono">LIVE EPG</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Language Quick Pills */}
                <div className="flex space-x-1.5 overflow-x-auto pb-2 scrollbar-none text-[11px] font-medium">
                  <span className="px-2.5 py-1 rounded-lg bg-brand-600 text-white flex-shrink-0">
                    Hindi (140)
                  </span>
                  <span className="px-2.5 py-1 rounded-lg bg-white/5 text-slate-300 flex-shrink-0">
                    Tamil (95)
                  </span>
                  <span className="px-2.5 py-1 rounded-lg bg-white/5 text-slate-300 flex-shrink-0">
                    Telugu (110)
                  </span>
                  <span className="px-2.5 py-1 rounded-lg bg-white/5 text-slate-300 flex-shrink-0">
                    Sports
                  </span>
                </div>

                {/* Simulated Channel / Content List */}
                <div className="space-y-2 mt-1">
                  <div className="p-2 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between">
                    <div className="flex items-center space-x-2.5">
                      <div className="w-8 h-8 rounded-lg bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-xs">
                        TV1
                      </div>
                      <div>
                        <div className="text-xs font-medium text-white">Colors Cinema HD</div>
                        <div className="text-[10px] text-slate-400">Prime Movie Premiere • 2h 15m</div>
                      </div>
                    </div>
                    <Play className="w-3.5 h-3.5 text-brand-400" />
                  </div>

                  <div className="p-2 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between">
                    <div className="flex items-center space-x-2.5">
                      <div className="w-8 h-8 rounded-lg bg-accent-purple/20 text-accent-purple flex items-center justify-center font-bold text-xs">
                        VOD
                      </div>
                      <div>
                        <div className="text-xs font-medium text-white">Interstellar (2014)</div>
                        <div className="text-[10px] text-emerald-400">Downloaded • 2.4 GB • Offline</div>
                      </div>
                    </div>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                  </div>
                </div>

                {/* Simulated Bottom Navigation */}
                <div className="mt-4 pt-2 border-t border-white/5 grid grid-cols-4 text-center text-[10px] text-slate-400">
                  <div className="text-brand-400 font-semibold flex flex-col items-center">
                    <span className="w-1 h-1 rounded-full bg-brand-400 mb-0.5" />
                    Live TV
                  </div>
                  <div>Movies</div>
                  <div>Downloads</div>
                  <div>Settings</div>
                </div>

              </div>

              {/* Floating Badge 1: Resilient Downloader */}
              <div className="absolute -bottom-4 -left-6 bg-slate-900/95 border border-emerald-500/30 p-2.5 rounded-2xl shadow-xl backdrop-blur-md flex items-center space-x-3 text-xs hidden sm:flex">
                <div className="w-8 h-8 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center">
                  <Download className="w-4 h-4" />
                </div>
                <div>
                  <div className="font-semibold text-white">Offline Chunked Downloader</div>
                  <div className="text-[10px] text-emerald-300">Auto-recovery on network drops</div>
                </div>
              </div>

              {/* Floating Badge 2: On-Device Vector AI */}
              <div className="absolute -top-3 -right-6 bg-slate-900/95 border border-brand-500/30 p-2.5 rounded-2xl shadow-xl backdrop-blur-md flex items-center space-x-2.5 text-xs hidden sm:flex">
                <div className="w-7 h-7 rounded-xl bg-brand-500/20 text-brand-300 flex items-center justify-center">
                  <Sparkles className="w-3.5 h-3.5" />
                </div>
                <div>
                  <div className="font-semibold text-white">100% On-Device Privacy</div>
                  <div className="text-[10px] text-slate-400">Zero cloud logs or telemetry</div>
                </div>
              </div>

            </div>
          </div>

        </div>
      </div>
    </section>
  );
};
