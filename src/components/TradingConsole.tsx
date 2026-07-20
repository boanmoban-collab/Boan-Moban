import React, { useState, useEffect } from "react";
import { ShieldCheck, Info, Sparkles } from "lucide-react";
import { APIConfig, MexcBalance } from "../types";

interface TradingConsoleProps {
  symbol: string;
  currentPrice: number;
  balances: MexcBalance[];
  apiConfig: APIConfig;
  onPlaceOrder: (order: {
    symbol: string;
    side: "BUY" | "SELL";
    type: "LIMIT" | "MARKET";
    quantity: number;
    price?: number;
    stopLoss?: number;
    takeProfit?: number;
    leverage?: number;
    marginMode?: "CROSS" | "ISOLATED";
  }) => void;
  isSubmitting: boolean;
}

export const TradingConsole: React.FC<TradingConsoleProps> = ({
  symbol,
  currentPrice,
  balances,
  apiConfig,
  onPlaceOrder,
  isSubmitting
}) => {
  const [side, setSide] = useState<"BUY" | "SELL">("BUY");
  const [type, setType] = useState<"LIMIT" | "MARKET">("LIMIT");
  const [price, setPrice] = useState<string>("");
  const [quantity, setQuantity] = useState<string>("");
  const [leverage, setLeverage] = useState<number>(1);
  const [marginMode, setMarginMode] = useState<"CROSS" | "ISOLATED">("CROSS");
  
  // Risk management features
  const [useStopLoss, setUseStopLoss] = useState<boolean>(false);
  const [stopLoss, setStopLoss] = useState<string>("");
  const [useTakeProfit, setUseTakeProfit] = useState<boolean>(false);
  const [takeProfit, setTakeProfit] = useState<string>("");

  // Auto-fill current price when Limit mode is clicked or price updates
  useEffect(() => {
    if (type === "LIMIT" && !price) {
      setPrice(currentPrice.toString());
    }
  }, [currentPrice, type]);

  // Find balances
  const baseAsset = symbol.replace("USDT", "");
  const usdtBalance = balances.find((b) => b.asset === "USDT")?.free || "0.00";
  const baseAssetBalance = balances.find((b) => b.asset === baseAsset)?.free || "0.0000";

  const availableCapital = side === "BUY" ? parseFloat(usdtBalance) : parseFloat(baseAssetBalance);
  const capitalLabel = side === "BUY" ? "USDT" : baseAsset;

  // Percentage sliders
  const handlePercentClick = (percent: number) => {
    if (availableCapital <= 0) return;
    
    if (side === "BUY") {
      const activePrice = type === "LIMIT" ? parseFloat(price) || currentPrice : currentPrice;
      if (activePrice <= 0) return;
      
      // Calculate max buying quantity with leverage factored
      const totalUSDTToUse = availableCapital * (percent / 100);
      const leveragedUSDT = totalUSDTToUse * leverage;
      const qty = leveragedUSDT / activePrice;
      
      setQuantity(qty.toFixed(4));
    } else {
      // Selling asset
      const qty = availableCapital * (percent / 100);
      setQuantity(qty.toFixed(4));
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const qtyNum = parseFloat(quantity);
    if (isNaN(qtyNum) || qtyNum <= 0) return;

    const orderParams: any = {
      symbol,
      side,
      type,
      quantity: qtyNum,
      leverage,
      marginMode
    };

    if (type === "LIMIT") {
      const priceNum = parseFloat(price);
      if (isNaN(priceNum) || priceNum <= 0) return;
      orderParams.price = priceNum;
    }

    if (useStopLoss && stopLoss) {
      orderParams.stopLoss = parseFloat(stopLoss);
    }

    if (useTakeProfit && takeProfit) {
      orderParams.takeProfit = parseFloat(takeProfit);
    }

    onPlaceOrder(orderParams);
  };

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-4 flex flex-col h-full select-none text-zinc-100">
      {/* Side Selectors */}
      <div className="grid grid-cols-2 p-1 bg-zinc-900 rounded-lg border border-zinc-850 mb-4">
        <button
          type="button"
          onClick={() => setSide("BUY")}
          className={`py-2 text-xs font-bold rounded-md cursor-pointer transition-all ${
            side === "BUY"
              ? "bg-emerald-600 text-zinc-100 shadow-[0_2px_8px_rgba(16,185,129,0.3)]"
              : "text-zinc-400 hover:text-zinc-200"
          }`}
        >
          BUY
        </button>
        <button
          type="button"
          onClick={() => setSide("SELL")}
          className={`py-2 text-xs font-bold rounded-md cursor-pointer transition-all ${
            side === "SELL"
              ? "bg-rose-600 text-zinc-100 shadow-[0_2px_8px_rgba(244,63,94,0.3)]"
              : "text-zinc-400 hover:text-zinc-200"
          }`}
        >
          SELL
        </button>
      </div>

      <form onSubmit={handleSubmit} className="flex-1 flex flex-col space-y-4">
        {/* Margin & Leverage (Futures Style) */}
        <div className="bg-zinc-900/40 p-3 rounded-lg border border-zinc-900 space-y-3">
          <div className="flex items-center justify-between text-xs">
            <span className="text-zinc-400 flex items-center gap-1">
              Margin Mode <Info className="w-3 h-3 text-zinc-500" />
            </span>
            <div className="flex items-center space-x-1 bg-zinc-950 px-1.5 py-0.5 rounded border border-zinc-850">
              <button
                type="button"
                onClick={() => setMarginMode("CROSS")}
                className={`px-2 py-0.5 text-[9px] font-bold rounded ${
                  marginMode === "CROSS" ? "bg-blue-600 text-white" : "text-zinc-400 hover:text-zinc-200"
                }`}
              >
                Cross
              </button>
              <button
                type="button"
                onClick={() => setMarginMode("ISOLATED")}
                className={`px-2 py-0.5 text-[9px] font-bold rounded ${
                  marginMode === "ISOLATED" ? "bg-blue-600 text-white" : "text-zinc-400 hover:text-zinc-200"
                }`}
              >
                Isolated
              </button>
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between text-xs mb-1">
              <span className="text-zinc-400">Leverage (Futures)</span>
              <span className="font-mono text-blue-400 font-bold">{leverage}x</span>
            </div>
            <input
              type="range"
              min="1"
              max="50"
              value={leverage}
              onChange={(e) => setLeverage(parseInt(e.target.value))}
              className="w-full accent-blue-500 h-1 bg-zinc-950 rounded-lg cursor-pointer"
            />
            <div className="flex justify-between text-[8px] font-mono text-zinc-600 px-1 mt-0.5">
              <span>1x</span>
              <span>10x</span>
              <span>25x</span>
              <span>50x</span>
            </div>
          </div>
        </div>

        {/* Order Type Buttons */}
        <div className="flex space-x-2 border-b border-zinc-900 pb-2">
          {["LIMIT", "MARKET"].map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setType(t as any)}
              className={`text-xs font-semibold pb-1.5 px-1 relative cursor-pointer ${
                type === t ? "text-blue-500" : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              {t}
              {type === t && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-500"></div>}
            </button>
          ))}
        </div>

        {/* Dynamic Inputs */}
        <div className="space-y-3">
          {type === "LIMIT" && (
            <div>
              <label className="block text-[11px] font-bold text-zinc-400 mb-1 font-mono">Price (USDT)</label>
              <div className="relative">
                <input
                  type="number"
                  step="any"
                  required
                  placeholder="Price"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-xs font-mono font-semibold focus:outline-none focus:border-blue-500"
                />
                <button
                  type="button"
                  onClick={() => setPrice(currentPrice.toString())}
                  className="absolute right-2 top-1.5 px-1.5 py-0.5 bg-zinc-950 border border-zinc-800 rounded text-[9px] font-bold text-zinc-400 hover:text-zinc-100"
                >
                  Last
                </button>
              </div>
            </div>
          )}

          <div>
            <label className="block text-[11px] font-bold text-zinc-400 mb-1 font-mono">
              Amount ({baseAsset})
            </label>
            <input
              type="number"
              step="any"
              required
              placeholder="0.00"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-lg px-3 py-2 text-xs font-mono font-semibold focus:outline-none focus:border-blue-500"
            />
          </div>
        </div>

        {/* Shortcuts slider */}
        <div className="grid grid-cols-4 gap-1.5 font-mono">
          {[25, 50, 75, 100].map((percent) => (
            <button
              key={percent}
              type="button"
              onClick={() => handlePercentClick(percent)}
              className="py-1 text-[10px] font-bold bg-zinc-900 border border-zinc-850 hover:bg-zinc-800 text-zinc-400 hover:text-zinc-200 rounded transition-all cursor-pointer"
            >
              {percent}%
            </button>
          ))}
        </div>

        {/* Risk Management Checkboxes */}
        <div className="bg-zinc-900/20 p-2.5 rounded-lg border border-zinc-900 text-xs space-y-2">
          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="sl-check"
              checked={useStopLoss}
              onChange={(e) => setUseStopLoss(e.target.checked)}
              className="rounded text-blue-500 bg-zinc-900 border-zinc-800 focus:ring-0"
            />
            <label htmlFor="sl-check" className="text-[10px] font-semibold text-zinc-400 cursor-pointer">
              Set Stop Loss (SL)
            </label>
          </div>
          {useStopLoss && (
            <input
              type="number"
              step="any"
              placeholder="Stop Loss Target Price"
              value={stopLoss}
              onChange={(e) => setStopLoss(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded px-2.5 py-1.5 text-[10px] font-mono focus:outline-none"
            />
          )}

          <div className="flex items-center space-x-2 pt-1">
            <input
              type="checkbox"
              id="tp-check"
              checked={useTakeProfit}
              onChange={(e) => setUseTakeProfit(e.target.checked)}
              className="rounded text-blue-500 bg-zinc-900 border-zinc-800 focus:ring-0"
            />
            <label htmlFor="tp-check" className="text-[10px] font-semibold text-zinc-400 cursor-pointer">
              Set Take Profit (TP)
            </label>
          </div>
          {useTakeProfit && (
            <input
              type="number"
              step="any"
              placeholder="Take Profit Target Price"
              value={takeProfit}
              onChange={(e) => setTakeProfit(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded px-2.5 py-1.5 text-[10px] font-mono focus:outline-none"
            />
          )}
        </div>

        {/* Available Balance Display */}
        <div className="flex justify-between items-center text-[10px] text-zinc-500 pt-1 font-mono">
          <span>Available Balance:</span>
          <span className="font-semibold text-zinc-300">
            {availableCapital.toFixed(4)} {capitalLabel}
          </span>
        </div>

        {/* Submit Execution Action */}
        <button
          type="submit"
          disabled={isSubmitting}
          className={`w-full py-2.5 rounded-lg text-xs font-extrabold tracking-wide uppercase transition-all duration-300 cursor-pointer ${
            side === "BUY"
              ? "bg-emerald-600 hover:bg-emerald-500 text-white shadow-lg hover:shadow-emerald-600/20"
              : "bg-rose-600 hover:bg-rose-500 text-white shadow-lg hover:shadow-rose-600/20"
          } ${isSubmitting ? "opacity-50 cursor-not-allowed" : ""}`}
        >
          {isSubmitting ? (
            <span className="flex items-center justify-center gap-1.5">
              <svg className="animate-spin h-3.5 w-3.5 text-white" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Executing Order...
            </span>
          ) : (
            `${side} ${baseAsset} ${type}`
          )}
        </button>

        {/* Security Indicator */}
        <div className="flex items-center justify-center space-x-1.5 text-[9px] text-zinc-600">
          <ShieldCheck className="w-3.5 h-3.5 text-blue-500" />
          <span>Proxied TLS AES-256 Server Encryption</span>
        </div>
      </form>
    </div>
  );
};
