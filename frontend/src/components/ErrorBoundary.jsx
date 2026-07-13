import { Component } from 'react'

export class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    console.error('Unhandled React error', { error, info })
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="state-box error">
          <strong>Something went wrong</strong>
          <span>Please refresh the page or try again in a moment.</span>
        </div>
      )
    }
    return this.props.children
  }
}
