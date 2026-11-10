import { useEffect, useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { api, inr, when } from './api';

/* ---------- small helpers ---------- */
const Badge = ({ v }) => <span className={`badge ${String(v).toLowerCase()}`}>{String(v).replace('_', ' ')}</span>;
const Err = ({ msg }) => (msg ? <div className="alert error">{msg}</div> : null);
const Table = ({ cols, rows, empty = 'Nothing to show yet.' }) =>
  rows.length === 0 ? <p className="muted">{empty}</p> : (
    <div className="scroll">
      <table>
        <thead><tr>{cols.map((c) => <th key={c.h}>{c.h}</th>)}</tr></thead>
        <tbody>{rows.map((r, i) => <tr key={r.id ?? i}>{cols.map((c) => <td key={c.h}>{c.r(r)}</td>)}</tr>)}</tbody>
      </table>
    </div>
  );
function useLoad(path) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const reload = useCallback(() => api(path).then(setData).catch((e) => setError(e.message)), [path]);
  useEffect(() => { reload(); }, [reload]);
  return { data, error, reload };
}

/* ---------- auth ---------- */
export function Login({ onLogin }) {
  const [f, setF] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const submit = async (e) => {
    e.preventDefault(); setError('');
    try { onLogin(await api('/auth/login', { method: 'POST', body: f })); } catch (err) { setError(err.message); }
  };
  return (
    <div className="authbox card">
      <h2>Sign in</h2>
      <Err msg={error} />
      <form onSubmit={submit}>
        <label>Email<input type="email" required value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} /></label>
        <label>Password<input type="password" required value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></label>
        <button>Login</button>
      </form>
      <p className="muted">New here? <Link to="/register">Open an account</Link></p>
    </div>
  );
}

export function Register({ onLogin }) {
  const [f, setF] = useState({ fullName: '', email: '', phone: '', password: '' });
  const [error, setError] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault(); setError('');
    try { onLogin(await api('/auth/register', { method: 'POST', body: f })); } catch (err) { setError(err.message); }
  };
  return (
    <div className="authbox card">
      <h2>Open an account</h2>
      <Err msg={error} />
      <form onSubmit={submit}>
        <label>Full name<input required value={f.fullName} onChange={set('fullName')} /></label>
        <label>Email<input type="email" required value={f.email} onChange={set('email')} /></label>
        <label>Mobile (10 digits)<input required pattern="\d{10}" value={f.phone} onChange={set('phone')} /></label>
        <label>Password (min 8)<input type="password" required minLength={8} value={f.password} onChange={set('password')} /></label>
        <button>Register</button>
      </form>
      <p className="muted">Already registered? <Link to="/login">Sign in</Link></p>
    </div>
  );
}

/* ---------- customer: dashboard (KYC + accounts) ---------- */
export function Dashboard() {
  const me = useLoad('/customers/me');
  const accounts = useLoad('/customers/me/accounts');
  const alerts = useLoad('/notifications/me');
  const [k, setK] = useState({ fullName: '', dob: '', pan: '', aadhaar: '', address: '' });
  const [msg, setMsg] = useState({ error: '', ok: '' });
  const c = me.data;
  useEffect(() => { if (c) setK((x) => ({ ...x, fullName: c.fullName, dob: c.dob || '', address: c.address || '' })); }, [c]);

  const submit = async (e) => {
    e.preventDefault(); setMsg({ error: '', ok: '' });
    try {
      await api('/customers/me/kyc', { method: 'PUT', body: { ...k, pan: k.pan.toUpperCase() } });
      setMsg({ error: '', ok: 'KYC submitted. An admin will review it shortly.' });
      me.reload();
    } catch (err) { setMsg({ error: err.message, ok: '' }); }
  };
  const set = (key) => (e) => setK({ ...k, [key]: e.target.value });
  const locked = c?.kycStatus === 'VERIFIED';

  return (
    <>
      <Err msg={me.error || accounts.error} />
      <div className="grid">
        <section className="card">
          <h3>My accounts</h3>
          {(accounts.data || []).map((a) => (
            <div key={a.id} className="account">
              <div><b>{a.accountType}</b> · {a.accountNumber}<br /><span className="muted">IFSC {a.ifsc}</span></div>
              <div className="bal">{inr(a.balance)}</div>
            </div>
          ))}
          <Link to="/pay"><button>Make a payment</button></Link>
        </section>

        <section className="card">
          <h3>Profile <Badge v={c?.kycStatus || '...'} /></h3>
          {c && (
            <dl>
              <dt>Name</dt><dd>{c.fullName}</dd>
              <dt>Email</dt><dd>{c.email}</dd>
              <dt>Mobile</dt><dd>{c.phone}</dd>
              <dt>PAN</dt><dd>{c.panMasked || '—'}</dd>
              <dt>Aadhaar</dt><dd>{c.aadhaarMasked || '—'}</dd>
            </dl>
          )}
          {c && c.kycStatus !== 'VERIFIED' && <p className="alert warn">Payments are enabled only after KYC is verified.</p>}
        </section>
      </div>

      <section className="card">
        <h3>KYC details</h3>
        <Err msg={msg.error} />
        {msg.ok && <div className="alert ok">{msg.ok}</div>}
        <form onSubmit={submit} className="formgrid">
          <label>Full name (as per PAN)<input required disabled={locked} value={k.fullName} onChange={set('fullName')} /></label>
          <label>Date of birth<input type="date" required disabled={locked} value={k.dob} onChange={set('dob')} /></label>
          <label>PAN<input required disabled={locked} maxLength={10} placeholder="ABCDE1234F" value={k.pan} onChange={set('pan')} /></label>
          <label>Aadhaar (12 digits)<input required disabled={locked} pattern="\d{12}" value={k.aadhaar} onChange={set('aadhaar')} /></label>
          <label className="wide">Address<input required disabled={locked} value={k.address} onChange={set('address')} /></label>
          <button disabled={locked}>{locked ? 'KYC verified' : 'Submit KYC'}</button>
        </form>
      </section>

      <section className="card">
        <h3>Recent alerts</h3>
        <Table cols={[{ h: 'When', r: (n) => when(n.createdAt) }, { h: 'Message', r: (n) => n.message }]} rows={(alerts.data || []).slice(0, 5)} empty="No alerts yet." />
      </section>
    </>
  );
}

