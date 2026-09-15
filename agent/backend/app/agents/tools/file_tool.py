from .registry import BaseTool
from typing import Dict, Any
from ...models.schemas import RiskLevel
import os
import pathlib

class FileManagerTool(BaseTool):
    name = "file_manager"
    description = "Read and list files in safe data directory. Write requires confirmation."
    description_fa = "خواندن و لیست فایل‌ها در پوشه امن داده. نوشتن نیاز به تایید دارد."
    risk_level = RiskLevel.MEDIUM

    def __init__(self, base_path: str = "./data"):
        self.base_path = pathlib.Path(base_path).resolve()
        self.base_path.mkdir(exist_ok=True)

    async def execute(self, input_data: Dict[str, Any], context: Dict[str, Any] = None) -> Dict[str, Any]:
        operation = input_data.get("operation", "list")
        path = input_data.get("path", "")
        content = input_data.get("content", "")

        # Security: prevent path traversal
        target_path = (self.base_path / path).resolve()
        if not str(target_path).startswith(str(self.base_path)):
            return {"success": False, "error": "Path traversal blocked by zero-trust"}

        try:
            if operation == "list":
                if not target_path.exists():
                    return {"success": True, "files": [], "path": str(path)}
                
                if target_path.is_file():
                    return {"success": True, "files": [target_path.name], "is_file": True}
                
                files = [f.name for f in target_path.iterdir()]
                return {"success": True, "files": files, "path": str(path), "count": len(files)}

            elif operation == "read":
                if not target_path.exists() or not target_path.is_file():
                    return {"success": False, "error": "File not found"}
                
                # Limit file size
                if target_path.stat().st_size > 1024 * 1024:  # 1MB
                    return {"success": False, "error": "File too large (max 1MB)"}
                
                text = target_path.read_text(encoding="utf-8", errors="ignore")
                return {
                    "success": True,
                    "path": str(path),
                    "content": text[:5000],  # Limit output
                    "size": len(text)
                }

            elif operation == "write":
                # This is HIGH risk - should be gated by zero-trust
                target_path.parent.mkdir(parents=True, exist_ok=True)
                target_path.write_text(content, encoding="utf-8")
                return {
                    "success": True,
                    "path": str(path),
                    "bytes_written": len(content),
                    "requires_audit": True
                }

            else:
                return {"success": False, "error": f"Unknown operation: {operation}"}

        except Exception as e:
            return {"success": False, "error": str(e)}
