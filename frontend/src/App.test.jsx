import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.jsx'
import * as api from './api'

vi.mock('./api')

async function startCall(user) {
  await user.click(screen.getByRole('button', { name: 'Start a call' }))
  await screen.findByText(/Thank you for calling/)
}

async function fillAndVerify(user) {
  await user.type(screen.getByLabelText('Full name'), 'Finley Placeholder')
  await user.type(screen.getByLabelText('Date of birth'), '1963-09-09')
  await user.click(screen.getByRole('button', { name: 'Verify' }))
}

beforeEach(() => {
  vi.resetAllMocks()
  api.getInfo.mockResolvedValue({ demoMode: false })
  api.startCall.mockResolvedValue({ sessionId: 's1', greeting: 'Thank you for calling Results Line.' })
})

describe('call screen', () => {
  it('shows the simulation banner and a start button before a call', () => {
    render(<App />)
    expect(screen.getByText(/Simulation only/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Start a call' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
  })

  it('shows the demo banner only in demo mode', async () => {
    api.getInfo.mockResolvedValue({ demoMode: true })
    render(<App />)
    expect(await screen.findByText(/Demo mode/)).toBeInTheDocument()
  })

  it('does not show the demo banner when demo mode is off', async () => {
    render(<App />)
    await waitFor(() => expect(api.getInfo).toHaveBeenCalled())
    expect(screen.queryByText(/Demo mode/)).not.toBeInTheDocument()
  })

  it('starts a call and asks for name and date of birth', async () => {
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    expect(screen.getByLabelText('Full name')).toBeInTheDocument()
    expect(screen.getByLabelText('Date of birth')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Verify' })).toBeDisabled()
  })

  it('stays on verification after a failed attempt and shows the message', async () => {
    api.verify.mockResolvedValue({ outcome: 'FAILED', message: "I couldn't verify those details." })
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    expect(await screen.findByText("I couldn't verify those details.")).toBeInTheDocument()
    expect(screen.getByLabelText('Full name')).toBeInTheDocument()
    expect(screen.queryByLabelText('Your question')).not.toBeInTheDocument()
  })

  it('ends the call when the check is locked', async () => {
    api.verify.mockResolvedValue({ outcome: 'LOCKED', message: 'Passing you to staff.' })
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    expect(await screen.findByText('This call has ended.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Your question')).not.toBeInTheDocument()
  })

  it('moves to questions after verification and shows an answer with its record fields', async () => {
    api.verify.mockResolvedValue({ outcome: 'VERIFIED', message: "You're verified." })
    api.ask.mockResolvedValue({
      type: 'answer',
      text: 'Your record shows 5 polyps.',
      fieldsUsed: ['polypCount'],
      reason: null,
    })
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    await user.type(await screen.findByLabelText('Your question'), 'How many polyps did I have?')
    await user.click(screen.getByRole('button', { name: 'Ask' }))

    expect(await screen.findByText('Your record shows 5 polyps.')).toBeInTheDocument()
    expect(screen.getByText('Record fields used: polypCount')).toBeInTheDocument()
    expect(api.ask).toHaveBeenCalledWith('s1', 'How many polyps did I have?')
  })

  it('shows a handoff with its staff note and no record fields', async () => {
    api.verify.mockResolvedValue({ outcome: 'VERIFIED', message: "You're verified." })
    api.ask.mockResolvedValue({
      type: 'handoff',
      text: "I'll pass you to a staff member.",
      fieldsUsed: [],
      reason: 'medical advice',
    })
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    await user.type(await screen.findByLabelText('Your question'), 'So I am fine?')
    await user.click(screen.getByRole('button', { name: 'Ask' }))

    expect(await screen.findByText("I'll pass you to a staff member.")).toBeInTheDocument()
    expect(screen.getByText('Staff note: medical advice')).toBeInTheDocument()
    expect(screen.queryByText(/Record fields used/)).not.toBeInTheDocument()
  })

  it('shows an error message when the server call fails', async () => {
    api.verify.mockRejectedValue(new Error('Request failed (500)'))
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong')
  })

  it('starting a new call resets the transcript', async () => {
    api.verify.mockResolvedValue({ outcome: 'FAILED', message: 'Try again.' })
    const user = userEvent.setup()
    render(<App />)
    await startCall(user)
    await fillAndVerify(user)
    await screen.findByText('Try again.')
    await user.click(screen.getByRole('button', { name: 'Start a new call' }))
    await waitFor(() => expect(screen.queryByText('Try again.')).not.toBeInTheDocument())
    expect(api.startCall).toHaveBeenCalledTimes(2)
  })
})