/* ---------- customer: make payment ---------- */
export function Pay() {
  const accounts = useLoad('/customers/me/accounts');
  const blank = { fromAccount: '', mode: 'UPI', toAccount: '', ifsc: '', cardLast4: '', amount: '', remarks: '' };
  const [f, setF] = useState(blank);
  const [idem, setIdem] = useState(() => crypto.randomUUID());
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const from = f.fromAccount || accounts.data?.[0]?.accountNumber || '';
  const label = { UPI: 'Payee VPA (name@bank) or account no.', NEFT: 'Beneficiary account number', CARD: 'Merchant name' }[f.mode];

  const submit = async (e) => {
    e.preventDefault(); setError(''); setResult(null); setBusy(true);
    try {
      const body = { fromAccount: from, mode: f.mode, toAccount: f.toAccount, amount: Number(f.amount), remarks: f.remarks || undefined,
        ifsc: f.mode === 'NEFT' ? f.ifsc.toUpperCase() : undefined, cardLast4: f.mode === 'CARD' ? f.cardLast4 : undefined };
      const r = await api('/payments', { method: 'POST', body, headers: { 'Idempotency-Key': idem } });
      setResult(r); setIdem(crypto.randomUUID());
      if (r.status === 'SUCCESS') { setF({ ...blank, fromAccount: from, mode: f.mode }); accounts.reload(); }
    } catch (err) { setError(err.message); } finally { setBusy(false); }
  };

  return (
    <div className="card narrow">
      <h3>Make a payment</h3>
      <Err msg={error || accounts.error} />
      {result && (
        <div className={`alert ${result.status === 'SUCCESS' ? 'ok' : result.status === 'PENDING' ? 'warn' : 'error'}`}>
          <b>{result.status}</b> · Txn ID <code>{result.txnId}</code> · {inr(result.amount)}
          {result.failureReason && <div>{result.failureReason}</div>}
        </div>
      )}
      <form onSubmit={submit}>
        <label>From account
          <select value={from} onChange={set('fromAccount')}>
            {(accounts.data || []).map((a) => <option key={a.id} value={a.accountNumber}>{a.accountNumber} — {inr(a.balance)}</option>)}
          </select>
        </label>
        <label>Mode
          <select value={f.mode} onChange={set('mode')}><option>UPI</option><option>NEFT</option><option>CARD</option></select>
        </label>
        <label>{label}<input required value={f.toAccount} onChange={set('toAccount')} /></label>
        {f.mode === 'NEFT' && <label>IFSC<input required maxLength={11} placeholder="HDFC0001234" value={f.ifsc} onChange={set('ifsc')} /></label>}
        {f.mode === 'CARD' && <label>Card last 4 digits<input required pattern="\d{4}" maxLength={4} value={f.cardLast4} onChange={set('cardLast4')} /></label>}
        <label>Amount (INR)<input type="number" min="1" step="0.01" required value={f.amount} onChange={set('amount')} /></label>
        <label>Remarks<input value={f.remarks} onChange={set('remarks')} /></label>
        <button disabled={busy || !from}>{busy ? 'Processing…' : 'Pay now'}</button>
      </form>
      <p className="muted">Limits per transaction: UPI ₹1,00,000 · NEFT ₹10,00,000 · Card ₹2,00,000</p>
    </div>
  );
}

