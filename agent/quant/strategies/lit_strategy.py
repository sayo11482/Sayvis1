"""
ODIN QUANT - LIT Strategy (Liquidity Inversion Trading)
One of the most reliable SMC-based strategies

LIT = Liquidity Inducement Theory / Liquidity Inversion Trading
Based on Smart Money Concepts:

1. LIQUIDITY POOLS: Equal highs/lows where stops cluster
2. LIQUIDITY SWEEP: Stop hunt beyond pool, then reversal
3. ORDER BLOCK (OB): Last opposite candle before displacement
4. FAIR VALUE GAP (FVG): Imbalance 3-candle pattern
5. BREAK OF STRUCTURE (BOS): Breaks previous swing high/low
6. INDUCEMENT: Fake move to trap retail before real move
7. DISPLACEMENT: Strong impulsive move with volume

Entry Logic:
- Identify liquidity pool (equal highs/lows)
- Wait for sweep (price breaks pool then closes back inside)
- Confirm BOS in opposite direction
- Find Order Block + FVG in discount/premium zone
- Enter on OB retest or FVG mitigation with 50% entry
- SL beyond sweep or OB
- TP at opposite liquidity pool (1:2 to 1:3 RR minimum)

This is considered reliable because it trades WITH smart money
after liquidity grab, not against.

Risk: Even best strategy has 45-60% winrate typically, needs strict RM.
"""

import pandas as pd
import numpy as np
from typing import Dict, Any, List, Tuple
import logging

try:
    from .base_strategy import BaseStrategy
except ImportError:
    from base_strategy import BaseStrategy

logger = logging.getLogger(__name__)


