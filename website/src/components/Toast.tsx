import React from 'react';
import { CheckCircle } from 'lucide-react';

interface ToastProps {
  message: string | null;
  onClose: () => void;
}

export const Toast: React.FC<ToastProps> = ({ message }) => {
  if (!message) return null;

  return (
    <div
      className="fixed bottom-6 right-6 z-50 flex items-center space-x-2 px-4 py-3 rounded-2xl bg-emerald-950/90 border border-emerald-500/40 text-emerald-200 shadow-2xl backdrop-blur-md animate-in slide-in-from-bottom-5 duration-200 text-xs sm:text-sm font-medium"
      role="status"
      aria-live="polite"
    >
      <CheckCircle className="w-4 h-4 text-emerald-400 flex-shrink-0" />
      <span>{message}</span>
    </div>
  );
};
