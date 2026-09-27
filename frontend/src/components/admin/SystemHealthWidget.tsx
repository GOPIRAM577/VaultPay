import React from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useAuthStore } from '../../store/useAuthStore';
import { Server, Activity, Database, CheckCircle, AlertOctagon } from 'lucide-react';

const API_URL = 'http://localhost:8080';

export const SystemHealthWidget = () => {
  const token = useAuthStore((state) => state.token);

  const { data: outboxStats } = useQuery({
    queryKey: ['admin-outbox-stats'],
    queryFn: async () => {
      const res = await axios.get(`${API_URL}/admin/outbox/stats`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return res.data;
    },
    refetchInterval: 10000, // Refresh every 10 seconds per requirements
  });

  const { data: reconStats } = useQuery({
    queryKey: ['admin-recon-stats'],
    queryFn: async () => {
      const res = await axios.get(`${API_URL}/admin/reconciliation`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return res.data;
    },
    refetchInterval: 60000,
  });

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8 animate-fade-in">
      {/* Outbox Event Stats */}
      <div className="bg-gray-800/50 backdrop-blur-xl border border-gray-700/50 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <Server className="h-24 w-24 text-blue-500" />
        </div>
        <div className="relative z-10">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <Activity className="h-5 w-5 text-blue-400" />
              Event Outbox Status
            </h3>
            <span className="flex items-center gap-1.5 text-xs text-blue-400 bg-blue-900/30 px-2 py-1 rounded-full border border-blue-800/50 font-medium">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
              </span>
              Live (10s)
            </span>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="bg-gray-900/60 rounded-xl p-4 border border-gray-800">
              <div className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Pending</div>
              <div className="text-2xl font-bold text-amber-400">{outboxStats?.pending || 0}</div>
            </div>
            <div className="bg-gray-900/60 rounded-xl p-4 border border-gray-800">
              <div className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Processed</div>
              <div className="text-2xl font-bold text-emerald-400">{outboxStats?.processed || 0}</div>
            </div>
            <div className="bg-gray-900/60 rounded-xl p-4 border border-gray-800">
              <div className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">Failed</div>
              <div className="text-2xl font-bold text-red-400">{outboxStats?.failed || 0}</div>
            </div>
          </div>
        </div>
      </div>

      {/* Reconciliation Stats */}
      <div className="bg-gray-800/50 backdrop-blur-xl border border-gray-700/50 rounded-2xl p-6 shadow-xl relative overflow-hidden group">
        <div className="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
          <Database className="h-24 w-24 text-purple-500" />
        </div>
        <div className="relative z-10">
          <div className="flex items-center mb-6">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <CheckCircle className="h-5 w-5 text-purple-400" />
              Ledger Reconciliation
            </h3>
          </div>

          <div className="flex items-center justify-between bg-gray-900/60 rounded-xl p-5 border border-gray-800">
            <div>
              <div className="text-sm font-medium text-gray-400 mb-1">Last Run Time</div>
              <div className="text-white font-medium">
                {reconStats?.lastRunAt ? new Date(reconStats.lastRunAt).toLocaleString() : 'Never'}
              </div>
            </div>
            <div className="text-right">
              <div className="text-sm font-medium text-gray-400 mb-1">Mismatch Count</div>
              {reconStats?.mismatchCount > 0 ? (
                <div className="text-2xl font-bold text-red-400 flex items-center gap-2 justify-end">
                  <AlertOctagon className="h-5 w-5" />
                  {reconStats.mismatchCount}
                </div>
              ) : (
                <div className="text-2xl font-bold text-emerald-400">
                  0
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