/* ---------- customer: transaction history ---------- */
export function History() {
  const pays = useLoad('/payments/me');
  const ledger = useLoad('/ledger/me');
  const [tab, setTab] = useState('payments');
  return (
    <section className="card">
      <div className="tabs">
        <button className={tab === 'payments' ? 'active' : ''} onClick={() => setTab('payments')}>Payments</button>
        <button className={tab === 'ledger' ? 'active' : ''} onClick={() => setTab('ledger')}>Ledger (statement)</button>
      </div>
      <Err msg={pays.error || ledger.error} />
      {tab === 'payments' ? (
        <Table rows={pays.data || []} cols={[
          { h: 'Date', r: (p) => when(p.createdAt) }, { h: 'Txn ID', r: (p) => <code>{p.txnId}</code> },
          { h: 'Mode', r: (p) => p.mode }, { h: 'To', r: (p) => p.toAccount }, { h: 'Amount', r: (p) => inr(p.amount) },
          { h: 'Status', r: (p) => <Badge v={p.status} /> }, { h: 'Note', r: (p) => p.failureReason || p.remarks || '' }]} />
      ) : (
        <Table rows={ledger.data || []} cols={[
          { h: 'Date', r: (l) => when(l.createdAt) }, { h: 'Txn ID', r: (l) => <code>{l.txnId}</code> },
          { h: 'Type', r: (l) => <Badge v={l.entryType} /> }, { h: 'Amount', r: (l) => inr(l.amount) },
          { h: 'Balance after', r: (l) => inr(l.balanceAfter) }, { h: 'Description', r: (l) => l.description }]} />
      )}
    </section>
  );
}

/* ---------- admin dashboard ---------- */
export function Admin() {
  const stats = useLoad('/payments/stats');
  const customers = useLoad('/customers');
  const accounts = useLoad('/customers/accounts');
  const pays = useLoad('/payments');
  const audit = useLoad('/ledger/audit');
  const [tab, setTab] = useState('customers');
  const [error, setError] = useState('');

  const decide = async (id, status) => {
    setError('');
    try { await api(`/customers/${id}/kyc-status?status=${status}`, { method: 'PUT' }); customers.reload(); }
    catch (e) { setError(e.message); }
  };
  const refresh = () => [stats, customers, accounts, pays, audit].forEach((x) => x.reload());
  const s = stats.data;
  const pending = (customers.data || []).filter((c) => c.kycStatus === 'PENDING').length;

  return (
    <>
      <Err msg={error || stats.error || customers.error} />
      <div className="stats">
        <div className="stat"><span>Customers</span><b>{customers.data?.length ?? '…'}</b></div>
        <div className="stat"><span>KYC pending</span><b>{pending}</b></div>
        <div className="stat"><span>Transactions</span><b>{s?.totalTransactions ?? '…'}</b></div>
        <div className="stat"><span>Success / Failed</span><b>{s ? `${s.successful} / ${s.failed}` : '…'}</b></div>
        <div className="stat"><span>Settled volume</span><b>{s ? inr(s.successVolume) : '…'}</b></div>
        <div className="stat"><span>By mode</span><b className="small">{s ? Object.entries(s.byMode).map(([k, v]) => `${k}: ${v}`).join(' · ') || '—' : '…'}</b></div>
      </div>

      <section className="card">
        <div className="tabs">
          {['customers', 'accounts', 'transactions', 'audit'].map((t) => (
            <button key={t} className={tab === t ? 'active' : ''} onClick={() => setTab(t)}>{t[0].toUpperCase() + t.slice(1)}</button>
          ))}
          <button className="ghost right" onClick={refresh}>↻ Refresh</button>
        </div>
        {tab === 'customers' && <Table rows={customers.data || []} cols={[
          { h: 'ID', r: (c) => c.id }, { h: 'Name', r: (c) => c.fullName }, { h: 'Email', r: (c) => c.email }, { h: 'Phone', r: (c) => c.phone },
          { h: 'PAN', r: (c) => c.panMasked || '—' }, { h: 'KYC', r: (c) => <Badge v={c.kycStatus} /> },
          { h: 'Action', r: (c) => c.kycStatus === 'PENDING' ? (
            <>
              <button className="sm" onClick={() => decide(c.id, 'VERIFIED')}>Verify</button>{' '}
              <button className="sm danger" onClick={() => decide(c.id, 'REJECTED')}>Reject</button>
            </>) : '' }]} />}
        {tab === 'accounts' && <Table rows={accounts.data || []} cols={[
          { h: 'Account', r: (a) => a.accountNumber }, { h: 'User', r: (a) => a.userId }, { h: 'Type', r: (a) => a.accountType },
          { h: 'Balance', r: (a) => inr(a.balance) }, { h: 'Status', r: (a) => <Badge v={a.status} /> }]} />}
        {tab === 'transactions' && <Table rows={pays.data || []} cols={[
          { h: 'Date', r: (p) => when(p.createdAt) }, { h: 'Txn ID', r: (p) => <code>{p.txnId}</code> }, { h: 'User', r: (p) => p.userEmail },
          { h: 'Mode', r: (p) => p.mode }, { h: 'To', r: (p) => p.toAccount }, { h: 'Amount', r: (p) => inr(p.amount) },
          { h: 'Status', r: (p) => <Badge v={p.status} /> }, { h: 'Reason', r: (p) => p.failureReason || '' }]} />}
        {tab === 'audit' && <Table rows={audit.data || []} cols={[
          { h: 'When', r: (a) => when(a.createdAt) }, { h: 'Actor', r: (a) => a.actor }, { h: 'Action', r: (a) => a.action }, { h: 'Details', r: (a) => a.details }]} />}
      </section>
    </>
  );
}
