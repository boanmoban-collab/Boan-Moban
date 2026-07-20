import React, { useState } from "react";
import { Wallet, Landmark, Shuffle, ArrowRightLeft, Percent } from "lucide-react";
import { MexcBalance } from "../types";

interface PortfolioWalletProps {
  balances: MexcBalance[];
  simulatedFuturesBalances: MexcBalance[];
  simulatedFundingBalances: MexcBalance[];
  currentPrice: number;
}

export const PortfolioWallet: React.FC<PortfolioWalletProps> = ({
  balances,
  simulatedFuturesBalances,
  simulatedFundingBalances,
  currentPrice
}) => {
  const [activeTab, setActiveTab] = useState<"SPOT" | "FUTURES" | "FUNDING">("SPOT");
  const [transferFrom, setTransferFrom] = useState<string>("SPOT");
  const [transferTo, setTransferTo] = useState<string>("FUTURES");
  const [transferAsset, setTransferAsset] = useState<string>("USDT");
  const [transferAmount, setTransferAmount] = useState<string>("");
  const [transferStatus, setTransferStatus] = useState<string>("");

  const getActiveBalances = () => {
    switch (activeTab) {
      case "FUTURES":
        return simulatedFuturesBalances;
      case "FUNDING":
        return simulatedFundingBalances;
      default:
        return balances;
    }
  };

  const handleTransfer = (e: React.FormEvent) => {
    e.preventDefault();
    const amt = parseFloat(transferAmount);
    if (isNaN(amt) || amt <= 0) {
      setTransferStatus("Please enter a valid transfer amount.");
      return;
    }
    
    setTransferStatus("Transferring assets...");
    setTimeout(() => {
      setTransferStatus(`Successfully transferred ${amt} ${transferAsset} from ${transferFrom} to ${transferTo}!`);
      setTransferAmount("");
      // Clear message after 3s
      setTimeout(() => setTransferStatus(""), 4000);
    }, 1200);
  };

  // Compute total estimate valuation in USDT
  const calculateTotalUSDT = () => {
    let spotTotal = balances.reduce((sum, item) => {
      const price = item.asset === "USDT" ? 1 : item.asset === "BTC" ? currentPrice : item.asset === "MX" ? 5.25 : 0;
      return sum + (parseFloat(item.free) + parseFloat(item.locked)) * price;
    }, 0);

    let futuresTotal = simulatedFuturesBalances.reduce((sum, item) => {
      const price = item.asset === "USDT" ? 1 : item.asset === "BTC" ? currentPrice : 0;
      return sum + (parseFloat(item.free) + parseFloat(item.locked)) * price;
    }, 0);

    let fundingTotal = simulatedFundingBalances.reduce((sum, item) => {
      const price = item.asset === "USDT" ? 1 : item.asset === "BTC" ? currentPrice : 0;
      return sum + (parseFloat(item.free) + parseFloat(item.locked)) * price;
    }, 0);

    return {
      SPOT: spotTotal,
      FUTURES: futuresTotal,
      FUNDING: fundingTotal,
      COMBINED: spotTotal + futuresTotal + fundingTotal
    };
  };

  const valuation = calculateTotalUSDT();

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden p-4 flex flex-col h-full select-none text-zinc-100">
      <div className="flex items-center justify-between border-b border-zinc-900 pb-3 mb-4">
        <div className="flex items-center space-x-2">
          <Wallet className="w-4 h-4 text-blue-500" />
          <h3 className="text-zinc-300 font-bold text-xs tracking-tight">Multi-Wallet Hub</h3>
        </div>
        <span className="text-[10px] font-mono text-zinc-500">
          Combined Balances: <span className="text-zinc-300 font-bold">${valuation.COMBINED.toLocaleString(undefined, { maximumFractionDigits: 2 })} USDT</span>
        </span>
      </div>

      {/* Wallet Tabs */}
      <div className="grid grid-cols-3 gap-1 bg-zinc-900/60 p-1 rounded-lg border border-zinc-900 mb-4 text-center">
        {(["SPOT", "FUTURES", "FUNDING"] as const).map((tab) => {
          const val = tab === "SPOT" ? valuation.SPOT : tab === "FUTURES" ? valuation.FUTURES : valuation.FUNDING;
          return (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`py-1.5 px-2 rounded-md transition-all cursor-pointer ${
                activeTab === tab
                  ? "bg-zinc-850 text-blue-400 border border-zinc-800 shadow-sm"
                  : "text-zinc-400 hover:text-zinc-200"
              }`}
            >
              <div className="text-[10px] font-extrabold">{tab}</div>
              <div className="text-[9px] font-mono font-medium opacity-80 mt-0.5">
                ${val.toLocaleString(undefined, { maximumFractionDigits: 1 })}
              </div>
            </button>
          );
        })}
      </div>

      {/* Asset Table */}
      <div className="flex-1 overflow-y-auto max-h-[160px] pr-1.5 no-scrollbar border border-zinc-900 rounded-lg bg-zinc-950 p-2.5 space-y-2">
        <div className="grid grid-cols-3 text-[10px] text-zinc-500 font-semibold border-b border-zinc-900 pb-1 mb-1.5 font-mono">
          <span>Asset</span>
          <span className="text-right">Available</span>
          <span className="text-right">Locked</span>
        </div>

        {getActiveBalances().map((item) => (
          <div key={item.asset} className="grid grid-cols-3 text-xs font-mono py-1 border-b border-zinc-900/40 last:border-0 hover:bg-zinc-900/20">
            <div className="flex items-center space-x-1.5">
              <span className="font-bold text-zinc-100">{item.asset}</span>
              <span className="text-[9px] text-zinc-500">
                {item.asset === "USDT" ? "Stable" : "Crypto"}
              </span>
            </div>
            <span className="text-right font-medium text-zinc-300">
              {parseFloat(item.free).toLocaleString(undefined, { maximumFractionDigits: 5 })}
            </span>
            <span className="text-right text-zinc-500">
              {parseFloat(item.locked).toLocaleString(undefined, { maximumFractionDigits: 5 })}
            </span>
          </div>
        ))}
      </div>

      {/* Futures Margin Metrics (Only displayed when FUTURES is active) */}
      {activeTab === "FUTURES" && (
        <div className="mt-4 p-3 bg-zinc-900/40 rounded-lg border border-zinc-850 space-y-1.5 text-[10px] font-mono">
          <div className="flex justify-between">
            <span className="text-zinc-500">Maintenance Margin (MM)</span>
            <span className="text-zinc-300">$12.45 USDT</span>
          </div>
          <div className="flex justify-between">
            <span className="text-zinc-500">Margin Ratio (MR)</span>
            <span className="text-emerald-500 font-bold flex items-center">
              1.45% <Percent className="w-2.5 h-2.5 ml-0.5" />
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-zinc-500">Unrealized Position PnL</span>
            <span className="text-emerald-500 font-bold">+$18.75 USDT</span>
          </div>
        </div>
      )}

      {/* Internal Wallet Transfers Widget */}
      <div className="mt-4 border-t border-zinc-900 pt-4">
        <div className="flex items-center space-x-2 mb-2">
          <Shuffle className="w-3.5 h-3.5 text-blue-500" />
          <span className="text-[10px] font-extrabold uppercase text-zinc-400">Fast Account Transfer</span>
        </div>

        <form onSubmit={handleTransfer} className="space-y-2">
          <div className="grid grid-cols-7 items-center bg-zinc-900 rounded border border-zinc-850 text-[10px] p-1 font-mono">
            <select
              value={transferFrom}
              onChange={(e) => setTransferFrom(e.target.value)}
              className="col-span-3 bg-transparent border-none text-zinc-300 font-semibold focus:outline-none focus:ring-0 text-center"
            >
              <option value="SPOT" className="bg-zinc-900">Spot</option>
              <option value="FUTURES" className="bg-zinc-900">Futures</option>
              <option value="FUNDING" className="bg-zinc-900">Funding</option>
            </select>
            
            <div className="col-span-1 flex justify-center text-zinc-500">
              <ArrowRightLeft className="w-3.5 h-3.5" />
            </div>

            <select
              value={transferTo}
              onChange={(e) => setTransferTo(e.target.value)}
              className="col-span-3 bg-transparent border-none text-zinc-300 font-semibold focus:outline-none focus:ring-0 text-center"
            >
              <option value="FUTURES" className="bg-zinc-900">Futures</option>
              <option value="SPOT" className="bg-zinc-900">Spot</option>
              <option value="FUNDING" className="bg-zinc-900">Funding</option>
            </select>
          </div>

          <div className="flex space-x-2">
            <input
              type="number"
              step="any"
              placeholder="Amount to transfer..."
              value={transferAmount}
              onChange={(e) => setTransferAmount(e.target.value)}
              className="flex-1 bg-zinc-900 border border-zinc-800 rounded px-2 py-1 text-[10px] font-mono focus:outline-none"
              required
            />
            <select
              value={transferAsset}
              onChange={(e) => setTransferAsset(e.target.value)}
              className="bg-zinc-900 border border-zinc-800 rounded px-1 text-[10px] font-mono text-zinc-300"
            >
              <option value="USDT">USDT</option>
              <option value="BTC">BTC</option>
              <option value="MX">MX</option>
            </select>
            <button
              type="submit"
              className="bg-blue-600 hover:bg-blue-500 text-white font-extrabold text-[10px] px-3.5 py-1 rounded cursor-pointer"
            >
              Transfer
            </button>
          </div>
          
          {transferStatus && (
            <div className="text-[9px] font-semibold text-center text-blue-400 border border-blue-950 bg-blue-950/20 py-1.5 rounded animate-fade-in font-mono">
              {transferStatus}
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
