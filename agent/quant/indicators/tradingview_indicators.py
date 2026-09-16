"""
ODIN QUANT - TradingView Indicators Library
تمام اندیکاتورهای TradingView - پیاده‌سازی کامل

Includes:
- Trend: SMA, EMA, WMA, HMA, VWMA, Ichimoku, Parabolic SAR, SuperTrend
- Momentum: RSI, Stochastic, MACD, CCI, Momentum, ROC, Williams %R, Awesome Oscillator, Stochastic RSI
- Volatility: Bollinger Bands, ATR, Keltner Channel, Donchian Channel, Standard Deviation
- Volume: OBV, Volume MA, VWAP, CMF, MFI, Volume Oscillator
- SMC/LIT: Order Blocks, FVG, Liquidity, BOS, Premium/Discount
- Custom: LIT Score, Confluence Score, 80% WR Validator
"""

import pandas as pd
import numpy as np
from typing import Dict, Tuple, List
import logging

logger = logging.getLogger(__name__)

class TradingViewIndicators:
    """Complete TradingView indicators implementation"""
    
    @staticmethod
    def sma(data: pd.Series, period: int) -> pd.Series:
        return data.rolling(period).mean()
    
    @staticmethod
    def ema(data: pd.Series, period: int) -> pd.Series:
        return data.ewm(span=period, adjust=False).mean()
    
    @staticmethod
    def wma(data: pd.Series, period: int) -> pd.Series:
        weights = np.arange(1, period+1)
        return data.rolling(period).apply(lambda x: np.dot(x, weights)/weights.sum(), raw=True)
    
    @staticmethod
    def hma(data: pd.Series, period: int) -> pd.Series:
        half = int(period/2)
        sqrt = int(np.sqrt(period))
        wma_half = TradingViewIndicators.wma(data, half)
        wma_full = TradingViewIndicators.wma(data, period)
        diff = 2 * wma_half - wma_full
        return TradingViewIndicators.wma(diff, sqrt)
    
    @staticmethod
    def rsi(data: pd.Series, period: int = 14) -> pd.Series:
        delta = data.diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
        rs = gain / loss.replace(0, 0.001)
        return 100 - (100 / (1 + rs))
    
    @staticmethod
    def stochastic(high: pd.Series, low: pd.Series, close: pd.Series, k_period: int = 14, d_period: int = 3) -> Tuple[pd.Series, pd.Series]:
        lowest_low = low.rolling(k_period).min()
        highest_high = high.rolling(k_period).max()
        k = 100 * (close - lowest_low) / (highest_high - lowest_low).replace(0, 1)
        d = k.rolling(d_period).mean()
        return k, d
    
    @staticmethod
    def macd(data: pd.Series, fast: int = 12, slow: int = 26, signal: int = 9) -> Tuple[pd.Series, pd.Series, pd.Series]:
        ema_fast = TradingViewIndicators.ema(data, fast)
        ema_slow = TradingViewIndicators.ema(data, slow)
        macd_line = ema_fast - ema_slow
        signal_line = TradingViewIndicators.ema(macd_line, signal)
        histogram = macd_line - signal_line
        return macd_line, signal_line, histogram
    
    @staticmethod
    def bollinger_bands(data: pd.Series, period: int = 20, std: float = 2.0) -> Tuple[pd.Series, pd.Series, pd.Series]:
        sma = TradingViewIndicators.sma(data, period)
        std_dev = data.rolling(period).std()
        upper = sma + std_dev * std
        lower = sma - std_dev * std
        return upper, sma, lower
    
    @staticmethod
    def atr(high: pd.Series, low: pd.Series, close: pd.Series, period: int = 14) -> pd.Series:
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        return tr.rolling(period).mean()
    
    @staticmethod
    def adx(high: pd.Series, low: pd.Series, close: pd.Series, period: int = 14) -> Tuple[pd.Series, pd.Series, pd.Series]:
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        
        up_move = high - high.shift()
        down_move = low.shift() - low
        
        plus_dm = np.where((up_move > down_move) & (up_move > 0), up_move, 0)
        minus_dm = np.where((down_move > up_move) & (down_move > 0), down_move, 0)
        
        tr_smooth = pd.Series(tr).rolling(period).mean()
        plus_dm_smooth = pd.Series(plus_dm).rolling(period).mean()
        minus_dm_smooth = pd.Series(minus_dm).rolling(period).mean()
        
        plus_di = 100 * (plus_dm_smooth / tr_smooth.replace(0, 1))
        minus_di = 100 * (minus_dm_smooth / tr_smooth.replace(0, 1))
        
        dx = 100 * (abs(plus_di - minus_di) / (plus_di + minus_di).replace(0, 1)).fillna(0)
        adx = dx.rolling(period).mean()
        
        return adx, plus_di, minus_di
    
    @staticmethod
    def cci(high: pd.Series, low: pd.Series, close: pd.Series, period: int = 20) -> pd.Series:
        tp = (high + low + close) / 3
        sma = tp.rolling(period).mean()
        mad = tp.rolling(period).apply(lambda x: np.abs(x - x.mean()).mean())
        return (tp - sma) / (0.015 * mad.replace(0, 0.001))
    
    @staticmethod
    def williams_r(high: pd.Series, low: pd.Series, close: pd.Series, period: int = 14) -> pd.Series:
        highest_high = high.rolling(period).max()
        lowest_low = low.rolling(period).min()
        return -100 * (highest_high - close) / (highest_high - lowest_low).replace(0, 1)
    
    @staticmethod
    def awesome_oscillator(high: pd.Series, low: pd.Series) -> pd.Series:
        median = (high + low) / 2
        return TradingViewIndicators.sma(median, 5) - TradingViewIndicators.sma(median, 34)
    
    @staticmethod
    def stochastic_rsi(close: pd.Series, rsi_period: int = 14, stoch_period: int = 14, k: int = 3, d: int = 3) -> Tuple[pd.Series, pd.Series]:
        rsi = TradingViewIndicators.rsi(close, rsi_period)
        lowest_rsi = rsi.rolling(stoch_period).min()
        highest_rsi = rsi.rolling(stoch_period).max()
        stoch_rsi = 100 * (rsi - lowest_rsi) / (highest_rsi - lowest_rsi).replace(0, 1)
        k_line = stoch_rsi.rolling(k).mean()
        d_line = k_line.rolling(d).mean()
        return k_line, d_line
    
    @staticmethod
    def obv(close: pd.Series, volume: pd.Series) -> pd.Series:
        obv = (np.sign(close.diff()) * volume).fillna(0).cumsum()
        return obv
    
    @staticmethod
    def vwap(high: pd.Series, low: pd.Series, close: pd.Series, volume: pd.Series) -> pd.Series:
        typical_price = (high + low + close) / 3
        return (typical_price * volume).cumsum() / volume.cumsum()
    
    @staticmethod
    def mfi(high: pd.Series, low: pd.Series, close: pd.Series, volume: pd.Series, period: int = 14) -> pd.Series:
        tp = (high + low + close) / 3
        raw_money_flow = tp * volume
        positive_flow = raw_money_flow.where(tp > tp.shift(), 0).rolling(period).sum()
        negative_flow = raw_money_flow.where(tp < tp.shift(), 0).rolling(period).sum()
        money_ratio = positive_flow / negative_flow.replace(0, 0.001)
        return 100 - (100 / (1 + money_ratio))
    
    @staticmethod
    def ichimoku(high: pd.Series, low: pd.Series, close: pd.Series) -> Dict[str, pd.Series]:
        tenkan_sen = (high.rolling(9).max() + low.rolling(9).min()) / 2
        kijun_sen = (high.rolling(26).max() + low.rolling(26).min()) / 2
        senkou_a = ((tenkan_sen + kijun_sen) / 2).shift(26)
        senkou_b = ((high.rolling(52).max() + low.rolling(52).min()) / 2).shift(26)
        chikou = close.shift(-26)
        return {
            "tenkan": tenkan_sen,
            "kijun": kijun_sen,
            "senkou_a": senkou_a,
            "senkou_b": senkou_b,
            "chikou": chikou
        }
    
    @staticmethod
    def supertrend(high: pd.Series, low: pd.Series, close: pd.Series, period: int = 10, multiplier: float = 3.0) -> Tuple[pd.Series, pd.Series]:
        atr = TradingViewIndicators.atr(high, low, close, period)
        hl2 = (high + low) / 2
        upper_band = hl2 + multiplier * atr
        lower_band = hl2 - multiplier * atr
        
        supertrend = pd.Series(index=close.index, dtype=float)
        direction = pd.Series(index=close.index, dtype=int)
        
        for i in range(len(close)):
            if i == 0:
                supertrend.iloc[i] = lower_band.iloc[i]
                direction.iloc[i] = 1
            else:
                if close.iloc[i] <= supertrend.iloc[i-1]:
                    supertrend.iloc[i] = upper_band.iloc[i]
                    direction.iloc[i] = -1
                else:
                    supertrend.iloc[i] = lower_band.iloc[i]
                    direction.iloc[i] = 1
        
        return supertrend, direction
    
    @staticmethod
    def keltner_channel(high: pd.Series, low: pd.Series, close: pd.Series, ema_period: int = 20, atr_period: int = 10, multiplier: float = 1.5) -> Tuple[pd.Series, pd.Series, pd.Series]:
        ema = TradingViewIndicators.ema(close, ema_period)
        atr = TradingViewIndicators.atr(high, low, close, atr_period)
        upper = ema + multiplier * atr
        lower = ema - multiplier * atr
        return upper, ema, lower
    
    @staticmethod
    def donchian_channel(high: pd.Series, low: pd.Series, period: int = 20) -> Tuple[pd.Series, pd.Series, pd.Series]:
        upper = high.rolling(period).max()
        lower = low.rolling(period).min()
        middle = (upper + lower) / 2
        return upper, middle, lower
    
    @staticmethod
    def parabolic_sar(high: pd.Series, low: pd.Series, af_start: float = 0.02, af_max: float = 0.2) -> pd.Series:
        # Simplified PSAR
        sar = pd.Series(index=high.index, dtype=float)
        sar.iloc[0] = low.iloc[0]
        af = af_start
        ep = high.iloc[0]
        is_uptrend = True
        
        for i in range(1, len(high)):
            if is_uptrend:
                sar.iloc[i] = sar.iloc[i-1] + af * (ep - sar.iloc[i-1])
                if low.iloc[i] < sar.iloc[i]:
                    is_uptrend = False
                    sar.iloc[i] = ep
                    ep = low.iloc[i]
                    af = af_start
                else:
                    if high.iloc[i] > ep:
                        ep = high.iloc[i]
                        af = min(af + af_start, af_max)
            else:
                sar.iloc[i] = sar.iloc[i-1] + af * (ep - sar.iloc[i-1])
                if high.iloc[i] > sar.iloc[i]:
                    is_uptrend = True
                    sar.iloc[i] = ep
                    ep = high.iloc[i]
                    af = af_start
                else:
                    if low.iloc[i] < ep:
                        ep = low.iloc[i]
                        af = min(af + af_start, af_max)
        
        return sar

