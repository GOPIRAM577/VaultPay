import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useAuthStore } from '../../store/useAuthStore';
import { Activity, FileText, Download, Calendar } from 'lucide-react';

const API_URL = 'http://localhost:8080';

export const SettlementsTab = () => {
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0]);
  const token = useAuthStore((state) => state.token);
  const [isDownloading, setIsDownloading] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-settlements', date],
    queryFn: async () => {
      if (!date) return [];
      const res = await axios.get(`${API_URL}/admin/settlements/${date}/report`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return res.data;
    },
    enabled: !!date,
  });

  const handleDownloadCsv = async () => {
    try {
      setIsDownloading(true);
      const res = await axios.get(`${API_URL}/admin/settlements/${date}/report.csv`, {
        headers: { Authorization: `Bearer ${token}` },
        responseType: 'blob', // Important for file downloads!
      });
      
      const blob = new Blob([res.data], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `settlement_${date}.csv`);
      document.body.appendChild(link);
      link.click();
      
      // Cleanup
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to download CSV', err);
      alert('Failed to download CSV. Please try again.');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2">
          <FileText className="h-5 w-5 text-blue-400" />
          <h2 className="text-xl font-semibold text-white">Daily Settlement Reports</h2>
        </div>
        
        <div className="flex items-center gap-3">
          <div className="relative">
            <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="pl-9 pr-4 py-2 bg-gray-800/80 border border-gray-700 rounded-lg text-sm text-white focus:outline-none focus:border-blue-500 transition-colors"
            />
          </div>
          <button
            onClick={handleDownloadCsv}
            disabled={!date || isDownloading || !data || data.length === 0}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 disabled:bg-gray-700 disabled:text-gray-500 text-white text-sm font-medium rounded-lg transition-colors shadow-lg"
          >
            {isDownloading ? <Activity className="h-4 w-4 animate-spin" /> : <Download className="h-4 w-4" />}
            Download CSV
          </button>
        </div>
      </div>

      <div className="bg-gray-800/50 backdrop-blur-xl border border-gray-700/50 rounded-2xl overflow-hidden shadow-2xl">
        {isLoading ? (
          <div className="flex justify-center p-12"><Activity className="h-8 w-8 animate-spin text-blue-500" /></div>
        ) : !data || data.length === 0 ? (
          <div className="p-12 text-center text-gray-400">
            No settlement records found for {date}.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-gray-900/80 text-xs uppercase text-gray-400">
                <tr>
                  <th className="px-6 py-4">Wallet ID</th>
                  <th className="px-6 py-4">Total Credits</th>
                  <th className="px-6 py-4">Total Debits</th>
                  <th className="px-6 py-4">Net Position</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700/50 bg-gray-800/30">
                {data.map((report: any) => (
                  <tr key={report.id} className="hover:bg-gray-800/50 transition-colors">
                    <td className="px-6 py-4 font-mono text-xs text-gray-400">{report.walletId}</td>
                    <td className="px-6 py-4 text-emerald-400 font-mono">+{parseFloat(report.totalCredits).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                    <td className="px-6 py-4 text-red-400 font-mono">-{parseFloat(report.totalDebits).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 rounded-full text-xs font-mono font-bold ${
                        parseFloat(report.netPosition) >= 0 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
                      }`}>
                        {parseFloat(report.netPosition) >= 0 ? '+' : ''}{parseFloat(report.netPosition).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
