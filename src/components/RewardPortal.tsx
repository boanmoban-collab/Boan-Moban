import React, { useState } from "react";
import { Sparkles, Award, ShieldAlert, CheckCircle, HelpCircle, ArrowUpRight } from "lucide-react";

interface RewardTask {
  id: string;
  title: string;
  description: string;
  rewardValue: string;
  status: "CLAIMED" | "AVAILABLE" | "IN_PROGRESS" | "MANUAL_REQUIRED";
  mexcDirectLink: string;
}

export const RewardPortal: React.FC = () => {
  const [tasks] = useState<RewardTask[]>([
    {
      id: "mexc-kyc-bonus",
      title: "KYC Verification Milestones",
      description: "Complete Primary and Advanced Identity verification in your MEXC account settings.",
      rewardValue: "30 USDT Futures Bonus",
      status: "MANUAL_REQUIRED",
      mexcDirectLink: "https://www.mexc.com/user/security"
    },
    {
      id: "mexc-first-dep",
      title: "First Net Deposit Task",
      description: "Perform a net deposit of at least 100 USDT into your Spot account within 7 days of sign-up.",
      rewardValue: "20 USDT Cashback",
      status: "IN_PROGRESS",
      mexcDirectLink: "https://www.mexc.com/p2p"
    },
    {
      id: "mexc-spot-volume",
      title: "Spot Trading Volume Milestones",
      description: "Reach a total spot trading volume of 1,000 USDT on any trading pairs.",
      rewardValue: "10 MX Tokens",
      status: "IN_PROGRESS",
      mexcDirectLink: "https://www.mexc.com/exchange"
    },
    {
      id: "mexc-futures-volume",
      title: "Futures Trading Master",
      description: "Open your first contract trade and accumulate 10,000 USDT in futures trading volume.",
      rewardValue: "50 USDT Futures Voucher",
      status: "MANUAL_REQUIRED",
      mexcDirectLink: "https://contract.mexc.com/"
    }
  ]);

  return (
    <div className="bg-zinc-950 border border-zinc-800 rounded-xl overflow-hidden p-4 flex flex-col h-full select-none text-zinc-100">
      <div className="flex items-center justify-between border-b border-zinc-900 pb-3 mb-4">
        <div className="flex items-center space-x-2">
          <Award className="w-4 h-4 text-emerald-500 animate-bounce" />
          <h3 className="text-zinc-300 font-bold text-xs tracking-tight">Exchange Reward Center</h3>
        </div>
        <span className="text-[10px] text-zinc-500 font-mono font-bold">ACTIVE MEXC EVENTS</span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-3 pr-1.5 no-scrollbar max-h-[280px]">
        {/* API Limitation Warning Banner */}
        <div className="bg-zinc-900/40 border border-zinc-850 p-3 rounded-lg flex items-start space-x-2.5">
          <ShieldAlert className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
          <div className="text-[10px] leading-relaxed text-zinc-400">
            <span className="font-bold text-zinc-200 block mb-0.5">MEXC Security Guard Active</span>
            MEXC security guidelines prohibit third-party APIs from claiming financial rewards or modifying account verification directly. These actions must be performed manually in your official dashboard.
          </div>
        </div>

        {/* Reward Task Cards */}
        <div className="space-y-2">
          {tasks.map((task) => {
            return (
              <div
                key={task.id}
                className="bg-zinc-900/20 hover:bg-zinc-900/40 p-3 rounded-lg border border-zinc-900 flex flex-col justify-between transition-all"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h4 className="text-xs font-bold text-zinc-200">{task.title}</h4>
                    <p className="text-[10.5px] text-zinc-500 mt-1 leading-relaxed">
                      {task.description}
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="text-[10px] font-mono font-extrabold text-emerald-400 block">
                      {task.rewardValue}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between border-t border-zinc-950 mt-2.5 pt-2">
                  <div className="flex items-center space-x-1.5 text-[9px] font-mono font-bold">
                    {task.status === "MANUAL_REQUIRED" ? (
                      <span className="text-amber-500 bg-amber-950/20 border border-amber-900/30 px-2 py-0.5 rounded">
                        Manual Settings Required
                      </span>
                    ) : task.status === "IN_PROGRESS" ? (
                      <span className="text-blue-400 bg-blue-950/20 border border-blue-900/30 px-2 py-0.5 rounded">
                        Scanning Active Trade Volume
                      </span>
                    ) : (
                      <span className="text-emerald-500 bg-emerald-950/20 border border-emerald-900/30 px-2 py-0.5 rounded flex items-center">
                        <CheckCircle className="w-2.5 h-2.5 mr-0.5" /> Completed
                      </span>
                    )}
                  </div>

                  <a
                    href={task.mexcDirectLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[9px] font-extrabold text-blue-400 hover:text-blue-300 flex items-center space-x-0.5 transition-all"
                  >
                    <span>Complete on MEXC</span>
                    <ArrowUpRight className="w-2.5 h-2.5" />
                  </a>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
