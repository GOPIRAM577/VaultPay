import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useAuthStore } from '../../store/useAuthStore';
import { Activity, ShieldAlert, AlertTriangle } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

const API_URL = 'http://localhost:8080';

export const FraudAlertsTab = () => {
  const token = useAuthStore((state) => state.token);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-fraud-alerts'],
    queryFn: async () => {
      const res = await axios.get(`${API_URL}/admin/fraud/alerts?page=0&size=50`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      return res.data;
    },
    refetchInterval: 30000,
  });

  if (isLoading) {
    return <div className="flex justify-center p-12"><Activity className="h-8 w-8 animate-spin text-red-500" /></div>;
  }

  const alerts = data?.content || [];

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center gap-2 mb-4">
        <ShieldAlert className="h-5 w-5 text-red-400" />
        <h2 className="text-xl font-semibold text-white">Fraud Alerts Feed</h2>
      </div>

      {alerts.length === 0 ? (
        <div className="bg-gray-800/30 border border-gray-700/50 rounded-xl p-8 text-center text-gray-400">
          No fraud alerts found. The system is clean!
        </div>
      ) : (
        <div className="grid gap-6">
          {alerts.map((alert: any) => {
            const rulesTriggered = alert.fraudAssessment?.rulesTriggered || [];
            
            // Format data for recharts
            const chartData = rulesTriggered.map((rule: any) => ({
              name: rule.ruleName.replace('_RULE', '').replace('_', ' '),
              score: parseFloat(rule.contribution.toFixed(2))
            }));

            return (
              <div key={alert.payment.id} className="bg-gray-800/40 border border-red-500/20 rounded-xl overflow-hidden shadow-lg">
                <div className="bg-red-950/30 p-4 border-b border-red-500/10 flex justify-between items-center">
                  <div>
                    <div className="flex items-center gap-2">
                      <AlertTriangle className="h-4 w-4 text-red-400" />
                      <span className="font-mono text-sm text-red-300">Payment {alert.payment.id.split('-')[0]}</span>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-red-500/20 text-red-400">
                        {alert.payment.status}
                      </span>
                    </div>
                    <div className="text-sm text-gray-400 mt-1">
                      Amount: {alert.payment.amount} INR | Total Risk Score: {alert.fraudAssessment?.riskScore}
                    </div>
                  </div>
                  <div className="text-xs text-gray-500">
                    {new Date(alert.payment.createdAt).toLocaleString()}
                  </div>
                </div>

                <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* Chart Section */}
                  <div className="bg-gray-900/50 p-4 rounded-xl border border-gray-800">
                    <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4">Risk Score Breakdown</h3>
                    <div className="h-48 w-full">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={chartData} layout="vertical" margin={{ top: 5, right: 30, left: 40, bottom: 5 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="#374151" horizontal={false} />
                          <XAxis type="number" domain={[0, 1]} tick={{ fill: '#9ca3af', fontSize: 12 }} />
                          <YAxis dataKey="name" type="category" tick={{ fill: '#9ca3af', fontSize: 10 }} width={80} />
                          <Tooltip 
                            contentStyle={{ backgroundColor: '#1f2937', border: 'none', borderRadius: '8px', color: '#fff' }}
                            itemStyle={{ color: '#f87171' }}
                          />
                          <Bar dataKey="score" fill="#ef4444" radius={[0, 4, 4, 0]} barSize={20} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>

                  {/* AI Narrative Section */}
                  <div className="bg-gray-900/50 p-4 rounded-xl border border-gray-800 flex flex-col">
                    <h3 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">AI Assessment Narrative</h3>
                    <div className="flex-1 overflow-y-auto pr-2 custom-scrollbar">
                      <p className="text-sm text-gray-300 leading-relaxed">
                        {alert.fraudAssessment?.aiNarrative || "No AI narrative available for this assessment."}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
