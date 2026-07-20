import express from "express";
import path from "path";
import crypto from "crypto";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

const app = express();
const PORT = 3000;

app.use(express.json());

// Initialize Gemini SDK with User-Agent telemetry
const apiKey = process.env.GEMINI_API_KEY;
let ai: GoogleGenAI | null = null;
if (apiKey) {
  ai = new GoogleGenAI({
    apiKey: apiKey,
    httpOptions: {
      headers: {
        "User-Agent": "aistudio-build",
      },
    },
  });
} else {
  console.warn("WARNING: GEMINI_API_KEY environment variable is not set. AI Market Intelligence will be unavailable.");
}

// 1. Time Synchronization Endpoint
// Fetches official server time from MEXC to calculate and maintain precision alignment
app.get("/api/mexc/time", async (req, res) => {
  try {
    const startTime = Date.now();
    const response = await fetch("https://api.mexc.com/api/v3/time");
    if (!response.ok) {
      throw new Error(`MEXC time service responded with status ${response.status}`);
    }
    const data = (await response.json()) as { serverTime: number };
    const endTime = Date.now();
    const latency = Math.round((endTime - startTime) / 2);
    
    // Provide serverTime and calculated client-server skew offset
    res.json({
      serverTime: data.serverTime,
      clientTime: endTime,
      latency,
      offset: data.serverTime - (endTime - latency)
    });
  } catch (error: any) {
    res.status(500).json({ error: "Failed to sync time with MEXC", details: error.message });
  }
});

// 2. Secure MEXC Private API Proxy
// Signs and proxies API requests on behalf of the client.
// Receives MEXC credentials dynamically via secure request headers (x-mexc-apikey & x-mexc-apisecret).
// This prevents hardcoding, handles signing server-side, and leaves zero footprint in logs.
app.all("/api/mexc/proxy", async (req, res) => {
  try {
    const method = (req.query.method || req.body.method || "GET") as string;
    const apiPath = (req.query.path || req.body.path) as string;
    const clientParams = (req.query.params || req.body.params || {}) as Record<string, any>;

    if (!apiPath) {
      res.status(400).json({ error: "Missing required parameter 'path'" });
      return;
    }

    // Is it a signed endpoint?
    const requiresSignature = !apiPath.includes("/api/v3/time") && 
                              !apiPath.includes("/api/v3/exchangeInfo") && 
                              !apiPath.includes("/api/v3/depth") && 
                              !apiPath.includes("/api/v3/trades") && 
                              !apiPath.includes("/api/v3/klines") &&
                              !apiPath.includes("/api/v3/ticker");

    const mexcApiKey = req.headers["x-mexc-apikey"] as string;
    const mexcApiSecret = req.headers["x-mexc-apisecret"] as string;

    // Build base request URL
    const baseUrl = "https://api.mexc.com";
    const targetUrl = new URL(apiPath, baseUrl);

    // Validate headers only if signature is required
    if (requiresSignature && !mexcApiKey) {
      res.status(400).json({ error: "X-MEXC-APIKEY header is required for authenticated MEXC requests" });
      return;
    }

    let finalQueryString = "";

    if (requiresSignature) {
      if (!mexcApiSecret) {
        res.status(400).json({ error: "X-MEXC-APISECRET header is required for authenticated requests" });
        return;
      }

      // Sync and add timestamp
      // We accept a client-computed timestamp or generate one
      let timestamp = clientParams.timestamp || Date.now();
      
      // Merge all parameters
      const mergedParams = {
        ...clientParams,
        timestamp,
        recvWindow: clientParams.recvWindow || 10000,
      };

      // Construct query string exactly
      const queryParts = Object.keys(mergedParams)
        .filter(key => mergedParams[key] !== undefined && mergedParams[key] !== null)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(mergedParams[key].toString())}`);
      
      const queryString = queryParts.join("&");
      
      // Sign using HMAC-SHA256
      const signature = crypto
        .createHmac("sha256", mexcApiSecret)
        .update(queryString)
        .digest("hex");

      finalQueryString = `${queryString}&signature=${signature}`;
    } else {
      // Unsigned endpoint
      const queryParts = Object.keys(clientParams)
        .filter(key => clientParams[key] !== undefined && clientParams[key] !== null)
        .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(clientParams[key].toString())}`);
      
      finalQueryString = queryParts.join("&");
    }

    // Attach finalized query parameters to target URL
    if (finalQueryString) {
      targetUrl.search = finalQueryString;
    }

    // Configure proxy fetch request headers
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    if (mexcApiKey) {
      headers["X-MEXC-APIKEY"] = mexcApiKey;
    }

    const requestOptions: RequestInit = {
      method: method.toUpperCase(),
      headers,
    };

    // Forward request to official MEXC endpoint
    const response = await fetch(targetUrl.toString(), requestOptions);
    const textData = await response.text();

    let jsonData;
    try {
      jsonData = JSON.parse(textData);
    } catch {
      jsonData = { text: textData };
    }

    // Forward exact response code and status back to client
    res.status(response.status).json(jsonData);
  } catch (error: any) {
    res.status(500).json({ error: "Proxy connection error", details: error.message });
  }
});

