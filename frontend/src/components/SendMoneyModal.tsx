import React, { useState } from 'react';
import api from '../lib/api';
import { useQueryClient } from '@tanstack/react-query';
import {
  X, Send, Search, AlertCircle, Loader2,
  DollarSign, ChevronRight, Wifi, WifiOff,
  CheckCircle2, XCircle, Clock, ShieldCheck, Zap, StickyNote,
} from 'lucide-react';
import { usePaymentWebSocket, type PaymentStatus } from '../hooks/usePaymentWebSocket';

// ─── Types ───────────────────────────────────────────────────────────────────
type Step = 'details' | 'confirm' | 'processing' | 'status';
type SearchType = 'upi' | 'phone';

interface ResolvedUser {
  walletId: string;
  identifier: string; // the phone/upi that was resolved
}

// ─── Status display helpers ───────────────────────────────────────────────────
const STATUS_META: Record<
  PaymentStatus | 'INITIATED',
  { label: string; color: string; icon: React.ReactNode; done: boolean }
> = {
  INITIATED: {
    label: 'Payment Initiated',
    color: 'text-blue-400',
    icon: <Zap className="w-5 h-5" />,
    done: false,
  },
  FRAUD_REVIEW: {
    label: 'Fraud Review',
    color: 'text-yellow-400',
    icon: <ShieldCheck className="w-5 h-5" />,
    done: false,
  },
  PROCESSING: {
    label: 'Processing Transfer',
    color: 'text-primary',
    icon: <Loader2 className="w-5 h-5 animate-spin" />,
    done: false,
  },
  COMPLETED: {
    label: 'Payment Successful',
    color: 'text-green-400',
    icon: <CheckCircle2 className="w-5 h-5" />,
    done: true,
  },
  FAILED: {
    label: 'Payment Failed',
    color: 'text-red-400',
    icon: <XCircle className="w-5 h-5" />,
    done: true,
  },
  REJECTED: {
    label: 'Payment Rejected',
    color: 'text-red-400',
    icon: <XCircle className="w-5 h-5" />,
    done: true,
  },
};

const STATUS_ORDER: PaymentStatus[] = ['INITIATED', 'FRAUD_REVIEW', 'PROCESSING', 'COMPLETED'];

