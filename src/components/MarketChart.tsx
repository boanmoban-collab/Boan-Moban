import React, { useState, useRef, useEffect } from "react";
import { ArrowUpRight, Maximize2, Layers } from "lucide-react";
import { KlineData } from "../types";

interface MarketChartProps {
  klines: KlineData[];
  symbol: string;
  interval: string;
  onChangeInterval: (interval: string) => void;
  currentPrice: number;
}

export const MarketChart: React.FC<MarketChartProps> = ({
  klines,
  symbol,
  interval,
  onChangeInterval,
  currentPrice
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 600, height: 350 });
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [mousePos, setMousePos] = useState({ x: 0, y: 0 });

  // Handle resizing dynamically to be responsive in all container sizes
  useEffect(() => {
    if (!containerRef.current) return;
    const observer = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        setDimensions({
          width: Math.max(width, 300),
          height: Math.max(height, 280)
        });
      }
    });
    observer.observe(containerRef.current);
    return () => observer.disconnect();
  }, []);

  const { width, height } = dimensions;
  const paddingRight = 65; // Price scale panel on the right
  const paddingBottom = 30; // Time scale panel at the bottom
  const chartWidth = width - paddingRight;
  const chartHeight = height - paddingBottom;
  const candleAreaHeight = chartHeight * 0.75; // 75% height for candlesticks
  const volumeAreaHeight = chartHeight * 0.22; // 22% height for volume
  const volumeAreaTop = chartHeight * 0.78; // Start volume after candles

  if (klines.length === 0) {
    return (
      <div className="bg-zinc-950 border border-zinc-800 rounded-xl h-[400px] flex items-center justify-center">
        <div className="text-center text-zinc-500 font-mono text-sm">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto mb-4"></div>
          Loading Candlestick Feed...
        </div>
      </div>
    );
  }

  // Calculate scales
  const prices = klines.flatMap((k) => [k.high, k.low]);
  const minPrice = Math.min(...prices) * 0.9995; // 0.05% safety margin
  const maxPrice = Math.max(...prices) * 1.0005;
  const priceRange = maxPrice - minPrice;

  const volumes = klines.map((k) => k.volume);
  const maxVolume = Math.max(...volumes) || 1;

  // Pixel converters
  const getX = (index: number) => {
    return (index / (klines.length - 1)) * (chartWidth - 20) + 10;
  };

  const getY = (price: number) => {
    return candleAreaHeight - ((price - minPrice) / priceRange) * (candleAreaHeight - 20) - 10;
  };

  const getVolY = (volume: number) => {
    return height - paddingBottom - (volume / maxVolume) * (volumeAreaHeight - 10) - 5;
  };

  // Determine active OHLC data
  const activeIndex = hoveredIndex !== null ? hoveredIndex : klines.length - 1;
  const activeKline = klines[activeIndex];

  // Mouse interactivity for crosshair
  const handleMouseMove = (e: React.MouseEvent<SVGSVGElement, MouseEvent>) => {
    if (!containerRef.current) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    // Boundary checks
    if (x >= 0 && x <= chartWidth && y >= 0 && y <= chartHeight) {
      setMousePos({ x, y });
      
      // Calculate nearest kline index
      const ratio = (x - 10) / (chartWidth - 20);
      const index = Math.round(ratio * (klines.length - 1));
      const boundedIndex = Math.max(0, Math.min(klines.length - 1, index));
      setHoveredIndex(boundedIndex);
    } else {
      setHoveredIndex(null);
    }
  };

  const handleMouseLeave = () => {
    setHoveredIndex(null);
  };

  // Format tick numbers nicely
  const formatPrice = (val: number) => {
    return val.toLocaleString(undefined, {
      minimumFractionDigits: val > 1000 ? 2 : 4,
      maximumFractionDigits: val > 1000 ? 2 : 4
    });
  };

  // Calculate horizontal grid lines (price levels)
  const gridCount = 5;
  const gridLines = Array.from({ length: gridCount }).map((_, i) => {
    const price = maxPrice - (i * priceRange) / (gridCount - 1);
    return { y: getY(price), price };
  });

  // Calculate vertical time markers
  const step = Math.max(1, Math.floor(klines.length / 5));
  const timeMarkers = klines
    .filter((_, i) => i % step === 0)
    .map((kline, i) => {
      const idx = klines.indexOf(kline);
      const date = new Date(kline.time);
      let timeStr = date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit", hour12: false });
      if (interval === "1d") {
        timeStr = date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
      }
      return { x: getX(idx), label: timeStr };
    });

  // Latest candle color
  const priceChange = activeKline.close - activeKline.open;
  const isUp = priceChange >= 0;

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden shadow-xl p-4 flex flex-col h-full select-none">
      {/* Header Info Panel */}
      <div className="flex flex-wrap items-center justify-between border-b border-zinc-900 pb-3 mb-3 gap-2">
        <div className="flex items-center space-x-3">
          <div className="bg-zinc-900 px-2.5 py-1 rounded border border-zinc-800 flex items-center space-x-1.5">
            <span className="text-xs font-bold text-blue-400">{symbol.replace("USDT", "")}</span>
            <span className="text-[10px] text-zinc-500">SPOT</span>
          </div>
          <div className="flex items-center space-x-4">
            <span className={`text-base font-mono font-bold ${isUp ? "text-emerald-500" : "text-rose-500"}`}>
              ${formatPrice(activeKline.close)}
            </span>
            <span className={`text-xs font-mono font-medium ${isUp ? "text-emerald-500" : "text-rose-500"}`}>
              {isUp ? "+" : ""}
              {((priceChange / activeKline.open) * 100).toFixed(2)}%
            </span>
          </div>
        </div>

        {/* Dynamic Interval Buttons */}
        <div className="flex items-center space-x-1 bg-zinc-900/60 p-1 rounded-md border border-zinc-800">
          {["1m", "5m", "15m", "1h", "1d"].map((item) => (
            <button
              key={item}
              onClick={() => onChangeInterval(item)}
              className={`px-2.5 py-1 text-[10px] font-mono font-bold rounded cursor-pointer transition-all ${
                interval === item
                  ? "bg-blue-600 text-zinc-100 shadow"
                  : "text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800"
              }`}
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      {/* OHLC Bar Display */}
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 bg-zinc-900/30 px-3 py-1.5 rounded-lg border border-zinc-900/80 mb-3 text-[11px] font-mono">
        <span className="text-zinc-500">
          O: <span className={isUp ? "text-emerald-500" : "text-rose-500"}>{formatPrice(activeKline.open)}</span>
        </span>
        <span className="text-zinc-500">
          H: <span className="text-zinc-300">{formatPrice(activeKline.high)}</span>
        </span>
        <span className="text-zinc-500">
          L: <span className="text-zinc-300">{formatPrice(activeKline.low)}</span>
        </span>
        <span className="text-zinc-500">
          C: <span className={isUp ? "text-emerald-500" : "text-rose-500"}>{formatPrice(activeKline.close)}</span>
        </span>
        <span className="text-zinc-500 ml-auto hidden sm:inline">
          Vol: <span className="text-zinc-300">{activeKline.volume.toLocaleString(undefined, { maximumFractionDigits: 2 })}</span>
        </span>
      </div>

      {/* SVG Canvas Area */}
      <div ref={containerRef} className="flex-1 relative min-h-[220px] bg-zinc-950">
        <svg
          width={width}
          height={height}
          onMouseMove={handleMouseMove}
          onMouseLeave={handleMouseLeave}
          className="absolute inset-0 cursor-crosshair overflow-visible"
        >
          {/* Clip path for drawing region */}
          <defs>
            <clipPath id="chart-area">
              <rect x="0" y="0" width={chartWidth} height={chartHeight} />
            </clipPath>
          </defs>

          {/* Grid lines (horizontal) */}
          {gridLines.map((line, i) => (
            <g key={i}>
              <line
                x1="0"
                y1={line.y}
                x2={chartWidth}
                y2={line.y}
                stroke="#18181b"
                strokeWidth="1"
                strokeDasharray="2,2"
              />
              <text
                x={chartWidth + 5}
                y={line.y + 4}
                fill="#71717a"
                fontSize="9"
                fontFamily="JetBrains Mono, monospace"
              >
                {formatPrice(line.price)}
              </text>
            </g>
          ))}

          {/* Time scale tick lines */}
          {timeMarkers.map((marker, i) => (
            <g key={i}>
              <line
                x1={marker.x}
                y1="0"
                x2={marker.x}
                y2={chartHeight}
                stroke="#18181b"
                strokeWidth="1"
                strokeDasharray="2,2"
              />
              <text
                x={marker.x}
                y={chartHeight + 16}
                textAnchor="middle"
                fill="#71717a"
                fontSize="9"
                fontFamily="JetBrains Mono, monospace"
              >
                {marker.label}
              </text>
            </g>
          ))}

          {/* Draw volume bars in clipping area */}
          <g clipPath="url(#chart-area)">
            {klines.map((kline, i) => {
              const x = getX(i);
              const volY = getVolY(kline.volume);
              const barWidth = Math.max(1.5, (chartWidth / klines.length) * 0.7);
              const candleUp = kline.close >= kline.open;
              const fill = candleUp ? "rgba(16, 185, 129, 0.2)" : "rgba(244, 63, 94, 0.2)";
              const stroke = candleUp ? "rgba(16, 185, 129, 0.4)" : "rgba(244, 63, 94, 0.4)";

              return (
                <rect
                  key={i}
                  x={x - barWidth / 2}
                  y={volY}
                  width={barWidth}
                  height={height - paddingBottom - volY}
                  fill={fill}
                  stroke={stroke}
                  strokeWidth="0.5"
                />
              );
            })}
          </g>

          {/* Draw Candlesticks */}
          <g clipPath="url(#chart-area)">
            {klines.map((kline, i) => {
              const x = getX(i);
              const highY = getY(kline.high);
              const lowY = getY(kline.low);
              const openY = getY(kline.open);
              const closeY = getY(kline.close);

              const candleUp = kline.close >= kline.open;
              const bodyColor = candleUp ? "#10b981" : "#f43f5e";
              const strokeColor = candleUp ? "#10b981" : "#f43f5e";

              const bodyTop = Math.min(openY, closeY);
              const bodyBottom = Math.max(openY, closeY);
              const bodyHeight = Math.max(1.5, bodyBottom - bodyTop);
              const bodyWidth = Math.max(2, (chartWidth / klines.length) * 0.75);

              return (
                <g key={i} className="transition-all duration-200">
                  {/* Wick */}
                  <line
                    x1={x}
                    y1={highY}
                    x2={x}
                    y2={lowY}
                    stroke={strokeColor}
                    strokeWidth="1.2"
                  />
                  {/* Real Body */}
                  <rect
                    x={x - bodyWidth / 2}
                    y={bodyTop}
                    width={bodyWidth}
                    height={bodyHeight}
                    fill={bodyColor}
                    stroke={strokeColor}
                    strokeWidth="0.5"
                  />
                </g>
              );
            })}
          </g>

          {/* Live Price Line Indicator */}
          <g clipPath="url(#chart-area)">
            {klines.length > 0 && (
              <g>
                <line
                  x1="0"
                  y1={getY(currentPrice)}
                  x2={chartWidth}
                  y2={getY(currentPrice)}
                  stroke={currentPrice >= klines[klines.length - 1].open ? "#10b981" : "#f43f5e"}
                  strokeWidth="1"
                  strokeDasharray="4,3"
                />
                <circle
                  cx={chartWidth}
                  cy={getY(currentPrice)}
                  r="3.5"
                  fill={currentPrice >= klines[klines.length - 1].open ? "#10b981" : "#f43f5e"}
                />
              </g>
            )}
          </g>

          {/* Cursor Interactive Crosshair Overlay */}
          {hoveredIndex !== null && (
            <g>
              {/* Vertical dotted Line */}
              <line
                x1={mousePos.x}
                y1="0"
                x2={mousePos.x}
                y2={chartHeight}
                stroke="#3b82f6"
                strokeWidth="1"
                strokeDasharray="3,3"
              />
              {/* Horizontal dotted Line */}
              <line
                x1="0"
                y1={mousePos.y}
                x2={chartWidth}
                y2={mousePos.y}
                stroke="#3b82f6"
                strokeWidth="1"
                strokeDasharray="3,3"
              />
              {/* Highlight current circle marker on line */}
              <circle
                cx={getX(hoveredIndex)}
                cy={getY(klines[hoveredIndex].close)}
                r="4.5"
                fill="#3b82f6"
                stroke="#09090b"
                strokeWidth="1.5"
              />

              {/* Price Label (right side hover tag) */}
              <g transform={`translate(${chartWidth + 4}, ${mousePos.y - 7})`}>
                <rect
                  x="0"
                  y="0"
                  width="58"
                  height="15"
                  rx="2"
                  fill="#1d4ed8"
                  stroke="#3b82f6"
                  strokeWidth="0.5"
                />
                <text
                  x="29"
                  y="11"
                  textAnchor="middle"
                  fill="#ffffff"
                  fontSize="8.5"
                  fontFamily="JetBrains Mono, monospace"
                  fontWeight="bold"
                >
                  {formatPrice(minPrice + ((candleAreaHeight - mousePos.y) / candleAreaHeight) * priceRange)}
                </text>
              </g>

              {/* Time Label (bottom side hover tag) */}
              <g transform={`translate(${mousePos.x - 30}, ${chartHeight + 1})`}>
                <rect
                  x="0"
                  y="0"
                  width="60"
                  height="14"
                  rx="2"
                  fill="#1d4ed8"
                  stroke="#3b82f6"
                  strokeWidth="0.5"
                />
                <text
                  x="30"
                  y="10"
                  textAnchor="middle"
                  fill="#ffffff"
                  fontSize="8"
                  fontFamily="JetBrains Mono, monospace"
                  fontWeight="bold"
                >
                  {(() => {
                    const date = new Date(klines[hoveredIndex].time);
                    if (interval === "1d") {
                      return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
                    }
                    return date.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit", hour12: false });
                  })()}
                </text>
              </g>
            </g>
          )}
        </svg>
      </div>
    </div>
  );
};
