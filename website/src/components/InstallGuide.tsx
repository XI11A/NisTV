import React, { useState } from 'react';
import { Smartphone, Tv, CheckCircle2, ShieldCheck, Download, Settings, Play } from 'lucide-react';
import { CURRENT_RELEASE } from '../data/appData';

export const InstallGuide: React.FC = () => {
  const [deviceType, setDeviceType] = useState<'mobile' | 'tv'>('mobile');

  return (
    <section id="install-guide" className="py-20 relative scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-12">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold mb-3">
            <span>STEP-BY-STEP SETUP</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
            How to Install <span className="text-gradient">NisTV Revamped</span>
          </h2>
          <p className="text-base text-slate-300">
            Installing an APK takes less than 60 seconds. Choose your target device below for tailored instructions.
          </p>

          {/* Device Toggle */}
          <div className="inline-flex p-1.5 rounded-2xl bg-white/5 border border-white/10 mt-8">
            <button
              onClick={() => setDeviceType('mobile')}
              className={`px-5 py-2.5 rounded-xl font-semibold text-xs sm:text-sm flex items-center space-x-2 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
                deviceType === 'mobile'
                  ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/30'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Smartphone className="w-4 h-4" />
              <span>Android Phone / Tablet</span>
            </button>

            <button
              onClick={() => setDeviceType('tv')}
              className={`px-5 py-2.5 rounded-xl font-semibold text-xs sm:text-sm flex items-center space-x-2 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
                deviceType === 'tv'
                  ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/30'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              <Tv className="w-4 h-4" />
              <span>Android TV / Fire Stick</span>
            </button>
          </div>
        </div>

        {/* Steps Cards */}
        {deviceType === 'mobile' ? (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 animate-in fade-in duration-300">
            {/* Step 1 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between relative group hover:border-brand-500/30 transition-all">
              <div>
                <div className="w-10 h-10 rounded-xl bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-sm mb-4">
                  01
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Download APK</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Tap the Download APK button on this website using Chrome, Firefox, or your mobile browser.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 flex items-center space-x-2 text-[11px] text-slate-400">
                <Download className="w-4 h-4 text-brand-400" />
                <span>Save to Downloads</span>
              </div>
            </div>

            {/* Step 2 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between relative group hover:border-brand-500/30 transition-all">
              <div>
                <div className="w-10 h-10 rounded-xl bg-accent-purple/20 text-accent-purple flex items-center justify-center font-bold text-sm mb-4">
                  02
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Allow Unknown Apps</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  If prompted with "File might be harmful", tap <strong>Download anyway</strong>. In Android Settings, toggle <strong>Allow from this source</strong> for your browser.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 flex items-center space-x-2 text-[11px] text-slate-400">
                <Settings className="w-4 h-4 text-accent-purple" />
                <span>Settings → Security</span>
              </div>
            </div>

            {/* Step 3 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between relative group hover:border-brand-500/30 transition-all">
              <div>
                <div className="w-10 h-10 rounded-xl bg-accent-cyan/20 text-accent-cyan flex items-center justify-center font-bold text-sm mb-4">
                  03
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Tap to Install</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Open the downloaded <code>{CURRENT_RELEASE.apkFileName}</code> from your notifications drawer or Files app and confirm installation.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 flex items-center space-x-2 text-[11px] text-slate-400">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>Takes ~5 seconds</span>
              </div>
            </div>

            {/* Step 4 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between relative group hover:border-brand-500/30 transition-all">
              <div>
                <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center font-bold text-sm mb-4">
                  04
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Launch & Stream</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Open NisTV Revamped, select your regional languages in the onboarding screen, and start watching Live TV and movies!
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 flex items-center space-x-2 text-[11px] text-slate-400">
                <Play className="w-4 h-4 text-emerald-400" />
                <span>Zero login needed</span>
              </div>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 animate-in fade-in duration-300">
            {/* TV Step 1 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-xl bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold text-sm mb-4">
                  01
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Get "Downloader"</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  On your Android TV or Amazon Fire TV, search for and install the free <strong>Downloader by AFTVnews</strong> from your TV's app store.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 text-[11px] text-slate-400">
                Official TV Store App
              </div>
            </div>

            {/* TV Step 2 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-xl bg-accent-purple/20 text-accent-purple flex items-center justify-center font-bold text-sm mb-4">
                  02
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Enable Developer Perms</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Go to TV Settings → My Fire TV / Device Preferences → Developer Options → <strong>Install Unknown Apps</strong> → turn <strong>ON</strong> for Downloader.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 text-[11px] text-slate-400">
                Allows Sideloading
              </div>
            </div>

            {/* TV Step 3 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-xl bg-accent-cyan/20 text-accent-cyan flex items-center justify-center font-bold text-sm mb-4">
                  03
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Enter Direct URL</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Open the Downloader app, enter the APK download URL, and press <strong>Go</strong>. The APK will download directly onto your TV.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 text-[11px] text-slate-400">
                High-speed local caching
              </div>
            </div>

            {/* TV Step 4 */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 flex flex-col justify-between">
              <div>
                <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center font-bold text-sm mb-4">
                  04
                </div>
                <h3 className="text-lg font-bold text-white mb-2">Install & Navigate</h3>
                <p className="text-xs text-slate-300 leading-relaxed">
                  Press Install. NisTV automatically opens in full TV Leanback mode with full remote control D-Pad navigation support.
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-white/5 text-[11px] text-slate-400">
                Remote D-Pad ready
              </div>
            </div>
          </div>
        )}

        {/* Security Note Alert Banner */}
        <div className="mt-10 p-5 rounded-2xl bg-brand-950/40 border border-brand-500/30 flex items-start space-x-3.5 max-w-4xl mx-auto">
          <ShieldCheck className="w-5 h-5 text-brand-400 flex-shrink-0 mt-0.5" />
          <div className="text-xs sm:text-sm text-slate-300 leading-relaxed">
            <strong className="text-white font-semibold">Why does Android ask for permission?</strong> When downloading APKs directly instead of the Play Store, Android requires user confirmation to prevent silent background installations. NisTV Revamped is 100% open-source, contains no adware, and requests only basic network and storage permissions for streaming and offline media.
          </div>
        </div>

      </div>
    </section>
  );
};
