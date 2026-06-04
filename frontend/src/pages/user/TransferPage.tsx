import { useEffect, useState } from 'react';
import api from '../../lib/api';
import { ArrowLeftRight, ArrowRight, CheckCircle2, AlertCircle } from 'lucide-react';

export default function TransferPage() {
  const [accounts, setAccounts] = useState<any[]>([]);
  const [form, setForm] = useState({ sourceAccountId: '', destinationAccountId: '', amount: '', description: '' });
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/user/accounts').then((res) => setAccounts(res.data.data || [])).catch(() => {});
  }, []);

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault(); setLoading(true); setError(''); setResult(null);
    try {
      const idempotencyKey = crypto.randomUUID();
      const res = await api.post('/user/transfer', form, { headers: { 'X-Idempotency-Key': idempotencyKey } });
      setResult(res.data.data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Transfer failed');
    } finally { setLoading(false); }
  };

  const selectedAccount = accounts.find((a: any) => a.id === form.sourceAccountId);

  return (
    <div className="max-w-xl mx-auto space-y-6">
      <div className="text-center mb-8">
        <div className="w-14 h-14 rounded-2xl bg-[var(--accent)]/10 flex items-center justify-center mx-auto mb-4">
          <ArrowLeftRight className="text-[var(--accent)]" size={24} />
        </div>
        <h1 className="text-2xl font-bold tracking-tight">Send Money</h1>
        <p className="text-sm text-[var(--text-muted)] mt-1">Instant, secure transfers between accounts</p>
      </div>

      {/* Success */}
      {result && (
        <div className="glass-card p-6 text-center animate-slide-up">
          <div className="w-14 h-14 rounded-full bg-[var(--success)]/10 flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="text-[var(--success)]" size={28} />
          </div>
          <h3 className="text-lg font-bold mb-1">Transfer Complete</h3>
          <p className="text-sm text-[var(--text-muted)] mb-3">Ref: <span className="font-mono text-[var(--accent)]">{result.referenceNumber}</span></p>
          <p className="text-3xl font-bold text-[var(--success)]">₹{parseFloat(result.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
          <button onClick={() => { setResult(null); setForm({ sourceAccountId: '', destinationAccountId: '', amount: '', description: '' }); }}
            className="btn-ghost mt-5 text-sm">New Transfer</button>
        </div>
      )}

      {/* Form */}
      {!result && (
        <form onSubmit={handleTransfer} className="glass-card p-6 space-y-5 animate-slide-up">
          {error && (
            <div className="flex items-center gap-3 p-4 rounded-2xl bg-[var(--danger)]/5 border border-[var(--danger)]/10">
              <AlertCircle size={18} className="text-[var(--danger)] flex-shrink-0" />
              <p className="text-sm text-[var(--danger)]">{error}</p>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">From Account</label>
            <select value={form.sourceAccountId} onChange={(e) => setForm({...form, sourceAccountId: e.target.value})} required className="input-field">
              <option value="">Select account</option>
              {accounts.filter((a: any) => a.status === 'ACTIVE').map((a: any) => (
                <option key={a.id} value={a.id}>
                  {a.accountNumber} — {a.currency} {parseFloat(a.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </option>
              ))}
            </select>
            {selectedAccount && (
              <p className="text-xs text-[var(--text-muted)] mt-2">
                Available: <span className="text-[var(--accent)] font-medium">{selectedAccount.currency} {parseFloat(selectedAccount.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</span>
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">To Account ID</label>
            <input type="text" value={form.destinationAccountId} onChange={(e) => setForm({...form, destinationAccountId: e.target.value})} required
              className="input-field" placeholder="Paste destination account UUID" />
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Amount</label>
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl font-bold text-[var(--text-muted)]">₹</span>
              <input type="number" step="0.01" min="0.01" value={form.amount} onChange={(e) => setForm({...form, amount: e.target.value})} required
                className="input-field pl-10 text-2xl font-bold" placeholder="0.00" />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Note <span className="normal-case">(optional)</span></label>
            <input type="text" value={form.description} onChange={(e) => setForm({...form, description: e.target.value})}
              className="input-field" placeholder="What's this for?" />
          </div>

          <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
            {loading ? <div className="w-5 h-5 border-2 border-black/20 border-t-black rounded-full animate-spin" /> : <>Transfer Now <ArrowRight size={16} /></>}
          </button>
        </form>
      )}
    </div>
  );
}
