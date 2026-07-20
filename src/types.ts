/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export interface MexcTicker {
  symbol: string;
  price: string;
  priceChangePercent?: string;
  volume?: string;
  highPrice?: string;
  lowPrice?: string;
}

export interface MexcBalance {
  asset: string;
  free: string;
  locked: string;
  // Simulated or calculated values for full portfolio view
  usdValue?: number;
  btcValue?: number;
}

export interface MexcAccount {
  accountType: string;
  balances: MexcBalance[];
  canTrade: boolean;
  canDeposit: boolean;
  canWithdraw: boolean;
  updateTime: number;
}

export interface MexcOrderBookEntry {
  price: string;
  quantity: string;
  total?: number; // Cumulative sum for visual depth
}

export interface MexcOrderBook {
  bids: MexcOrderBookEntry[];
  asks: MexcOrderBookEntry[];
}

export interface MexcTrade {
  id: string;
  price: string;
  qty: string;
  time: number;
  isBuyerMaker: boolean;
}

export interface MexcOrder {
  orderId: string;
  symbol: string;
  price: string;
  origQty: string;
  executedQty: string;
  status: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  time: number;
}

export interface KlineData {
  time: number; // timestamp
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface MarketIntel {
  symbol: string;
  sentimentScore: number;
  sentimentLabel: string;
  newsSummary: string[];
  technicalIndicators: {
    trend: string;
    rsi: string;
    macd: string;
    movingAverages: string;
  };
  volatility: 'High' | 'Medium' | 'Low' | string;
  trendStrength: 'Strong' | 'Moderate' | 'Weak' | string;
  decisionSupportAdvice: string;
  sources?: { title: string; url: string }[];
  timestamp: number;
}

export interface TimeSyncData {
  serverTime: number;
  clientTime: number;
  latency: number;
  offset: number;
}

export interface APIConfig {
  apiKey: string;
  apiSecret: string;
  isValidated: boolean;
  isLoading: boolean;
  error?: string;
}
