from .registry import BaseTool
from typing import Dict, Any
from datetime import datetime, timedelta
import jdatetime
from ...models.schemas import RiskLevel

class DateTimeTool(BaseTool):
    name = "datetime_tool"
    description = "Get current date/time, Persian Jalali date, and perform date calculations."
    description_fa = "دریافت تاریخ و زمان فعلی، تاریخ جلالی و انجام محاسبات تاریخی."
    risk_level = RiskLevel.LOW

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        operation = input_data.get("operation", "now")
        now = datetime.now()
        jalali = jdatetime.datetime.fromgregorian(datetime=now)

        if operation == "now":
            return {
                "success": True,
                "gregorian": now.isoformat(),
                "jalali": jalali.strftime("%Y/%m/%d %H:%M"),
                "jalali_fa": f"{jalali.year}/{jalali.month}/{jalali.day}",
                "weekday_en": now.strftime("%A"),
                "weekday_fa": self._weekday_fa(now.weekday()),
                "timestamp": int(now.timestamp()),
                "timezone": "Asia/Tehran"
            }
        
        elif operation == "add_days":
            days = int(input_data.get("days", 0))
            future = now + timedelta(days=days)
            future_jalali = jdatetime.datetime.fromgregorian(datetime=future)
            return {
                "success": True,
                "original": now.isoformat(),
                "result": future.isoformat(),
                "result_jalali": future_jalali.strftime("%Y/%m/%d"),
                "days_added": days
            }
        
        elif operation == "diff":
            date_str = input_data.get("date", "")
            try:
                target = datetime.fromisoformat(date_str)
                diff = target - now
                return {
                    "success": True,
                    "diff_days": diff.days,
                    "diff_seconds": diff.total_seconds(),
                    "is_past": diff.total_seconds() < 0
                }
            except Exception as e:
                return {"success": False, "error": f"Invalid date format: {e}"}
        
        else:
            return {"success": False, "error": f"Unknown operation: {operation}"}

    def _weekday_fa(self, weekday: int) -> str:
        days = ["دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه", "یک‌شنبه"]
        return days[weekday]
