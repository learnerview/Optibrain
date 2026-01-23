'use client'

import DashboardLayout from '@/components/dashboard/dashboard-layout'
import { useState, useRef, useEffect } from 'react'
import { Send, Sparkles, HelpCircle, TrendingUp, AlertCircle } from 'lucide-react'

const suggestedQuestions = [
  { icon: TrendingUp, text: 'What are my top cost drivers this month?' },
  { icon: AlertCircle, text: 'Why did my costs spike on Tuesday?' },
  { icon: Sparkles, text: 'What optimizations can save me the most money?' },
  { icon: HelpCircle, text: 'How does my multi-cloud spending compare?' },
]

const mockConversation = [
  {
    type: 'user',
    text: 'What are my biggest cost drivers?',
  },
  {
    type: 'assistant',
    text: 'Based on your cloud infrastructure, your top 3 cost drivers are:\n\n1. **Compute (45%)** - $4,520 this month\n   - Your EC2 instances are running at high capacity but with some underutilized instances\n   - Opportunity: Consolidate workloads and apply reserved instances\n\n2. **Storage (28%)** - $2,340 this month\n   - S3 buckets with old data in standard storage class\n   - Opportunity: Archive cold data to Glacier for 90% savings\n\n3. **Database (18%)** - $1,890 this month\n   - RDS instances running 24/7 but only used during business hours\n   - Opportunity: Schedule database instances or switch to serverless\n\nI can help you implement these optimizations automatically if you\'d like.',
  },
  {
    type: 'user',
    text: 'How much could I save if I implement all these?',
  },
  {
    type: 'assistant',
    text: 'Great question! Here\'s the financial impact:\n\n**Monthly Savings Potential: $5,240**\n**Annual Savings: $62,880**\n\nBreakdown:\n- Compute consolidation: $2,100/month\n- Storage archival: $2,106/month (90% of current S3 spend)\n- Database scheduling: $945/month\n\nSuccess Rate: 94% (based on similar optimizations)\nTime to Implement: 2-3 hours with automation\nRisk Level: Low (all are reversible)\n\nWould you like me to create an automated rule to implement these changes?',
  },
]

