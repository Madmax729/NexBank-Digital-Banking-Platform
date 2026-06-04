import { useEffect, useState } from 'react';
import api from '../../lib/api';
import { Clock, ArrowUpRight, ArrowDownRight, Search, Filter } from 'lucide-react';

export default function HistoryPage() {
  const [transactions, setTransactions] = useState<any[]>([]);
  const [accounts, setAccounts] = useState<any[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    api.get('/user/accounts').then((res) => {
      const accts = res.data.data || [];
      setAccounts(accts);
      if (accts.length > 0) {
        const ids = accts.map((a: any) => a.id).join(',');
        api.get(`/user/history?accountIds=${ids}`).then((r) => {
          setTransactions(r.data.data?.content || []);
        }).catch(() => {});
      }
    }).catch(() => {});
  }, []);

  const getStatusStyle = (status: string) => {
    const styles: Record<string, string> = {
      COMPLETED: 'bg-[var(--success)]/10 text-[var(--success)]',
      FAILED: 'bg-[var(--danger)]/10 text-[var(--danger)]',
      REVERSED: 'bg-[var(--warning)]/10 text-[var(--warning)]',
      PROCESSING: 'bg-[var(--info)]/10 text-[var(--info)]',
      PENDING: 'bg-[var(--warning)]/10 text-[var(--warning)]',
    };
    return styles[status] || styles.PENDING;
  };

  const filtered = transactions.filter((txn: any) =>
    !search || txn.referenceNumber?.toLowerCase().includes(search.toLowerCase()) ||
    txn.description?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">Transactions</h1>
        <p className="text-sm text-[var(--text-muted)] mt-1">Your complete transaction history</p>
      </div>

      {/* Search */}
      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
          <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
            className="input-field pl-10" placeholder="Search by reference or description..." />
        </div>
        <button className="btn-ghost flex items-center gap-2 text-sm">
          <Filter size={14} /> Filters
        </button>
      </div>

      {/* Transactions List */}
      {filtered.length === 0 ? (
        <div className="glass-card p-12 text-center">
          <div className="w-16 h-16 rounded-2xl bg-[var(--bg-elevated)] flex items-center justify-center mx-auto mb-4">
            <Clock className="text-[var(--text-muted)]" size={24} />
          </div>
          <h3 className="font-semibold mb-1">No transactions</h3>
          <p className="text-sm text-[var(--text-muted)]">{search ? 'No results match your search' : 'Make your first transfer to see it here'}</p>
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          {/* Header */}
          <div className="grid grid-cols-12 gap-4 px-5 py-3 border-b border-[var(--border)] text-[10px] font-semibold text-[var(--text-muted)] uppercase tracking-wider">
            <div className="col-span-1"></div>
            <div className="col-span-4">Transaction</div>
            <div className="col-span-2">Type</div>
            <div className="col-span-2 text-right">Amount</div>
            <div className="col-span-2">Status</div>
            <div className="col-span-1 text-right">Date</div>
          </div>

          {/* Rows */}
          {filtered.map((txn: any, i: number) => {
            const isOutgoing = accounts.some((a: any) => a.id === txn.sourceAccountId);
            return (
              <div key={txn.id}
                className="grid grid-cols-12 gap-4 px-5 py-4 items-center border-b border-[var(--border)]/50 hover:bg-[var(--bg-elevated)]/30 transition-colors animate-slide-up"
                style={{ animationDelay: `${i * 0.03}s` }}>
                <div className="col-span-1">
                  <div className={`w-9 h-9 rounded-xl flex items-center justify-center ${isOutgoing ? 'bg-[var(--danger)]/10' : 'bg-[var(--success)]/10'}`}>
                    {isOutgoing ? <ArrowUpRight size={16} className="text-[var(--danger)]" /> : <ArrowDownRight size={16} className="text-[var(--success)]" />}
                  </div>
                </div>
                <div className="col-span-4">
                  <p className="text-sm font-medium truncate">{txn.description || txn.type}</p>
                  <p className="text-xs text-[var(--text-muted)] font-mono">{txn.referenceNumber}</p>
                </div>
                <div className="col-span-2">
                  <span className="badge bg-[var(--bg-elevated)] text-[var(--text-secondary)]">{txn.type}</span>
                </div>
                <div className="col-span-2 text-right">
                  <p className={`text-sm font-bold ${isOutgoing ? 'text-[var(--danger)]' : 'text-[var(--success)]'}`}>
                    {isOutgoing ? '-' : '+'}₹{parseFloat(txn.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </p>
                </div>
                <div className="col-span-2">
                  <span className={`badge ${getStatusStyle(txn.status)}`}>{txn.status}</span>
                </div>
                <div className="col-span-1 text-right">
                  <p className="text-xs text-[var(--text-muted)]">{new Date(txn.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' })}</p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
