import { useState } from 'react';
import { Navbar } from '../components/Navbar';
import { WalletBalance } from '../components/WalletBalance';
import { QrCodeWidget } from '../components/QrCodeWidget';
import { RecentTransactions } from '../components/RecentTransactions';
import { TopUp } from '../components/TopUp';
import { SendMoneyModal } from '../components/SendMoneyModal';
import { Sparkles, Send } from 'lucide-react';

export const Dashboard = () => {
  const [activeTab, setActiveTab] = useState<'wallet' | 'transactions' | 'topup'>('wallet');
  const [modalOpen, setModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-background text-foreground px-4 py-6 md:px-8 relative overflow-hidden">
      {/* Ambient blur orbs */}
      <div className="absolute top-[-15%] left-[-15%] w-[45%] h-[45%] rounded-full bg-primary/10 blur-[130px] pointer-events-none" />
      <div className="absolute bottom-[-15%] right-[-15%] w-[45%] h-[45%] rounded-full bg-violet-600/10 blur-[130px] pointer-events-none" />

      <div className="max-w-6xl mx-auto relative z-10">
        <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

        <div className="animate-fade-in">
          {/* ── Wallet tab ─────────────────────────────────── */}
          {activeTab === 'wallet' && (
            <div className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-start">
                <div className="md:col-span-2 space-y-6">
                  {/* Welcome banner with Send Money CTA */}
                  <div className="glass-panel p-6 rounded-2xl border border-white/5 flex items-center justify-between">
                    <div className="space-y-1">
                      <div className="flex items-center gap-1.5 text-primary text-xs font-semibold uppercase tracking-wider">
                        <Sparkles className="w-3.5 h-3.5" />
                        Fintech Platform Active
                      </div>
                      <h2 className="text-xl font-bold text-white tracking-tight">Your PayFlow Wallet</h2>
                      <p className="text-xs text-muted-foreground">
                        Monitor balances, send money, or scan your QR code instantly.
                      </p>
                    </div>

                    {/* Primary Send Money button */}
                    <button
                      onClick={() => setModalOpen(true)}
                      className="flex items-center gap-2 bg-primary hover:bg-primary/90 text-white text-sm font-semibold px-4 py-2.5 rounded-xl shadow-lg shadow-primary/25 transition-all hover:scale-[1.03] active:scale-[0.97] shrink-0"
                    >
                      <Send className="w-4 h-4" />
                      Send Money
                    </button>
                  </div>

                  <WalletBalance />
                </div>

                <div className="md:col-span-1">
                  <QrCodeWidget />
                </div>
              </div>

              <RecentTransactions />
            </div>
          )}

          {/* ── Transactions tab ─────────────────────────── */}
          {activeTab === 'transactions' && (
            <div className="max-w-3xl mx-auto">
              <RecentTransactions />
            </div>
          )}

          {/* ── Top Up tab ─────────────────────────────────── */}
          {activeTab === 'topup' && (
            <div className="max-w-md mx-auto">
              <TopUp />
            </div>
          )}
        </div>
      </div>

      {/* Global Send Money Modal */}
      {modalOpen && <SendMoneyModal onClose={() => setModalOpen(false)} />}
    </div>
  );
};

export default Dashboard;
