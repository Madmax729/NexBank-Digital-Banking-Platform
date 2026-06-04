import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { Eye, EyeOff, ArrowRight, Zap } from 'lucide-react';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [mounted, setMounted] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  useEffect(() => { setMounted(true); }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid credentials. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-[var(--bg-primary)] overflow-hidden">
      {/* Floating gradients */}
      <div className="floating-gradient w-[600px] h-[600px] bg-[var(--accent)] top-[-200px] left-[-200px]" />
      <div className="floating-gradient w-[500px] h-[500px] bg-cyan-400 bottom-[-100px] right-[-100px]" style={{ animationDelay: '5s' }} />
      <div className="floating-gradient w-[300px] h-[300px] bg-purple-500 top-[40%] left-[60%]" style={{ animationDelay: '10s' }} />

      {/* Left Panel — Branding */}
      <div className="hidden lg:flex lg:w-1/2 relative items-center justify-center p-16">
        <div className={`max-w-lg transition-all duration-1000 ${mounted ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>
          <div className="flex items-center gap-3 mb-12">
            <div className="w-12 h-12 rounded-2xl bg-[var(--accent)] flex items-center justify-center">
              <Zap className="text-[var(--btn-primary-text)]" size={24} strokeWidth={2.5} />
            </div>
            <span className="text-2xl font-bold tracking-tight">NexBank</span>
          </div>

          <h1 className="text-6xl font-black tracking-tight leading-[1.05] mb-6">
            Banking for the<br />
            <span className="text-gradient">next generation.</span>
          </h1>

          <p className="text-lg text-[var(--text-secondary)] leading-relaxed mb-12 max-w-md">
            Enterprise-grade digital banking with instant transfers, multi-currency accounts, and real-time fraud detection.
          </p>

          {/* Stats */}
          <div className="grid grid-cols-3 gap-8">
            {[
              { value: '99.9%', label: 'Uptime SLA' },
              { value: '<100ms', label: 'Latency' },
              { value: '256-bit', label: 'Encryption' },
            ].map((stat, i) => (
              <div key={stat.label} className={`animate-slide-up`} style={{ animationDelay: `${0.3 + i * 0.15}s` }}>
                <p className="text-2xl font-bold text-[var(--accent)]">{stat.value}</p>
                <p className="text-xs text-[var(--text-muted)] mt-1 uppercase tracking-wider">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right Panel — Form */}
      <div className="flex-1 flex items-center justify-center p-6 lg:p-16">
        <div className={`w-full max-w-[420px] transition-all duration-700 delay-300 ${mounted ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-8'}`}>

          {/* Mobile logo */}
          <div className="flex items-center gap-3 mb-10 lg:hidden">
            <div className="w-10 h-10 rounded-xl bg-[var(--accent)] flex items-center justify-center">
              <Zap className="text-[var(--btn-primary-text)]" size={20} strokeWidth={2.5} />
            </div>
            <span className="text-xl font-bold tracking-tight">NexBank</span>
          </div>

          <div className="mb-8">
            <h2 className="text-3xl font-bold tracking-tight">Welcome back</h2>
            <p className="text-[var(--text-muted)] mt-2">Sign in to continue to your dashboard</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="flex items-center gap-3 p-4 rounded-2xl bg-red-500/5 border border-red-500/10">
                <div className="w-2 h-2 rounded-full bg-red-500 flex-shrink-0" />
                <p className="text-sm text-red-400">{error}</p>
              </div>
            )}

            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Email Address</label>
              <input
                type="email" value={email} onChange={(e) => setEmail(e.target.value)} required
                className="input-field"
                placeholder="you@company.com"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider mb-2">Password</label>
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} required
                  className="input-field pr-12"
                  placeholder="••••••••"
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)] transition-colors">
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading}
              className="btn-primary w-full flex items-center justify-center gap-2 mt-2">
              {loading ? (
                <div className="w-5 h-5 border-2 border-[var(--btn-primary-text)]/20 border-t-[var(--btn-primary-text)] rounded-full animate-spin" />
              ) : (
                <>Sign In <ArrowRight size={16} /></>
              )}
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-[var(--border)]">
            <p className="text-center text-sm text-[var(--text-muted)]">
              New to NexBank?{' '}
              <Link to="/register" className="text-[var(--accent)] hover:underline font-medium">
                Create an account
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
