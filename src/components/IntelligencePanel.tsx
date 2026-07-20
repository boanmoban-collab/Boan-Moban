import React, { useState } from "react";
import { Sparkles, BarChart2, Radio, Compass, AlertTriangle, ExternalLink } from "lucide-react";
import { MarketIntel } from "../types";

interface IntelligencePanelProps {
  symbol: string;
  onScan: () => Promise<void>;
  intel: MarketIntel | null;
  isLoading: boolean;
}

export const IntelligencePanel: React.FC<IntelligencePanelProps> = ({
  symbol,
  onScan,
  intel,
  isLoading
}) => {
  const [scanStep, setScanStep] = useState(0);

  // Cycle scanning messages for high-quality immersive UX
  React.useEffect(() => {
    if (!isLoading) {
      setScanStep(0);
      return;
    }
    const timer = setInterval(() => {
      setScanStep((prev) => (prev + 1) % 5);
    }, 2000);
    return () => clearInterval(timer);
  }, [isLoading]);

  const loadingStatuses = [
    "Establishing encrypted connection to Google Search Grounding...",
    "Crawling public news wires, regulatory filings, and media outlets...",
    "Injecting live price, volume, and open interest metrics...",
    "Computing technical oscillator matrices (RSI, MACD, EMAs)...",
    "Synthesizing sentiment vectors with Gemini 3.5 Flash..."
  ];

  const getGaugeColor = (score: number) => {
    if (score >= 70) return "text-emerald-500 stroke-emerald-500";
    if (score >= 40) return "text-yellow-500 stroke-yellow-500";
    return "text-rose-500 stroke-rose-500";
  };

  const getLabelColor = (label: string) => {
    const l = label.toLowerCase();
    if (l.includes("bullish")) return "bg-emerald-950/40 text-emerald-400 border border-emerald-900/50";
    if (l.includes("bearish")) return "bg-rose-950/40 text-rose-400 border border-rose-900/50";
    return "bg-yellow-950/40 text-yellow-400 border border-yellow-900/50";
  };

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden p-4 flex flex-col h-full select-none text-zinc-100">
      <div className="flex items-center justify-between border-b border-zinc-900 pb-3 mb-4">
        <div className="flex items-center space-x-2">
          <Sparkles className="w-4 h-4 text-purple-500 animate-pulse" />
          <h3 className="text-zinc-300 font-bold text-xs tracking-tight">AI Market Intelligence</h3>
        </div>
        <span className="text-[10px] bg-purple-950/40 text-purple-400 border border-purple-900/50 px-2 py-0.5 rounded font-mono font-bold">
          Gemini 3.5 Grounded
        </span>
      </div>

      {!intel && !isLoading ? (
        <div className="flex-1 flex flex-col items-center justify-center py-6 text-center">
          <div className="w-11 h-11 rounded-full bg-purple-950/20 flex items-center justify-center border border-purple-900/40 text-purple-400 mb-3 animate-pulse">
            <Radio className="w-5 h-5" />
          </div>
          <h4 className="text-sm font-semibold text-zinc-200">Generate Decision Support Metrics</h4>
          <p className="text-xs text-zinc-500 max-w-[260px] mt-1.5 leading-relaxed">
            Harness Google Search Grounding & Gemini to synthesize live market news, oscillators, and trend signals for {symbol.replace("USDT", "")}.
          </p>
          <button
            onClick={onScan}
            className="mt-4 px-4 py-2 bg-gradient-to-r from-purple-700 to-blue-700 hover:from-purple-600 hover:to-blue-600 text-white text-xs font-bold rounded-lg shadow-lg hover:shadow-purple-500/20 transition-all cursor-pointer"
          >
            Compute Intelligence
          </button>
        </div>
      ) : isLoading ? (
        <div className="flex-1 flex flex-col items-center justify-center py-8 text-center">
          <div className="relative w-16 h-16 mb-4">
            <div className="absolute inset-0 rounded-full border-4 border-zinc-900"></div>
            <div className="absolute inset-0 rounded-full border-4 border-t-purple-500 border-r-blue-500 animate-spin"></div>
            <div className="absolute inset-2.5 rounded-full bg-zinc-950 flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-purple-400 animate-pulse" />
            </div>
          </div>
          <div className="text-xs font-bold text-zinc-200 font-mono tracking-tight animate-pulse">
            SCANNING REAL-TIME DATAFLOWS
          </div>
          <div className="text-[10px] text-zinc-500 font-mono mt-2.5 max-w-[250px] leading-relaxed transition-all duration-300">
            {loadingStatuses[scanStep]}
          </div>
        </div>
      ) : (
        intel && (
          <div className="flex-1 overflow-y-auto space-y-4 pr-1.5 no-scrollbar max-h-[350px]">
            {/* Sentiment Meter (Semi-Circle Gauge) */}
            <div className="grid grid-cols-1 md:grid-cols-5 items-center gap-4 bg-zinc-900/20 p-3 rounded-lg border border-zinc-900">
              <div className="col-span-2 flex flex-col items-center justify-center border-b md:border-b-0 md:border-r border-zinc-900 pb-3 md:pb-0 md:pr-4">
                <div className="relative w-24 h-24 flex items-center justify-center">
                  <svg className="absolute inset-0 transform -rotate-90" viewBox="0 0 100 100">
                    <circle
                      cx="50"
                      cy="50"
                      r="40"
                      fill="none"
                      stroke="#18181b"
                      strokeWidth="8"
                    />
                    <circle
                      cx="50"
                      cy="50"
                      r="40"
                      fill="none"
                      className={`${getGaugeColor(intel.sentimentScore)} transition-all duration-1000`}
                      strokeWidth="8"
                      strokeDasharray="251.2"
                      strokeDashoffset={251.2 - (251.2 * intel.sentimentScore) / 100}
                      strokeLinecap="round"
                    />
                  </svg>
                  <div className="text-center z-10">
                    <span className="text-2xl font-mono font-black text-zinc-100">{intel.sentimentScore}</span>
                    <span className="text-[10px] text-zinc-500 block">/100</span>
                  </div>
                </div>
                <div className={`mt-2 px-2.5 py-0.5 rounded text-[10px] font-extrabold uppercase tracking-wider ${getLabelColor(intel.sentimentLabel)}`}>
                  {intel.sentimentLabel}
                </div>
              </div>

              {/* Volume, Trend & Volatility Details */}
              <div className="col-span-3 space-y-2 text-xs font-mono">
                <div className="flex justify-between border-b border-zinc-900 pb-1.5">
                  <span className="text-zinc-500">Asset Volatility</span>
                  <span className={`font-bold ${intel.volatility.toLowerCase() === 'high' ? 'text-rose-400' : 'text-zinc-300'}`}>
                    {intel.volatility}
                  </span>
                </div>
                <div className="flex justify-between border-b border-zinc-900 pb-1.5">
                  <span className="text-zinc-500">Trend Strength</span>
                  <span className="text-zinc-300 font-bold">{intel.trendStrength}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-zinc-500">Grounding Timestamp</span>
                  <span className="text-zinc-500">{new Date(intel.timestamp).toLocaleTimeString()}</span>
                </div>
              </div>
            </div>

            {/* News Crawler Bullets */}
            <div className="space-y-1.5">
              <h4 className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 flex items-center gap-1">
                <Radio className="w-3 h-3 text-red-500 animate-pulse" /> Live Crawl Intel
              </h4>
              <ul className="space-y-1.5 text-xs">
                {intel.newsSummary.map((item, idx) => (
                  <li key={idx} className="bg-zinc-900/30 p-2.5 rounded border border-zinc-900 leading-relaxed text-zinc-300 font-medium">
                    {item}
                  </li>
                ))}
              </ul>
            </div>

            {/* Technical Oscillation Matrices */}
            <div className="space-y-1.5">
              <h4 className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 flex items-center gap-1">
                <BarChart2 className="w-3.5 h-3.5 text-blue-500" /> Oscillator Matrix
              </h4>
              <div className="grid grid-cols-2 gap-2 text-[11px] font-mono">
                <div className="bg-zinc-900/20 p-2.5 rounded border border-zinc-900">
                  <div className="text-zinc-500 mb-0.5">Directional Trend</div>
                  <div className="text-zinc-200 font-semibold">{intel.technicalIndicators.trend}</div>
                </div>
                <div className="bg-zinc-900/20 p-2.5 rounded border border-zinc-900">
                  <div className="text-zinc-500 mb-0.5">RSI Oscillator</div>
                  <div className="text-zinc-200 font-semibold">{intel.technicalIndicators.rsi}</div>
                </div>
                <div className="bg-zinc-900/20 p-2.5 rounded border border-zinc-900">
                  <div className="text-zinc-500 mb-0.5">MACD Convergence</div>
                  <div className="text-zinc-200 font-semibold">{intel.technicalIndicators.macd}</div>
                </div>
                <div className="bg-zinc-900/20 p-2.5 rounded border border-zinc-900">
                  <div className="text-zinc-500 mb-0.5">Moving Averages</div>
                  <div className="text-zinc-200 font-semibold">{intel.technicalIndicators.movingAverages}</div>
                </div>
              </div>
            </div>

            {/* Citation Sources */}
            {intel.sources && intel.sources.length > 0 && (
              <div className="space-y-1.5">
                <h4 className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 flex items-center gap-1">
                  <Compass className="w-3.5 h-3.5 text-emerald-500" /> Grounding Citations
                </h4>
                <div className="flex flex-wrap gap-1.5">
                  {intel.sources.slice(0, 3).map((src, idx) => (
                    <a
                      key={idx}
                      href={src.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center space-x-1.5 bg-zinc-900 border border-zinc-850 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 text-[10px] px-2.5 py-1 rounded-md transition-all font-mono"
                    >
                      <span className="max-w-[120px] truncate">{src.title || "Source"}</span>
                      <ExternalLink className="w-2.5 h-2.5 shrink-0" />
                    </a>
                  ))}
                </div>
              </div>
            )}

            {/* Professional Decision Support Advice (Disclaimer included) */}
            <div className="bg-amber-950/20 border border-amber-900/40 p-3 rounded-lg flex items-start space-x-2">
              <AlertTriangle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
              <div className="text-[10.5px] leading-relaxed text-amber-300">
                <span className="font-bold block text-amber-200 mb-0.5">Decision Support Only</span>
                {intel.decisionSupportAdvice}
              </div>
            </div>

            {/* Recalculate Button */}
            <button
              onClick={onScan}
              className="w-full py-2 bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 text-zinc-300 text-xs font-bold rounded-lg transition-all cursor-pointer"
            >
              Recalculate AI Vectors
            </button>
          </div>
        )
      )}
    </div>
  );
};
