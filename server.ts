import express from "express";
import cors from "cors";
import axios from "axios";
import path from "path";
import { fileURLToPath } from "url";
import crypto from "crypto";
import { createServer as createViteServer } from "vite";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(cors());
  app.use(express.json());

  // 1. Time Sync Endpoint
  app.get("/api/time", async (req, res) => {
    try {
      const response = await axios.get("https://api.mexc.com/api/v3/time", { timeout: 4000 });
      res.json(response.data);
    } catch (error: any) {
      // Fallback to local server time
      res.json({ serverTime: Date.now() });
    }
  });

  // 2. Klines/Candles History Endpoint
  app.get("/api/klines", async (req, res) => {
    try {
      const { symbol = "BTCUSDT", interval = "1m", limit = "100" } = req.query;
      const response = await axios.get("https://api.mexc.com/api/v3/klines", {
        params: { symbol, interval, limit },
        timeout: 5000,
      });
      res.json(response.data);
    } catch (error: any) {
      res.status(500).json({ error: "Failed to fetch klines from MEXC API" });
    }
  });

  // 3. Generic Secure Proxy for MEXC Signed/Authenticated API Endpoints
  // Receives API Key, Secret, Endpoint path, HTTP Method, and parameters.
  // Performs server-side cryptographic HMAC-SHA256 signature generation to secure keys.
  app.post("/api/mexc/proxy", async (req, res) => {
    const { apiKey, apiSecret, endpoint, method = "GET", params = {} } = req.body;

    if (!apiKey || !apiSecret || !endpoint) {
      return res.status(400).json({ error: "apiKey, apiSecret, and endpoint are required" });
    }

    try {
      const timestamp = Date.now();
      const queryParams = { ...params, timestamp };

      // Sort parameters alphabetically as required by MEXC API
      const sortedKeys = Object.keys(queryParams).sort();
      const sortedQueryString = sortedKeys
        .map((key) => `${key}=${encodeURIComponent(queryParams[key])}`)
        .join("&");

      // Generate HMAC-SHA256 Signature
      const signature = crypto
        .createHmac("sha256", apiSecret)
        .update(sortedQueryString)
        .digest("hex");

      const finalQueryString = `${sortedQueryString}&signature=${signature}`;
      
      const isFutures = endpoint.includes("/api/v1/private/");
      const baseURL = isFutures ? "https://contract.mexc.com" : "https://api.mexc.com";
      const finalURL = `${baseURL}/${endpoint.replace(/^\//, "")}?${finalQueryString}`;

      const headers = {
        "X-MEXC-APIKEY": apiKey,
        "Content-Type": "application/json",
      };

      let response;
      if (method.toUpperCase() === "POST") {
        response = await axios.post(finalURL, {}, { headers, timeout: 6000 });
      } else {
        response = await axios.get(finalURL, { headers, timeout: 6000 });
      }

      res.json(response.data);
    } catch (error: any) {
      const status = error.response?.status || 500;
      const data = error.response?.data || { error: error.message };
      res.status(status).json(data);
    }
  });

  // 4. Vite Dev Server vs Production Static Serving
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
    console.log("Vite development middleware mounted.");
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
    console.log("Serving static production files from dist.");
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Maria Bot server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
