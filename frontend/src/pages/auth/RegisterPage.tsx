import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { ArrowRight, Zap, Shield, Globe, Cpu } from 'lucide-react';

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [mounted, setMounted] = useState(false);
  const register = useAuthStore((s) => s.register);
  const navigate = useNavigate();

  useEffect(() => { setMounted(true); }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setLoading(true);
    try {
      await register(fullName, email, password, phone || undefined);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally { setLoading(false); }
  };

  const features = [
    { icon: Shield, title: 'Bank-Grade Security', desc: 'JWT auth with 256-bit encryption' },
    { icon: Globe, title: 'Multi-Currency', desc: 'INR, USD, EUR, GBP support' },
    { icon: Cpu, title: 'Real-Time Processing', desc: 'Sub-100ms transaction latency' },
  ];

  return (
    <div className="min-h-screen flex bg-[var(--bg-primary)] overflow-hidden">
      <div className="floating-gradient w-[500px] h-[500px] bg-[var(--accent)] top-[-150px] right-[-150px]" />
      <div className="floating-gradient w-[400px] h-[400px] bg-cyan-400 bottom-[-100px] left-[-100px]" style={{ animationDelay: '7s' }} />

      {/* Left — Form */}
      <div className="flex-1 flex items-center justify-center p-6 lg:p-16">
        <div className={`w-full max-w-[420px] transition-all duration-700 ${mounted ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>
          <div className="flex items-center gap-3 mb-10">
            <div className="w-10 h-10 rounded-xl bg-[var(--accent)] flex items-center justify-center">
              <Zap className="text-[var(--btn-primary-text)]" size={20} strokeWidth={2.5} />
            </div>
            <span className="text-xl font-bold tracking-tight">NexBank</span>
          </div>

          <div className="mb-8">
            <h2 className="text-3xl font-bold tracking-tight">Create your account</h2>
            <p className="text-[var(--text-muted)] mt-2">Start banking in under 2 minutes</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <div className="flex items-center gap-3 p-4 rounded-2xl bg-red-500/5 border border-red-500/10">
                <div className="w-2 h-2 rounded-full bg-red-500 flex-shrink-0" />
                <p className="text-sm text-red-400">{error}</p>
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Full Name</label>
              <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} required className="input-field" placeholder="John Doe" />
            </div>
            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Email</label>
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required className="input-field" placeholder="you@company.com" />
            </div>
            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Phone <span className="text-[var(--text-muted)] normal-case">(optional)</span></label>
              <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} className="input-field" placeholder="+91 98765 43210" />
            </div>
            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Password</label>
              <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} className="input-field" placeholder="Min. 8 characters" />
            </div>

            <button type="submit" disabled={loading} className="btn-primary w-full flex items-center justify-center gap-2 mt-1">
              {loading ? <div className="w-5 h-5 border-2 border-[var(--btn-primary-text)]/20 border-t-[var(--btn-primary-text)] rounded-full animate-spin" /> : <>Get Started <ArrowRight size={16} /></>}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-[var(--border)]">
            <p className="text-center text-sm text-[var(--text-muted)]">
              Already have an account?{' '}
              <Link to="/login" className="text-[var(--accent)] hover:underline font-medium">Sign in</Link>
            </p>
          </div>
        </div>
      </div>

      {/* Right — Features */}
      <div className="hidden lg:flex lg:w-1/2 relative items-center justify-center p-16">
        <div className={`max-w-lg transition-all duration-1000 delay-200 ${mounted ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>
          <h2 className="text-4xl font-black tracking-tight mb-4">
            Built for <span className="text-gradient">modern finance.</span>
          </h2>
          <p className="text-[var(--text-secondary)] text-lg mb-12">
            Enterprise microservices architecture with real-time event-driven processing.
          </p>

          <div className="space-y-6">
            {features.map((f, i) => (
              <div key={f.title} className="glass-card glass-card-hover p-5 flex items-start gap-4 animate-slide-up" style={{ animationDelay: `${0.5 + i * 0.15}s` }}>
                <div className="w-11 h-11 rounded-xl bg-[var(--accent)]/10 flex items-center justify-center flex-shrink-0">
                  <f.icon size={20} className="text-[var(--accent)]" />
                </div>
                <div>
                  <p className="font-semibold text-sm">{f.title}</p>
                  <p className="text-xs text-[var(--text-muted)] mt-1">{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
