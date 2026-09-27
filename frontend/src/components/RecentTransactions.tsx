import { useInfiniteQuery } from '@tanstack/react-query';
import api from '../lib/api';
import { ArrowUpRight, ArrowDownLeft, Calendar, Loader2, AlertCircle } from 'lucide-react';

interface LedgerEntry {
  id: string;
  paymentId: string;
  walletId: string;
  seq: number;
  entryType: 'CREDIT' | 'DEBIT';
  amount: number;
  description: string;
  createdAt: string;
}

interface TransactionPage {
  transactions: LedgerEntry[];
  nextCursor: number;
  hasMore: boolean;
}

export const RecentTransactions = () => {
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    isLoading,
    error,
  } = useInfiniteQuery<TransactionPage>({
    queryKey: ['transactions'],
    queryFn: async ({ pageParam = 0 }) => {
      const response = await api.get('/wallets/me/transactions', {
        params: {
          cursor: pageParam,
          limit: 10,
        },
      });
      return response.data;
    },
    initialPageParam: 0,
    getNextPageParam: (lastPage) => {
      return lastPage.hasMore ? lastPage.nextCursor : undefined;
    },
  });

  const formatAmount = (amount: number, type: 'CREDIT' | 'DEBIT') => {
    const formatted = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(Math.abs(amount));

    return type === 'CREDIT' ? `+${formatted}` : `-${formatted}`;
  };

  const formatDate = (dateStr: string) => {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch (e) {
      return dateStr;
    }
  };

  const allTransactions = data?.pages.flatMap((page) => page.transactions) ?? [];

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col h-full relative">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-lg font-bold text-white tracking-tight">Recent Transactions</h2>
        <span className="text-xs text-muted-foreground bg-white/5 border border-white/10 px-2.5 py-1 rounded-lg">
          Total loaded: {allTransactions.length}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 max-h-[360px] pr-1">
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-12 gap-2">
            <Loader2 className="w-8 h-8 animate-spin text-primary" />
            <span className="text-xs text-muted-foreground">Loading transactions...</span>
          </div>
        ) : error ? (
          <div className="flex items-center justify-center gap-3 py-12 text-destructive">
            <AlertCircle className="w-6 h-6 shrink-0" />
            <span className="text-sm font-medium">Failed to load transaction history</span>
          </div>
        ) : allTransactions.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center text-muted-foreground">
            <Calendar className="w-10 h-10 mb-2 opacity-50" />
            <span className="text-sm">No transactions yet.</span>
            <span className="text-xs opacity-75 mt-0.5">Top up your wallet to get started.</span>
          </div>
        ) : (
          <div className="divide-y divide-white/5">
            {allTransactions.map((tx) => {
              const isCredit = tx.entryType === 'CREDIT';
              return (
                <div key={tx.id} className="flex items-center justify-between py-3.5 first:pt-0 last:pb-0">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center border shrink-0 ${
                        isCredit
                          ? 'bg-green-500/10 border-green-500/20 text-green-400'
                          : 'bg-red-500/10 border-red-500/20 text-red-400'
                      }`}
                    >
                      {isCredit ? (
                        <ArrowDownLeft className="w-5 h-5" />
                      ) : (
                        <ArrowUpRight className="w-5 h-5" />
                      )}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-white max-w-[200px] md:max-w-[280px] truncate">
                        {tx.description || (isCredit ? 'Credit Refill' : 'Payment Outflow')}
                      </p>
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {formatDate(tx.createdAt)}
                      </p>
                    </div>
                  </div>

                  <div className="text-right">
                    <span
                      className={`text-sm font-bold tracking-tight ${
                        isCredit ? 'text-green-400' : 'text-red-400'
                      }`}
                    >
                      {formatAmount(tx.amount, tx.entryType)}
                    </span>
                    <p className="text-[10px] text-muted-foreground font-mono mt-0.5">
                      seq: {tx.seq}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {hasNextPage && (
        <div className="mt-6 pt-4 border-t border-white/5 flex justify-center">
          <button
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
            className="w-full py-2.5 px-4 rounded-xl bg-white/5 hover:bg-white/10 text-white font-medium text-xs border border-white/10 flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50"
          >
            {isFetchingNextPage ? (
              <Loader2 className="w-4 h-4 animate-spin text-primary" />
            ) : (
              'Load More Transactions'
            )}
          </button>
        </div>
      )}
    </div>
  );
};
