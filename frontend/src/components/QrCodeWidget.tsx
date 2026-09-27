import { useState, useEffect } from 'react';
import api from '../lib/api';
import { QrCode, Loader2, AlertCircle, RefreshCw } from 'lucide-react';

export const QrCodeWidget = () => {
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const fetchQr = async () => {
    setLoading(true);
    setError(false);
    try {
      const response = await api.get('/wallets/me/qr', { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      setQrUrl(url);
    } catch (err) {
      console.error('Failed to load QR code', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQr();
    return () => {
      if (qrUrl) {
        URL.revokeObjectURL(qrUrl);
      }
    };
  }, []);

  const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col items-center justify-between min-h-[260px] relative">
      <div className="w-full flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <QrCode className="w-5 h-5 text-primary" />
          <span className="text-sm font-medium text-muted-foreground">My QR Code</span>
        </div>
        <button
          onClick={fetchQr}
          className="w-7 h-7 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-white/70 hover:text-white hover:bg-white/10 transition-all"
        >
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="flex-1 flex items-center justify-center w-full relative min-h-[160px]">
        {loading ? (
          <div className="flex flex-col items-center gap-2">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <span className="text-xs text-muted-foreground">Generating QR...</span>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center gap-2 text-destructive text-center p-4">
            <AlertCircle className="w-8 h-8" />
            <span className="text-xs font-medium">Failed to load QR Code</span>
          </div>
        ) : (
          <div className="p-3 bg-white rounded-xl border border-white/10 relative">
            {/* Design guide borders */}
            <div className="absolute -top-1.5 -left-1.5 w-4 h-4 border-t-2 border-l-2 border-primary rounded-tl" />
            <div className="absolute -top-1.5 -right-1.5 w-4 h-4 border-t-2 border-r-2 border-primary rounded-tr" />
            <div className="absolute -bottom-1.5 -left-1.5 w-4 h-4 border-b-2 border-l-2 border-primary rounded-bl" />
            <div className="absolute -bottom-1.5 -right-1.5 w-4 h-4 border-b-2 border-r-2 border-primary rounded-br" />
            
            <img 
              src={qrUrl || `${API_URL}/wallets/me/qr`} 
              alt="wallet QR" 
              className="w-36 h-36 object-contain"
            />
          </div>
        )}
      </div>

      <div className="text-[11px] text-muted-foreground text-center mt-4 leading-relaxed">
        Scan to instantly pay or request transfers.
      </div>
    </div>
  );
};
