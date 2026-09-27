import { useAuthStore } from '../store/useAuthStore';
import { useNavigate } from 'react-router-dom';
import { LogOut, Wallet, ArrowLeftRight } from 'lucide-react';

interface NavbarProps {
  activeTab?: 'wallet' | 'transactions' | 'topup';
  setActiveTab?: (tab: 'wallet' | 'transactions' | 'topup') => void;
}

export const Navbar = ({ activeTab, setActiveTab }: NavbarProps) => {
  const { user, clearAuth } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  const getInitials = (nameStr?: string) => {
    if (!nameStr) return 'U';
    return nameStr.split(' ').map((n) => n[0]).join('').slice(0, 2).toUpperCase();
  };

  return (
    <nav className="glass-panel sticky top-4 z-40 rounded-2xl border border-white/5 px-6 py-4 mb-8 flex flex-col md:flex-row items-center justify-between gap-4 shadow-xl">
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-white font-extrabold text-lg shadow-lg shadow-primary/20">
          P
        </div>
        <div>
          <span className="text-base font-bold text-white tracking-tight">PayFlow</span>
          <span className="text-[10px] block text-primary font-semibold uppercase tracking-wider">Dashboard</span>
        </div>
      </div>

      {/* Nav links / tab controllers */}
      <div className="flex items-center bg-white/5 border border-white/10 rounded-xl p-1 shrink-0 w-full md:w-auto">
        {setActiveTab && (
          <>
            <button
              onClick={() => setActiveTab('wallet')}
              className={`flex-1 md:flex-none px-4 py-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all ${
                activeTab === 'wallet'
                  ? 'bg-primary text-white shadow'
                  : 'text-muted-foreground hover:text-white hover:bg-white/5'
              }`}
            >
              <Wallet className="w-3.5 h-3.5" />
              Wallet
            </button>

            <button
              onClick={() => setActiveTab('transactions')}
              className={`flex-1 md:flex-none px-4 py-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all ${
                activeTab === 'transactions'
                  ? 'bg-primary text-white shadow'
                  : 'text-muted-foreground hover:text-white hover:bg-white/5'
              }`}
            >
              <ArrowLeftRight className="w-3.5 h-3.5" />
              Transactions
            </button>

            <button
              onClick={() => setActiveTab('topup')}
              className={`flex-1 md:flex-none px-4 py-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all ${
                activeTab === 'topup'
                  ? 'bg-primary text-white shadow'
                  : 'text-muted-foreground hover:text-white hover:bg-white/5'
              }`}
            >
              <Wallet className="w-3.5 h-3.5" />
              Top Up
            </button>
          </>
        )}
        
        {user?.role === 'ADMIN' && (
          <button
            onClick={() => navigate('/admin')}
            className="flex-1 md:flex-none px-4 py-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all text-purple-400 hover:text-purple-300 hover:bg-purple-900/20 ml-2 border border-purple-900/30"
          >
            Admin Panel
          </button>
        )}

        <button
          onClick={() => navigate('/dashboard')}
          className="flex-1 md:flex-none px-4 py-2 text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all text-emerald-400 hover:text-emerald-300 hover:bg-emerald-900/20 ml-2"
        >
          Dashboard
        </button>
      </div>

      {/* User profile & Logout */}
      <div className="flex items-center gap-4 w-full md:w-auto justify-between md:justify-end border-t md:border-t-0 border-white/5 pt-3 md:pt-0">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-primary/20 border border-primary/30 text-primary flex items-center justify-center font-bold text-xs">
            {getInitials(user?.name)}
          </div>
          <div className="text-left">
            <p className="text-xs font-bold text-white leading-none">{user?.name || 'Payflow User'}</p>
            <p className="text-[9px] text-muted-foreground mt-0.5 leading-none">{user?.email}</p>
          </div>
        </div>

        <button
          onClick={handleLogout}
          className="w-8 h-8 rounded-lg bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400 hover:bg-red-500/20 transition-all hover:scale-[1.02] shrink-0"
          title="Sign Out"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </nav>
  );
};
