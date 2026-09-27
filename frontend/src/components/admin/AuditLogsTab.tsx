import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useAuthStore } from '../../store/useAuthStore';
import { Activity, ScrollText, Filter } from 'lucide-react';

const API_URL = 'http://localhost:8080';

export const AuditLogsTab = () => {
  const [filterAction, setFilterAction] = useState('ALL');
  const token = useAuthStore((state) => state.token);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-audit-logs'],
    queryFn: async () => {
      // In a real app we'd pass filterAction to backend via query param. 
      // For now, we fetch all and filter in frontend.
      const res = await axios.get(`${API_URL}/admin/audit-logs?page=0&size=100&sort=timestamp,desc`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return res.data;
    },
    refetchInterval: 60000,
  });

  const logs = data?.content || [];
  const filteredLogs = filterAction === 'ALL' ? logs : logs.filter((log: any) => log.action === filterAction);

  // Derive unique actions for the filter dropdown
  const uniqueActions = ['ALL', ...Array.from(new Set(logs.map((l: any) => l.action)))];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2">
          <ScrollText className="h-5 w-5 text-indigo-400" />
          <h2 className="text-xl font-semibold text-white">System Audit Logs</h2>
        </div>
        
        <div className="flex items-center gap-2 bg-gray-800/80 border border-gray-700 rounded-lg px-3 py-1.5">
          <Filter className="h-4 w-4 text-gray-400" />
          <select 
            value={filterAction} 
            onChange={(e) => setFilterAction(e.target.value)}
            className="bg-transparent text-sm text-gray-200 focus:outline-none border-none appearance-none pr-4"
          >
            {uniqueActions.map((action: any) => (
              <option key={action} value={action} className="bg-gray-800 text-white">
                {action === 'ALL' ? 'All Actions' : action}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-gray-800/50 backdrop-blur-xl border border-gray-700/50 rounded-2xl overflow-hidden shadow-2xl">
        {isLoading ? (
          <div className="flex justify-center p-12"><Activity className="h-8 w-8 animate-spin text-indigo-500" /></div>
        ) : filteredLogs.length === 0 ? (
          <div className="p-12 text-center text-gray-400">
            No audit logs match the current filter.
          </div>
        ) : (
          <div className="overflow-x-auto max-h-[600px] overflow-y-auto custom-scrollbar">
            <table className="w-full text-left text-sm text-gray-300">
              <thead className="bg-gray-900/80 text-xs uppercase text-gray-400 sticky top-0">
                <tr>
                  <th className="px-6 py-4">Timestamp</th>
                  <th className="px-6 py-4">Actor ID (Admin)</th>
                  <th className="px-6 py-4">Action</th>
                  <th className="px-6 py-4">Target</th>
                  <th className="px-6 py-4">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-700/50 bg-gray-800/30">
                {filteredLogs.map((log: any) => (
                  <tr key={log.id} className="hover:bg-gray-800/50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap text-xs text-gray-400">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 font-mono text-xs text-gray-400">{log.actorId}</td>
                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 rounded text-[10px] font-bold bg-indigo-500/20 text-indigo-400">
                        {log.action}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-xs">
                      {log.entityType}: <span className="font-mono text-gray-400">{log.entityId}</span>
                    </td>
                    <td className="px-6 py-4 text-xs font-mono text-gray-400 max-w-xs truncate" title={log.metadata}>
                      {log.metadata}
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