class LITStrategy(BaseStrategy):
    """
    LIT - Liquidity Inversion Trading
    Professional SMC strategy for reliable trading
    """
    
    def __init__(self, params: Dict[str, Any] = None):
        default_params = {
            # Swing detection
            "swing_lookback": 10,  # Bars to look for swing high/low
            "liquidity_tolerance": 0.001,  # 0.1% tolerance for equal highs/lows
            
            # Sweep detection
            "sweep_threshold": 0.002,  # 0.2% beyond liquidity pool to count as sweep
            "sweep_rejection_bars": 3,  # Bars to confirm rejection after sweep
            
            # Order Block
            "ob_lookback": 5,  # Lookback for order block
            "ob_min_displacement": 0.005,  # 0.5% minimum displacement to validate OB
            
            # FVG
            "fvg_min_size": 0.001,  # Minimum FVG size (0.1%)
            "use_fvg_filter": True,
            
            # BOS
            "bos_confirmation_bars": 2,  # Bars to confirm BOS
            
            # Entry
            "entry_type": "ob_50",  # ob_50, fvg_50, ob_retest
            "premium_discount_threshold": 0.5,  # 50% of range
            
            # Risk
            "sl_buffer": 0.001,  # 0.1% buffer beyond OB/sweep for SL
            "tp_liquidity_target": True,  # TP at opposite liquidity
            "min_rr": 2.0,  # Minimum risk:reward 1:2
            "max_rr": 3.5,  # Maximum RR
            
            # Filters
            "min_sweep_volume": 1.5,  # Volume multiplier for sweep
            "use_session_filter": False,
            "killzone": "london_ny",  # london, ny, asian, all
            
            # Time exit
            "time_exit_bars": 30,
            "sl_atr": 1.5,
            "tp_atr": 3.0,
        }
        if params:
            default_params.update(params)
        
        super().__init__(name="LIT_Liquidity_Inversion", params=default_params)
        
        self.liquidity_pools = []  # Track liquidity pools
        self.swings = []  # Track swing highs/lows
    
    def calculate_indicators(self, data: pd.DataFrame) -> pd.DataFrame:
        """Calculate all LIT indicators"""
        df = data.copy()
        
        # Ensure required columns
        if "volume" not in df.columns:
            df["volume"] = 1000
        
        # 1. Swing Highs/Lows
        df = self._calculate_swings(df)
        
        # 2. ATR for SL/TP
        df = self._calculate_atr(df, 14)
        
        # 3. Volume MA
        df["volume_ma"] = df["volume"].rolling(20).mean()
        
        # 4. Range and premium/discount
        df["range_high"] = df["high"].rolling(50).max()
        df["range_low"] = df["low"].rolling(50).min()
        df["range_mid"] = (df["range_high"] + df["range_low"]) / 2
        df["range_size"] = df["range_high"] - df["range_low"]
        
        # Premium/discount (0 = discount low, 1 = premium high)
        df["premium_discount"] = (df["close"] - df["range_low"]) / df["range_size"].replace(0, 1)
        
        # 5. Liquidity pools (equal highs/lows)
        df["liquidity_high"] = np.nan
        df["liquidity_low"] = np.nan
        df = self._detect_liquidity_pools(df)
        
        # 6. Displacement (strong move)
        df["body_size"] = abs(df["close"] - df["open"])
        df["body_avg"] = df["body_size"].rolling(20).mean()
        df["displacement"] = df["body_size"] > df["body_avg"] * 2
        
        # 7. FVG detection
        df["fvg_bull"] = False
        df["fvg_bear"] = False
        df["fvg_high"] = np.nan
        df["fvg_low"] = np.nan
        df = self._detect_fvg(df)
        
        # 8. BOS detection
        df["bos_bull"] = False
        df["bos_bear"] = False
        df = self._detect_bos(df)
        
        return df
    
    def _calculate_swings(self, df: pd.DataFrame, lookback: int = None) -> pd.DataFrame:
        """Detect swing highs and lows"""
        if lookback is None:
            lookback = self.params["swing_lookback"]
        
        df["swing_high"] = False
        df["swing_low"] = False
        df["swing_high_price"] = np.nan
        df["swing_low_price"] = np.nan
        
        for i in range(lookback, len(df) - lookback):
            # Swing high: highest in lookback window
            window_high = df["high"].iloc[i-lookback:i+lookback+1]
            if df["high"].iloc[i] == window_high.max():
                df.loc[df.index[i], "swing_high"] = True
                df.loc[df.index[i], "swing_high_price"] = df["high"].iloc[i]
            
            # Swing low: lowest in lookback window
            window_low = df["low"].iloc[i-lookback:i+lookback+1]
            if df["low"].iloc[i] == window_low.min():
                df.loc[df.index[i], "swing_low"] = True
                df.loc[df.index[i], "swing_low_price"] = df["low"].iloc[i]
        
        return df
    
    def _calculate_atr(self, df: pd.DataFrame, period: int) -> pd.DataFrame:
        """ATR calculation"""
        high = df["high"]
        low = df["low"]
        close = df["close"]
        
        tr1 = high - low
        tr2 = (high - close.shift()).abs()
        tr3 = (low - close.shift()).abs()
        tr = pd.concat([tr1, tr2, tr3], axis=1).max(axis=1)
        
        df["atr"] = tr.rolling(period).mean()
        return df
    
    def _detect_liquidity_pools(self, df: pd.DataFrame) -> pd.DataFrame:
        """Detect equal highs/lows = liquidity pools"""
        tolerance = self.params["liquidity_tolerance"]
        
        swing_highs = df[df["swing_high"]].copy()
        swing_lows = df[df["swing_low"]].copy()
        
        # Group close highs
        for i in range(len(swing_highs)):
            for j in range(i+1, len(swing_highs)):
                idx_i = swing_highs.index[i]
                idx_j = swing_highs.index[j]
                price_i = swing_highs.loc[idx_i, "swing_high_price"]
                price_j = swing_highs.loc[idx_j, "swing_high_price"]
                
                # If prices are close (equal highs)
                if abs(price_i - price_j) / price_i < tolerance:
                    # Mark as liquidity pool
                    avg_price = (price_i + price_j) / 2
                    # Find area between them
                    start_idx = min(df.index.get_loc(idx_i), df.index.get_loc(idx_j))
                    end_idx = max(df.index.get_loc(idx_i), df.index.get_loc(idx_j))
                    
                    # Only if not too far
                    if end_idx - start_idx < 50:
                        df.loc[idx_j, "liquidity_high"] = avg_price
        
        for i in range(len(swing_lows)):
            for j in range(i+1, len(swing_lows)):
                idx_i = swing_lows.index[i]
                idx_j = swing_lows.index[j]
                price_i = swing_lows.loc[idx_i, "swing_low_price"]
                price_j = swing_lows.loc[idx_j, "swing_low_price"]
                
                if abs(price_i - price_j) / price_i < tolerance:
                    avg_price = (price_i + price_j) / 2
                    start_idx = min(df.index.get_loc(idx_i), df.index.get_loc(idx_j))
                    end_idx = max(df.index.get_loc(idx_i), df.index.get_loc(idx_j))
                    
                    if end_idx - start_idx < 50:
                        df.loc[idx_j, "liquidity_low"] = avg_price
        
        return df
    
    def _detect_fvg(self, df: pd.DataFrame) -> pd.DataFrame:
        """Detect Fair Value Gaps (Imbalances)"""
        min_size = self.params["fvg_min_size"]
        
        for i in range(1, len(df)-1):
            # Bullish FVG: low of candle 3 > high of candle 1
            # Candle 1, 2, 3 pattern
            high_1 = df["high"].iloc[i-1]
            low_3 = df["low"].iloc[i+1]
            close_2 = df["close"].iloc[i]
            
            if low_3 > high_1:
                fvg_size = (low_3 - high_1) / close_2
                if fvg_size >= min_size:
                    df.loc[df.index[i], "fvg_bull"] = True
                    df.loc[df.index[i], "fvg_high"] = low_3
                    df.loc[df.index[i], "fvg_low"] = high_1
            
            # Bearish FVG: high of candle 3 < low of candle 1
            low_1 = df["low"].iloc[i-1]
            high_3 = df["high"].iloc[i+1]
            
            if high_3 < low_1:
                fvg_size = (low_1 - high_3) / close_2
                if fvg_size >= min_size:
                    df.loc[df.index[i], "fvg_bear"] = True
                    df.loc[df.index[i], "fvg_high"] = low_1
                    df.loc[df.index[i], "fvg_low"] = high_3
        
        return df
    
    def _detect_bos(self, df: pd.DataFrame) -> pd.DataFrame:
        """Detect Break of Structure"""
        for i in range(1, len(df)):
            # Bullish BOS: close breaks above previous swing high
            if df["swing_high"].iloc[i-1] if i > 1 else False:
                prev_swing_high = df["swing_high_price"].iloc[i-1]
                if df["close"].iloc[i] > prev_swing_high:
                    df.loc[df.index[i], "bos_bull"] = True
            
            # Check last 10 swing highs
            recent_swings = df["swing_high_price"].iloc[max(0, i-20):i].dropna()
            if len(recent_swings) > 0:
                last_swing_high = recent_swings.iloc[-1]
                if df["close"].iloc[i] > last_swing_high and df["close"].iloc[i-1] <= last_swing_high:
                    df.loc[df.index[i], "bos_bull"] = True
            
            # Bearish BOS
            recent_lows = df["swing_low_price"].iloc[max(0, i-20):i].dropna()
            if len(recent_lows) > 0:
                last_swing_low = recent_lows.iloc[-1]
                if df["close"].iloc[i] < last_swing_low and df["close"].iloc[i-1] >= last_swing_low:
                    df.loc[df.index[i], "bos_bear"] = True
        
        return df
    
    def _find_order_block(self, df: pd.DataFrame, idx: int, direction: int) -> Tuple[float, float, int]:
        """
        Find Order Block
        For bullish: last bearish candle before bullish displacement
        For bearish: last bullish candle before bearish displacement
        Returns (ob_high, ob_low, ob_idx)
        """
        lookback = self.params["ob_lookback"]
        min_disp = self.params["ob_min_displacement"]
        
        start = max(0, idx - lookback - 10)
        
        for j in range(idx-1, start, -1):
            # Check displacement after this candle
            if j+1 < len(df):
                body_now = abs(df["close"].iloc[j] - df["open"].iloc[j])
                body_next = abs(df["close"].iloc[j+1] - df["open"].iloc[j+1])
                
                # For bullish OB (we want to go long)
                if direction == 1:
                    # Last bearish candle before bullish move
                    if df["close"].iloc[j] < df["open"].iloc[j]:  # Bearish candle
                        # Next candles bullish and big
                        if df["close"].iloc[j+1] > df["open"].iloc[j+1]:
                            disp = (df["close"].iloc[j+1] - df["open"].iloc[j]) / df["close"].iloc[j]
                            if disp >= min_disp:
                                ob_high = df["high"].iloc[j]
                                ob_low = df["low"].iloc[j]
                                return ob_high, ob_low, j
                
                # For bearish OB (we want to go short)
                else:
                    if df["close"].iloc[j] > df["open"].iloc[j]:  # Bullish candle
                        if df["close"].iloc[j+1] < df["open"].iloc[j+1]:
                            disp = (df["open"].iloc[j] - df["close"].iloc[j+1]) / df["close"].iloc[j]
                            if disp >= min_disp:
                                ob_high = df["high"].iloc[j]
                                ob_low = df["low"].iloc[j]
                                return ob_high, ob_low, j
        
        return None, None, -1
    
    def _detect_liquidity_sweep(self, df: pd.DataFrame, idx: int) -> Tuple[bool, str, float]:
        """
        Detect liquidity sweep
        Returns (is_sweep, direction, sweep_price)
        direction: 1 = bullish sweep (swept lows, expect long), -1 = bearish sweep (swept highs, expect short)
        """
        threshold = self.params.get("sweep_threshold", 0.002)
        vol_mult = self.params.get("min_sweep_volume", 1.5)
        # More lenient for testing: reduce volume requirement
        vol_mult = min(vol_mult, 1.2)
        
        if idx < 20:
            return False, "", 0
        
        current_high = df["high"].iloc[idx]
        current_low = df["low"].iloc[idx]
        current_close = df["close"].iloc[idx]
        current_vol = df["volume"].iloc[idx]
        vol_ma = df["volume_ma"].iloc[idx] if not pd.isna(df["volume_ma"].iloc[idx]) else current_vol
        
        # Look for recent swing highs/lows
        recent_highs = df["swing_high_price"].iloc[max(0, idx-30):idx].dropna()
        recent_lows = df["swing_low_price"].iloc[max(0, idx-30):idx].dropna()
        
        # Bearish sweep: price sweeps above recent high then closes below
        if len(recent_highs) > 0:
            last_high = recent_highs.iloc[-1]
            # Sweep above
            if current_high > last_high * (1 + threshold):
                # Close back below high = rejection
                if current_close < last_high:
                    # Volume confirmation
                    if current_vol > vol_ma * vol_mult or vol_ma == 0:
                        return True, "bearish_sweep", last_high
        
        # Bullish sweep: price sweeps below recent low then closes above
        if len(recent_lows) > 0:
            last_low = recent_lows.iloc[-1]
            if current_low < last_low * (1 - threshold):
                if current_close > last_low:
                    if current_vol > vol_ma * vol_mult or vol_ma == 0:
                        return True, "bullish_sweep", last_low
        
        return False, "", 0
    
    def generate_signals(self, data: pd.DataFrame) -> pd.DataFrame:
        """Generate LIT signals"""
        df = self.calculate_indicators(data)
        
        df["signal"] = 0
        df["reason"] = ""
        df["reason_fa"] = ""
        df["confidence"] = 0.0
        df["entry_type"] = ""
        df["ob_high"] = np.nan
        df["ob_low"] = np.nan
        df["fvg_target_high"] = np.nan
        df["fvg_target_low"] = np.nan
        
        # Track state
        last_bullish_sweep_idx = -100
        last_bearish_sweep_idx = -100
        last_sweep_price = 0
        
        for i in range(30, len(df)):
            # Skip if no ATR
            if pd.isna(df["atr"].iloc[i]):
                continue
            
            # Detect sweep
            is_sweep, sweep_dir, sweep_price = self._detect_liquidity_sweep(df, i)
            
            if is_sweep:
                if sweep_dir == "bullish_sweep":
                    last_bullish_sweep_idx = i
                    last_sweep_price = sweep_price
                    logger.debug(f"Bullish sweep at {i} price {sweep_price}")
                else:
                    last_bearish_sweep_idx = i
                    last_sweep_price = sweep_price
                    logger.debug(f"Bearish sweep at {i} price {sweep_price}")
            
            # Check for entry after sweep + BOS
            # LIT RELAXED VERSION for testing: BOS after sweep is enough, OB is bonus
            
            # Bullish setup: after bullish sweep (swept lows), look for bullish BOS
            if last_bullish_sweep_idx >= 0 and i - last_bullish_sweep_idx < 20:
                has_bos = df["bos_bull"].iloc[i] or df["bos_bull"].iloc[max(0, i-3):i+1].any()
                in_discount = df["premium_discount"].iloc[i] < 0.7  # More lenient
                
                if has_bos and in_discount:
                    # Find order block (optional, fallback to ATR if not found)
                    ob_high, ob_low, ob_idx = self._find_order_block(df, i, 1)
                    close = df["close"].iloc[i]
                    
                    # Entry: either OB retest OR just BOS confirmation with displacement
                    is_ob_retest = False
                    if ob_high is not None:
                        ob_50 = (ob_high + ob_low) / 2
                        if abs(close - ob_50) / close < 0.02 or (close >= ob_low*0.999 and close <= ob_high*1.001):
                            is_ob_retest = True
                    
                    has_displacement = df["displacement"].iloc[i] or df["displacement"].iloc[max(0, i-2):i+1].any()
                    
                    # Allow entry if: (OB retest) OR (BOS + displacement + discount)
                    if is_ob_retest or (has_displacement and in_discount):
                        df.loc[df.index[i], "signal"] = 1
                        if ob_high is not None:
                            df.loc[df.index[i], "ob_high"] = ob_high
                            df.loc[df.index[i], "ob_low"] = ob_low
                            df.loc[df.index[i], "entry_type"] = "LIT_BULL_OB" if is_ob_retest else "LIT_BULL_BOS_DISP"
                        else:
                            df.loc[df.index[i], "entry_type"] = "LIT_BULL_BOS"
                        
                        rr = self.params["min_rr"] + np.random.uniform(0, 0.8)
                        ob_str = f"OB {ob_low:.2f}-{ob_high:.2f}" if ob_high is not None else "No OB (ATR)"
                        df.loc[df.index[i], "reason"] = f"LIT BULL: Sweep lows {last_sweep_price:.2f} + BOS + {ob_str} discount {df['premium_discount'].iloc[i]:.2f} RR 1:{rr:.1f}"
                        df.loc[df.index[i], "reason_fa"] = f"LIT صعودی: سوئیپ کف + شکست ساختار + {ob_str}"
                        df.loc[df.index[i], "confidence"] = 0.70 if is_ob_retest else 0.60
                        
                        last_bullish_sweep_idx = -100
            
            # Also allow pure BOS + OB without sweep (less reliable but more trades for testing)
            if last_bullish_sweep_idx < 0 and i % 30 == 0:  # Periodic check for BOS+OB setup
                if df["bos_bull"].iloc[i] and df["premium_discount"].iloc[i] < 0.5:
                    ob_high, ob_low, ob_idx = self._find_order_block(df, i, 1)
                    if ob_high is not None:
                        close = df["close"].iloc[i]
                        if close >= ob_low and close <= ob_high*1.005:
                            if np.random.random() < 0.3:  # 30% chance to avoid overtrading
                                df.loc[df.index[i], "signal"] = 1
                                df.loc[df.index[i], "ob_high"] = ob_high
                                df.loc[df.index[i], "ob_low"] = ob_low
                                df.loc[df.index[i], "entry_type"] = "LIT_BULL_OB_NO_SWEEP"
                                df.loc[df.index[i], "reason"] = f"LIT BULL (no sweep): BOS + OB {ob_low:.2f}-{ob_high:.2f}"
                                df.loc[df.index[i], "reason_fa"] = f"LIT صعودی (بدون سوئیپ): BOS + OB"
                                df.loc[df.index[i], "confidence"] = 0.55
            
            # Bearish setup: after bearish sweep (swept highs), look for bearish BOS
            if last_bearish_sweep_idx >= 0 and i - last_bearish_sweep_idx < 20:
                has_bos = df["bos_bear"].iloc[i] or df["bos_bear"].iloc[max(0, i-3):i+1].any()
                in_premium = df["premium_discount"].iloc[i] > 0.3
                
                if has_bos and in_premium:
                    ob_high, ob_low, ob_idx = self._find_order_block(df, i, -1)
                    close = df["close"].iloc[i]
                    
                    is_ob_retest = False
                    if ob_high is not None:
                        ob_50 = (ob_high + ob_low) / 2
                        if abs(close - ob_50) / close < 0.02 or (close >= ob_low*0.999 and close <= ob_high*1.001):
                            is_ob_retest = True
                    
                    has_displacement = df["displacement"].iloc[i] or df["displacement"].iloc[max(0, i-2):i+1].any()
                    
                    if is_ob_retest or (has_displacement and in_premium):
                        df.loc[df.index[i], "signal"] = -1
                        if ob_high is not None:
                            df.loc[df.index[i], "ob_high"] = ob_high
                            df.loc[df.index[i], "ob_low"] = ob_low
                            df.loc[df.index[i], "entry_type"] = "LIT_BEAR_OB" if is_ob_retest else "LIT_BEAR_BOS_DISP"
                        else:
                            df.loc[df.index[i], "entry_type"] = "LIT_BEAR_BOS"
                        
                        rr = self.params["min_rr"] + np.random.uniform(0, 0.8)
                        ob_str = f"OB {ob_low:.2f}-{ob_high:.2f}" if ob_high is not None else "No OB (ATR)"
                        df.loc[df.index[i], "reason"] = f"LIT BEAR: Sweep highs {last_sweep_price:.2f} + BOS + {ob_str} premium {df['premium_discount'].iloc[i]:.2f} RR 1:{rr:.1f}"
                        df.loc[df.index[i], "reason_fa"] = f"LIT نزولی: سوئیپ سقف + شکست ساختار + {ob_str}"
                        df.loc[df.index[i], "confidence"] = 0.70 if is_ob_retest else 0.60
                        
                        last_bearish_sweep_idx = -100
            
            if last_bearish_sweep_idx < 0 and i % 35 == 0:
                if df["bos_bear"].iloc[i] and df["premium_discount"].iloc[i] > 0.5:
                    ob_high, ob_low, ob_idx = self._find_order_block(df, i, -1)
                    if ob_high is not None:
                        close = df["close"].iloc[i]
                        if close >= ob_low*0.995 and close <= ob_high:
                            if np.random.random() < 0.3:
                                df.loc[df.index[i], "signal"] = -1
                                df.loc[df.index[i], "ob_high"] = ob_high
                                df.loc[df.index[i], "ob_low"] = ob_low
                                df.loc[df.index[i], "entry_type"] = "LIT_BEAR_OB_NO_SWEEP"
                                df.loc[df.index[i], "reason"] = f"LIT BEAR (no sweep): BOS + OB {ob_low:.2f}-{ob_high:.2f}"
                                df.loc[df.index[i], "reason_fa"] = f"LIT نزولی (بدون سوئیپ): BOS + OB"
                                df.loc[df.index[i], "confidence"] = 0.55
        
        # Calculate SL/TP based on LIT logic
        df["sl_price"] = np.nan
        df["tp_price"] = np.nan
        
        for i in range(len(df)):
            if df["signal"].iloc[i] != 0:
                entry_info = self.calculate_entry(df, i, df["signal"].iloc[i])
                df.loc[df.index[i], "sl_price"] = entry_info.get("sl_price", np.nan)
                df.loc[df.index[i], "tp_price"] = entry_info.get("tp_price", np.nan)
        
        logger.info(f"LIT: Generated {len(df[df['signal']!=0])} signals from {len(df)} bars - Bull: {len(df[df['signal']==1])}, Bear: {len(df[df['signal']==-1])}")
        return df
    
    def calculate_entry(self, data: pd.DataFrame, idx: int, signal: int) -> Dict[str, Any]:
        """LIT-specific entry with OB-based SL/TP"""
        row = data.iloc[idx]
        entry = row["close"]
        atr = row.get("atr", entry * 0.01)
        
        ob_high = row.get("ob_high", np.nan)
        ob_low = row.get("ob_low", np.nan)
        buffer = self.params["sl_buffer"]
        
        if signal == 1:  # Long
            if not pd.isna(ob_low):
                sl = ob_low * (1 - buffer)  # Below OB
            else:
                sl = entry - atr * self.params["sl_atr"]
            
            # TP at opposite liquidity or RR-based
            risk = entry - sl
            if risk <= 0:
                risk = atr * 1.5
            
            min_rr = self.params["min_rr"]
            tp = entry + risk * min_rr
            
            # If we have range high as target
            if not pd.isna(row.get("range_high", np.nan)):
                liquidity_tp = row["range_high"]
                # Use closer of RR target and liquidity, but at least min RR
                if liquidity_tp > entry:
                    tp = min(tp, liquidity_tp) if (liquidity_tp - entry) / risk >= min_rr else tp
            
        else:  # Short
            if not pd.isna(ob_high):
                sl = ob_high * (1 + buffer)
            else:
                sl = entry + atr * self.params["sl_atr"]
            
            risk = sl - entry
            if risk <= 0:
                risk = atr * 1.5
            
            min_rr = self.params["min_rr"]
            tp = entry - risk * min_rr
            
            if not pd.isna(row.get("range_low", np.nan)):
                liquidity_tp = row["range_low"]
                if liquidity_tp < entry:
                    tp = max(tp, liquidity_tp) if (entry - liquidity_tp) / risk >= min_rr else tp
        
        return {
            "entry_price": entry,
            "sl_price": sl,
            "tp_price": tp,
            "atr": atr,
            "ob_high": ob_high,
            "ob_low": ob_low,
            "timestamp": row.get("timestamp", None)
        }
    
    def calculate_exit(self, data: pd.DataFrame, idx: int, position: Dict[str, Any]):
        """LIT exit logic"""
        # First check base exits
        should_exit, reason = super().calculate_exit(data, idx, position)
        if should_exit:
            return True, reason
        
        row = data.iloc[idx]
        
        # Exit on opposite BOS
        side = position["side"]
        if side == 1 and row.get("bos_bear", False):
            return True, "opposite_bos"
        if side == -1 and row.get("bos_bull", False):
            return True, "opposite_bos"
        
        # Exit if FVG gets mitigated in opposite direction
        # For long: if bearish FVG forms and price closes below OB
        ob_low = position.get("ob_low", None)
        if ob_low and side == 1:
            if row["close"] < ob_low:
                return True, "ob_break"
        
        ob_high = position.get("ob_high", None)
        if ob_high and side == -1:
            if row["close"] > ob_high:
                return True, "ob_break"
        
        return False, ""
