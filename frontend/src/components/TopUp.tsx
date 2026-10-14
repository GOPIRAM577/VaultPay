import React, { useState } from 'react';
import api from '../lib/api';
import { Wallet, Check, AlertCircle, Loader2, DollarSign } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';

export const TopUp = () => {
  const [topupAmount, setTopupAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const queryClient = useQueryClient();

  const handleTopup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!topupAmount) return;

    setSubmitting(true);
    setActionError(null);
    setSuccessMessage(null);

    try {
      await api.post('/wallets/topup', {
        amount: parseFloat(topupAmount),
      });

      setSuccessMessage(`Successfully refilled $${topupAmount} in wallet!`);
      setTopupAmount('');
      
      // Invalidate queries to refresh balance & transaction list
      queryClient.invalidateQueries({ queryKey: ['balance'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    } catch (err: any) {
      console.error(err);
      if (err.response?.data?.error) {
        setActionError(err.response.data.error);
      } else {
        setActionError('Failed to top up wallet. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="glass-panel p-6 rounded-2xl border border-white/5 flex flex-col h-full">
      <div className="flex items-center gap-2 mb-6">
        <div className="w-8 h-8 rounded-lg bg-primary/20 flex items-center justify-center text-primary">
          <Wallet className="w-4 h-4" />
        </div>
        <div>
          <h3 className="text-sm font-bold text-white">Top Up Wallet</h3>
          <p className="text-xs text-muted-foreground">Add funds to your PayFlow balance</p>
        </div>
      </div>

      {successMessage && (
        <div className="mb-5 p-4 rounded-xl bg-green-500/10 border border-green-500/20 text-green-400 text-sm flex items-start gap-2.5">
          <Check className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Success</p>
            <p className="opacity-90">{successMessage}</p>
          </div>
        </div>
      )}

      {actionError && (
        <div className="mb-5 p-4 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-sm flex items-start gap-2.5">
          <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Operation Failed</p>
            <p className="opacity-90">{actionError}</p>
          </div>
        </div>
      )}

      <form onSubmit={handleTopup} className="space-y-4">
        <div>
          <label className="block text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-1.5">
            Refill Amount (USD)
          </label>
          <div className="relative">
            <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <input
              type="number"
              step="0.01"
              required
              min="0.01"
              value={topupAmount}
              onChange={(e) => setTopupAmount(e.target.value)}
              placeholder="0.00"
              className="w-full pl-8 pr-4 py-2.5 rounded-xl glass-input text-white text-xs placeholder:text-muted-foreground/50 outline-none"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={submitting || !topupAmount}
          className="w-full py-3 px-4 rounded-xl bg-primary hover:bg-primary/95 text-white font-medium text-xs flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none shadow-lg shadow-primary/20 hover:shadow-primary/30"
        >
          {submitting ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <>
              <Wallet className="w-3.5 h-3.5" />
              Initiate Refill
            </>
          )}
        </button>
      </form>
    </div>
  );
};