export default function CopilotPage() {
  const [messages, setMessages] = useState(mockConversation)
  const [input, setInput] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const messagesEndRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = () => {
    if (!input.trim()) return

    const newMessage = { type: 'user', text: input }
    setMessages([...messages, newMessage])
    setInput('')
    setIsTyping(true)

    // Simulate AI response
    setTimeout(() => {
      const responses = [
        'Based on your data, I recommend optimizing your compute resources. You could save approximately $3,200/month by consolidating your EC2 instances.',
        'I\'ve analyzed your infrastructure and found that your storage costs could be reduced by 60% by archiving old data to Glacier.',
        'Your current multi-cloud strategy is costing you an extra 15% compared to optimized configuration. Would you like me to generate a detailed optimization plan?',
      ]
      const randomResponse = responses[Math.floor(Math.random() * responses.length)]
      setMessages(prev => [...prev, { type: 'assistant', text: randomResponse }])
      setIsTyping(false)
    }, 1500)
  }

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-indigo-600/20 to-blue-600/20 blur-3xl rounded-3xl"></div>
          <div className="relative">
            <div className="flex items-center gap-3 mb-2">
              <Sparkles className="w-8 h-8 text-purple-400" />
              <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-purple-400 via-indigo-400 to-blue-400">AI Copilot</h1>
            </div>
            <p className="text-muted-foreground">Natural language interface for cloud cost intelligence and optimization</p>
          </div>
        </div>

        {/* Chat Interface */}
        <div className="grid md:grid-cols-3 gap-6">
          {/* Main Chat */}
          <div className="md:col-span-2 space-y-4">
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-purple-500/20 rounded-2xl h-[600px] flex flex-col">
              {/* Messages Area */}
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {messages.length === 0 ? (
                  <div className="flex items-center justify-center h-full">
                    <div className="text-center">
                      <Sparkles className="w-16 h-16 text-purple-400/30 mx-auto mb-4" />
                      <p className="text-slate-400">Start a conversation about your cloud costs</p>
                    </div>
                  </div>
                ) : (
                  <>
                    {messages.map((msg, idx) => (
                      <div key={idx} className={`flex ${msg.type === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <div
                          className={`max-w-xs lg:max-w-md px-4 py-3 rounded-xl ${
                            msg.type === 'user'
                              ? 'bg-gradient-to-r from-purple-500 to-indigo-500 text-white rounded-br-none'
                              : 'bg-slate-800/50 border border-slate-700/50 text-slate-300 rounded-bl-none'
                          }`}
                        >
                          <p className="text-sm whitespace-pre-wrap">{msg.text}</p>
                        </div>
                      </div>
                    ))}
                    {isTyping && (
                      <div className="flex justify-start">
                        <div className="bg-slate-800/50 border border-slate-700/50 text-slate-300 px-4 py-3 rounded-xl rounded-bl-none">
                          <div className="flex gap-2">
                            <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce"></div>
                            <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                            <div className="w-2 h-2 rounded-full bg-purple-400 animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                          </div>
                        </div>
                      </div>
                    )}
                    <div ref={messagesEndRef} />
                  </>
                )}
              </div>

              {/* Input Area */}
              <div className="border-t border-slate-700/50 p-4">
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                    placeholder="Ask me anything about your cloud costs..."
                    className="flex-1 bg-slate-700/30 border border-slate-600/50 rounded-lg px-4 py-2 text-white placeholder-slate-400 focus:outline-none focus:border-purple-500/50"
                  />
                  <button
                    onClick={handleSendMessage}
                    disabled={!input.trim() || isTyping}
                    className="px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-500 text-white rounded-lg font-medium hover:from-purple-600 hover:to-indigo-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <Send className="w-5 h-5" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Sidebar - Suggested Questions */}
          <div className="space-y-4">
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-indigo-500/20 rounded-2xl p-6">
              <h3 className="text-lg font-bold text-cyan-300 mb-4">Suggested Questions</h3>
              <div className="space-y-3">
                {suggestedQuestions.map((q, idx) => {
                  const Icon = q.icon
                  return (
                    <button
                      key={idx}
                      onClick={() => {
                        setInput(q.text)
                        setTimeout(() => handleSendMessage(), 100)
                      }}
                      className="w-full text-left p-3 bg-slate-800/30 hover:bg-slate-700/30 border border-slate-700/50 hover:border-indigo-500/30 rounded-lg transition group"
                    >
                      <div className="flex items-start gap-3">
                        <Icon className="w-4 h-4 text-indigo-400 mt-0.5 flex-shrink-0" />
                        <span className="text-sm text-slate-300 group-hover:text-cyan-300 transition">{q.text}</span>
                      </div>
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Capabilities */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-blue-500/20 rounded-2xl p-6">
              <h3 className="text-lg font-bold text-cyan-300 mb-4">Copilot Capabilities</h3>
              <ul className="space-y-2">
                {['Cost analysis', 'Anomaly detection', 'Optimization recommendations', 'Forecasting & trends', 'Team insights', 'Compliance reporting'].map((cap, idx) => (
                  <li key={idx} className="flex items-center gap-2 text-sm text-slate-300">
                    <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
                    {cap}
                  </li>
                ))}
              </ul>
            </div>

            {/* Quick Stats */}
            <div className="backdrop-blur-xl bg-gradient-to-br from-slate-900/60 to-slate-800/40 border border-emerald-500/20 rounded-2xl p-6">
              <h3 className="text-lg font-bold text-cyan-300 mb-4">Current Insights</h3>
              <div className="space-y-3">
                <div>
                  <p className="text-xs text-slate-400 uppercase mb-1">Avg Response</p>
                  <p className="text-sm font-bold text-emerald-400">2.3 seconds</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400 uppercase mb-1">Accuracy</p>
                  <p className="text-sm font-bold text-emerald-400">94.7%</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400 uppercase mb-1">Context</p>
                  <p className="text-sm font-bold text-emerald-400">Real-time data</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
