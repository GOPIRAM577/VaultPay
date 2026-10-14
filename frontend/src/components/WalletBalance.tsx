import { useQuery } from '@tanstack/react-query';
import api from '../lib/api';
import { Wallet, RefreshCw, AlertCircle } from 'lucide-react';

interface BalanceData {
  balance: number;
  currency: string;
}

export const WalletBalance = () => {
  const { data, isLoading, error, refetch, isFetching } = useQuery<BalanceData>({
    queryKey: ['balance'],
    queryFn: async () => {
      const response = await api.get('/wallets/me/balance');
      return response.data;
    },
    refetchInterval: 30000, // Refetch every 30 seconds
  });

  const formatBalance = (amount: number, currency: string) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency || 'USD',
    }).format(amount);
  };

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 relative overflow-hidden flex flex-col justify-between min-h-[180px]">
      {/* Decorative background glow */}
      <div className="absolute top-0 right-0 w-32 h-32 bg-primary/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex items-center justify-between z-10">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-lg bg-primary/15 border border-primary/20 text-primary flex items-center justify-center">
            <Wallet className="w-5 h-5" />
          </div>
          <span className="text-sm font-medium text-muted-foreground">Wallet Balance</span>
        </div>
        <button
          onClick={() => refetch()}
          disabled={isLoading || isFetching}
          className="w-8 h-8 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-white/70 hover:text-white transition-all hover:bg-white/10 disabled:opacity-50"
          title="Refresh Balance"
        >
          <RefreshCw className={`w-4 h-4 ${isFetching ? 'animate-spin' : ''}`} />
        </button>
      </div>

      <div className="mt-4 z-10">
        {isLoading ? (
          <div className="space-y-2 animate-pulse">
            <div className="h-9 w-40 bg-white/10 rounded-lg" />
            <div className="h-4 w-20 bg-white/5 rounded" />
          </div>
        ) : error ? (
          <div className="flex items-center gap-2 text-destructive mt-1">
            <AlertCircle className="w-5 h-5 shrink-0" />
            <span className="text-xs font-medium">Failed to load balance</span>
          </div>
        ) : (
          <div>
            <div className="text-3xl font-extrabold text-white tracking-tight">
              {formatBalance(data?.balance ?? 0, data?.currency ?? 'USD')}
            </div>
            <div className="text-[11px] text-muted-foreground mt-1 flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500" />
              Auto-syncs every 30s
            </div>
          </div>
        )}
      </div>

      <div className="border-t border-white/5 pt-4 mt-4 flex items-center justify-between text-xs text-muted-foreground z-10">
        <span>Account Currency</span>
        <span className="font-semibold text-white uppercase">{data?.currency ?? 'USD'}</span>
      </div>
    </div>
  );
};
