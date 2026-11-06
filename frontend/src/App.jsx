import { useEffect, useRef, useState } from 'react'
import * as api from './api'
import './App.css'

const GENERIC_ERROR = 'Something went wrong. Please try again.'

export default function App() {
  const [demoMode, setDemoMode] = useState(false)
  const [sessionId, setSessionId] = useState(null)
  // idle (no call) | verify (asking who is calling) | ask (verified) | ended (handed to staff)
  const [stage, setStage] = useState('idle')
  const [messages, setMessages] = useState([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [name, setName] = useState('')
  const [dob, setDob] = useState('')
  const [question, setQuestion] = useState('')
  const endRef = useRef(null)

  useEffect(() => {
    api
      .getInfo()
      .then((info) => setDemoMode(Boolean(info.demoMode)))
      .catch(() => {})
  }, [])

  useEffect(() => {
    endRef.current?.scrollIntoView?.({ behavior: 'smooth' })
  }, [messages])

  const addMessage = (message) => setMessages((current) => [...current, message])

  async function startCall() {
    setBusy(true)
    setError('')
    try {
      const call = await api.startCall()
      setSessionId(call.sessionId)
      setMessages([{ role: 'assistant', text: call.greeting }])
      setName('')
      setDob('')
      setQuestion('')
      setStage('verify')
    } catch {
      setError(GENERIC_ERROR)
    } finally {
      setBusy(false)
    }
  }

  async function submitVerify(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    addMessage({ role: 'caller', text: `${name}, born ${dob}` })
    try {
      const result = await api.verify(sessionId, name, dob)
      addMessage({ role: 'assistant', text: result.message })
      if (result.outcome === 'VERIFIED') setStage('ask')
      if (result.outcome === 'LOCKED') setStage('ended')
    } catch {
      setError(GENERIC_ERROR)
    } finally {
      setBusy(false)
    }
  }

  async function submitQuestion(event) {
    event.preventDefault()
    const text = question.trim()
    if (!text) return
    setBusy(true)
    setError('')
    addMessage({ role: 'caller', text })
    setQuestion('')
    try {
      const reply = await api.ask(sessionId, text)
      addMessage({
        role: 'assistant',
        kind: reply.type,
        text: reply.text,
        fieldsUsed: reply.fieldsUsed,
        reason: reply.reason,
      })
    } catch {
      setError(GENERIC_ERROR)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="app">
      <h1>Results Line</h1>
      <p className="banner">
        Simulation only. All patient data is made up. This is not medical advice.
      </p>
      {demoMode && (
        <p className="banner demo">
          Demo mode: replies come from a simple keyword script, not Claude.
        </p>
      )}

      {stage === 'idle' && (
        <div className="start">
          <button onClick={startCall} disabled={busy}>
            Start a call
          </button>
        </div>
      )}

      {stage !== 'idle' && (
        <>
          <ol className="transcript" aria-label="Call transcript">
            {messages.map((m, i) => (
              <li key={i} className={`bubble ${m.role} ${m.kind === 'handoff' ? 'handoff' : ''}`}>
                <span className="who">{m.role === 'assistant' ? 'Results Line' : 'You'}</span>
                <span className="text">{m.text}</span>
                {m.fieldsUsed && m.fieldsUsed.length > 0 && (
                  <span className="meta">Record fields used: {m.fieldsUsed.join(', ')}</span>
                )}
                {m.reason && <span className="meta">Staff note: {m.reason}</span>}
              </li>
            ))}
            <li ref={endRef} aria-hidden="true" />
          </ol>

          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}

          {stage === 'verify' && (
            <form onSubmit={submitVerify} className="entry">
              <label>
                Full name
                <input value={name} onChange={(e) => setName(e.target.value)} autoComplete="off" />
              </label>
              <label>
                Date of birth
                <input type="date" value={dob} onChange={(e) => setDob(e.target.value)} />
              </label>
              <button type="submit" disabled={busy || !name.trim() || !dob}>
                Verify
              </button>
            </form>
          )}

          {stage === 'ask' && (
            <form onSubmit={submitQuestion} className="entry">
              <label>
                Your question
                <input
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  maxLength={500}
                  autoComplete="off"
                />
              </label>
              <button type="submit" disabled={busy || !question.trim()}>
                Ask
              </button>
            </form>
          )}

          {stage === 'ended' && <p className="ended">This call has ended.</p>}

          <button className="secondary" onClick={startCall} disabled={busy}>
            Start a new call
          </button>
        </>
      )}
    </main>
  )
}
