export interface MEXCConfig {
  apiKey: string;
  apiSecret: string;
  isSandbox: boolean;
  autoTransferRewards: boolean;
  leverage: number;
  eventDurationMinutes: number;
}

export interface TradePosition {
  id: string;
  pair: string;
  type: "LONG" | "SHORT";
  entryPrice: number;
  currentPrice: number;
  amount: number;
  leverage: number;
  pnl: number;
  pnlPercent: number;
  timestamp: number;
  status: "ACTIVE" | "CLOSED";
  stopLoss: number | null;
  takeProfit: number | null;
  closePrice?: number;
  closeTimestamp?: number;
}

export interface RewardTransferLog {
  id: string;
  amount: number;
  asset: string;
  fromAccount: string;
  toAccount: string;
  status: string;
  timestamp: number;
}

export interface BotLog {
  id: string;
  timestamp: number;
  type: "INFO" | "SUCCESS" | "WARNING" | "ERROR";
  message: string;
}

export interface Candle {
  time: number;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface MarketInsight {
  asset: string;
  sentiment: "BULLISH" | "BEARISH" | "NEUTRAL";
  sentimentScore: number;
  volatility: "HIGH" | "MEDIUM" | "LOW";
  rsi: number;
  volumeBreakout: boolean;
  openInterestTrend: "INCREASING" | "DECREASING" | "FLAT";
  suggestedSignal: "BUY_LONG" | "SELL_SHORT" | "HOLD_NEUTRAL";
}

export interface NewsArticle {
  id: string;
  title: string;
  category: "BTC" | "ETH" | "Altcoins" | "Global" | string;
  source: string;
  timestamp: number;
  sentiment: "BULLISH" | "BEARISH" | "NEUTRAL" | "POSITIVE" | "NEGATIVE";
  impactScore: number;
}

export type DashboardTab =
  | "DASHBOARD"
  | "MARKETS"
  | "FUTURES"
  | "WALLET"
  | "ORDERS"
  | "SETTINGS";

export interface SpotAssetBalance {
  asset: string;
  free: string;
  locked: string;
}

export interface FuturesAssetData {
  currency: string;
  availableBalance: number;
  bonus: number;
  positionMargin: number;
}
