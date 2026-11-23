import { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { StatsBar } from './components/StatsBar';
import { Features } from './components/Features';
import { AppScreensPreview } from './components/AppScreensPreview';
import { DownloadSection } from './components/DownloadSection';
import { InstallGuide } from './components/InstallGuide';
import { SpecsSection } from './components/SpecsSection';
import { ChangelogSection } from './components/ChangelogSection';
import { FaqSection } from './components/FaqSection';
import { Footer } from './components/Footer';
import { DownloadModal } from './components/DownloadModal';
import { QrModal } from './components/QrModal';
import { Toast } from './components/Toast';

export function App() {
  const [downloadModalOpen, setDownloadModalOpen] = useState(false);
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    // Check if user arrived via update notification or download link
    const params = new URLSearchParams(window.location.search);
    const hasDownloadQuery = params.has('download') || params.get('action') === 'download';
    const isDownloadPath = window.location.pathname.includes('/download');
    const isDownloadHash = window.location.hash === '#download' || window.location.hash === '#download-started';

    if (hasDownloadQuery || isDownloadPath || isDownloadHash) {
      setDownloadModalOpen(true);
    }
  }, []);

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100 selection:bg-brand-500/30 selection:text-white flex flex-col font-sans">
      {/* Top Navigation */}
      <Navbar
        onOpenDownloadModal={() => setDownloadModalOpen(true)}
        onOpenQrModal={() => setQrModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1">
        <Hero
          onOpenDownloadModal={() => setDownloadModalOpen(true)}
          onOpenQrModal={() => setQrModalOpen(true)}
          onTriggerToast={triggerToast}
        />

        <StatsBar />

        <Features />

        <AppScreensPreview />

        <DownloadSection
          onOpenDownloadModal={() => setDownloadModalOpen(true)}
          onOpenQrModal={() => setQrModalOpen(true)}
          onTriggerToast={triggerToast}
        />

        <InstallGuide />

        <SpecsSection />

        <ChangelogSection />

        <FaqSection />
      </main>

      {/* Footer */}
      <Footer onOpenDownloadModal={() => setDownloadModalOpen(true)} />

      {/* Interactive Modals & Overlays */}
      <DownloadModal
        isOpen={downloadModalOpen}
        onClose={() => setDownloadModalOpen(false)}
        onTriggerToast={triggerToast}
      />

      <QrModal
        isOpen={qrModalOpen}
        onClose={() => setQrModalOpen(false)}
        onTriggerToast={triggerToast}
      />

      <Toast message={toastMessage} onClose={() => setToastMessage(null)} />
    </div>
  );
}

export default App;
