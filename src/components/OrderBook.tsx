import React from "react";
import { ArrowDown, ArrowUp } from "lucide-react";
import { MexcOrderBook } from "../types";

interface OrderBookProps {
  orderBook: MexcOrderBook;
  currentPrice: number;
  prevPrice?: number;
  symbol: string;
}

export const OrderBook: React.FC<OrderBookProps> = ({
  orderBook,
  currentPrice,
  prevPrice = currentPrice,
  symbol
}) => {
  const isPriceUp = currentPrice >= prevPrice;

  // Calculate cumulative depth totals
  const bidsWithTotals = [...orderBook.bids].map((bid, index, arr) => {
    let total = 0;
    for (let i = 0; i <= index; i++) {
      total += parseFloat(arr[i].quantity);
    }
    return { ...bid, total };
  });

  const asksWithTotals = [...orderBook.asks].map((ask, index, arr) => {
    let total = 0;
    for (let i = 0; i <= index; i++) {
      total += parseFloat(arr[i].quantity);
    }
    return { ...ask, total };
  });

  const maxTotalBid = bidsWithTotals.length > 0 ? bidsWithTotals[bidsWithTotals.length - 1].total || 1 : 1;
  const maxTotalAsk = asksWithTotals.length > 0 ? asksWithTotals[asksWithTotals.length - 1].total || 1 : 1;
  const maxTotal = Math.max(maxTotalBid, maxTotalAsk);

  // Calculate spread
  const topBid = orderBook.bids[0] ? parseFloat(orderBook.bids[0].price) : 0;
  const topAsk = orderBook.asks[0] ? parseFloat(orderBook.asks[0].price) : 0;
  const spreadValue = topAsk - topBid;
  const spreadPercent = topBid > 0 ? (spreadValue / topBid) * 100 : 0;

  const formatPrice = (val: string) => {
    const num = parseFloat(val);
    return num.toLocaleString(undefined, {
      minimumFractionDigits: num > 1000 ? 2 : 4,
      maximumFractionDigits: num > 1000 ? 2 : 4
    });
  };

  const formatQty = (val: string) => {
    const num = parseFloat(val);
    return num.toLocaleString(undefined, {
      minimumFractionDigits: 4,
      maximumFractionDigits: 4
    });
  };

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden p-4 flex flex-col h-full select-none font-mono text-xs">
      <div className="flex items-center justify-between border-b border-zinc-900 pb-3 mb-2.5">
        <h3 className="text-zinc-300 font-bold tracking-tight text-xs">Order Book</h3>
        <span className="text-[10px] text-zinc-500">Depth (10x)</span>
      </div>

      {/* Columns Header */}
      <div className="grid grid-cols-3 text-zinc-500 text-[10px] font-semibold pb-1.5 border-b border-zinc-950">
        <span>Price (USDT)</span>
        <span className="text-right">Size ({symbol.replace("USDT", "")})</span>
        <span className="text-right">Total</span>
      </div>

      {/* Asks (Sells) - Rendered in reverse order (highest price at top, lowest at bottom) */}
      <div className="flex-1 flex flex-col justify-end space-y-0.5 py-1 min-h-[120px]">
        {asksWithTotals
          .slice()
          .reverse()
          .map((ask, i) => {
            const depthPercent = ((ask.total || 0) / maxTotal) * 100;
            return (
              <div
                key={`ask-${i}`}
                className="grid grid-cols-3 relative py-0.5 hover:bg-zinc-900/40 cursor-pointer text-zinc-100"
              >
                {/* Horizontal Depth visual fill */}
                <div
                  className="absolute right-0 top-0 bottom-0 bg-rose-950/15 transition-all duration-300"
                  style={{ width: `${depthPercent}%` }}
                ></div>
                <span className="text-rose-500 relative z-10">{formatPrice(ask.price)}</span>
                <span className="text-right relative z-10">{formatQty(ask.quantity)}</span>
                <span className="text-right text-zinc-400 relative z-10">{formatQty(ask.total?.toString() || "0")}</span>
              </div>
            );
          })}
      </div>

      {/* Mid Market Price Section */}
      <div className="my-2.5 py-2.5 border-y border-zinc-900/80 bg-zinc-900/10 flex items-center justify-between px-1">
        <div className="flex items-center space-x-1.5">
          <span className={`text-sm font-bold flex items-center ${isPriceUp ? "text-emerald-500" : "text-rose-500"}`}>
            {currentPrice.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            {isPriceUp ? <ArrowUp className="w-3.5 h-3.5 ml-0.5" /> : <ArrowDown className="w-3.5 h-3.5 ml-0.5" />}
          </span>
          <span className="text-[10px] text-zinc-500">
            ${spreadValue.toFixed(2)} ({spreadPercent.toFixed(2)}%)
          </span>
        </div>
        <span className="text-[10px] text-zinc-500">Spread</span>
      </div>

      {/* Bids (Buys) */}
      <div className="flex-1 flex flex-col space-y-0.5 py-1 min-h-[120px]">
        {bidsWithTotals.map((bid, i) => {
          const depthPercent = ((bid.total || 0) / maxTotal) * 100;
          return (
            <div
              key={`bid-${i}`}
              className="grid grid-cols-3 relative py-0.5 hover:bg-zinc-900/40 cursor-pointer text-zinc-100"
            >
              {/* Horizontal Depth visual fill */}
              <div
                className="absolute right-0 top-0 bottom-0 bg-emerald-950/15 transition-all duration-300"
                style={{ width: `${depthPercent}%` }}
              ></div>
              <span className="text-emerald-500 relative z-10">{formatPrice(bid.price)}</span>
              <span className="text-right relative z-10">{formatQty(bid.quantity)}</span>
              <span className="text-right text-zinc-400 relative z-10">{formatQty(bid.total?.toString() || "0")}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