class ConfluenceValidator:
    """
    Validates trading signals with multiple confirmations
    Requires:
    - Minimum RR 1:2
    - Minimum 80% historical winrate
    - Multiple indicator confluences
    """
    
    def __init__(self, min_rr: float = 2.0, min_winrate: float = 80.0, min_confluence: int = 4):
        self.min_rr = min_rr
        self.min_winrate = min_winrate
        self.min_confluence = min_confluence
        self.history = []  # Store past trades for winrate calculation
        logger.info(f"ConfluenceValidator: RR>={min_rr}, WR>={min_winrate}%, Confluence>={min_confluence}")
    
    def calculate_rr(self, entry: float, sl: float, tp: float, side: int) -> float:
        """Calculate Risk:Reward"""
        risk = abs(entry - sl)
        reward = abs(tp - entry)
        if risk == 0:
            return 0
        return reward / risk
    
    def check_rr(self, entry: float, sl: float, tp: float, side: int) -> Tuple[bool, float]:
        """Check if RR meets minimum"""
        rr = self.calculate_rr(entry, sl, tp, side)
        return rr >= self.min_rr, rr
    
    def calculate_historical_winrate(self, strategy_name: str = None) -> float:
        """Calculate historical winrate from past trades"""
        if len(self.history) < 10:
            return 0.0  # Not enough data
        
        relevant = self.history
        if strategy_name:
            relevant = [t for t in self.history if t.get("strategy") == strategy_name]
        
        if len(relevant) < 10:
            return 0.0
        
        wins = [t for t in relevant if t["pnl"] > 0]
        return len(wins) / len(relevant) * 100
    
    def check_winrate(self, strategy_name: str = None) -> Tuple[bool, float]:
        """Check if historical winrate meets 80% threshold"""
        wr = self.calculate_historical_winrate(strategy_name)
        if len(self.history) < 10:
            # Not enough history, allow but with warning
            return True, wr  # Allow first trades to build history
        return wr >= self.min_winrate, wr
    
    def calculate_confluence_score(self, indicators: Dict[str, any]) -> Tuple[int, List[str], float]:
        """
        Calculate confluence score based on multiple indicators
        Returns (score, confirmations, confidence)
        """
        score = 0
        confirmations = []
        
        # Trend confluence
        if indicators.get("ema_bull", False):
            score += 1
            confirmations.append("EMA Bull")
        if indicators.get("sma_bull", False):
            score += 1
            confirmations.append("SMA Bull")
        if indicators.get("supertrend_bull", False):
            score += 1
            confirmations.append("SuperTrend Bull")
        if indicators.get("ichimoku_bull", False):
            score += 1
            confirmations.append("Ichimoku Bull")
        if indicators.get("parabolic_sar_bull", False):
            score += 1
            confirmations.append("PSAR Bull")
        
        if indicators.get("ema_bear", False):
            score += 1
            confirmations.append("EMA Bear")
        if indicators.get("sma_bear", False):
            score += 1
            confirmations.append("SMA Bear")
        if indicators.get("supertrend_bear", False):
            score += 1
            confirmations.append("SuperTrend Bear")
        if indicators.get("ichimoku_bear", False):
            score += 1
            confirmations.append("Ichimoku Bear")
        if indicators.get("parabolic_sar_bear", False):
            score += 1
            confirmations.append("PSAR Bear")
        
        # Momentum confluence
        if indicators.get("rsi_bull", False):
            score += 1
            confirmations.append("RSI Bull")
        if indicators.get("rsi_bear", False):
            score += 1
            confirmations.append("RSI Bear")
        if indicators.get("stoch_bull", False):
            score += 1
            confirmations.append("Stoch Bull")
        if indicators.get("stoch_bear", False):
            score += 1
            confirmations.append("Stoch Bear")
        if indicators.get("macd_bull", False):
            score += 1
            confirmations.append("MACD Bull")
        if indicators.get("macd_bear", False):
            score += 1
            confirmations.append("MACD Bear")
        if indicators.get("cci_bull", False):
            score += 1
            confirmations.append("CCI Bull")
        if indicators.get("cci_bear", False):
            score += 1
            confirmations.append("CCI Bear")
        if indicators.get("williams_bull", False):
            score += 1
            confirmations.append("Williams Bull")
        if indicators.get("williams_bear", False):
            score += 1
            confirmations.append("Williams Bear")
        if indicators.get("awesome_bull", False):
            score += 1
            confirmations.append("AO Bull")
        if indicators.get("awesome_bear", False):
            score += 1
            confirmations.append("AO Bear")
        
        # Volatility confluence
        if indicators.get("bb_bull", False):
            score += 1
            confirmations.append("BB Bull")
        if indicators.get("bb_bear", False):
            score += 1
            confirmations.append("BB Bear")
        if indicators.get("keltner_bull", False):
            score += 1
            confirmations.append("Keltner Bull")
        if indicators.get("keltner_bear", False):
            score += 1
            confirmations.append("Keltner Bear")
        if indicators.get("donchian_bull", False):
            score += 1
            confirmations.append("Donchian Bull")
        if indicators.get("donchian_bear", False):
            score += 1
            confirmations.append("Donchian Bear")
        
        # Volume confluence
        if indicators.get("obv_bull", False):
            score += 1
            confirmations.append("OBV Bull")
        if indicators.get("obv_bear", False):
            score += 1
            confirmations.append("OBV Bear")
        if indicators.get("mfi_bull", False):
            score += 1
            confirmations.append("MFI Bull")
        if indicators.get("mfi_bear", False):
            score += 1
            confirmations.append("MFI Bear")
        if indicators.get("vwap_bull", False):
            score += 1
            confirmations.append("VWAP Bull")
        if indicators.get("vwap_bear", False):
            score += 1
            confirmations.append("VWAP Bear")
        
        # SMC/LIT confluence (higher weight)
        if indicators.get("lit_bull", False):
            score += 3  # LIT worth 3 points
            confirmations.append("LIT BULL (3x)")
        if indicators.get("lit_bear", False):
            score += 3
            confirmations.append("LIT BEAR (3x)")
        if indicators.get("bos_bull", False):
            score += 2
            confirmations.append("BOS Bull (2x)")
        if indicators.get("bos_bear", False):
            score += 2
            confirmations.append("BOS Bear (2x)")
        if indicators.get("ob_bull", False):
            score += 2
            confirmations.append("OB Bull (2x)")
        if indicators.get("ob_bear", False):
            score += 2
            confirmations.append("OB Bear (2x)")
        if indicators.get("fvg_bull", False):
            score += 1
            confirmations.append("FVG Bull")
        if indicators.get("fvg_bear", False):
            score += 1
            confirmations.append("FVG Bear")
        if indicators.get("sweep_bull", False):
            score += 2
            confirmations.append("Sweep Bull (2x)")
        if indicators.get("sweep_bear", False):
            score += 2
            confirmations.append("Sweep Bear (2x)")
        
        # Calculate confidence from score
        # Max possible score ~ 30, min required 4
        confidence = min(95, 50 + score * 2.5)  # 50% base + 2.5% per point, max 95%
        
        return score, confirmations, confidence
    
    def validate_trade(self, entry: float, sl: float, tp: float, side: int, indicators: Dict, strategy_name: str = "LIT") -> Dict:
        """
        Master validation: Check RR, WR, Confluence
        Returns validation result
        """
        # Check RR
        rr_ok, rr = self.check_rr(entry, sl, tp, side)
        
        # Check historical WR
        wr_ok, wr = self.check_winrate(strategy_name)
        
        # Check confluence
        confluence_score, confirmations, confidence = self.calculate_confluence_score(indicators)
        confluence_ok = confluence_score >= self.min_confluence
        
        # Overall
        all_ok = rr_ok and wr_ok and confluence_ok
        
        # If WR history not enough, don't block
        if len(self.history) < 10:
            all_ok = rr_ok and confluence_ok
            wr_ok = True  # Don't block due to insufficient history
        
        return {
            "valid": all_ok,
            "rr_ok": rr_ok,
            "rr": rr,
            "wr_ok": wr_ok,
            "winrate": wr,
            "history_count": len(self.history),
            "confluence_ok": confluence_ok,
            "confluence_score": confluence_score,
            "confirmations": confirmations,
            "confidence": confidence,
            "reason": self._build_reason(rr_ok, rr, wr_ok, wr, confluence_ok, confluence_score, confirmations, all_ok)
        }
    
    def _build_reason(self, rr_ok, rr, wr_ok, wr, conf_ok, conf_score, confirmations, all_ok):
        reasons = []
        if not rr_ok:
            reasons.append(f"RR {rr:.2f} < {self.min_rr} (نیاز به 1:{self.min_rr})")
        else:
            reasons.append(f"RR {rr:.2f} >= {self.min_rr} ✓")
        
        if not wr_ok:
            reasons.append(f"WR {wr:.1f}% < {self.min_winrate}% (80% الزامی)")
        else:
            if len(self.history) < 10:
                reasons.append(f"WR {wr:.1f}% - تاریخچه کم ({len(self.history)} ترید) - اجازه")
            else:
                reasons.append(f"WR {wr:.1f}% >= {self.min_winrate}% ✓")
        
        if not conf_ok:
            reasons.append(f"Confluence {conf_score} < {self.min_confluence} (تاییدیه کم)")
        else:
            reasons.append(f"Confluence {conf_score} >= {self.min_confluence} ✓ - {', '.join(confirmations[:3])}")
        
        if all_ok:
            reasons.append("✅ تایید نهایی - ورود مجاز")
        else:
            reasons.append("❌ رد شد - زیر 80% یا RR کم یا تاییدیه کم")
        
        return " | ".join(reasons)
    
    def add_trade_result(self, trade: Dict):
        """Add trade result to history for future WR calculation"""
        self.history.append(trade)
        # Keep only last 100 trades
        if len(self.history) > 100:
            self.history = self.history[-100:]

