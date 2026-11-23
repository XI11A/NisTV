import React from 'react';
import { SYSTEM_SPECS } from '../data/appData';
import { Info } from 'lucide-react';

export const SpecsSection: React.FC = () => {
  return (
    <section id="specs" className="py-20 relative scroll-mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-14">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-accent-purple/10 border border-accent-purple/20 text-accent-purple text-xs font-semibold mb-3">
            <span>COMPATIBILITY & HARDWARE</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
            System Requirements & <span className="text-gradient">Compatibility</span>
          </h2>
          <p className="text-base text-slate-300">
            NisTV Revamped is heavily optimized with low memory footprint and hardware video acceleration across all Android hardware architectures.
          </p>
        </div>

        {/* Specs Table / Card */}
        <div className="glass-panel rounded-3xl overflow-hidden border border-white/10 shadow-2xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-white/10 bg-white/5 text-xs font-bold uppercase tracking-wider text-slate-300">
                  <th className="py-4 px-6">Requirement</th>
                  <th className="py-4 px-6 text-slate-400">Minimum Spec</th>
                  <th className="py-4 px-6 text-brand-300">Recommended Spec</th>
                  <th className="py-4 px-6 text-slate-400 hidden sm:table-cell">Notes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5 text-sm">
                {SYSTEM_SPECS.map((spec) => (
                  <tr key={spec.feature} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-4 px-6 font-semibold text-white whitespace-nowrap">
                      {spec.feature}
                    </td>
                    <td className="py-4 px-6 text-slate-300 font-mono text-xs">
                      {spec.minimum}
                    </td>
                    <td className="py-4 px-6 text-brand-300 font-semibold font-mono text-xs">
                      {spec.recommended}
                    </td>
                    <td className="py-4 px-6 text-slate-400 text-xs hidden sm:table-cell">
                      {spec.note}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Footer note inside card */}
          <div className="p-4 sm:p-6 bg-black/40 border-t border-white/5 flex items-center justify-between text-xs text-slate-400">
            <div className="flex items-center space-x-2">
              <Info className="w-4 h-4 text-brand-400 flex-shrink-0" />
              <span>Universal APK auto-detects 32-bit vs 64-bit ARM/x86 native libraries during installation.</span>
            </div>
            <span className="font-mono text-slate-500 hidden md:inline">Target SDK: 34 (Android 14)</span>
          </div>
        </div>

      </div>
    </section>
  );
};
