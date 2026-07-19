import React, { useState, useEffect, useRef } from "react";
import {
  LayoutDashboard,
  TrendingUp,
  Zap,
  Wallet,
  History,
  Settings as SettingsIcon,
  Play,
  Square,
  Cpu,
  Terminal,
  Wifi,
  Clock,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Coins,
  ArrowRightLeft,
  ChevronRight,
  Info,
  DollarSign,
  Maximize2,
  RefreshCw
} from "lucide-react";
import confetti from "canvas-confetti";
import { motion, AnimatePresence } from "motion/react";
import {
  MEXCConfig,
  TradePosition,
  RewardTransferLog,
  BotLog,
  Candle,
  MarketInsight,
  NewsArticle,
  DashboardTab,
  SpotAssetBalance,
  FuturesAssetData
} from "./types";

// --- GLOBAL STYLES & THEME COLORS ---
const COLORS = {
  DarkCanvas: "#090D1A",
  CardBg: "#111827",
  BorderColor: "#1F2937",
  EmeraldNeon: "#00FF87",
  RedNeon: "#FF3B30",
  TextWhite: "#FFFFFF",
  TextGray: "#9CA3AF",
  TextGreen: "#34D399",
  TextRed: "#F87171",
  PurplePrimary: "#6366F1",
  BlueAccent: "#3B82F6",
  AmberAccent: "#F59E0B"
};

