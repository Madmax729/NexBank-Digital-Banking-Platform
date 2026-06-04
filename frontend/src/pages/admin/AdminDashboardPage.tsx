import { useEffect, useState } from 'react';
import api from '../../lib/api';
import { Shield, AlertTriangle, Activity, TrendingUp, FileText, Heart, Zap, BarChart3, ArrowUpRight } from 'lucide-react';

function AnimatedStat({ value, suffix = '' }: { value: number; suffix?: string }) {
  const [display, setDisplay] = useState(0);
  useEffect(() => {
    const duration = 1000;
    const start = Date.now();
    const animate = () => {
      const elapsed = Date.now() - start;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.floor(value * eased));
      if (progress < 1) requestAnimationFrame(animate);
      else setDisplay(value);
    };
    animate();
  }, [value]);
  return <>{display.toLocaleString()}{suffix}</>;
}

export default function AdminDashboardPage() {
  const [analytics, setAnalytics] = useState<any>({});
  const [fraudAlerts, setFraudAlerts] = useState<any[]>([]);
  const [auditLogs, setAuditLogs] = useState<any[]>([]);

  useEffect(() => {
    api.get('/admin/analytics/dashboard').then(r => setAnalytics(r.data.data || {})).catch(() => {});
    api.get('/admin/fraud-alerts?size=5').then(r => setFraudAlerts(r.data.data?.content || [])).catch(() => {});
    api.get('/admin/audit-logs?size=8').then(r => setAuditLogs(r.data.data?.content || [])).catch(() => {});
  }, []);

  const stats = [
    { label: 'Total Transactions', value: analytics.totalTransactions || 0, icon: Activity, color: 'var(--accent)', bgColor: 'var(--accent)' },
    { label: 'Completed', value: analytics.completedTransactions || 0, icon: TrendingUp, color: 'var(--success)', bgColor: 'var(--success)' },
    { label: 'Failed', value: analytics.failedTransactions || 0, icon: AlertTriangle, color: 'var(--danger)', bgColor: 'var(--danger)' },
    { label: 'Fraud Alerts', value: analytics.fraudAlerts || 0, icon: Shield, color: 'var(--warning)', bgColor: 'var(--warning)' },
  ];

  const services = [
    { name: 'Auth Service', port: 8081, status: 'UP' },
    { name: 'Account Service', port: 8083, status: 'UP' },
    { name: 'Transaction Engine', port: 8084, status: 'UP' },
    { name: 'Ledger Service', port: 8085, status: 'UP' },
    { name: 'Currency Service', port: 8087, status: 'UP' },
    { name: 'Fraud Detection', port: 8088, status: 'UP' },
    { name: 'Notifications', port: 8089, status: 'UP' },
    { name: 'Audit Service', port: 8090, status: 'UP' },
  ];

  return (
    <div className="space-y-6 max-w-7xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-3">
            <Shield size={24} className="text-[var(--accent)]" /> Control Center
          </h1>
          <p className="text-sm text-[var(--text-muted)] mt-1">System monitoring & analytics dashboard</p>
        </div>
        <div className="badge bg-[var(--success)]/10 text-[var(--success)]">
          <div className="w-1.5 h-1.5 rounded-full bg-[var(--success)] mr-1.5 animate-pulse" />
          All Systems Operational
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, i) => (
          <div key={stat.label} className="glass-card glass-card-hover p-5 animate-slide-up" style={{ animationDelay: `${i * 0.1}s` }}>
            <div className="flex items-center justify-between mb-4">
              <div className="w-10 h-10 rounded-xl flex items-center justify-center" style={{ background: `color-mix(in srgb, ${stat.bgColor} 10%, transparent)` }}>
                <stat.icon size={18} style={{ color: stat.color }} />
              </div>
              <ArrowUpRight size={14} className="text-[var(--text-muted)]" />
            </div>
            <p className="text-3xl font-bold tracking-tight animate-count-up">
              <AnimatedStat value={stat.value} />
            </p>
            <p className="text-xs text-[var(--text-muted)] mt-1 uppercase tracking-wider">{stat.label}</p>
          </div>
        ))}
      </div>

      {/* Success Rate + System Health — Bento */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Success Rate */}
        <div className="glass-card p-6 relative overflow-hidden">
          <div className="floating-gradient w-[200px] h-[200px] bg-[var(--accent)] -bottom-16 -right-16" />
          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-4">
              <BarChart3 size={16} className="text-[var(--accent)]" />
              <span className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Success Rate</span>
            </div>
            <p className="text-5xl font-black tracking-tight text-[var(--accent)]">
              {analytics.successRate ? `${Math.round(analytics.successRate)}%` : '—'}
            </p>
            <p className="text-xs text-[var(--text-muted)] mt-2">Transaction completion rate</p>
          </div>
        </div>

        {/* System Health */}
        <div className="lg:col-span-2 glass-card p-6">
          <div className="flex items-center gap-2 mb-5">
            <Heart size={16} className="text-[var(--danger)]" />
            <span className="text-xs font-medium text-[var(--text-muted)] uppercase tracking-wider">Service Health</span>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            {services.map((svc, i) => (
              <div key={svc.name} className="p-3 rounded-xl bg-[var(--bg-primary)] border border-[var(--border)] hover:border-[var(--border-hover)] transition-all animate-slide-up" style={{ animationDelay: `${0.3 + i * 0.05}s` }}>
                <div className="flex items-center gap-2 mb-1">
                  <div className="w-2 h-2 rounded-full bg-[var(--success)]" style={{ animation: 'pulse-slow 2s ease-in-out infinite', animationDelay: `${i * 0.3}s` }} />
                  <span className="text-xs font-semibold">{svc.name}</span>
                </div>
                <p className="text-[10px] text-[var(--text-muted)] font-mono">:{svc.port}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Fraud Alerts + Audit Logs */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Fraud Alerts */}
        <div className="glass-card p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <AlertTriangle size={16} className="text-[var(--warning)]" />
              <span className="text-sm font-semibold">Fraud Alerts</span>
            </div>
            <span className="badge bg-[var(--warning)]/10 text-[var(--warning)]">{fraudAlerts.length}</span>
          </div>
          {fraudAlerts.length === 0 ? (
            <div className="p-6 text-center">
              <Shield size={20} className="text-[var(--text-muted)] mx-auto mb-2" />
              <p className="text-xs text-[var(--text-muted)]">No active alerts</p>
            </div>
          ) : (
            <div className="space-y-2">
              {fraudAlerts.map((alert: any, i: number) => (
                <div key={alert.id} className="p-3 rounded-xl bg-[var(--bg-primary)] border border-[var(--border)] flex items-center justify-between animate-slide-up" style={{ animationDelay: `${0.4 + i * 0.08}s` }}>
                  <div>
                    <p className="text-sm font-medium">{alert.rule}</p>
                    <p className="text-xs text-[var(--text-muted)]">₹{parseFloat(alert.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</p>
                  </div>
                  <span className={`badge ${alert.status === 'FLAGGED' ? 'bg-[var(--warning)]/10 text-[var(--warning)]' : alert.status === 'CLEARED' ? 'bg-[var(--success)]/10 text-[var(--success)]' : 'bg-[var(--danger)]/10 text-[var(--danger)]'}`}>
                    {alert.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Audit Logs */}
        <div className="glass-card p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <FileText size={16} className="text-[var(--info)]" />
              <span className="text-sm font-semibold">Audit Trail</span>
            </div>
            <span className="badge bg-[var(--info)]/10 text-[var(--info)]">{auditLogs.length}</span>
          </div>
          {auditLogs.length === 0 ? (
            <div className="p-6 text-center">
              <FileText size={20} className="text-[var(--text-muted)] mx-auto mb-2" />
              <p className="text-xs text-[var(--text-muted)]">No logs yet</p>
            </div>
          ) : (
            <div className="space-y-2">
              {auditLogs.map((log: any, i: number) => (
                <div key={log.id} className="p-3 rounded-xl bg-[var(--bg-primary)] border border-[var(--border)] flex items-center justify-between animate-slide-up" style={{ animationDelay: `${0.4 + i * 0.05}s` }}>
                  <div className="flex items-center gap-3">
                    <div className="w-7 h-7 rounded-lg bg-[var(--bg-elevated)] flex items-center justify-center">
                      <Zap size={12} className="text-[var(--accent)]" />
                    </div>
                    <div>
                      <p className="text-xs font-medium">{log.action}</p>
                      <p className="text-[10px] text-[var(--text-muted)]">{new Date(log.createdAt).toLocaleString('en-IN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: 'short' })}</p>
                    </div>
                  </div>
                  <span className="text-[10px] text-[var(--text-muted)] font-mono">{(log.userId || 'system').substring(0, 8)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
