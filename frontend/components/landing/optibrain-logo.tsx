"use client";

export default function OptiBrainLogo() {
  return (
    <div className="flex items-center gap-3">
      <div className="relative w-10 h-10 rounded-lg bg-gradient-to-br from-blue-500 to-cyan-600 flex items-center justify-center shadow-lg flex-shrink-0 hover:shadow-xl transition">
        {/* Enhanced brain icon with neural network design */}
        <svg
          viewBox="0 0 24 24"
          className="w-5 h-5"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Left brain lobe */}
          <path
            d="M8.5 7C7 7 6 8.5 6 10C6 11.5 6.5 13 8 13.5"
            stroke="white"
            strokeWidth="1.5"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {/* Right brain lobe */}
          <path
            d="M15.5 7C17 7 18 8.5 18 10C18 11.5 17.5 13 16 13.5"
            stroke="white"
            strokeWidth="1.5"
            fill="none"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {/* Left hemisphere curves */}
          <path
            d="M8 9C7.5 9.5 7 10.5 7 11.5"
            stroke="white"
            strokeWidth="1"
            fill="none"
            strokeLinecap="round"
          />
          <path
            d="M8.5 9.5C8.2 10 8 11 8.2 12"
            stroke="white"
            strokeWidth="0.8"
            fill="none"
            strokeLinecap="round"
          />
          {/* Right hemisphere curves */}
          <path
            d="M16 9C16.5 9.5 17 10.5 17 11.5"
            stroke="white"
            strokeWidth="1"
            fill="none"
            strokeLinecap="round"
          />
          <path
            d="M15.5 9.5C15.8 10 16 11 15.8 12"
            stroke="white"
            strokeWidth="0.8"
            fill="none"
            strokeLinecap="round"
          />
          {/* Central neural pathway */}
          <path
            d="M12 6L12 18"
            stroke="white"
            strokeWidth="1.2"
            fill="none"
            strokeLinecap="round"
          />
          {/* Neural nodes */}
          <circle cx="12" cy="8" r="0.8" fill="white" opacity="0.9" />
          <circle cx="12" cy="12" r="0.9" fill="white" />
          <circle cx="12" cy="16" r="0.8" fill="white" opacity="0.9" />
          {/* Top connector node */}
          <circle cx="10" cy="9" r="0.6" fill="white" opacity="0.8" />
          <circle cx="14" cy="9" r="0.6" fill="white" opacity="0.8" />
          {/* Bottom connector nodes */}
          <circle cx="9.5" cy="15" r="0.6" fill="white" opacity="0.8" />
          <circle cx="14.5" cy="15" r="0.6" fill="white" opacity="0.8" />
          {/* Neural connections */}
          <line
            x1="10"
            y1="9"
            x2="12"
            y2="12"
            stroke="white"
            strokeWidth="0.8"
            opacity="0.6"
            strokeLinecap="round"
          />
          <line
            x1="14"
            y1="9"
            x2="12"
            y2="12"
            stroke="white"
            strokeWidth="0.8"
            opacity="0.6"
            strokeLinecap="round"
          />
          <line
            x1="9.5"
            y1="15"
            x2="12"
            y2="12"
            stroke="white"
            strokeWidth="0.8"
            opacity="0.6"
            strokeLinecap="round"
          />
          <line
            x1="14.5"
            y1="15"
            x2="12"
            y2="12"
            stroke="white"
            strokeWidth="0.8"
            opacity="0.6"
            strokeLinecap="round"
          />
        </svg>
      </div>

      {/* Brand Text */}
      <span className="text-lg font-bold tracking-tight">
        <span className="bg-gradient-to-r from-blue-400 via-cyan-400 to-blue-400 bg-clip-text text-transparent">
          OptiBrain
        </span>
      </span>
    </div>
  );
}