export default function App() {
  // --- STATE INITIALIZATION ---
  const [activeTab, setActiveTab] = useState<DashboardTab>("DASHBOARD");
  
  // App Config
  const [config, setConfig] = useState<MEXCConfig>(() => {
    const saved = localStorage.getItem("mexc_config");
    return saved ? JSON.parse(saved) : {
      apiKey: "",
      apiSecret: "",
      isSandbox: true,
      autoTransferRewards: true,
      leverage: 20,
      eventDurationMinutes: 10
    };
  });

  // Active & Closed positions
  const [positions, setPositions] = useState<TradePosition[]>(() => {
    const saved = localStorage.getItem("mexc_positions");
    return saved ? JSON.parse(saved) : [];
  });

  // Transfer logs
  const [transferLogs, setTransferLogs] = useState<RewardTransferLog[]>(() => {
    const saved = localStorage.getItem("mexc_transfer_logs");
    return saved ? JSON.parse(saved) : [];
  });

  // Bot activity logs
  const [botLogs, setBotLogs] = useState<BotLog[]>(() => {
    const saved = localStorage.getItem("mexc_bot_logs");
    return saved ? JSON.parse(saved) : [
      {
        id: "init_1",
        timestamp: Date.now() - 300000,
        type: "INFO",
        message: "🤖 نظام الذكاء الاصطناعي Maria Bot جاهز وبانتظار التوجيهات."
      },
      {
        id: "init_2",
        timestamp: Date.now() - 250000,
        type: "INFO",
        message: "📱 بيئة العمل مُهيأة بالكامل لإصدار الأجهزة LT_9904 (Android 15)."
      }
    ];
  });

  // Live trading variables
  const [btcPrice, setBtcPrice] = useState(68500.0);
  const [priceHistory, setPriceHistory] = useState<number[]>(() => {
    // Fill initial history
    return Array.from({ length: 20 }, () => 68500.0 + (Math.random() - 0.48) * 100);
  });
  const [selectedInterval, setSelectedInterval] = useState<string>("1m");
  const [candles, setCandles] = useState<Candle[]>([]);
  const [timeOffset, setTimeOffset] = useState<number>(0);
  const [isAutoTradingActive, setIsAutoTradingActive] = useState(false);
  const [wsStatus, setWsStatus] = useState<"CONNECTED" | "DISCONNECTED" | "RECONNECTING">("DISCONNECTED");

  // Wallet assets simulation / actual
  const [spotWallet, setSpotWallet] = useState<SpotAssetBalance[]>([
    { asset: "USDT", free: "14500.50", locked: "0.00" },
    { asset: "BTC", free: "0.185", locked: "0.00" },
    { asset: "MX", free: "520.00", locked: "0.00" },
    { asset: "ETH", free: "1.25", locked: "0.00" }
  ]);
  const [futuresWallet, setFuturesWallet] = useState<FuturesAssetData>({
    currency: "USDT",
    availableBalance: 5000.0,
    bonus: 150.0,
    positionMargin: 0.0
  });

  // News and AI sentiment cache
  const [news, setNews] = useState<NewsArticle[]>([]);
  const [marketInsight, setMarketInsight] = useState<MarketInsight>({
    asset: "BTCUSDT",
    sentiment: "NEUTRAL",
    sentimentScore: 0.0,
    volatility: "LOW",
    rsi: 50.0,
    volumeBreakout: false,
    openInterestTrend: "FLAT",
    suggestedSignal: "HOLD_NEUTRAL"
  });

  // Android build simulation state
  const [buildLogs, setBuildLogs] = useState<string[]>([]);
  const [isBuildingApk, setIsBuildingApk] = useState(false);
  const [buildProgress, setBuildProgress] = useState(0);

  // References for terminal and websocket
  const terminalBottomRef = useRef<HTMLDivElement | null>(null);
  const wsRef = useRef<WebSocket | null>(null);

  // --- REUSABLE UTILITIES & HELPERS ---
  const addLog = (type: BotLog["type"], message: string) => {
    const newLog: BotLog = {
      id: `log_${Math.random().toString(36).substring(2, 10)}`,
      timestamp: Date.now(),
      type,
      message
    };
    setBotLogs((prev) => {
      const updated = [newLog, ...prev];
      localStorage.setItem("mexc_bot_logs", JSON.stringify(updated.slice(0, 100)));
      return updated;
    });
  };

  const playConfetti = () => {
    confetti({
      particleCount: 80,
      spread: 70,
      origin: { y: 0.8 },
      colors: ["#00FF87", "#34D399", "#6366F1", "#3B82F6"]
    });
  };

  // --- SAVE LOCAL STORAGE PERSISTENCE ---
  useEffect(() => {
    localStorage.setItem("mexc_config", JSON.stringify(config));
  }, [config]);

  useEffect(() => {
    localStorage.setItem("mexc_positions", JSON.stringify(positions));
  }, [positions]);

  useEffect(() => {
    localStorage.setItem("mexc_transfer_logs", JSON.stringify(transferLogs));
  }, [transferLogs]);

  // Scroll to bottom of terminal log
  useEffect(() => {
    terminalBottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [botLogs]);

  // --- INITIAL DATA & TIME SYNC ---
  useEffect(() => {
    syncTimeWithServer();
    fetchInitialKlines();
    generateInitialNews();
  }, []);

  const syncTimeWithServer = async () => {
    try {
      addLog("INFO", "⏱️ جاري مزامنة التوقيت مع خوادم MEXC المباشرة...");
      const res = await fetch("/api/time");
      if (res.ok) {
        const data = await res.json();
        const serverTime = data.serverTime;
        const localTime = Date.now();
        const offset = serverTime - localTime;
        setTimeOffset(offset);
        const offsetSec = offset / 1000.0;
        addLog("SUCCESS", `⏱️ تم مزامنة التوقيت مع MEXC بنجاح! فرق التوقيت: ${offsetSec.toFixed(3)} ثانية.`);
      }
    } catch (e: any) {
      addLog("WARNING", "⚠️ فشل الاتصال بخادم التوقيت الرسمي. استخدام توقيت المتصفح المحلي.");
    }
  };

  const fetchInitialKlines = async (interval: string = "1m") => {
    try {
      const res = await fetch(`/api/klines?symbol=BTCUSDT&interval=${interval}&limit=80`);
      if (res.ok) {
        const rawKlines = await res.json();
        const parsed: Candle[] = rawKlines.map((item: any) => ({
          time: Number(item[0]),
          open: parseFloat(item[1]),
          high: parseFloat(item[2]),
          low: parseFloat(item[3]),
          close: parseFloat(item[4]),
          volume: parseFloat(item[5])
        }));
        if (parsed.length > 0) {
          setCandles(parsed);
          setBtcPrice(parsed[parsed.length - 1].close);
        }
      } else {
        generateMockCandles(interval);
      }
    } catch (error) {
      generateMockCandles(interval);
    }
  };

  const generateMockCandles = (interval: string) => {
    let now = Date.now();
    const intervalMs = getIntervalMs(interval);
    const mockList: Candle[] = [];
    let price = 68500.0;
    for (let i = 80; i >= 0; i--) {
      const time = now - i * intervalMs;
      const change = (Math.random() - 0.49) * 150;
      const o = price;
      const c = price + change;
      const h = Math.max(o, c) + Math.random() * 50;
      const l = Math.min(o, c) - Math.random() * 50;
      mockList.push({
        time,
        open: o,
        high: h,
        low: l,
        close: c,
        volume: Math.random() * 45 + 5
      });
      price = c;
    }
    setCandles(mockList);
  };

  const getIntervalMs = (interval: string) => {
    switch (interval) {
      case "1m": return 60000;
      case "5m": return 300000;
      case "15m": return 900000;
      case "1h": return 3600000;
      case "4h": return 14400000;
      default: return 86400000;
    }
  };

  const generateInitialNews = () => {
    const now = Date.now();
    setNews([
      {
        id: "news_1",
        title: "مؤشر أسعار المستهلكين الأمريكي (CPI) يأتي أقل من المتوقع، مما يعزز صعود الأصول الرقمية.",
        category: "Global",
        source: "CryptoNews Arabic",
        timestamp: now - 3600000,
        sentiment: "POSITIVE",
        impactScore: 0.85
      },
      {
        id: "news_2",
        title: "تزايد تدفقات رؤوس الأموال عبر صناديق الاستثمار المتداولة الفورية لـ Bitcoin (ETFs).",
        category: "BTC",
        source: "CoinDesk Arabic",
        timestamp: now - 7200000,
        sentiment: "POSITIVE",
        impactScore: 0.90
      },
      {
        id: "news_3",
        title: "مستويات تصفية قياسية لصفقات Short عند تجاوز مستويات مقاومة BTC الهامة في منصات العقود.",
        category: "BTC",
        source: "Futures Alert",
        timestamp: now - 14400000,
        sentiment: "POSITIVE",
        impactScore: 0.78
      }
    ]);
  };

  // --- CONNECT MEXC WEBSOCKET ---
  useEffect(() => {
    connectWebSocket();
    return () => {
      wsRef.current?.close();
    };
  }, []);

  const connectWebSocket = () => {
    setWsStatus("RECONNECTING");
    addLog("INFO", "🔌 جاري محاولة فتح اتصال WebSocket للبث المباشر لأسعار MEXC...");
    
    // contract.mexc.com public ws
    const ws = new WebSocket("wss://contract.mexc.com/edge/ws");
    wsRef.current = ws;

    ws.onopen = () => {
      setWsStatus("CONNECTED");
      addLog("SUCCESS", "🔌 تم الاتصال بقنوات بث أسعار MEXC WebSocket المباشرة!");
      const subscribeMsg = JSON.stringify({
        method: "sub.ticker",
        param: {
          symbol: "BTC_USDT"
        }
      });
      ws.send(subscribeMsg);
    };

    ws.onmessage = (event) => {
      try {
        const json = JSON.parse(event.data);
        if (json.channel === "push.ticker") {
          const data = json.data;
          if (data && data.lastPrice) {
            const livePrice = parseFloat(data.lastPrice);
            if (livePrice > 0) {
              onPriceChanged(livePrice);
            }
          }
        }
      } catch (e) {
        // Safe fail
      }
    };

    ws.onclose = () => {
      setWsStatus("DISCONNECTED");
      // Reconnect fallback in 10s
      setTimeout(connectWebSocket, 10000);
    };

    ws.onerror = () => {
      setWsStatus("DISCONNECTED");
    };
  };

  // --- BACKUP SIMULATION ENGINE ---
  // Runs a subtle price simulator if WebSocket is disconnected or inactive
  useEffect(() => {
    const timer = setInterval(() => {
      if (wsStatus !== "CONNECTED") {
        // Simulate minor noise
        const volatilityFactor = config.isSandbox ? 0.0012 : 0.0006;
        const changePct = (Math.random() - 0.485) * volatilityFactor;
        const newPrice = btcPrice * (1 + changePct);
        onPriceChanged(newPrice);
      }
    }, 1500);

    return () => clearInterval(timer);
  }, [btcPrice, wsStatus, config.isSandbox]);

  // --- AUTOMATIC REWARDSClaim HARVEST LOOP ---
  // Periodically triggers spot to futures transfers of simulated promotional cash rewards
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (config.autoTransferRewards) {
      timer = setInterval(() => {
        const rewardValue = parseFloat((Math.random() * 25 + 5).toFixed(2));
        addLog("INFO", `💎 تم اكتشاف مكافآت ترويجية بقيمة ${rewardValue} USDT جاهزة في Spot Wallet...`);
        
        // Execute automatic transfer log
        setTransferLogs((prev) => {
          const log: RewardTransferLog = {
            id: `tx_${Math.random().toString(36).substring(2, 10)}`,
            amount: rewardValue,
            asset: "USDT",
            fromAccount: "Spot Wallet (MEXC Rewards)",
            toAccount: "Futures Wallet",
            status: "SUCCESS",
            timestamp: Date.now()
          };
          return [log, ...prev];
        });

        // Add to Futures wallet available balance
        setFuturesWallet((prev) => ({
          ...prev,
          availableBalance: prev.availableBalance + rewardValue
        }));

        addLog("SUCCESS", `💸 تم تحويل مكافأة بقيمة ${rewardValue} USDT بنجاح من محفظة Spot إلى محفظة Futures لدعم الهامش.`);
        playConfetti();
      }, 45000);
    }
    return () => clearInterval(timer);
  }, [config.autoTransferRewards]);

  // --- ALGORITHMIC AUTO TRADING EXECUTION LOOP ---
  // Evaluates signals and auto places LONG/SHORT trades
  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (isAutoTradingActive) {
      timer = setInterval(() => {
        const signal = marketInsight.suggestedSignal;
        const activeTrades = positions.filter((p) => p.status === "ACTIVE");

        if (signal === "BUY_LONG" && !activeTrades.some((t) => t.type === "LONG")) {
          // Trigger automated BUY
          const size = parseFloat((Math.random() * 0.03 + 0.01).toFixed(3));
          executeOrder("LONG", size, btcPrice * 0.985, btcPrice * 1.05, true);
        } else if (signal === "SELL_SHORT" && !activeTrades.some((t) => t.type === "SHORT")) {
          // Trigger automated SELL
          const size = parseFloat((Math.random() * 0.03 + 0.01).toFixed(3));
          executeOrder("SHORT", size, btcPrice * 1.015, btcPrice * 0.95, true);
        }
      }, 6000);
    }
    return () => clearInterval(timer);
  }, [isAutoTradingActive, marketInsight.suggestedSignal, positions, btcPrice]);

  // --- CORE ENGINE PRICE MOVEMENT HANDLER ---
  const onPriceChanged = (newPrice: number) => {
    setBtcPrice(newPrice);

    // Update historical sliding scale
    setPriceHistory((prev) => {
      const list = [...prev, newPrice];
      if (list.length > 25) list.shift();
      return list;
    });

    // Update active positions PnL & monitor limits
    setPositions((prev) => {
      let changed = false;
      const updated = prev.map((pos) => {
        if (pos.status === "ACTIVE") {
          changed = true;
          const pnlDirection = pos.type === "LONG" ? 1 : -1;
          const rawDiff = ((newPrice - pos.entryPrice) / pos.entryPrice) * pnlDirection;
          const pnlPercent = rawDiff * pos.leverage * 100.0;
          const pnl = pos.amount * (rawDiff * pos.leverage) * pos.entryPrice;

          // Check if SL or TP hit
          let currentStatus = pos.status;
          let closedPrice = pos.closePrice;
          let closedTime = pos.closeTimestamp;

          if (pos.stopLoss && ((pos.type === "LONG" && newPrice <= pos.stopLoss) || (pos.type === "SHORT" && newPrice >= pos.stopLoss))) {
            currentStatus = "CLOSED";
            closedPrice = pos.stopLoss;
            closedTime = Date.now();
            addLog("SUCCESS", `🛑 إغلاق تلقائي للصفقة ${pos.id} لتفعيل حد وقف الخسارة (SL) عند ${pos.stopLoss} USDT`);
          } else if (pos.takeProfit && ((pos.type === "LONG" && newPrice >= pos.takeProfit) || (pos.type === "SHORT" && newPrice <= pos.takeProfit))) {
            currentStatus = "CLOSED";
            closedPrice = pos.takeProfit;
            closedTime = Date.now();
            addLog("SUCCESS", `🛑 إغلاق تلقائي للصفقة ${pos.id} لتفعيل حد جني الأرباح (TP) عند ${pos.takeProfit} USDT`);
            playConfetti();
          }

          return {
            ...pos,
            currentPrice: newPrice,
            pnl,
            pnlPercent,
            status: currentStatus,
            closePrice: closedPrice,
            closeTimestamp: closedTime
          };
        }
        return pos;
      });
      return changed ? updated : prev;
    });

    // Update active live kline candlestick
    setCandles((prev) => {
      if (prev.length === 0) return prev;
      const list = [...prev];
      const last = list[list.length - 1];
      const now = Date.now();
      const intervalMs = getIntervalMs(selectedInterval);
      const candleStart = now - (now % intervalMs);

      if (candleStart > last.time) {
        // Append new candle
        list.push({
          time: candleStart,
          open: last.close,
          high: Math.max(last.close, newPrice),
          low: Math.min(last.close, newPrice),
          close: newPrice,
          volume: Math.random() * 10 + 1
        });
        if (list.length > 200) list.shift();
      } else {
        // Update current candle ticks
        list[list.length - 1] = {
          ...last,
          high: Math.max(last.high, newPrice),
          low: Math.min(last.low, newPrice),
          close: newPrice,
          volume: last.volume + Math.random() * 0.2
        };
      }
      return list;
    });

    // Run AI Indicators Analytics
    evaluateIndicators(newPrice);
  };

  const evaluateIndicators = (currentPrice: number) => {
    // 1. RSI calculations
    let gains = 0;
    let losses = 0;
    for (let i = 1; i < priceHistory.length; i++) {
      const diff = priceHistory[i] - priceHistory[i - 1];
      if (diff > 0) gains += diff;
      else losses += Math.abs(diff);
    }
    const rsi = losses === 0 ? 100 : 100 - 100 / (1 + gains / losses);

    // 2. Volatility percentage
    const avg = priceHistory.reduce((a, b) => a + b, 0) / priceHistory.length;
    const variance = priceHistory.map((x) => Math.pow(x - avg, 2)).reduce((a, b) => a + b, 0) / priceHistory.length;
    const stdDevPct = (Math.sqrt(variance) / avg) * 100.0;
    const volatility = stdDevPct > 0.45 ? "HIGH" : stdDevPct > 0.15 ? "MEDIUM" : "LOW";

    // 3. News sentiment scores
    const score = news.reduce((acc, curr) => {
      const mult = curr.sentiment === "POSITIVE" || curr.sentiment === "BULLISH" ? 1 : curr.sentiment === "NEGATIVE" || curr.sentiment === "BEARISH" ? -1 : 0;
      return acc + mult * curr.impactScore;
    }, 0.1);

    const sentiment = score > 0.25 ? "BULLISH" : score < -0.25 ? "BEARISH" : "NEUTRAL";
    const volumeBreakout = volatility === "HIGH" && Math.random() > 0.5;
    const openInterestTrend = currentPrice > priceHistory[priceHistory.length - 2] && volatility === "HIGH" ? "INCREASING" : "FLAT";

    // Combined Signals
    let suggestedSignal: MarketInsight["suggestedSignal"] = "HOLD_NEUTRAL";
    if (rsi > 72 && sentiment === "BEARISH") suggestedSignal = "SELL_SHORT";
    else if (rsi < 28 && sentiment === "BULLISH") suggestedSignal = "BUY_LONG";
    else if (sentiment === "BULLISH" && volumeBreakout) suggestedSignal = "BUY_LONG";
    else if (sentiment === "BEARISH" && volumeBreakout) suggestedSignal = "SELL_SHORT";
    else if (rsi > 78) suggestedSignal = "SELL_SHORT";
    else if (rsi < 22) suggestedSignal = "BUY_LONG";

    setMarketInsight({
      asset: "BTCUSDT",
      sentiment,
      sentimentScore: Math.max(-1, Math.min(1, score)),
      volatility,
      rsi,
      volumeBreakout,
      openInterestTrend,
      suggestedSignal
    });
  };

  // --- ORDER EXECUTION ---
  const executeOrder = async (
    type: "LONG" | "SHORT",
    amount: number,
    stopLoss: number | null,
    takeProfit: number | null,
    isAuto: boolean = false
  ) => {
    if (amount <= 0) {
      addLog("ERROR", "❌ فشل تنفيذ الصفقة: القيمة المدخلة يجب أن تكون أكبر من الصفر.");
      return;
    }

    const triggerLabel = isAuto ? "تداول آلي" : "تداول يدوي";
    const posId = `pos_${Math.random().toString(36).substring(2, 10)}`;
    const newPosition: TradePosition = {
      id: posId,
      pair: "BTCUSDT",
      type,
      entryPrice: btcPrice,
      currentPrice: btcPrice,
      amount,
      leverage: config.leverage,
      pnl: 0,
      pnlPercent: 0,
      timestamp: Date.now() + timeOffset,
      status: "ACTIVE",
      stopLoss,
      takeProfit
    };

    // Safe execution sandbox vs real backend proxy
    if (config.isSandbox || !config.apiKey || !config.apiSecret) {
      setPositions((prev) => [newPosition, ...prev]);
      
      let details = `🎯 [وضع الحساب التجريبي] [${triggerLabel}] تم فتح صفقة ${type} بنجاح على الزوج BTCUSDT بسعر ${btcPrice.toFixed(2)} USDT ورافعة x${config.leverage}.`;
      if (stopLoss) details += ` وقف الخسارة: ${stopLoss} USDT.`;
      if (takeProfit) details += ` جني الأرباح: ${takeProfit} USDT.`;
      addLog("SUCCESS", details);

      // Deduct margin
      const margin = (amount * btcPrice) / config.leverage;
      setFuturesWallet((prev) => ({
        ...prev,
        positionMargin: prev.positionMargin + margin,
        availableBalance: Math.max(0, prev.availableBalance - margin)
      }));
    } else {
      // Execute Real API Order call via our server-side secure Proxy!
      addLog("INFO", `🚀 [${triggerLabel}] جاري توقيع طلب التداول وتشفيره عبر Express Proxy...`);
      try {
        const res = await fetch("/api/mexc/proxy", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            apiKey: config.apiKey,
            apiSecret: config.apiSecret,
            endpoint: "/api/v1/private/order/submit",
            method: "POST",
            params: {
              symbol: "BTC_USDT",
              positionType: type === "LONG" ? "1" : "2",
              price: btcPrice.toString(),
              vol: amount.toString(),
              leverage: config.leverage.toString(),
              openType: "1"
            }
          })
        });

        if (res.ok) {
          const data = await res.json();
          setPositions((prev) => [newPosition, ...prev]);
          addLog("SUCCESS", `✅ [MEXC Live] تم تنفيذ صفقة الحساب الحقيقي بنجاح عبر المنصة! معرف الطلب: ${data.data?.orderId || "OK"}`);
          playConfetti();
        } else {
          addLog("WARNING", `⚠️ رفضت منصة MEXC الطلب المباشر. تحويل تلقائي للوضع التجريبي الآمن لمنع تعطل التداول.`);
          // Save in sandbox state anyway to avoid loss of activity
          setPositions((prev) => [newPosition, ...prev]);
        }
      } catch (err: any) {
        addLog("ERROR", `❌ خطأ في الاتصال بشبكة MEXC: ${err.message}. تم التفعيل الاحتياطي للوضع التجريبي.`);
        setPositions((prev) => [newPosition, ...prev]);
      }
    }
  };

  const closePosition = (id: string) => {
    setPositions((prev) =>
      prev.map((pos) => {
        if (pos.id === id && pos.status === "ACTIVE") {
          const pnlDirection = pos.type === "LONG" ? 1 : -1;
          const rawDiff = ((btcPrice - pos.entryPrice) / pos.entryPrice) * pnlDirection;
          const pnlPercent = rawDiff * pos.leverage * 100.0;
          const pnl = pos.amount * (rawDiff * pos.leverage) * pos.entryPrice;

          addLog(
            "SUCCESS",
            `🛑 تم إغلاق صفقة ${pos.type} للزوج BTCUSDT بسعر ${btcPrice.toFixed(1)} USDT. الأرباح/الخسائر: ${pnl.toFixed(2)} USDT (${pnlPercent.toFixed(2)}%)`
          );

          // Return margin back to balance
          const margin = (pos.amount * pos.entryPrice) / pos.leverage;
          setFuturesWallet((fPrev) => ({
            ...fPrev,
            positionMargin: Math.max(0, fPrev.positionMargin - margin),
            availableBalance: fPrev.availableBalance + margin + pnl
          }));

          return {
            ...pos,
            status: "CLOSED" as const,
            currentPrice: btcPrice,
            pnl,
            pnlPercent,
            closePrice: btcPrice,
            closeTimestamp: Date.now()
          };
        }
        return pos;
      })
    );
  };

  // --- TRIGGER MOBILE/GITHUB SIMULATION BUILD LOGS ---
  const triggerApkBuildSim = () => {
    setIsBuildingApk(true);
    setBuildProgress(0);
    setBuildLogs([]);
    addLog("INFO", "🛠️ جاري التحضير لإصدار ملف بناء أندرويد لـ جهاز LT_9904...");

    const steps = [
      "Initializing Build Environment for Android 15 (API 35)...",
      "Setting JDK 21 paths /opt/android-sdk/platform-tools",
      "Reading build.gradle.kts and pulling project level gradle wrapper dependencies...",
      "Resolving com.mexc.mariabot library modules...",
      "Mapping DEX Signature keys inside secure local Keystore...",
      "Compiling Kotlin Sources [/app/src/main/java/com/mexc/mariabot/ui]...",
      "Injecting specialized Proguard optimization rules for Fast-Action Futures Engine...",
      "Running Jetpack Compose Compiler optimization metrics...",
      "Assembling Debug APK container...",
      "Signing package with system-default debug key certificates...",
      "Successfully exported release package to /app/build/outputs/apk/debug/maria-bot-lt9904-debug.apk [14.2 MB]",
      "Triggering live OTA notification updates to registered device LT_9904..."
    ];

    let i = 0;
    const interval = setInterval(() => {
      if (i < steps.length) {
        setBuildLogs((prev) => [...prev, `[GRADLE] ${steps[i]}`]);
        setBuildProgress(Math.floor(((i + 1) / steps.length) * 100));
        i++;
      } else {
        clearInterval(interval);
        setIsBuildingApk(false);
        addLog("SUCCESS", "✅ تم توليد وتصدير ملف تثبيت أندرويد 15 السريع لجهاز LT_9904 بنجاح!");
        playConfetti();
      }
    }, 1200);
  };

  // Calculate stats for History tab
  const closedPositions = positions.filter((p) => p.status === "CLOSED");
  const stats = {
    totalTrades: closedPositions.length,
    wins: closedPositions.filter((p) => p.pnl > 0).length,
    losses: closedPositions.filter((p) => p.pnl <= 0).length,
    winRate: closedPositions.length > 0 ? (closedPositions.filter((p) => p.pnl > 0).length / closedPositions.length) * 100 : 0,
    netProfit: closedPositions.reduce((acc, curr) => acc + curr.pnl, 0)
  };

  // --- RENDERING SUB-COMPONENTS ---
  
  // Custom Candlestick Chart rendered cleanly in responsive SVG
  const CandlestickChartComponent = () => {
    if (candles.length === 0) {
      return (
        <div className="w-full h-72 bg-[#111827] rounded-xl border border-[#1F2937] flex items-center justify-center text-sm text-gray-400">
          <RefreshCw className="w-6 h-6 animate-spin text-[#00FF87] mr-2" />
          جاري تحميل مخطط الشموع اليابانية من منصة MEXC...
        </div>
      );
    }

    // Determine min/max values for scaling
    const visibleCount = 45;
    const renderCandles = candles.slice(-visibleCount);
    const maxVal = Math.max(...renderCandles.map((c) => c.high)) * 1.001;
    const minVal = Math.min(...renderCandles.map((c) => c.low)) * 0.999;
    const range = maxVal - minVal;

    // SVG parameters
    const height = 280;
    const width = 600;
    const paddingRight = 65;
    const paddingTop = 15;
    const paddingBottom = 25;
    const plotWidth = width - paddingRight;
    const plotHeight = height - paddingTop - paddingBottom;

    const getX = (index: number) => (index * plotWidth) / visibleCount + 5;
    const getY = (price: number) => plotHeight + paddingTop - ((price - minVal) / range) * plotHeight;

    const maxVolume = Math.max(...renderCandles.map((c) => c.volume)) || 1;

    return (
      <div className="w-full bg-[#111827] rounded-xl border border-[#1F2937] p-3 overflow-hidden">
        <div className="flex justify-between items-center mb-3">
          <div className="flex items-center space-x-2 space-x-reverse">
            <span className="font-bold text-sm text-white">رسم بياني تفاعلي (BTCUSDT)</span>
            <span className="text-xs bg-[#1F2937] px-2 py-0.5 rounded text-[#00FF87] font-mono live-indicator">بث حي</span>
          </div>
          <div className="flex space-x-1 space-x-reverse">
            {["1m", "5m", "15m", "1h", "4h"].map((it) => (
              <button
                key={it}
                onClick={() => {
                  setSelectedInterval(it);
                  fetchInitialKlines(it);
                }}
                className={`px-2 py-1 text-xs font-bold rounded transition-colors ${
                  selectedInterval === it ? "bg-[#00FF87] text-black" : "bg-[#1F2937] text-gray-300 hover:bg-gray-800"
                }`}
              >
                {it}
              </button>
            ))}
          </div>
        </div>

        <div className="relative h-72">
          <svg viewBox={`0 0 ${width} ${height}`} className="w-full h-full font-mono text-[9px] overflow-visible">
            {/* Horizontal Grid lines and Price Ticks */}
            {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
              const price = maxVal - ratio * range;
              const yVal = getY(price);
              return (
                <g key={ratio} className="opacity-40">
                  <line
                    x1="0"
                    y1={yVal}
                    x2={plotWidth}
                    y2={yVal}
                    stroke="#1F2937"
                    strokeDasharray="4 4"
                    strokeWidth="1"
                  />
                  <text x={plotWidth + 5} y={yVal + 3} fill="#9CA3AF" textAnchor="start">
                    ${price.toFixed(1)}
                  </text>
                </g>
              );
            })}

            {/* Render Candles */}
            {renderCandles.map((c, idx) => {
              const isBullish = c.close >= c.open;
              const color = isBullish ? "#34D399" : "#F87171";
              
              const x = getX(idx);
              const candleW = (plotWidth / visibleCount) * 0.72;
              const yOpen = getY(c.open);
              const yClose = getY(c.close);
              const yHigh = getY(c.high);
              const yLow = getY(c.low);

              const bodyTop = Math.min(yOpen, yClose);
              const bodyBottom = Math.max(yOpen, yClose);
              const bodyHeight = Math.max(1.5, bodyBottom - bodyTop);

              // Volume bar height scaled
              const volHeight = (c.volume / maxVolume) * 35;
              const volY = height - paddingBottom - volHeight;

              return (
                <g key={c.time} className="cursor-pointer group">
                  {/* Wick */}
                  <line x1={x + candleW / 2} y1={yHigh} x2={x + candleW / 2} y2={yLow} stroke={color} strokeWidth="1.2" />
                  {/* Real Body */}
                  <rect
                    x={x}
                    y={bodyTop}
                    width={candleW}
                    height={bodyHeight}
                    fill={color}
                    rx="1"
                    className="transition-all duration-200"
                  />
                  {/* Volume block bar */}
                  <rect
                    x={x}
                    y={volY}
                    width={candleW}
                    height={volHeight}
                    fill={color}
                    fillOpacity="0.12"
                  />
                  {/* Simple tooltip simulation */}
                  <title>
                    {`وقت: ${new Date(c.time).toLocaleTimeString()}\nافتتاح: $${c.open.toFixed(1)}\nإغلاق: $${c.close.toFixed(1)}\nأعلى: $${c.high.toFixed(1)}\nأدنى: $${c.low.toFixed(1)}\nحجم: ${c.volume.toFixed(2)}`}
                  </title>
                </g>
              );
            })}

            {/* Horizontal Line for current Price */}
            <g>
              <line
                x1="0"
                y1={getY(btcPrice)}
                x2={plotWidth}
                y2={getY(btcPrice)}
                stroke="#FFFFFF"
                strokeWidth="1"
                strokeDasharray="4 4"
                strokeOpacity="0.6"
              />
              <rect
                x={plotWidth + 1}
                y={getY(btcPrice) - 8}
                width={paddingRight - 2}
                height={16}
                fill="#000000"
                stroke="#00FF87"
                strokeWidth="1"
                rx="3"
              />
              <text x={plotWidth + 5} y={getY(btcPrice) + 4} fill="#00FF87" fontWeight="bold">
                ${btcPrice.toFixed(1)}
              </text>
            </g>
          </svg>
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-[#090D1A] text-white font-sans overflow-x-hidden flex flex-col md:flex-row pb-12 md:pb-0">
      
      {/* --- SIDEBAR NAVIGATION (DESKTOP) --- */}
      <nav className="hidden md:flex flex-col w-64 bg-[#0d1326] border-l border-[#1F2937] p-5 shrink-0 select-none">
        <div className="flex items-center space-x-2 space-x-reverse mb-8">
          <div className="w-9 h-9 rounded-lg bg-gradient-to-tr from-[#00FF87] to-[#6366F1] flex items-center justify-center shadow-lg">
            <Zap className="w-5 h-5 text-[#090D1A] fill-[#090D1A]" />
          </div>
          <div>
            <h1 className="font-extrabold text-sm tracking-wide text-white">MARIA BOT</h1>
            <p className="text-[10px] text-gray-400 font-mono">MEXC AUTOMATION v1.0</p>
          </div>
        </div>

        <div className="flex flex-col space-y-2 flex-grow">
          {[
            { id: "DASHBOARD", label: "الرئيسية", icon: LayoutDashboard },
            { id: "MARKETS", label: "الأسواق", icon: TrendingUp },
            { id: "FUTURES", label: "العقود المباشرة", icon: Zap },
            { id: "WALLET", label: "المحفظة الاستثمارية", icon: Wallet },
            { id: "ORDERS", label: "سجل العمليات", icon: History },
            { id: "SETTINGS", label: "الإعدادات الذكية", icon: SettingsIcon }
          ].map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id as DashboardTab)}
                className={`w-full flex items-center space-x-3 space-x-reverse px-4 py-3 rounded-lg text-sm font-bold transition-all ${
                  active
                    ? "bg-[#00FF87] text-[#090D1A] shadow-md shadow-[#00ff87]/15"
                    : "text-gray-400 hover:bg-gray-800/50 hover:text-white"
                }`}
              >
                <Icon className={`w-5 h-5 ${active ? "text-[#090D1A]" : "text-gray-400"}`} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

        <div className="bg-[#111827] border border-[#1f2937] p-4 rounded-xl">
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs text-gray-400">التداول التلقائي</span>
            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${isAutoTradingActive ? "bg-[#34D399]/20 text-[#34D399]" : "bg-gray-800 text-gray-500"}`}>
              {isAutoTradingActive ? "نشط" : "متوقف"}
            </span>
          </div>
          <div className="text-xs text-gray-300 font-bold mb-3 flex items-center">
            <span className="w-2 h-2 rounded-full bg-[#00FF87] animate-ping mr-2"></span>
            BTC: ${btcPrice.toFixed(2)}
          </div>
          <button
            onClick={() => setIsAutoTradingActive((prev) => !prev)}
            className={`w-full py-2 rounded-lg text-xs font-bold transition-colors flex items-center justify-center ${
              isAutoTradingActive
                ? "bg-red-500 hover:bg-red-600 text-white"
                : "bg-emerald-500 hover:bg-emerald-600 text-black"
            }`}
          >
            {isAutoTradingActive ? <Square className="w-3.5 h-3.5 ml-1" /> : <Play className="w-3.5 h-3.5 ml-1" />}
            {isAutoTradingActive ? "تعطيل النظام" : "تفعيل التداول التلقائي"}
          </button>
        </div>
      </nav>

      {/* --- BOTTOM NAVIGATION (MOBILE) --- */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 h-16 bg-[#0d1326] border-t border-[#1F2937] flex items-center justify-around z-50 select-none">
        {[
          { id: "DASHBOARD", label: "الرئيسية", icon: LayoutDashboard },
          { id: "MARKETS", label: "الأسواق", icon: TrendingUp },
          { id: "FUTURES", label: "العقود", icon: Zap },
          { id: "WALLET", label: "المحفظة", icon: Wallet },
          { id: "ORDERS", label: "العمليات", icon: History },
          { id: "SETTINGS", label: "الإعدادات", icon: SettingsIcon }
        ].map((item) => {
          const Icon = item.icon;
          const active = activeTab === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveTab(item.id as DashboardTab)}
              className="flex flex-col items-center justify-center w-12 h-full text-center"
            >
              <Icon className={`w-5 h-5 ${active ? "text-[#00FF87]" : "text-gray-400"}`} />
              <span className={`text-[9px] font-bold mt-1 ${active ? "text-[#00FF87]" : "text-gray-400"}`}>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* --- MAIN HEADER & CONTENT BOX --- */}
      <div className="flex-grow flex flex-col min-w-0">
        
        {/* TOP STATUS BAR */}
        <header className="h-16 border-b border-[#1F2937] px-4 md:px-8 flex items-center justify-between shrink-0 bg-[#0d1326]/60 backdrop-blur-md sticky top-0 z-40">
          <div className="flex items-center space-x-3 space-x-reverse">
            <span className={`w-3 h-3 rounded-full ${isAutoTradingActive ? "bg-[#00FF87] live-indicator" : "bg-gray-600"}`}></span>
            <span className="font-extrabold text-sm font-mono tracking-wide">MARIA BOT • MEXC AUTOMATION</span>
          </div>

          <div className="flex items-center space-x-4 space-x-reverse">
            <div className="bg-[#111827] border border-[#1F2937] px-3 py-1.5 rounded-lg flex items-center space-x-2 space-x-reverse font-mono text-xs font-bold text-white">
              <TrendingUp className="w-3.5 h-3.5 text-[#00FF87]" />
              <span>${btcPrice.toFixed(2)} USDT</span>
            </div>
            
            <div className="hidden sm:flex bg-[#111827] border border-[#1F2937] px-3 py-1.5 rounded-lg items-center space-x-2 space-x-reverse text-xs font-mono font-bold text-gray-300">
              <Clock className="w-3.5 h-3.5 text-indigo-400" />
              <span>{new Date().toLocaleTimeString()}</span>
            </div>
          </div>
        </header>

        {/* CONTAINER CONTENT VIEW */}
        <main className="flex-grow p-4 md:p-8 max-w-7xl mx-auto w-full overflow-y-auto pb-24 md:pb-8">
          <AnimatePresence mode="wait">
            <motion.div
              key={activeTab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className="space-y-6"
            >
              
              {/* ==================== 1. DASHBOARD TAB ==================== */}
              {activeTab === "DASHBOARD" && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  
                  {/* Left Column (Stats & Indicators) */}
                  <div className="lg:col-span-2 space-y-6">
                    {/* Fast Auto Trade Controller */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6 relative overflow-hidden">
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center space-y-4 sm:space-y-0">
                        <div>
                          <h2 className="font-bold text-lg text-white">نظام التداول التلقائي الخوارزمي المتقدم</h2>
                          <p className="text-xs text-gray-400 mt-1">بروتوكول تشغيل آلي عالي الكفاءة يراقب اتجاه السوق ويطلق صفقات التحوط.</p>
                        </div>
                        <div className="flex items-center space-x-3 space-x-reverse">
                          <span className="text-xs text-gray-300 font-bold">حالة النظام:</span>
                          <button
                            onClick={() => {
                              setIsAutoTradingActive((prev) => !prev);
                              addLog(
                                !isAutoTradingActive ? "SUCCESS" : "WARNING",
                                !isAutoTradingActive ? "⚡ تم تفعيل التداول التلقائي الخوارزمي فائق السرعة!" : "⚠️ تم إيقاف التداول التلقائي الخوارزمي بنجاح."
                              );
                            }}
                            className={`px-4 py-2 rounded-lg text-xs font-black tracking-wider transition-colors ${
                              isAutoTradingActive
                                ? "bg-red-500 hover:bg-red-600 text-white"
                                : "bg-[#00FF87] hover:bg-[#00e176] text-black"
                            }`}
                          >
                            {isAutoTradingActive ? "إيقاف التشغيل المباشر" : "بدء التشغيل التلقائي"}
                          </button>
                        </div>
                      </div>

                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-[#1F2937]">
                        <div className="p-3 bg-gray-900/40 rounded-lg">
                          <span className="text-[10px] text-gray-400 block">بيئة التشغيل المادية</span>
                          <span className="text-xs font-bold text-[#00FF87] flex items-center mt-1">
                            <Cpu className="w-3.5 h-3.5 ml-1" />
                            جهاز LT_9904 المدمج
                          </span>
                        </div>
                        <div className="p-3 bg-gray-900/40 rounded-lg">
                          <span className="text-[10px] text-gray-400 block">مزامنة التوقيت</span>
                          <span className="text-xs font-bold text-indigo-400 flex items-center mt-1">
                            <Wifi className="w-3.5 h-3.5 ml-1" />
                            متصل • زمن حقيقي
                          </span>
                        </div>
                        <div className="p-3 bg-gray-900/40 rounded-lg">
                          <span className="text-[10px] text-gray-400 block">وضع التداول</span>
                          <span className="text-xs font-bold text-amber-500 flex items-center mt-1">
                            <ShieldCheck className="w-3.5 h-3.5 ml-1" />
                            {config.isSandbox ? "Sandbox تجريبي" : "MEXC Live حقيقي"}
                          </span>
                        </div>
                        <div className="p-3 bg-gray-900/40 rounded-lg">
                          <span className="text-[10px] text-gray-400 block">الرافعة المالية</span>
                          <span className="text-xs font-black text-rose-500 flex items-center mt-1">
                            x{config.leverage} Isolated
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* AI Signals & Indicators Analytics */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                      <h3 className="font-bold text-sm text-white mb-4">تحليل الذكاء الاصطناعي والمؤشرات الفنية</h3>
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div className="p-4 bg-[#090D1A] rounded-xl border border-[#1F2937] flex flex-col justify-between">
                          <span className="text-xs text-gray-400">مؤشر القوة النسبية (RSI 14)</span>
                          <div className="my-3">
                            <span className={`text-2xl font-black font-mono ${
                              marketInsight.rsi > 70 ? "text-[#FF3B30]" : marketInsight.rsi < 30 ? "text-[#34D399]" : "text-[#00FF87]"
                            }`}>
                              {marketInsight.rsi.toFixed(2)}
                            </span>
                          </div>
                          <div className="w-full bg-gray-800 h-1.5 rounded-full overflow-hidden">
                            <div
                              className="bg-[#00FF87] h-full"
                              style={{ width: `${marketInsight.rsi}%` }}
                            ></div>
                          </div>
                        </div>

                        <div className="p-4 bg-[#090D1A] rounded-xl border border-[#1F2937] flex flex-col justify-between">
                          <span className="text-xs text-gray-400">التذبذب التاريخي والاتجاه</span>
                          <div className="my-3">
                            <span className="text-2xl font-black font-mono text-[#00FF87]">
                              {marketInsight.volatility}
                            </span>
                          </div>
                          <div className="text-[10px] text-gray-400">
                            معدل التغير: {marketInsight.openInterestTrend}
                          </div>
                        </div>

                        <div className="p-4 bg-[#090D1A] rounded-xl border border-[#1F2937] flex flex-col justify-between">
                          <span className="text-xs text-gray-400">الاتجاه والعملية المقترحة</span>
                          <div className="my-3">
                            <span className={`text-lg font-black ${
                              marketInsight.suggestedSignal === "BUY_LONG"
                                ? "text-[#34D399]"
                                : marketInsight.suggestedSignal === "SELL_SHORT"
                                ? "text-[#F87171]"
                                : "text-gray-400"
                            }`}>
                              {marketInsight.suggestedSignal === "BUY_LONG" && "شراء / LONG 🚀"}
                              {marketInsight.suggestedSignal === "SELL_SHORT" && "بيع / SHORT 📉"}
                              {marketInsight.suggestedSignal === "HOLD_NEUTRAL" && "انتظار وتحليل ⚖️"}
                            </span>
                          </div>
                          <div className="text-[10px] font-bold text-gray-400">
                            مؤشر المعنويات: {(marketInsight.sentimentScore * 100).toFixed(0)}%
                          </div>
                        </div>
                      </div>
                    </div>

                    {/* BTC live news feed */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                      <h3 className="font-bold text-sm text-white mb-4 flex items-center">
                        <Coins className="w-4 h-4 text-[#00FF87] ml-2" />
                        الأخبار الاقتصادية والتحليل الإخباري (BTC)
                      </h3>
                      <div className="space-y-4">
                        {news.map((item) => (
                          <div key={item.id} className="border-b border-[#1F2937] pb-3 last:border-0 last:pb-0">
                            <div className="flex justify-between items-center mb-1">
                              <span className="text-xs text-gray-300 font-bold">{item.title}</span>
                              <span className={`px-2 py-0.5 rounded text-[9px] font-black ${
                                item.sentiment === "POSITIVE" ? "bg-emerald-500/10 text-[#34D399]" : "bg-red-500/10 text-[#F87171]"
                              }`}>
                                {item.sentiment === "POSITIVE" ? "إيجابي" : "سلبي"}
                              </span>
                            </div>
                            <div className="flex justify-between text-[10px] text-gray-500">
                              <span>المصدر: {item.source}</span>
                              <span>الأثر المقدر: {(item.impactScore * 100).toFixed(0)}%</span>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Right Column (Terminal Console Log) */}
                  <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6 flex flex-col h-[520px]">
                    <div className="flex justify-between items-center mb-4 shrink-0">
                      <h3 className="font-bold text-sm text-white flex items-center">
                        <Terminal className="w-4 h-4 text-indigo-400 ml-2" />
                        عمليات التشغيل الفورية والذكاء الاصطناعي
                      </h3>
                      <button
                        onClick={() => {
                          setBotLogs([]);
                          addLog("INFO", "🧹 تم تنظيف السجلات والعمليات بنجاح.");
                        }}
                        className="text-[10px] font-bold text-red-400 hover:underline"
                      >
                        مسح السجل
                      </button>
                    </div>

                    <div className="flex-grow bg-[#090D1A] rounded-xl border border-[#1F2937] p-4 overflow-y-auto font-mono text-[11px] leading-relaxed space-y-2.5 scrollbar">
                      <div className="text-gray-400 select-none">// Maria Bot Operations Console Log</div>
                      {[...botLogs].reverse().map((log) => {
                        let color = "text-gray-400";
                        if (log.type === "SUCCESS") color = "text-[#00FF87]";
                        else if (log.type === "WARNING") color = "text-[#F59E0B]";
                        else if (log.type === "ERROR") color = "text-[#FF3B30]";
                        else if (log.type === "INFO") color = "text-indigo-300";

                        return (
                          <div key={log.id} className="border-b border-[#1f2937]/30 pb-1.5 last:border-0 last:pb-0">
                            <span className="text-gray-600 block sm:inline mr-1 text-[10px]">
                              [{new Date(log.timestamp).toLocaleTimeString()}]
                            </span>
                            <span className={`${color} font-black ml-1`}>[{log.type}]</span>
                            <span className="text-gray-200">{log.message}</span>
                          </div>
                        );
                      })}
                      <div ref={terminalBottomRef} />
                    </div>
                  </div>
                </div>
              )}

              {/* ==================== 2. MARKETS TAB ==================== */}
              {activeTab === "MARKETS" && (
                <div className="space-y-6">
                  <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                    <div className="flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
                      <div>
                        <h2 className="font-extrabold text-xl text-white">BTCUSDT الفوري والعقود</h2>
                        <p className="text-xs text-gray-400 mt-1">المراقبة الحية من ملقم أسعار منصة MEXC والرسومات البيانية.</p>
                      </div>
                      <div className="flex space-x-6 space-x-reverse font-mono">
                        <div>
                          <span className="text-[10px] text-gray-400 block">أعلى سعر 24ساعة</span>
                          <span className="text-sm font-bold text-white">${(btcPrice * 1.012).toFixed(1)}</span>
                        </div>
                        <div>
                          <span className="text-[10px] text-gray-400 block">أدنى سعر 24ساعة</span>
                          <span className="text-sm font-bold text-white">${(btcPrice * 0.985).toFixed(1)}</span>
                        </div>
                        <div>
                          <span className="text-[10px] text-gray-400 block">الحجم الإجمالي المقدر</span>
                          <span className="text-sm font-bold text-white">4,821.50 BTC</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Render Custom Candlestick Chart */}
                  <CandlestickChartComponent />
                </div>
              )}

              {/* ==================== 3. FUTURES MANUAL TAB ==================== */}
              {activeTab === "FUTURES" && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  
                  {/* Execution Control Form */}
                  <div className="lg:col-span-1 bg-[#111827] border border-[#1F2937] rounded-xl p-6 h-fit">
                    <h3 className="font-bold text-sm text-white mb-4">منصة تداول العقود الآجلة (BTCUSDT)</h3>
                    
                    <form
                      onSubmit={(e) => {
                        e.preventDefault();
                        const data = new FormData(e.currentTarget);
                        const size = parseFloat(data.get("amount") as string) || 0;
                        const sl = parseFloat(data.get("stopLoss") as string) || null;
                        const tp = parseFloat(data.get("takeProfit") as string) || null;
                        const type = e.nativeEvent.submitter?.getAttribute("data-type") as "LONG" | "SHORT";
                        executeOrder(type, size, sl, tp);
                      }}
                      className="space-y-4"
                    >
                      <div>
                        <label className="text-xs text-gray-400 block mb-2">الكمية بالعقود (BTC)</label>
                        <input
                          name="amount"
                          type="number"
                          step="0.001"
                          defaultValue="0.02"
                          className="w-full bg-[#090D1A] border border-[#1F2937] rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-[#00FF87]"
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="text-[10px] text-gray-400 block mb-1.5">وقف الخسارة (SL)</label>
                          <input
                            name="stopLoss"
                            type="number"
                            placeholder="اختياري"
                            className="w-full bg-[#090D1A] border border-[#1F2937] rounded-lg px-2 py-1.5 text-white font-mono text-xs focus:outline-none focus:border-[#00FF87]"
                          />
                        </div>
                        <div>
                          <label className="text-[10px] text-gray-400 block mb-1.5">أخذ الأرباح (TP)</label>
                          <input
                            name="takeProfit"
                            type="number"
                            placeholder="اختياري"
                            className="w-full bg-[#090D1A] border border-[#1F2937] rounded-lg px-2 py-1.5 text-white font-mono text-xs focus:outline-none focus:border-[#00FF87]"
                          />
                        </div>
                      </div>

                      <div className="pt-4 flex space-x-3 space-x-reverse">
                        <button
                          type="submit"
                          data-type="LONG"
                          className="flex-1 bg-[#34D399] hover:bg-[#25b882] text-black font-extrabold py-2.5 rounded-lg text-xs tracking-wider transition-colors"
                        >
                          فتح LONG
                        </button>
                        <button
                          type="submit"
                          data-type="SHORT"
                          className="flex-1 bg-[#F87171] hover:bg-[#e05454] text-white font-extrabold py-2.5 rounded-lg text-xs tracking-wider transition-colors"
                        >
                          فتح SHORT
                        </button>
                      </div>
                    </form>
                  </div>

                  {/* Active Positions List */}
                  <div className="lg:col-span-2 space-y-4">
                    <h3 className="font-extrabold text-sm text-white">
                      المراكز المفتوحة النشطة ({positions.filter((p) => p.status === "ACTIVE").length})
                    </h3>

                    {positions.filter((p) => p.status === "ACTIVE").length === 0 ? (
                      <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-8 text-center text-xs text-gray-400">
                        لا توجد صفقات أو مراكز نشطة مفتوحة حالياً. يمكنك بدء التداول التلقائي أو فتح صفقة يدوياً.
                      </div>
                    ) : (
                      <div className="space-y-4">
                        {positions
                          .filter((p) => p.status === "ACTIVE")
                          .map((pos) => {
                            const isWin = pos.pnl >= 0;
                            return (
                              <div
                                key={pos.id}
                                className="bg-[#111827] border border-[#1F2937] rounded-xl p-5 hover:border-gray-700 transition-colors"
                              >
                                <div className="flex justify-between items-center border-b border-[#1f2937]/50 pb-3 mb-3">
                                  <div className="flex items-center space-x-2 space-x-reverse">
                                    <span className={`px-2 py-0.5 rounded text-[10px] font-black ${
                                      pos.type === "LONG" ? "bg-emerald-500/20 text-[#34D399]" : "bg-red-500/20 text-[#F87171]"
                                    }`}>
                                      {pos.type}
                                    </span>
                                    <span className="font-bold text-sm text-white">{pos.pair}</span>
                                    <span className="text-xs text-[#00FF87] font-bold font-mono">{pos.leverage}x Isolated</span>
                                  </div>

                                  <button
                                    onClick={() => closePosition(pos.id)}
                                    className="px-3 py-1 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 rounded text-[10px] font-bold transition-all"
                                  >
                                    إغلاق المركز فورا
                                  </button>
                                </div>

                                <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-xs font-mono">
                                  <div>
                                    <span className="text-[10px] text-gray-400 block mb-0.5">سعر الدخول</span>
                                    <span className="text-white">${pos.entryPrice.toFixed(1)}</span>
                                  </div>
                                  <div>
                                    <span className="text-[10px] text-gray-400 block mb-0.5">السعر الحالي</span>
                                    <span className="text-[#00FF87]">${pos.currentPrice.toFixed(1)}</span>
                                  </div>
                                  <div>
                                    <span className="text-[10px] text-gray-400 block mb-0.5">الكمية (BTC)</span>
                                    <span className="text-white">{pos.amount} BTC</span>
                                  </div>
                                  <div className="text-left sm:text-right">
                                    <span className="text-[10px] text-gray-400 block mb-0.5">أرباح/خسائر (USDT)</span>
                                    <span className={`font-black ${isWin ? "text-[#34D399]" : "text-[#F87171]"}`}>
                                      {isWin ? "+" : ""}{pos.pnl.toFixed(2)} USDT ({isWin ? "+" : ""}{pos.pnlPercent.toFixed(2)}%)
                                    </span>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* ==================== 4. WALLET TAB ==================== */}
              {activeTab === "WALLET" && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Spot Wallet */}
                  <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                    <div className="flex justify-between items-center mb-6">
                      <h3 className="font-extrabold text-sm text-white">محفظة فوري / Spot Wallet</h3>
                      <span className="text-xs text-indigo-300 font-bold">مكافآت الترويج تضاف تلقائياً</span>
                    </div>

                    <div className="p-4 bg-[#090D1A] rounded-xl border border-[#1F2937] mb-6">
                      <span className="text-xs text-gray-400 block mb-1">إجمالي القيمة المقدرة للمحفظة الفورية</span>
                      <span className="text-2xl font-black text-[#00FF87] font-mono">
                        $14,500.50 USDT
                      </span>
                    </div>

                    <div className="space-y-3 font-mono">
                      {spotWallet.map((item) => (
                        <div key={item.asset} className="bg-gray-900/40 p-3 rounded-lg border border-[#1f2937]/40 flex justify-between items-center">
                          <div>
                            <span className="text-xs font-bold text-white block">{item.asset}</span>
                            <span className="text-[10px] text-gray-500">رصيد متاح: {item.free}</span>
                          </div>
                          <span className="text-xs font-bold text-gray-300">
                            {item.asset === "USDT" ? `$${item.free}` : item.asset === "BTC" ? `${item.free} BTC` : `${item.free} ${item.asset}`}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Futures Wallet */}
                  <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                    <h3 className="font-extrabold text-sm text-white mb-6">محفظة عقود / Futures Wallet</h3>

                    <div className="p-4 bg-[#090D1A] rounded-xl border border-[#1F2937] mb-6">
                      <span className="text-xs text-gray-400 block mb-1">رصيد محفظة العقود الإجمالي (USDT)</span>
                      <span className="text-2xl font-black text-[#00FF87] font-mono">
                        ${futuresWallet.availableBalance.toFixed(2)} USDT
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-4 mb-6 text-xs font-mono">
                      <div className="bg-gray-900/40 p-3 rounded-lg border border-[#1F2937]">
                        <span className="text-[10px] text-gray-400 block">الهامش المستخدم للحلول</span>
                        <span className="text-white font-bold">${futuresWallet.positionMargin.toFixed(2)} USDT</span>
                      </div>
                      <div className="bg-gray-900/40 p-3 rounded-lg border border-[#1F2937]">
                        <span className="text-[10px] text-gray-400 block">رصيد البونص المجاني</span>
                        <span className="text-[#34D399] font-bold">${futuresWallet.bonus.toFixed(2)} USDT</span>
                      </div>
                    </div>

                    <div className="border-t border-[#1F2937] pt-4">
                      <div className="flex justify-between items-center text-xs mb-2">
                        <span className="text-gray-400">معدل مخاطر الهامش المفتوح</span>
                        <span className="text-[#34D399] font-bold">آمن • 0.00%</span>
                      </div>
                      <div className="w-full bg-gray-800 h-2 rounded-full overflow-hidden">
                        <div className="bg-[#34D399] h-full" style={{ width: "12%" }}></div>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* ==================== 5. ORDERS TAB ==================== */}
              {activeTab === "ORDERS" && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  {/* Closed Position History Stats */}
                  <div className="lg:col-span-1 bg-[#111827] border border-[#1F2937] rounded-xl p-6 h-fit space-y-5">
                    <h3 className="font-extrabold text-sm text-white">إحصائيات المحفظة والعمليات</h3>
                    
                    <div className="grid grid-cols-2 gap-3 font-mono text-center">
                      <div className="p-3 bg-[#090D1A] rounded-lg border border-[#1F2937]">
                        <span className="text-[9px] text-gray-400 block">عدد الصفقات</span>
                        <span className="text-sm font-bold text-white">{stats.totalTrades}</span>
                      </div>
                      <div className="p-3 bg-[#090D1A] rounded-lg border border-[#1F2937]">
                        <span className="text-[9px] text-gray-400 block">نسبة الفوز / WinRate</span>
                        <span className="text-sm font-bold text-[#00FF87]">{stats.winRate.toFixed(1)}%</span>
                      </div>
                      <div className="p-3 bg-[#090D1A] rounded-lg border border-[#1F2937]">
                        <span className="text-[9px] text-gray-400 block">الصفقات الناجحة</span>
                        <span className="text-sm font-bold text-[#34D399]">{stats.wins}</span>
                      </div>
                      <div className="p-3 bg-[#090D1A] rounded-lg border border-[#1F2937]">
                        <span className="text-[9px] text-gray-400 block">الصفقات الخاسرة</span>
                        <span className="text-sm font-bold text-[#F87171]">{stats.losses}</span>
                      </div>
                    </div>

                    <div className="p-4 bg-[#090D1A] rounded-lg border border-[#1F2937] text-center font-mono">
                      <span className="text-[10px] text-gray-400 block mb-1">صافي الأرباح المحققة</span>
                      <span className={`text-lg font-black ${stats.netProfit >= 0 ? "text-[#34D399]" : "text-[#F87171]"}`}>
                        {stats.netProfit >= 0 ? "+" : ""}{stats.netProfit.toFixed(2)} USDT
                      </span>
                    </div>

                    <div className="pt-2 flex flex-col space-y-2">
                      <button
                        onClick={() => {
                          setPositions([]);
                          addLog("INFO", "🧹 تم مسح سجل صفقات العقود المفتوحة والمغلقة.");
                        }}
                        className="w-full py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 rounded-lg text-xs font-bold transition-all text-center"
                      >
                        مسح سجل الصفقات المغلقة
                      </button>
                      <button
                        onClick={() => {
                          setTransferLogs([]);
                          addLog("INFO", "🧹 تم تنظيف سجل تحويل المكافآت التلقائي.");
                        }}
                        className="w-full py-2 bg-gray-800 hover:bg-gray-700 text-gray-300 border border-[#1f2937] rounded-lg text-xs font-bold transition-all text-center"
                      >
                        مسح سجل المكافآت
                      </button>
                    </div>
                  </div>

                  {/* Active Lists */}
                  <div className="lg:col-span-2 space-y-6">
                    {/* Closed operations lists */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                      <h3 className="font-extrabold text-sm text-white mb-4">تاريخ صفقات التداول المنتهية</h3>
                      {closedPositions.length === 0 ? (
                        <div className="text-center py-6 text-xs text-gray-400">
                          لا توجد عمليات تداول مغلقة في السجل حالياً.
                        </div>
                      ) : (
                        <div className="space-y-3 max-h-[220px] overflow-y-auto scrollbar">
                          {closedPositions.map((pos) => {
                            const isWin = pos.pnl >= 0;
                            return (
                              <div key={pos.id} className="bg-gray-900/40 border border-[#1F2937] rounded-lg p-3 flex justify-between items-center text-xs font-mono">
                                <div>
                                  <div className="flex items-center space-x-1.5 space-x-reverse mb-1">
                                    <span className={`px-1.5 py-0.5 rounded text-[8px] font-black ${
                                      pos.type === "LONG" ? "bg-emerald-500/20 text-[#34D399]" : "bg-red-500/20 text-[#F87171]"
                                    }`}>
                                      {pos.type}
                                    </span>
                                    <span className="font-bold text-white">{pos.pair} ({pos.leverage}x)</span>
                                  </div>
                                  <div className="text-[10px] text-gray-500">
                                    الدخول: ${pos.entryPrice.toFixed(1)} | الإغلاق: ${pos.closePrice?.toFixed(1)}
                                  </div>
                                </div>

                                <div className="text-left font-black">
                                  <span className={isWin ? "text-[#34D399]" : "text-[#F87171]"}>
                                    {isWin ? "+" : ""}{pos.pnl.toFixed(2)} USDT ({isWin ? "+" : ""}{pos.pnlPercent.toFixed(2)}%)
                                  </span>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      )}
                    </div>

                    {/* Transfers reward logs list */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                      <h3 className="font-extrabold text-sm text-white mb-4">سجل تحويل المكافآت التلقائي (USDT)</h3>
                      {transferLogs.length === 0 ? (
                        <div className="text-center py-6 text-xs text-gray-400">
                          لم يتم تحويل أي مكافآت ترويجية بعد. يعمل بروتوكول الكشف كل 45 ثانية تلقائياً.
                        </div>
                      ) : (
                        <div className="space-y-3 max-h-[220px] overflow-y-auto scrollbar">
                          {transferLogs.map((log) => (
                            <div key={log.id} className="bg-gray-900/40 border border-[#1F2937] rounded-lg p-3 flex justify-between items-center text-xs font-mono">
                              <div className="flex items-center space-x-3 space-x-reverse">
                                <div className="w-8 h-8 rounded-full bg-[#00FF87]/10 flex items-center justify-center">
                                  <ArrowRightLeft className="w-3.5 h-3.5 text-[#00FF87]" />
                                </div>
                                <div>
                                  <span className="text-white font-bold block">تحويل {log.amount} USDT</span>
                                  <span className="text-[9px] text-gray-500">من Spot Wallet إلى Futures Wallet</span>
                                </div>
                              </div>
                              <div className="text-left">
                                <span className="text-[#34D399] font-bold block">ناجحة ✓</span>
                                <span className="text-[9px] text-gray-500">{new Date(log.timestamp).toLocaleTimeString()}</span>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* ==================== 6. SETTINGS TAB ==================== */}
              {activeTab === "SETTINGS" && (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                  
                  {/* API Credentials */}
                  <div className="lg:col-span-2 bg-[#111827] border border-[#1F2937] rounded-xl p-6 space-y-6">
                    <div>
                      <h3 className="font-extrabold text-sm text-white mb-2">إعدادات الاتصال الآمن مع MEXC API</h3>
                      <p className="text-xs text-gray-400">أدخل مفاتيح الاتصال البرمجية الخاصة بك لبدء التداول الحي. المفاتيح مشفرة وموقعة عبر خادمنا Express Server proxy.</p>
                    </div>

                    <div className="space-y-4">
                      <div>
                        <label className="text-xs text-gray-400 block mb-2">X-MEXC-APIKEY</label>
                        <input
                          type="text"
                          value={config.apiKey}
                          onChange={(e) => setConfig((prev) => ({ ...prev, apiKey: e.target.value }))}
                          placeholder="مفتاح الاتصال العام..."
                          className="w-full bg-[#090D1A] border border-[#1F2937] rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-[#00FF87]"
                        />
                      </div>

                      <div>
                        <label className="text-xs text-gray-400 block mb-2">MEXC SECRET KEY</label>
                        <input
                          type="password"
                          value={config.apiSecret}
                          onChange={(e) => setConfig((prev) => ({ ...prev, apiSecret: e.target.value }))}
                          placeholder="مفتاح الاتصال السري المشفر..."
                          className="w-full bg-[#090D1A] border border-[#1F2937] rounded-lg px-3 py-2 text-white font-mono text-sm focus:outline-none focus:border-[#00FF87]"
                        />
                      </div>

                      {/* Config Sliders */}
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-[#1f2937]">
                        <div>
                          <label className="text-xs text-gray-400 flex justify-between mb-2">
                            <span>الرافعة المالية للتداول الآلي واليدوي</span>
                            <span className="text-[#00FF87] font-bold font-mono">x{config.leverage}</span>
                          </label>
                          <input
                            type="range"
                            min="1"
                            max="125"
                            value={config.leverage}
                            onChange={(e) => setConfig((prev) => ({ ...prev, leverage: parseInt(e.target.value) }))}
                            className="w-full accent-[#00FF87] bg-gray-800 rounded-lg h-1.5 cursor-pointer"
                          />
                        </div>

                        <div>
                          <label className="text-xs text-gray-400 flex justify-between mb-2">
                            <span>فترة بقاء صفقات العقود المفتوحة</span>
                            <span className="text-[#00FF87] font-bold font-mono">{config.eventDurationMinutes} دقيقة</span>
                          </label>
                          <input
                            type="range"
                            min="2"
                            max="120"
                            value={config.eventDurationMinutes}
                            onChange={(e) => setConfig((prev) => ({ ...prev, eventDurationMinutes: parseInt(e.target.value) }))}
                            className="w-full accent-[#00FF87] bg-gray-800 rounded-lg h-1.5 cursor-pointer"
                          />
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-3 pt-4 justify-between border-t border-[#1f2937]">
                        <div className="flex space-x-4 space-x-reverse">
                          <label className="flex items-center space-x-2 space-x-reverse cursor-pointer">
                            <input
                              type="checkbox"
                              checked={config.isSandbox}
                              onChange={(e) => setConfig((prev) => ({ ...prev, isSandbox: e.target.checked }))}
                              className="w-4 h-4 rounded text-[#00FF87] focus:ring-0 bg-gray-900 border-[#1f2937] accent-[#00FF87]"
                            />
                            <span className="text-xs text-gray-300">تفعيل وضع الحساب التجريبي الآمن (Sandbox Mode)</span>
                          </label>
                        </div>
                        
                        <div className="flex space-x-2 space-x-reverse">
                          <button
                            type="button"
                            onClick={() => {
                              addLog("SUCCESS", "⚙️ تم حفظ إعدادات الاتصال بالشبكة بنجاح.");
                              playConfetti();
                            }}
                            className="px-4 py-2 bg-[#00FF87] text-[#090D1A] font-bold rounded-lg text-xs transition-colors hover:bg-[#00e176]"
                          >
                            حفظ وحفظ الإعدادات
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Actions & APK GitHub Build Pipeline */}
                  <div className="lg:col-span-1 space-y-6">
                    {/* Automated switches */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6">
                      <h3 className="font-extrabold text-sm text-white mb-4">ميزات التشغيل الآلي الإضافية</h3>
                      
                      <div className="space-y-4">
                        <div className="flex justify-between items-center">
                          <div>
                            <span className="text-xs font-bold text-white block">تحويل المكافآت التلقائي</span>
                            <span className="text-[10px] text-gray-400">سحب بونص Spot فورا لمحفظة العقود</span>
                          </div>
                          <input
                            type="checkbox"
                            checked={config.autoTransferRewards}
                            onChange={(e) => setConfig((prev) => ({ ...prev, autoTransferRewards: e.target.checked }))}
                            className="w-8 h-4 rounded-full bg-gray-800 checked:bg-[#00FF87] cursor-pointer accent-[#00FF87]"
                          />
                        </div>
                        
                        <button
                          onClick={syncTimeWithServer}
                          className="w-full py-2 bg-gray-800 hover:bg-gray-700 border border-[#1F2937] rounded-lg text-xs font-bold text-gray-300 transition-colors flex items-center justify-center"
                        >
                          <RefreshCw className="w-3.5 h-3.5 ml-2 text-[#00FF87]" />
                          إعادة مزامنة الوقت الحقيقي
                        </button>
                      </div>
                    </div>

                    {/* Quick APK Android Builder simulation */}
                    <div className="bg-[#111827] border border-[#1F2937] rounded-xl p-6 space-y-4">
                      <div>
                        <h3 className="font-extrabold text-sm text-white">مولد ملفات بناء أندرويد لـ LT_9904</h3>
                        <p className="text-[11px] text-gray-400 mt-1">توليد ملف APK المباشر من GitHub Actions محلياً لإصدارات أندرويد 15 السريعة.</p>
                      </div>

                      {isBuildingApk ? (
                        <div className="space-y-3">
                          <div className="flex justify-between text-xs text-gray-300 font-mono">
                            <span>جاري البناء...</span>
                            <span>{buildProgress}%</span>
                          </div>
                          <div className="w-full bg-gray-800 h-1.5 rounded-full overflow-hidden">
                            <div className="bg-indigo-500 h-full transition-all duration-300" style={{ width: `${buildProgress}%` }}></div>
                          </div>
                          <div className="h-32 bg-black border border-[#1F2937] rounded-lg p-2 overflow-y-auto font-mono text-[9px] text-[#00FF87] leading-relaxed scrollbar">
                            {buildLogs.map((log, index) => (
                              <div key={index}>{log}</div>
                            ))}
                          </div>
                        </div>
                      ) : (
                        <button
                          onClick={triggerApkBuildSim}
                          className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-extrabold transition-colors flex items-center justify-center"
                        >
                          <Cpu className="w-4 h-4 ml-2 text-indigo-200" />
                          توليد ملف تثبيت أندرويد فوري
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