// ─── Main component ───────────────────────────────────────────────────────────
export const SendMoneyModal = ({ onClose }: { onClose: () => void }) => {
  const [step, setStep] = useState<Step>('details');

  // Step 1: Details
  const [searchQuery, setSearchQuery] = useState('');
  const [searchType, setSearchType] = useState<SearchType>('upi');
  const [amount, setAmount] = useState('');
  const [note, setNote] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Step 2: Confirm / resolve
  const [resolving, setResolving] = useState(false);
  const [resolvedUser, setResolvedUser] = useState<ResolvedUser | null>(null);
  const [resolveError, setResolveError] = useState<string | null>(null);

  // Step 3/4: Payment result
  const [paymentId, setPaymentId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [currentStatus, setCurrentStatus] = useState<PaymentStatus>('INITIATED');
  const [statusHistory, setStatusHistory] = useState<PaymentStatus[]>([]);

  const queryClient = useQueryClient();

  // We don't use useQuery here without a queryFn anymore.
  // Instead, we fetch the balance synchronously from the cache when validating.

  // WebSocket subscription — only active after payment is initiated
  const { connected } = usePaymentWebSocket({
    paymentId: step === 'processing' || step === 'status' ? paymentId : null,
    onStatusUpdate: (status) => {
      setCurrentStatus(status);
      setStatusHistory((prev) =>
        prev.includes(status) ? prev : [...prev, status]
      );
      if (status === 'COMPLETED' || status === 'FAILED' || status === 'REJECTED') {
        setStep('status');
        queryClient.invalidateQueries({ queryKey: ['balance'] });
        queryClient.invalidateQueries({ queryKey: ['transactions'] });
      }
    },
  });

  // ── Validation (Step 1) ─────────────────────────────────────────────────────
  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    if (!searchQuery.trim()) {
      errors.recipient = 'Recipient is required.';
    }

    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum)) {
      errors.amount = 'Amount is required.';
    } else if (amountNum <= 0) {
      errors.amount = 'Amount must be greater than ₹0.';
    } else if (amountNum > 10000) {
      errors.amount = 'Maximum amount is ₹10,000.';
    } else {
      const balanceData = queryClient.getQueryData<{ balance: number; currency: string }>(['balance']);
      if (balanceData && amountNum > balanceData.balance) {
        errors.amount = `Insufficient balance. Current balance: ₹${balanceData.balance.toFixed(2)}.`;
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // ── Proceed to Confirm (Step 2): resolve recipient ──────────────────────────
  const handleProceedToConfirm = async () => {
    if (!validate()) return;

    setResolving(true);
    setResolveError(null);
    setResolvedUser(null);

    try {
      const params = searchType === 'upi'
        ? { upi: searchQuery.trim() }
        : { phone: searchQuery.trim() };

      const response = await api.get('/users/resolve', { params });
      if (response.data?.walletId) {
        setResolvedUser({ walletId: response.data.walletId, identifier: searchQuery.trim() });
        setStep('confirm');
      } else {
        setResolveError('Could not find a wallet for this recipient.');
      }
    } catch (err: any) {
      if (err.response?.data?.error) {
        setResolveError(err.response.data.error);
      } else if (err.response?.status === 404) {
        setResolveError('No user found with this UPI / Phone. Please double-check.');
      } else {
        setResolveError('Lookup failed. Please try again.');
      }
    } finally {
      setResolving(false);
    }
  };

  // ── Confirm Payment (Step 3): POST /payments/initiate ──────────────────────
  const handleConfirmPayment = async () => {
    if (!resolvedUser) return;

    setSubmitting(true);
    setSubmitError(null);

    // Generate idempotency key using native crypto
    const idempotencyKey = crypto.randomUUID();

    try {
      const response = await api.post(
        '/payments/initiate',
        {
          receiverWalletId: resolvedUser.walletId,
          amount: parseFloat(amount),
        },
        {
          headers: { 'Idempotency-Key': idempotencyKey },
        }
      );

      const initiated = response.data;
      setPaymentId(initiated.id);
      setCurrentStatus('INITIATED');
      setStatusHistory(['INITIATED']);
      setStep('processing');
    } catch (err: any) {
      if (err.response?.data?.message) {
        setSubmitError(err.response.data.message);
      } else if (err.response?.data?.error) {
        setSubmitError(err.response.data.error);
      } else {
        setSubmitError('Failed to initiate payment. Check your balance and try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleClose = () => {
    if (step === 'processing') return; // block close while processing
    onClose();
  };

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4"
        onClick={step !== 'processing' ? handleClose : undefined}
      >
        {/* Modal container */}
        <div
          className="relative w-full max-w-md glass-panel rounded-2xl shadow-2xl border border-white/10 animate-fade-in overflow-hidden"
          onClick={(e) => e.stopPropagation()}
        >
          {/* Modal header */}
          <div className="flex items-center justify-between p-6 border-b border-white/5">
            <div>
              <h2 className="text-lg font-bold text-white tracking-tight">Send Money</h2>
              <p className="text-[11px] text-muted-foreground mt-0.5">
                {step === 'details' && 'Enter recipient & amount'}
                {step === 'confirm' && 'Review before confirming'}
                {step === 'processing' && 'Tracking payment status...'}
                {step === 'status' && 'Payment complete'}
              </p>
            </div>

            {step !== 'processing' && (
              <button
                onClick={handleClose}
                className="w-8 h-8 rounded-lg bg-white/5 border border-white/10 flex items-center justify-center text-white/60 hover:text-white hover:bg-white/10 transition-all"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Step indicator */}
          <div className="flex px-6 pt-4 gap-2">
            {(['details', 'confirm', 'processing'] as Step[]).map((s, i) => (
              <div
                key={s}
                className={`h-1 flex-1 rounded-full transition-all ${
                  ['details', 'confirm', 'processing', 'status'].indexOf(step) >= i
                    ? 'bg-primary'
                    : 'bg-white/10'
                }`}
              />
            ))}
          </div>

          {/* ── STEP 1: Details ─────────────────────────────────────────── */}
          {step === 'details' && (
            <div className="p-6 space-y-5">
              {/* Recipient */}
              <div>
                <label className="block text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  Recipient
                </label>
                <div className="flex gap-2">
                  <select
                    value={searchType}
                    onChange={(e) => setSearchType(e.target.value as SearchType)}
                    className="bg-white/5 border border-white/10 rounded-xl px-3 text-xs text-white outline-none h-11"
                  >
                    <option value="upi" className="bg-background">UPI</option>
                    <option value="phone" className="bg-background">Phone</option>
                  </select>
                  <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/60" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        setFieldErrors((prev) => ({ ...prev, recipient: '' }));
                      }}
                      placeholder={searchType === 'upi' ? 'user@upi' : '+91 9876543210'}
                      className="w-full pl-9 pr-4 py-2.5 h-11 rounded-xl glass-input text-white text-sm placeholder:text-muted-foreground/50 outline-none"
                    />
                  </div>
                </div>
                {fieldErrors.recipient && (
                  <p className="mt-1.5 text-xs text-red-400 flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" /> {fieldErrors.recipient}
                  </p>
                )}
              </div>

              {/* Amount */}
              <div>
                <label className="block text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  Amount (₹1 – ₹10,000)
                </label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/60" />
                  <input
                    type="number"
                    step="0.01"
                    min="1"
                    max="10000"
                    value={amount}
                    onChange={(e) => {
                      setAmount(e.target.value);
                      setFieldErrors((prev) => ({ ...prev, amount: '' }));
                    }}
                    placeholder="0.00"
                    className="w-full pl-9 pr-4 py-2.5 h-11 rounded-xl glass-input text-white text-sm placeholder:text-muted-foreground/50 outline-none"
                  />
                </div>
                {fieldErrors.amount && (
                  <p className="mt-1.5 text-xs text-red-400 flex items-center gap-1">
                    <AlertCircle className="w-3.5 h-3.5" /> {fieldErrors.amount}
                  </p>
                )}
              </div>

              {/* Note (optional) */}
              <div>
                <label className="block text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  Note <span className="normal-case font-normal">(optional)</span>
                </label>
                <div className="relative">
                  <StickyNote className="absolute left-3 top-3 w-4 h-4 text-muted-foreground/60" />
                  <textarea
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Add a short note..."
                    rows={2}
                    maxLength={120}
                    className="w-full pl-9 pr-4 py-2.5 rounded-xl glass-input text-white text-sm placeholder:text-muted-foreground/50 outline-none resize-none"
                  />
                </div>
              </div>

              {resolveError && (
                <div className="p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  {resolveError}
                </div>
              )}

              <button
                onClick={handleProceedToConfirm}
                disabled={resolving}
                className="w-full py-3 rounded-xl bg-primary hover:bg-primary/95 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-60 shadow-lg shadow-primary/20"
              >
                {resolving ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <>
                    Continue
                    <ChevronRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          )}

          {/* ── STEP 2: Confirm ─────────────────────────────────────────── */}
          {step === 'confirm' && resolvedUser && (
            <div className="p-6 space-y-4">
              <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden divide-y divide-white/5 text-sm">
                <div className="flex justify-between items-center px-4 py-3">
                  <span className="text-muted-foreground text-xs">Recipient</span>
                  <span className="text-white font-semibold">{resolvedUser.identifier}</span>
                </div>
                <div className="flex justify-between items-center px-4 py-3">
                  <span className="text-muted-foreground text-xs">Wallet ID</span>
                  <span className="text-white font-mono text-[11px] truncate max-w-[160px]">
                    {resolvedUser.walletId}
                  </span>
                </div>
                <div className="flex justify-between items-center px-4 py-3">
                  <span className="text-muted-foreground text-xs">Amount</span>
                  <span className="text-white font-bold text-base">₹{parseFloat(amount).toFixed(2)}</span>
                </div>
                {note && (
                  <div className="flex justify-between items-start px-4 py-3">
                    <span className="text-muted-foreground text-xs shrink-0">Note</span>
                    <span className="text-white text-xs text-right max-w-[200px] leading-relaxed">{note}</span>
                  </div>
                )}
              </div>

              {submitError && (
                <div className="p-3 rounded-xl bg-destructive/10 border border-destructive/20 text-destructive text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  {submitError}
                </div>
              )}

              <div className="flex gap-3">
                <button
                  onClick={() => { setStep('details'); setSubmitError(null); }}
                  className="flex-1 py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white font-semibold text-sm transition-all"
                >
                  Back
                </button>
                <button
                  onClick={handleConfirmPayment}
                  disabled={submitting}
                  className="flex-[2] py-3 rounded-xl bg-primary hover:bg-primary/95 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-all active:scale-[0.98] disabled:opacity-60 shadow-lg shadow-primary/20"
                >
                  {submitting ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <>
                      <Send className="w-4 h-4" />
                      Confirm Payment
                    </>
                  )}
                </button>
              </div>
            </div>
          )}

          {/* ── STEP 3: Processing + WebSocket Live Tracker ──────────────── */}
          {(step === 'processing' || step === 'status') && (
            <div className="p-6 space-y-6">
              {/* WS connection indicator */}
              <div className="flex items-center justify-between text-xs">
                <span className="text-muted-foreground">Live tracking</span>
                <div className={`flex items-center gap-1.5 font-medium ${connected ? 'text-green-400' : 'text-muted-foreground'}`}>
                  {connected ? <Wifi className="w-3.5 h-3.5" /> : <WifiOff className="w-3.5 h-3.5" />}
                  {connected ? 'WebSocket Connected' : 'Connecting...'}
                </div>
              </div>

              {/* Payment ID */}
              {paymentId && (
                <div className="bg-black/40 border border-white/5 rounded-xl px-4 py-3 text-xs font-mono text-muted-foreground flex items-center gap-2">
                  <Clock className="w-3.5 h-3.5 shrink-0" />
                  <span className="truncate">{paymentId}</span>
                </div>
              )}

              {/* Timeline */}
              <div className="space-y-3">
                {STATUS_ORDER.map((s, idx) => {
                  const meta = STATUS_META[s];
                  const isReached = statusHistory.includes(s);
                  const isCurrent = currentStatus === s && step !== 'status';
                  const isFailed = currentStatus === 'FAILED';
                  void isFailed; // used for type narrowing only

                  return (
                    <div key={s} className="flex items-center gap-3">
                      <div
                        className={`w-8 h-8 rounded-full border flex items-center justify-center shrink-0 transition-all ${
                          isReached
                            ? `${meta.color} border-current bg-current/10`
                            : 'border-white/10 text-white/20'
                        }`}
                      >
                        {isReached ? (
                          s === 'PROCESSING' && (statusHistory.includes('COMPLETED') || currentStatus === 'COMPLETED') ? (
                            <CheckCircle2 className="w-5 h-5" />
                          ) : s === 'PROCESSING' && (statusHistory.includes('FAILED') || currentStatus === 'FAILED' || statusHistory.includes('REJECTED') || currentStatus === 'REJECTED') ? (
                            <XCircle className="w-5 h-5" />
                          ) : (
                            meta.icon
                          )
                        ) : (
                          <span className="text-xs">{idx + 1}</span>
                        )}
                      </div>
                      <div className="flex-1">
                        <p className={`text-sm font-medium ${isReached ? 'text-white' : 'text-white/30'}`}>
                          {meta.label}
                        </p>
                      </div>
                      {isCurrent && <div className="w-2 h-2 rounded-full bg-primary animate-pulse shrink-0" />}
                    </div>
                  );
                })}

                {/* FAILED / REJECTED state */}
                {(currentStatus === 'FAILED' || currentStatus === 'REJECTED') && (
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full border border-red-500/30 text-red-400 bg-red-500/10 flex items-center justify-center shrink-0">
                      <XCircle className="w-4 h-4" />
                    </div>
                    <p className="text-sm font-medium text-red-400">{STATUS_META[currentStatus].label}</p>
                  </div>
                )}
              </div>

              {/* Final actions */}
              {step === 'status' && (
                <button
                  onClick={onClose}
                  className="w-full py-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-white font-semibold text-sm transition-all"
                >
                  {currentStatus === 'COMPLETED' ? '✓ Done' : '✕ Close'}
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  );
};
