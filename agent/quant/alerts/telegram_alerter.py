"""
ODIN QUANT - Telegram Alerter
"""
import logging
from typing import Dict, Any
import asyncio

logger = logging.getLogger(__name__)

class TelegramAlerter:
    def __init__(self, bot_token: str = None, chat_id: str = None, enabled: bool = False):
        self.bot_token = bot_token
        self.chat_id = chat_id
        self.enabled = enabled and bot_token and chat_id
        
        if self.enabled:
            logger.info("Telegram alerter enabled")
        else:
            logger.info("Telegram alerter disabled (no token/chat_id)")
    
    async def send_message(self, message: str):
        if not self.enabled:
            logger.info(f"[TELEGRAM MOCK] {message[:100]}...")
            return True
        
        try:
            import telegram
            bot = telegram.Bot(token=self.bot_token)
            await bot.send_message(chat_id=self.chat_id, text=message, parse_mode="Markdown")
            return True
        except Exception as e:
            logger.error(f"Telegram send failed: {e}")
            return False
    
    def send_sync(self, message: str):
        """Sync version"""
        try:
            asyncio.run(self.send_message(message))
        except:
            logger.info(f"[TELEGRAM SYNC MOCK] {message[:100]}...")
    
    def alert_entry(self, symbol: str, side: int, price: float, size: float, reason: str):
        side_str = "LONG 🟢" if side == 1 else "SHORT 🔴"
        msg = f"🚀 *ODIN ENTRY* {side_str}\nSymbol: `{symbol}`\nPrice: `{price:.2f}`\nSize: `{size:.4f}`\nReason: {reason}"
        self.send_sync(msg)
    
    def alert_exit(self, symbol: str, pnl: float, reason: str):
        emoji = "✅" if pnl > 0 else "❌"
        msg = f"{emoji} *ODIN EXIT*\nSymbol: `{symbol}`\nPnL: `${pnl:.2f}`\nReason: {reason}"
        self.send_sync(msg)
    
    def alert_kill_switch(self, daily_pnl: float, daily_dd: float):
        msg = f"🔴 *KILL-SWITCH ACTIVATED!*\nDaily PnL: `${daily_pnl:.2f}`\nDaily DD: `{daily_dd:.2f}%`\nAll new trades blocked!"
        self.send_sync(msg)
    
    def alert_daily_summary(self, status: Dict[str, Any]):
        msg = f"📊 *ODIN DAILY SUMMARY*\nCapital: `${status['current_capital']:.2f}`\nDaily PnL: `${status['daily_pnl']:.2f}`\nOpen: {status['open_positions']}\nTotal PnL: `${status['total_pnl']:.2f}` ({status['total_pnl_percent']:.2f}%)"
        self.send_sync(msg)
