import React from 'react';
import { CHANGELOG_DATA } from '../data/appData';
import { GitCommit, PlusCircle, ArrowUpCircle, CheckCircle } from 'lucide-react';

export const ChangelogSection: React.FC = () => {
  return (
    <section id="changelog" className="py-20 relative scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-14">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-300 text-xs font-semibold mb-3">
            <span>RELEASE HISTORY</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
            Changelog & <span className="text-gradient">Release Notes</span>
          </h2>
          <p className="text-base text-slate-300">
            Track every architectural improvement, stream failover enhancement, and feature addition.
          </p>
        </div>

        {/* Changelog Entries */}
        <div className="max-w-4xl mx-auto space-y-8">
          {CHANGELOG_DATA.map((release) => (
            <div key={release.version} className="glass-panel rounded-3xl p-6 sm:p-10 border border-white/10 shadow-xl">
              {/* Release Header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-6 border-b border-white/10">
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 rounded-xl bg-brand-500/20 text-brand-300 flex items-center justify-center font-bold">
                    <GitCommit className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center space-x-2">
                      <h3 className="text-xl font-bold text-white">{release.version}</h3>
                      <span className="px-2.5 py-0.5 rounded-full bg-brand-500/20 text-brand-300 border border-brand-500/30 text-xs font-semibold">
                        {release.tag}
                      </span>
                    </div>
                    <span className="text-xs text-slate-400 font-mono">{release.date}</span>
                  </div>
                </div>

                <div className="text-xs font-mono text-emerald-400 flex items-center space-x-1.5">
                  <CheckCircle className="w-4 h-4" />
                  <span>Production Stable</span>
                </div>
              </div>

              {/* Sections: Added, Improved, Fixed */}
              <div className="pt-6 space-y-6 text-sm">
                
                {/* Added */}
                {release.added.length > 0 && (
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center space-x-2 mb-3">
                      <PlusCircle className="w-4 h-4" />
                      <span>New Features & Capabilities</span>
                    </h4>
                    <ul className="space-y-2 text-xs sm:text-sm text-slate-300">
                      {release.added.map((item, idx) => (
                        <li key={idx} className="flex items-start space-x-2.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 flex-shrink-0 mt-2" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Improved */}
                {release.improved.length > 0 && (
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-brand-300 flex items-center space-x-2 mb-3">
                      <ArrowUpCircle className="w-4 h-4" />
                      <span>Performance & Polish</span>
                    </h4>
                    <ul className="space-y-2 text-xs sm:text-sm text-slate-300">
                      {release.improved.map((item, idx) => (
                        <li key={idx} className="flex items-start space-x-2.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-brand-400 flex-shrink-0 mt-2" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Fixed */}
                {release.fixed.length > 0 && (
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-accent-cyan flex items-center space-x-2 mb-3">
                      <CheckCircle className="w-4 h-4" />
                      <span>Bug Fixes</span>
                    </h4>
                    <ul className="space-y-2 text-xs sm:text-sm text-slate-300">
                      {release.fixed.map((item, idx) => (
                        <li key={idx} className="flex items-start space-x-2.5">
                          <span className="w-1.5 h-1.5 rounded-full bg-accent-cyan flex-shrink-0 mt-2" />
                          <span>{item}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

              </div>
            </div>
          ))}
        </div>

      </div>
    </section>
  );
};