// 3. AI Market Intelligence & Decision Support
// Uses Gemini 3.5 Flash model with Google Search Grounding to research real-time news articles,
// sentiment, technical trend strength, and provide advisory decision support.
app.post("/api/mexc/intelligence", async (req, res) => {
  try {
    const { symbol } = req.body;
    if (!symbol) {
      res.status(400).json({ error: "Missing token symbol (e.g. BTC, ETH)" });
      return;
    }

    if (!ai) {
      res.status(503).json({ 
        error: "Gemini AI engine is not initialized. Please ensure GEMINI_API_KEY is configured in your project secrets." 
      });
      return;
    }

    // Query template specifying search grounding and exact response schema
    const prompt = `Perform high-fidelity research on the cryptocurrency token with symbol '${symbol}'.
Use Google Search Grounding to gather the absolute latest news (sentiment, volatility, regulatory updates, volume trends, and open interest metrics).
Then calculate technical indicator models (RSI trend, MACD, Moving Averages) based on recent news and trend data.

Produce decision support advisory notes indicating sentiment index, technical trends, and overall summary.
You MUST provide strict decision support advice. Remind the trader that predictions do not guarantee profit and they must execute risk management (stop losses).

Format your output strictly to match the requested JSON schema. Make sure all numerical scores are realistic.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        tools: [{ googleSearch: {} }],
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          required: [
            "symbol",
            "sentimentScore",
            "sentimentLabel",
            "newsSummary",
            "technicalIndicators",
            "volatility",
            "trendStrength",
            "decisionSupportAdvice"
          ],
          properties: {
            symbol: { type: Type.STRING },
            sentimentScore: { type: Type.INTEGER, description: "A score from 0 (Extremely Bearish) to 100 (Extremely Bullish)" },
            sentimentLabel: { type: Type.STRING, description: "Bearish, Bullish, Neutral, or highly volatile" },
            newsSummary: { 
              type: Type.ARRAY, 
              items: { type: Type.STRING },
              description: "List of the most relevant news facts or events fetched recently (3-5 items)"
            },
            technicalIndicators: {
              type: Type.OBJECT,
              required: ["trend", "rsi", "macd", "movingAverages"],
              properties: {
                trend: { type: Type.STRING, description: "Short term directional trend" },
                rsi: { type: Type.STRING, description: "Approximate RSI range (e.g., Overbought 72, Neutral 48, Oversold 25)" },
                macd: { type: Type.STRING, description: "MACD status (e.g., Bullish crossover, Bearish convergence)" },
                movingAverages: { type: Type.STRING, description: "MA indicators (e.g., trading above 50-day EMA)" }
              }
            },
            volatility: { type: Type.STRING, description: "High, Medium, or Low" },
            trendStrength: { type: Type.STRING, description: "Strong, Moderate, or Weak" },
            decisionSupportAdvice: { 
              type: Type.STRING, 
              description: "Detailed professional advisory and decision-support text emphasizing risk guidelines, caution, and stop-loss placement." 
            }
          }
        }
      }
    });

    const resultText = response.text;
    if (!resultText) {
      throw new Error("Empty response returned from Gemini AI");
    }

    const jsonResult = JSON.parse(resultText.trim());
    
    // Append search grounding source links if available
    const groundingChunks = response.candidates?.[0]?.groundingMetadata?.groundingChunks;
    const sources = groundingChunks
      ? groundingChunks
          .map((chunk: any) => chunk.web)
          .filter((web: any) => web && web.uri)
          .map((web: any) => ({ title: web.title, url: web.uri }))
      : [];

    res.json({
      ...jsonResult,
      sources,
      timestamp: Date.now()
    });

  } catch (error: any) {
    res.status(500).json({ error: "Failed to compile market intelligence", details: error.message });
  }
});

// 4. Vite Dev Server & Static Bundle Assembly
async function bootstrap() {
  if (process.env.NODE_ENV !== "production") {
    // Development mode
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
    console.log("Vite development middleware loaded.");
  } else {
    // Production mode
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
    console.log("Serving production static assets from dist.");
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`MEXC Trading Terminal running on http://0.0.0.0:${PORT}`);
  });
}

bootstrap().catch((err) => {
  console.error("Bootstrap failed:", err);
  process.exit(1);
});
