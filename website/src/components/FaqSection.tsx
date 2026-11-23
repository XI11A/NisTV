import React, { useState } from 'react';
import { FAQ_DATA } from '../data/appData';
import { ChevronDown, HelpCircle, Search } from 'lucide-react';

export const FaqSection: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(0);
  const [filterCategory, setFilterCategory] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const filteredFaqs = FAQ_DATA.filter((faq) => {
    const matchesCategory = filterCategory === 'all' || faq.category === filterCategory;
    const matchesSearch =
      faq.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
      faq.answer.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const categories = [
    { id: 'all', label: 'All Questions' },
    { id: 'download', label: 'Download & Verification' },
    { id: 'installation', label: 'Installation & TV' },
    { id: 'playback', label: 'Streaming & Features' },
    { id: 'privacy', label: 'Privacy & Security' },
  ];

  return (
    <section id="faq" className="py-20 relative scroll-mt-20">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        
        {/* Header */}
        <div className="text-center mb-12">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-accent-cyan/10 border border-accent-cyan/20 text-accent-cyan text-xs font-semibold mb-3">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>COMMONLY ASKED QUESTIONS</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight mb-4">
            Frequently Asked <span className="text-gradient">Questions</span>
          </h2>
          <p className="text-base text-slate-300">
            Everything you need to know about downloading, sideloading, streaming, and offline playback.
          </p>

          {/* Search Bar */}
          <div className="relative mt-8 max-w-md mx-auto">
            <Search className="w-4 h-4 text-slate-400 absolute left-4 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search questions (e.g. Android TV, safe, offline)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-11 pr-4 py-3 rounded-2xl bg-white/5 border border-white/10 text-white placeholder-slate-400 text-sm focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition-all"
            />
          </div>

          {/* Category Filter Pills */}
          <div className="flex items-center justify-start sm:justify-center space-x-2 overflow-x-auto pb-2 mt-4 scrollbar-none">
            {categories.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setFilterCategory(cat.id)}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all whitespace-nowrap focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
                  filterCategory === cat.id
                    ? 'bg-brand-600 text-white shadow-md shadow-brand-600/30'
                    : 'bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white border border-white/5'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* FAQ Accordion List */}
        <div className="space-y-4">
          {filteredFaqs.length > 0 ? (
            filteredFaqs.map((faq, idx) => {
              const isOpen = openIndex === idx;
              return (
                <div
                  key={faq.question}
                  className="glass-panel rounded-2xl border border-white/10 overflow-hidden transition-colors"
                >
                  <button
                    onClick={() => setOpenIndex(isOpen ? null : idx)}
                    className="w-full py-4 px-6 text-left flex items-center justify-between space-x-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 min-h-[52px]"
                    aria-expanded={isOpen}
                  >
                    <span className="font-semibold text-white text-sm sm:text-base pr-2">
                      {faq.question}
                    </span>
                    <ChevronDown
                      className={`w-5 h-5 text-slate-400 transition-transform duration-200 flex-shrink-0 ${
                        isOpen ? 'rotate-180 text-brand-400' : ''
                      }`}
                    />
                  </button>

                  {isOpen && (
                    <div className="px-6 pb-5 pt-1 text-slate-300 text-xs sm:text-sm leading-relaxed border-t border-white/5 animate-in fade-in duration-200">
                      {faq.answer}
                    </div>
                  )}
                </div>
              );
            })
          ) : (
            <div className="text-center py-12 text-slate-400 glass-panel rounded-2xl border border-white/10">
              <p>No questions matched your search query "{searchQuery}".</p>
              <button
                onClick={() => {
                  setSearchQuery('');
                  setFilterCategory('all');
                }}
                className="mt-3 text-xs text-brand-400 underline font-semibold"
              >
                Reset filters
              </button>
            </div>
          )}
        </div>

      </div>
    </section>
  );
};