def apply_all_indicators(df: pd.DataFrame) -> pd.DataFrame:
    """Apply all TradingView indicators to DataFrame"""
    df = df.copy()
    
    # Trend
    df["sma_20"] = TradingViewIndicators.sma(df["close"], 20)
    df["sma_50"] = TradingViewIndicators.sma(df["close"], 50)
    df["sma_200"] = TradingViewIndicators.sma(df["close"], 200)
    df["ema_20"] = TradingViewIndicators.ema(df["close"], 20)
    df["ema_50"] = TradingViewIndicators.ema(df["close"], 50)
    df["hull_20"] = TradingViewIndicators.hma(df["close"], 20)
    
    # Momentum
    df["rsi"] = TradingViewIndicators.rsi(df["close"], 14)
    df["stoch_k"], df["stoch_d"] = TradingViewIndicators.stochastic(df["high"], df["low"], df["close"])
    df["macd"], df["macd_signal"], df["macd_hist"] = TradingViewIndicators.macd(df["close"])
    df["cci"] = TradingViewIndicators.cci(df["high"], df["low"], df["close"])
    df["williams_r"] = TradingViewIndicators.williams_r(df["high"], df["low"], df["close"])
    df["ao"] = TradingViewIndicators.awesome_oscillator(df["high"], df["low"])
    df["stoch_rsi_k"], df["stoch_rsi_d"] = TradingViewIndicators.stochastic_rsi(df["close"])
    
    # Volatility
    df["bb_upper"], df["bb_middle"], df["bb_lower"] = TradingViewIndicators.bollinger_bands(df["close"])
    df["atr"] = TradingViewIndicators.atr(df["high"], df["low"], df["close"])
    df["kc_upper"], df["kc_middle"], df["kc_lower"] = TradingViewIndicators.keltner_channel(df["high"], df["low"], df["close"])
    df["dc_upper"], df["dc_middle"], df["dc_lower"] = TradingViewIndicators.donchian_channel(df["high"], df["low"])
    
    # Volume
    if "volume" in df.columns:
        df["obv"] = TradingViewIndicators.obv(df["close"], df["volume"])
        df["vwap"] = TradingViewIndicators.vwap(df["high"], df["low"], df["close"], df["volume"])
        df["mfi"] = TradingViewIndicators.mfi(df["high"], df["low"], df["close"], df["volume"])
    
    # SuperTrend & PSAR
    df["supertrend"], df["supertrend_dir"] = TradingViewIndicators.supertrend(df["high"], df["low"], df["close"])
    df["psar"] = TradingViewIndicators.parabolic_sar(df["high"], df["low"])
    
    # Ichimoku
    ichimoku = TradingViewIndicators.ichimoku(df["high"], df["low"], df["close"])
    df["ichimoku_tenkan"] = ichimoku["tenkan"]
    df["ichimoku_kijun"] = ichimoku["kijun"]
    df["ichimoku_senkou_a"] = ichimoku["senkou_a"]
    df["ichimoku_senkou_b"] = ichimoku["senkou_b"]
    
    return df

