import React from 'react';
import { HIGHLIGHT_STATS } from '../data/appData';

export const StatsBar: React.FC = () => {
  return (
    <section className="relative z-10 -mt-6 sm:-mt-10 mb-16 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="rounded-3xl glass-panel p-6 sm:p-8 shadow-2xl">
        <div className="grid grid-cols-2 md:grid-cols-5 gap-6 sm:gap-8 divide-y sm:divide-y-0 sm:divide-x divide-white/10">
          {HIGHLIGHT_STATS.map((stat, idx) => (
            <div
              key={stat.label}
              className={`text-center ${idx > 0 ? 'pt-4 sm:pt-0' : ''}`}
            >
              <div className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-white mb-1">
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-white via-brand-200 to-accent-cyan">
                  {stat.value}
                </span>
              </div>
              <div className="text-xs sm:text-sm font-semibold text-slate-200">
                {stat.label}
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                {stat.detail}
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
