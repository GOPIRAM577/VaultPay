import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';
import { User, Shield, Wallet, Activity, CheckCircle, XCircle, Lock, Unlock, ShieldAlert, FileText, ScrollText } from 'lucide-react';
import { FraudAlertsTab } from '../components/admin/FraudAlertsTab';
import { SettlementsTab } from '../components/admin/SettlementsTab';
import { AuditLogsTab } from '../components/admin/AuditLogsTab';
import { SystemHealthWidget } from '../components/admin/SystemHealthWidget';

const API_URL = 'http://localhost:8080';

export const AdminPanel = () => {
  const [activeTab, setActiveTab] = useState<'users' | 'wallets' | 'fraud' | 'settlements' | 'audit'>('users');
  const token = useAuthStore((state) => state.token);
  const queryClient = useQueryClient();

  const authHeader = {
    headers: { Authorization: `Bearer ${token}` }
  };

  const { data: usersData, isLoading: usersLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: async () => {
      const res = await axios.get(`${API_URL}/admin/users?page=0&size=50`, authHeader);
      return res.data;
    }
  });

  const { data: walletsData, isLoading: walletsLoading } = useQuery({
    queryKey: ['admin-wallets'],
    queryFn: async () => {
      const res = await axios.get(`${API_URL}/admin/wallets?page=0&size=50`, authHeader);
      return res.data;
    }
  });

  const toggleUser = useMutation({
    mutationFn: async (userId: string) => {
      const res = await axios.patch(`${API_URL}/admin/users/${userId}/activate`, {}, authHeader);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    }
  });

  const toggleWallet = useMutation({
    mutationFn: async (walletId: string) => {
      const res = await axios.patch(`${API_URL}/admin/wallets/${walletId}/status`, {}, authHeader);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-wallets'] });
    }
  });

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white flex items-center gap-3">
            <Shield className="h-8 w-8 text-emerald-400" />
            Admin Dashboard
          </h1>
          <p className="mt-1 text-gray-400">Manage platform users and wallets</p>
        </div>
      </div>

      <SystemHealthWidget />

      <div className="bg-gray-800/50 backdrop-blur-xl border border-gray-700/50 rounded-2xl overflow-hidden shadow-2xl">
        <div className="flex border-b border-gray-700/50 overflow-x-auto custom-scrollbar">
          <button
            onClick={() => setActiveTab('users')}
            className={`flex-1 flex items-center justify-center gap-2 py-4 min-w-[150px] text-sm font-medium transition-colors ${
              activeTab === 'users' ? 'text-emerald-400 border-b-2 border-emerald-400 bg-gray-800/80' : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/30'
            }`}
          >
            <User className="h-5 w-5" />
            Users
          </button>
          <button
            onClick={() => setActiveTab('wallets')}
            className={`flex-1 flex items-center justify-center gap-2 py-4 min-w-[150px] text-sm font-medium transition-colors ${
              activeTab === 'wallets' ? 'text-emerald-400 border-b-2 border-emerald-400 bg-gray-800/80' : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/30'
            }`}
          >
            <Wallet className="h-5 w-5" />
            Wallets
          </button>
          <button
            onClick={() => setActiveTab('fraud')}
            className={`flex-1 flex items-center justify-center gap-2 py-4 min-w-[150px] text-sm font-medium transition-colors ${
              activeTab === 'fraud' ? 'text-red-400 border-b-2 border-red-400 bg-gray-800/80' : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/30'
            }`}
          >
            <ShieldAlert className="h-5 w-5" />
            Fraud Alerts
          </button>
          <button
            onClick={() => setActiveTab('settlements')}
            className={`flex-1 flex items-center justify-center gap-2 py-4 min-w-[150px] text-sm font-medium transition-colors ${
              activeTab === 'settlements' ? 'text-blue-400 border-b-2 border-blue-400 bg-gray-800/80' : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/30'
            }`}
          >
            <FileText className="h-5 w-5" />
            Settlements
          </button>
          <button
            onClick={() => setActiveTab('audit')}
            className={`flex-1 flex items-center justify-center gap-2 py-4 min-w-[150px] text-sm font-medium transition-colors ${
              activeTab === 'audit' ? 'text-indigo-400 border-b-2 border-indigo-400 bg-gray-800/80' : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/30'
            }`}
          >
            <ScrollText className="h-5 w-5" />
            Audit Logs
          </button>
        </div>

        <div className="p-6">
          {activeTab === 'users' && (
            <div className="space-y-4">
              {usersLoading ? (
                <div className="flex justify-center p-12"><Activity className="h-8 w-8 animate-spin text-emerald-500" /></div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-gray-700/50">
                  <table className="w-full text-left text-sm text-gray-300">
                    <thead className="bg-gray-900/80 text-xs uppercase text-gray-400">
                      <tr>
                        <th className="px-6 py-4">Name</th>
                        <th className="px-6 py-4">Email</th>
                        <th className="px-6 py-4">Role</th>
                        <th className="px-6 py-4">Status</th>
                        <th className="px-6 py-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-700/50 bg-gray-800/30">
                      {usersData?.content?.map((u: any) => (
                        <tr key={u.id} className="hover:bg-gray-800/50 transition-colors">
                          <td className="px-6 py-4 font-medium text-white">{u.name}</td>
                          <td className="px-6 py-4 text-gray-400">{u.email}</td>
                          <td className="px-6 py-4">
                            <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${u.role === 'ADMIN' ? 'bg-purple-900/30 text-purple-400 border-purple-800' : 'bg-gray-800 text-gray-300 border-gray-700'}`}>
                              {u.role}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <span className={`flex items-center gap-1.5 text-xs font-medium ${u.isActive ? 'text-emerald-400' : 'text-red-400'}`}>
                              {u.isActive ? <CheckCircle className="h-4 w-4" /> : <XCircle className="h-4 w-4" />} 
                              {u.isActive ? 'ACTIVE' : 'INACTIVE'}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <button
                              onClick={() => toggleUser.mutate(u.id)}
                              disabled={toggleUser.isPending}
                              className={`px-4 py-2 rounded-lg text-xs font-medium transition-all ${
                                u.isActive ? 'bg-red-500/10 text-red-400 hover:bg-red-500/20' : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20'
                              }`}
                            >
                              {u.isActive ? 'Deactivate' : 'Activate'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === 'wallets' && (
            <div className="space-y-4">
              {walletsLoading ? (
                <div className="flex justify-center p-12"><Activity className="h-8 w-8 animate-spin text-emerald-500" /></div>
              ) : (
                <div className="overflow-x-auto rounded-xl border border-gray-700/50">
                  <table className="w-full text-left text-sm text-gray-300">
                    <thead className="bg-gray-900/80 text-xs uppercase text-gray-400">
                      <tr>
                        <th className="px-6 py-4">Wallet ID</th>
                        <th className="px-6 py-4">UPI Handle</th>
                        <th className="px-6 py-4">Balance</th>
                        <th className="px-6 py-4">Status</th>
                        <th className="px-6 py-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-700/50 bg-gray-800/30">
                      {walletsData?.content?.map((w: any) => (
                        <tr key={w.id} className="hover:bg-gray-800/50 transition-colors">
                          <td className="px-6 py-4 font-mono text-xs text-gray-400">{w.id}</td>
                          <td className="px-6 py-4 font-medium text-white">{w.upiHandle}</td>
                          <td className="px-6 py-4 text-gray-300 font-mono">
                            {w.currency} {parseFloat(w.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                          </td>
                          <td className="px-6 py-4">
                            <span className={`flex items-center gap-1.5 text-xs font-medium ${w.status === 'ACTIVE' ? 'text-emerald-400' : 'text-cyan-400'}`}>
                              {w.status === 'ACTIVE' ? <Unlock className="h-4 w-4" /> : <Lock className="h-4 w-4" />} 
                              {w.status === 'ACTIVE' ? 'ACTIVE' : 'FROZEN'}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <button
                              onClick={() => toggleWallet.mutate(w.id)}
                              disabled={toggleWallet.isPending}
                              className={`px-4 py-2 rounded-lg text-xs font-medium transition-all ${
                                w.status === 'ACTIVE' ? 'bg-cyan-500/10 text-cyan-400 hover:bg-cyan-500/20' : 'bg-emerald-500/10 text-emerald-400 hover:bg-emerald-500/20'
                              }`}
                            >
                              {w.status === 'ACTIVE' ? 'Freeze' : 'Unfreeze'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
          {activeTab === 'fraud' && (
            <FraudAlertsTab />
          )}

          {activeTab === 'settlements' && (
            <SettlementsTab />
          )}

          {activeTab === 'audit' && (
            <AuditLogsTab />
          )}
        </div>
      </div>
    </div>
  );
};