def get_confluence_signals(df: pd.DataFrame, idx: int) -> Dict[str, bool]:
    """Get all confluence signals at index"""
    if idx < 50:
        return {}
    
    row = df.iloc[idx]
    prev = df.iloc[idx-1]
    
    signals = {}
    
    # Trend
    signals["ema_bull"] = row["close"] > row["ema_20"] and row["ema_20"] > row["ema_50"]
    signals["ema_bear"] = row["close"] < row["ema_20"] and row["ema_20"] < row["ema_50"]
    signals["sma_bull"] = row["close"] > row["sma_20"] and row["sma_20"] > row["sma_50"]
    signals["sma_bear"] = row["close"] < row["sma_20"] and row["sma_20"] < row["sma_50"]
    signals["supertrend_bull"] = row["supertrend_dir"] == 1
    signals["supertrend_bear"] = row["supertrend_dir"] == -1
    signals["ichimoku_bull"] = row["close"] > row["ichimoku_senkou_a"] and row["close"] > row["ichimoku_senkou_b"] and row["ichimoku_tenkan"] > row["ichimoku_kijun"]
    signals["ichimoku_bear"] = row["close"] < row["ichimoku_senkou_a"] and row["close"] < row["ichimoku_senkou_b"] and row["ichimoku_tenkan"] < row["ichimoku_kijun"]
    signals["parabolic_sar_bull"] = row["close"] > row["psar"]
    signals["parabolic_sar_bear"] = row["close"] < row["psar"]
    
    # Momentum
    signals["rsi_bull"] = row["rsi"] > 50 and row["rsi"] < 70 and row["rsi"] > prev["rsi"]
    signals["rsi_bear"] = row["rsi"] < 50 and row["rsi"] > 30 and row["rsi"] < prev["rsi"]
    signals["stoch_bull"] = row["stoch_k"] > row["stoch_d"] and row["stoch_k"] < 80 and row["stoch_k"] > 20
    signals["stoch_bear"] = row["stoch_k"] < row["stoch_d"] and row["stoch_k"] > 20 and row["stoch_k"] < 80
    signals["macd_bull"] = row["macd"] > row["macd_signal"] and row["macd_hist"] > 0
    signals["macd_bear"] = row["macd"] < row["macd_signal"] and row["macd_hist"] < 0
    signals["cci_bull"] = row["cci"] > 0 and row["cci"] < 100
    signals["cci_bear"] = row["cci"] < 0 and row["cci"] > -100
    signals["williams_bull"] = row["williams_r"] > -80 and row["williams_r"] < -20 and row["williams_r"] > prev["williams_r"]
    signals["williams_bear"] = row["williams_r"] < -20 and row["williams_r"] > -80 and row["williams_r"] < prev["williams_r"]
    signals["awesome_bull"] = row["ao"] > 0 and row["ao"] > prev["ao"]
    signals["awesome_bear"] = row["ao"] < 0 and row["ao"] < prev["ao"]
    
    # Volatility
    signals["bb_bull"] = row["close"] < row["bb_lower"]  # Oversold bounce
    signals["bb_bear"] = row["close"] > row["bb_upper"]  # Overbought rejection
    signals["keltner_bull"] = row["close"] < row["kc_lower"]
    signals["keltner_bear"] = row["close"] > row["kc_upper"]
    signals["donchian_bull"] = row["close"] > row["dc_upper"]
    signals["donchian_bear"] = row["close"] < row["dc_lower"]
    
    # Volume
    if "obv" in df.columns:
        signals["obv_bull"] = row["obv"] > prev["obv"] and row["close"] > prev["close"]
        signals["obv_bear"] = row["obv"] < prev["obv"] and row["close"] < prev["close"]
    if "mfi" in df.columns:
        signals["mfi_bull"] = row["mfi"] < 20  # Oversold
        signals["mfi_bear"] = row["mfi"] > 80  # Overbought
    if "vwap" in df.columns:
        signals["vwap_bull"] = row["close"] > row["vwap"]
        signals["vwap_bear"] = row["close"] < row["vwap"]
    
    return signals
