import React from 'react';

/**
 * Reusable Verified Citizen Badge component.
 * Displays official municipal verification shield, genuine complaint count,
 * and informational tooltip explaining the reputation criteria.
 */
export default function VerifiedCitizenBadge({
  isVerified = false,
  genuineCount = 0,
  size = 'md', // 'sm', 'md', 'lg'
  showCount = true,
  className = ''
}) {
  if (!isVerified && genuineCount < 5) {
    if (genuineCount > 0 && showCount) {
      return (
        <span
          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-800 text-slate-300 border border-slate-700 ${className}`}
          title={`${genuineCount} genuine complaint${genuineCount === 1 ? '' : 's'} verified. ${Math.max(0, 5 - genuineCount)} more needed for Verified Citizen Badge.`}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-cyan-400"></span>
          <span>{genuineCount}/5 Verified</span>
        </span>
      );
    }
    return null;
  }

  const sizeClasses = {
    sm: 'text-xs px-2 py-0.5 gap-1',
    md: 'text-xs px-2.5 py-1 gap-1.5',
    lg: 'text-sm px-3.5 py-1.5 gap-2'
  };

  const iconSizes = {
    sm: 'w-3.5 h-3.5',
    md: 'w-4 h-4',
    lg: 'w-5 h-5'
  };

  return (
    <div
      className={`inline-flex items-center font-semibold rounded-full border shadow-sm transition-all duration-200 bg-gradient-to-r from-amber-500/15 via-emerald-500/15 to-teal-500/15 border-amber-400/40 text-amber-300 ${sizeClasses[size] || sizeClasses.md} ${className}`}
      title={`Verified Citizen Badge: Officially recognized civic contributor with ${genuineCount} genuine complaints verified by municipal department officers.`}
    >
      <svg
        className={`${iconSizes[size] || iconSizes.md} text-amber-400 flex-shrink-0 animate-pulse`}
        viewBox="0 0 20 20"
        fill="currentColor"
        aria-hidden="true"
      >
        <path
          fillRule="evenodd"
          d="M10 1a4.5 4.5 0 00-4.5 4.5v1.077C3.99 6.883 3 8.328 3 10c0 4.142 4.03 8.167 6.444 9.682a1.08 1.08 0 001.112 0C12.97 18.167 17 14.142 17 10c0-1.672-.99-3.117-2.5-3.423V5.5A4.5 4.5 0 0010 1zm2.5 5.5v.06a7.07 7.07 0 00-5 0V5.5a2.5 2.5 0 015 0zm-1.72 5.78a.75.75 0 00-1.06-1.06L7.5 13.44l-1.22-1.22a.75.75 0 00-1.06 1.06l1.75 1.75a.75.75 0 001.06 0l3.25-3.25z"
          clipRule="evenodd"
        />
      </svg>
      <span className="tracking-wide">Verified Citizen</span>
      {showCount && genuineCount > 0 && (
        <span className="bg-amber-400/20 text-amber-200 text-[10px] px-1.5 py-0.2 rounded-full font-bold ml-0.5 border border-amber-400/30">
          {genuineCount}
        </span>
      )}
    </div>
  );
}
