import React, { useState } from 'react';
import { Tv, Film, Download, Play, Globe, Check, HardDrive, PlayCircle, Cast } from 'lucide-react';

export const AppScreensPreview: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'livetv' | 'screencast' | 'vod' | 'downloads' | 'player' | 'languages'>('livetv');

  const tabs = [
    { id: 'livetv', label: 'Live TV & EPG', icon: Tv },
    { id: 'screencast', label: 'Screencast & TV', icon: Cast },
    { id: 'vod', label: 'Movies & Series', icon: Film },
    { id: 'downloads', label: 'Offline Downloader', icon: Download },
    { id: 'player', label: 'ExoPlayer & PiP', icon: Play },
    { id: 'languages', label: 'Regional Languages', icon: Globe },
  ] as const;

  return (
    <section id="preview" className="py-20 relative scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-12">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-accent-cyan/10 border border-accent-cyan/20 text-accent-cyan text-xs font-semibold mb-3">
            <span>INTERACTIVE APP EXPLORER</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
            Take a Look Inside <span className="text-gradient">NisTV Revamped</span>
          </h2>
          <p className="text-base text-slate-300">
            A native Android experience designed for maximum fluidity, dark mode eye comfort, and high-DPI crystal-clear displays.
          </p>
        </div>

        {/* Tab Controls */}
        <div className="flex items-center justify-start sm:justify-center space-x-2 overflow-x-auto pb-4 mb-8 scrollbar-none" role="tablist">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                role="tab"
                aria-selected={isActive}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2.5 rounded-xl font-medium text-xs sm:text-sm flex items-center space-x-2 flex-shrink-0 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
                  isActive
                    ? 'bg-gradient-to-r from-brand-600 to-brand-500 text-white shadow-lg shadow-brand-600/30 font-semibold'
                    : 'bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white border border-white/5'
                }`}
              >
                <Icon className={`w-4 h-4 ${isActive ? 'text-white' : 'text-slate-400'}`} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Tab Content Display */}
        <div className="glass-panel rounded-3xl p-6 sm:p-10 border border-white/10 relative overflow-hidden">
          
          {/* TAB 1: Live TV & EPG */}
          {activeTab === 'livetv' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-brand-500/20 text-brand-300 border border-brand-500/30">
                  Feature Spotlight: Live TV
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  Ultra-Fast IPTV Channel Surfing with Real-time EPG
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Browse over 1,000 live streams categorized by language and genre (Sports, News, Entertainment, Kids, Movies). The Electronic Program Guide shows current schedules and upcoming programs so you never miss a moment.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Multi-mirror failover cascades within 200ms</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Quick channel favorites and custom pinning</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Low-latency HLS/DASH adaptive bitrate switching</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                {/* Simulated EPG Grid */}
                <div className="flex items-center justify-between mb-4 border-b border-white/10 pb-3">
                  <div className="flex items-center space-x-2">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-pulse" />
                    <span className="text-xs font-bold uppercase tracking-wider text-white">Live Broadcast</span>
                  </div>
                  <span className="text-xs font-mono text-slate-400">Stream Status: Optimal (9.4 Mbps)</span>
                </div>

                <div className="space-y-2.5">
                  <div className="p-3 rounded-xl bg-brand-600/20 border border-brand-500/40 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-lg bg-brand-600 text-white font-bold flex items-center justify-center text-xs shadow">
                        SPT
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-white flex items-center space-x-2">
                          <span>Star Sports 1 HD</span>
                          <span className="text-[10px] bg-red-500/30 text-red-300 px-1.5 py-0.2 rounded font-mono">LIVE</span>
                        </div>
                        <div className="text-xs text-slate-300">ICC Champions Trophy: Matchday Live • 1080p</div>
                      </div>
                    </div>
                    <PlayCircle className="w-5 h-5 text-brand-400" />
                  </div>

                  <div className="p-3 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between opacity-80">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-lg bg-purple-600/30 text-purple-300 font-bold flex items-center justify-center text-xs">
                        NWS
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-white">NDTV 24x7 HD</div>
                        <div className="text-xs text-slate-400">Prime Time News Bulletin • Hindi/English</div>
                      </div>
                    </div>
                    <span className="text-[11px] text-slate-500 font-mono">19:30</span>
                  </div>

                  <div className="p-3 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between opacity-70">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-lg bg-emerald-600/30 text-emerald-300 font-bold flex items-center justify-center text-xs">
                        MOV
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-white">Sony MAX HD</div>
                        <div className="text-xs text-slate-400">Blockbuster Cinema Marathon • Hindi</div>
                      </div>
                    </div>
                    <span className="text-[11px] text-slate-500 font-mono">20:00</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB: Screencast & TV */}
          {activeTab === 'screencast' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-accent-cyan/20 text-accent-cyan border border-accent-cyan/30">
                  Feature Spotlight: Screencast
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  Beam Live TV & Movies Directly to Your Smart TV
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Turn your phone or tablet into the ultimate remote control. Cast full screen via Google Cast, Miracast, Samsung Smart View, or send stream URLs directly to DLNA receivers and media player apps.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Works with Chromecast, Android TV, Fire TV, and Apple TV (via DLNA)</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Dual-screen presentation: view controls on mobile while TV plays</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Direct stream URL handoff to VLC and Web Video Caster</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                <div className="flex items-center justify-between mb-4 border-b border-white/10 pb-3">
                  <div className="flex items-center space-x-2">
                    <Cast className="w-4 h-4 text-accent-cyan" />
                    <span className="text-xs font-bold text-white">Screencast & Remote Control</span>
                  </div>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300">
                    Living Room TV: Connected
                  </span>
                </div>

                <div className="space-y-3">
                  <div className="p-3.5 rounded-xl bg-white/5 border border-accent-cyan/40 flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-xl bg-accent-cyan/20 text-accent-cyan flex items-center justify-center font-bold">
                        <Cast className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-white">Living Room Android TV (4K)</div>
                        <div className="text-[10px] text-emerald-400">Active presentation streaming • 60 FPS</div>
                      </div>
                    </div>
                    <span className="text-xs font-mono text-slate-300">Mirrored</span>
                  </div>

                  <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between opacity-80">
                    <div className="flex items-center space-x-3">
                      <div className="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-300 flex items-center justify-center font-bold">
                        <Tv className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="text-xs font-semibold text-white">Fire TV Stick 4K Max</div>
                        <div className="text-[10px] text-slate-400">Available on local Wi-Fi (5 GHz)</div>
                      </div>
                    </div>
                    <button className="px-2.5 py-1 rounded-lg bg-white/10 text-[11px] text-white hover:bg-white/20 transition-colors">
                      Switch
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: Movies & Series */}
          {activeTab === 'vod' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-accent-purple/20 text-accent-purple border border-accent-purple/30">
                  Feature Spotlight: VOD Hub
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  TMDB-Powered Cinema & TV Series Index
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Discover trending films and binge-worthy television with complete cast biographies, ratings, trailers, seasons, and episode breakdowns.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Curated trending, top-rated, and personalized rows</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Instant stream resolution with multi-server providers</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Watchlist sync across sessions stored locally on device</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                <div className="flex items-center justify-between mb-4">
                  <span className="text-xs font-bold uppercase tracking-wider text-slate-300">Featured & Trending</span>
                  <span className="text-xs text-brand-400 font-semibold">Explore All (4,200+)</span>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div className="rounded-xl overflow-hidden bg-white/5 border border-white/10 relative group">
                    <div className="aspect-[2/3] bg-gradient-to-t from-slate-900 via-indigo-950 to-slate-800 p-2 flex flex-col justify-end">
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/60 text-emerald-300 font-bold w-fit mb-1">
                        ★ 8.8
                      </span>
                      <p className="text-xs font-semibold text-white truncate">Oppenheimer</p>
                      <p className="text-[10px] text-slate-400">2023 • 4K HDR</p>
                    </div>
                  </div>
                  <div className="rounded-xl overflow-hidden bg-white/5 border border-white/10 relative group">
                    <div className="aspect-[2/3] bg-gradient-to-t from-slate-900 via-purple-950 to-slate-800 p-2 flex flex-col justify-end">
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/60 text-emerald-300 font-bold w-fit mb-1">
                        ★ 8.6
                      </span>
                      <p className="text-xs font-semibold text-white truncate">Dune: Part Two</p>
                      <p className="text-[10px] text-slate-400">2024 • 1080p</p>
                    </div>
                  </div>
                  <div className="rounded-xl overflow-hidden bg-white/5 border border-white/10 relative group">
                    <div className="aspect-[2/3] bg-gradient-to-t from-slate-900 via-cyan-950 to-slate-800 p-2 flex flex-col justify-end">
                      <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-black/60 text-emerald-300 font-bold w-fit mb-1">
                        ★ 8.9
                      </span>
                      <p className="text-xs font-semibold text-white truncate">Kalki 2898 AD</p>
                      <p className="text-[10px] text-slate-400">2024 • Telugu/Hindi</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: Resilient Chunked Downloader */}
          {activeTab === 'downloads' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                  Feature Spotlight: Offline Manager
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  Multi-Thread Chunked Downloads with Auto-Recovery
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Download full films and TV shows in parallel chunk threads. If your connection drops or an expired token triggers HTTP 403, NisTV automatically regenerates links on resume without starting over.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Wi-Fi only mode protects your cellular mobile plan</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Background foreground service with notification controls</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Encrypted file integrity check ensures zero corruption</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                <div className="flex items-center justify-between mb-4 border-b border-white/10 pb-3">
                  <div className="flex items-center space-x-2">
                    <HardDrive className="w-4 h-4 text-emerald-400" />
                    <span className="text-xs font-bold text-white">Active Download Queue</span>
                  </div>
                  <span className="text-xs font-mono text-emerald-400">Speed: 18.2 MB/s (4 Threads)</span>
                </div>

                <div className="space-y-3">
                  {/* Item 1 - In Progress */}
                  <div className="p-3.5 rounded-xl bg-white/5 border border-emerald-500/30">
                    <div className="flex items-center justify-between mb-2">
                      <div>
                        <div className="text-xs font-semibold text-white">The Dark Knight (2008)</div>
                        <div className="text-[10px] text-slate-400">1.82 GB of 2.15 GB • 85% Complete</div>
                      </div>
                      <span className="text-xs font-mono text-emerald-400">45s remaining</span>
                    </div>
                    <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                      <div className="h-full bg-emerald-500 rounded-full w-[85%] transition-all" />
                    </div>
                  </div>

                  {/* Item 2 - Completed */}
                  <div className="p-3.5 rounded-xl bg-white/5 border border-white/5 flex items-center justify-between">
                    <div>
                      <div className="text-xs font-semibold text-white">Stranger Things S04E01</div>
                      <div className="text-[10px] text-emerald-400">Ready for offline viewing • 1.1 GB</div>
                    </div>
                    <span className="px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 text-[10px] font-semibold">
                      Completed
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: Player Engine & PiP */}
          {activeTab === 'player' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-accent-pink/20 text-accent-pink border border-accent-pink/30">
                  Feature Spotlight: Playback Engine
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  ExoPlayer 1.4 with Auto-PiP & Gesture Controls
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Enjoy hardware-accelerated playback with customizable subtitle tracks, multiple audio language selectors, touch swipe controls for volume/brightness, and seamless Picture-in-Picture.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Android 12+ Auto-PiP transitions instantly on home gesture</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>MediaSession audio background streaming support</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Aspect ratio stretch, crop, fit, and 16:9/21:9 zoom</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                <div className="relative rounded-xl overflow-hidden aspect-video bg-black flex items-center justify-center">
                  <img src="./images/splash.png" alt="ExoPlayer Preview" className="w-full h-full object-cover opacity-60" />
                  <div className="absolute inset-0 bg-black/40 flex flex-col justify-between p-4">
                    <div className="flex items-center justify-between text-xs text-white">
                      <span className="font-semibold">ExoPlayer 1.4.1 [Hardware HEVC]</span>
                      <span className="px-2 py-0.5 rounded bg-brand-500/40 text-brand-200 font-mono text-[10px]">
                        PiP Active
                      </span>
                    </div>
                    <div className="flex items-center justify-center space-x-6">
                      <button className="p-2 rounded-full bg-white/20 text-white backdrop-blur-md">
                        <Play className="w-6 h-6 fill-white" />
                      </button>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-300 font-mono">
                      <span>42:15</span>
                      <div className="flex-1 mx-3 h-1 bg-white/30 rounded-full overflow-hidden">
                        <div className="h-full bg-brand-400 w-1/3" />
                      </div>
                      <span>2:15:00</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 5: Regional Languages */}
          {activeTab === 'languages' && (
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center animate-in fade-in duration-300">
              <div className="lg:col-span-5 space-y-4">
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-accent-orange/20 text-accent-orange border border-accent-orange/30">
                  Feature Spotlight: Localization
                </span>
                <h3 className="text-2xl sm:text-3xl font-bold text-white">
                  First-Launch Regional Onboarding Tailored to You
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed">
                  Select your preferred entertainment languages during setup (Hindi, Tamil, Telugu, Malayalam, Bengali, Kannada, Punjabi, Marathi, etc.) to immediately customize your default channels, live guide, and discover catalog.
                </p>
                <div className="space-y-2 pt-2 text-xs text-slate-300">
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Multi-select support allows mixing regional + international feeds</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Change languages anytime with one tap in Settings</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Check className="w-4 h-4 text-emerald-400" />
                    <span>Native script badges (हिन्दी, தமிழ், తెలుగు, বাংলা)</span>
                  </div>
                </div>
              </div>

              <div className="lg:col-span-7 bg-[#0b0f19] rounded-2xl p-4 sm:p-6 border border-white/10 shadow-2xl">
                <span className="text-xs font-bold text-slate-300 uppercase tracking-wider block mb-3">
                  Supported Regional Languages & Channels
                </span>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                  {[
                    { name: 'Hindi', script: 'हिन्दी', count: '140+ Ch' },
                    { name: 'Tamil', script: 'தமிழ்', count: '95+ Ch' },
                    { name: 'Telugu', script: 'తెలుగు', count: '110+ Ch' },
                    { name: 'Malayalam', script: 'മലയാളം', count: '70+ Ch' },
                    { name: 'Kannada', script: 'ಕನ್ನಡ', count: '65+ Ch' },
                    { name: 'Bengali', script: 'বাংলা', count: '80+ Ch' },
                    { name: 'Marathi', script: 'मराठी', count: '75+ Ch' },
                    { name: 'Punjabi', script: 'ਪੰਜਾਬੀ', count: '50+ Ch' },
                    { name: 'English & Global', script: 'International', count: '300+ Ch' },
                  ].map((item) => (
                    <div key={item.name} className="p-3 rounded-xl bg-white/5 border border-white/10 hover:border-brand-500/40 transition-colors">
                      <div className="text-xs font-bold text-white">{item.name}</div>
                      <div className="text-[11px] text-brand-300 font-medium">{item.script}</div>
                      <div className="text-[10px] text-slate-400 mt-1">{item.count}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

        </div>

      </div>
    </section>
  );
};
