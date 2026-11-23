import React from 'react';
import { CORE_FEATURES } from '../data/appData';
import { Tv, Film, Download, ShieldCheck, PlayCircle, MonitorPlay, CheckCircle2, Cast, Users } from 'lucide-react';

const iconMap: Record<string, React.ReactNode> = {
  Users: <Users className="w-6 h-6 text-accent-purple" />,
  Cast: <Cast className="w-6 h-6 text-accent-cyan" />,
  Tv: <Tv className="w-6 h-6 text-brand-400" />,
  Film: <Film className="w-6 h-6 text-accent-purple" />,
  Download: <Download className="w-6 h-6 text-emerald-400" />,
  ShieldCheck: <ShieldCheck className="w-6 h-6 text-accent-cyan" />,
  PlayCircle: <PlayCircle className="w-6 h-6 text-accent-pink" />,
  MonitorPlay: <MonitorPlay className="w-6 h-6 text-accent-orange" />,
};

export const Features: React.FC = () => {
  return (
    <section id="features" className="py-20 relative scroll-mt-20">
      {/* Background ambient lighting */}
      <div className="absolute top-1/2 left-0 w-72 h-72 bg-brand-600/10 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute top-1/3 right-0 w-72 h-72 bg-accent-purple/10 rounded-full blur-[100px] pointer-events-none" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Section Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold mb-3">
            <span>ENGINEERED FOR SUPREME PERFORMANCE</span>
          </div>
          <h2 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight mb-4">
            Everything You Need in a <br className="hidden sm:inline" />
            <span className="text-gradient">Modern Android Media Player</span>
          </h2>
          <p className="text-base sm:text-lg text-slate-300 leading-relaxed">
            Built from the ground up using Kotlin, Jetpack Compose, Room SQLite, and Google ExoPlayer for instantaneous playback and unbreakable offline resilience.
          </p>
        </div>

        {/* Feature Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
          {CORE_FEATURES.map((feature) => (
            <div
              key={feature.id}
              className="glass-panel glass-panel-hover rounded-3xl p-6 sm:p-8 flex flex-col justify-between relative group"
            >
              <div>
                {/* Icon & Badge Header */}
                <div className="flex items-center justify-between mb-6">
                  <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center group-hover:scale-110 transition-transform shadow-lg">
                    {iconMap[feature.icon] || <Tv className="w-6 h-6 text-brand-400" />}
                  </div>
                  {feature.badge && (
                    <span className="text-[11px] font-semibold px-2.5 py-1 rounded-full bg-white/5 border border-white/10 text-slate-300 font-mono">
                      {feature.badge}
                    </span>
                  )}
                </div>

                {/* Title & Description */}
                <h3 className="text-xl font-bold text-white mb-3 group-hover:text-brand-300 transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-slate-300 leading-relaxed mb-6 font-normal">
                  {feature.description}
                </p>
              </div>

              {/* Highlight list */}
              <ul className="space-y-2.5 pt-4 border-t border-white/5" aria-label={`Highlights for ${feature.title}`}>
                {feature.highlights.map((item, idx) => (
                  <li key={idx} className="flex items-start space-x-2.5 text-xs text-slate-400">
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 flex-shrink-0 mt-0.5" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};
