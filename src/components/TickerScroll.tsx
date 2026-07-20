import React from "react";
import { TrendingUp, TrendingDown, RefreshCw } from "lucide-react";
import { MexcTicker } from "../types";

interface TickerScrollProps {
  tickers: MexcTicker[];
  selectedSymbol: string;
  onSelectSymbol: (symbol: string) => void;
  onRefresh: () => void;
  isSyncing: boolean;
  timeSkew?: number;
}

export const TickerScroll: React.FC<TickerScrollProps> = ({
  tickers,
  selectedSymbol,
  onSelectSymbol,
  onRefresh,
  isSyncing,
  timeSkew = 0
}) => {
  return (
    <div className="bg-zinc-950 border-b border-zinc-800 py-2.5 px-4 flex flex-wrap items-center justify-between gap-4 select-none">
      <div className="flex items-center space-x-6 overflow-x-auto no-scrollbar scroll-smooth flex-1 py-1">
        {tickers.map((ticker) => {
          const isSelected = ticker.symbol === selectedSymbol;
          const changePercent = parseFloat(ticker.priceChangePercent || "0");
          const isPositive = changePercent >= 0;
          const formattedPrice = parseFloat(ticker.price).toLocaleString(undefined, {
            minimumFractionDigits: ticker.symbol.includes("USDT") ? 2 : 6,
            maximumFractionDigits: ticker.symbol.includes("USDT") ? 2 : 6,
          });

          return (
            <button
              key={ticker.symbol}
              onClick={() => onSelectSymbol(ticker.symbol)}
              className={`flex items-center space-x-3 px-3 py-1.5 rounded-md transition-all duration-200 text-left cursor-pointer focus:outline-none shrink-0 ${
                isSelected
                  ? "bg-blue-950/40 border border-blue-500/50 shadow-[0_0_12px_rgba(59,130,246,0.15)]"
                  : "hover:bg-zinc-900 border border-transparent"
              }`}
            >
              <div>
                <div className="flex items-center space-x-1.5">
                  <span className="text-sm font-semibold text-zinc-100">{ticker.symbol.replace("USDT", "/USDT")}</span>
                  {isPositive ? (
                    <TrendingUp className="w-3 h-3 text-emerald-500" />
                  ) : (
                    <TrendingDown className="w-3 h-3 text-rose-500" />
                  )}
                </div>
                <div className="flex items-center space-x-2 mt-0.5">
                  <span className={`text-sm font-mono font-medium ${isSelected ? "text-blue-400" : "text-zinc-100"}`}>
                    ${formattedPrice}
                  </span>
                  <span
                    className={`text-xs font-mono font-semibold ${
                      isPositive ? "text-emerald-500" : "text-rose-500"
                    }`}
                  >
                    {isPositive ? "+" : ""}
                    {changePercent.toFixed(2)}%
                  </span>
                </div>
              </div>
            </button>
          );
        })}
      </div>

      <div className="flex items-center space-x-4 border-l border-zinc-800 pl-4 shrink-0">
        <div className="text-right">
          <div className="flex items-center space-x-1.5 justify-end">
            <span className="inline-block w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs font-semibold text-zinc-400">MEXC Engine Connected</span>
          </div>
          <div className="text-[10px] font-mono text-zinc-500 mt-0.5 flex items-center justify-end gap-1">
            <span>Latency: {Math.abs(timeSkew) < 200 ? "Sync" : `${Math.abs(timeSkew)}ms`}</span>
            <span>•</span>
            <span>UTC: {new Date(Date.now() + timeSkew).toISOString().slice(11, 19)}</span>
          </div>
        </div>
        <button
          onClick={onRefresh}
          disabled={isSyncing}
          className={`p-2 rounded-md bg-zinc-900 border border-zinc-800 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 transition-all cursor-pointer focus:outline-none ${
            isSyncing ? "animate-spin" : ""
          }`}
          title="Force Sync Server Time & Tickers"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
