from datetime import datetime, timezone
from pathlib import Path

from flask import Flask, jsonify, request


app = Flask(__name__)
DATA_DIR = Path("/app/data")


@app.get("/health")
def health():
    return jsonify({"status": "UP", "service": "scfs-mock", "timestamp": datetime.now(timezone.utc).isoformat()})


@app.get("/")
def index():
    return jsonify({"service": "scfs-mock", "message": "Mock external data source is running"})


@app.post("/ocr/recognize")
@app.post("/api/v1/ocr/recognize")
def recognize():
    """Return deterministic OCR-shaped data for local development."""
    uploaded = request.files.get("file")
    file_name = uploaded.filename if uploaded else request.form.get("fileName", "unknown")
    return jsonify(
        {
            "success": True,
            "fileName": file_name,
            "text": "这是本地 Mock OCR 返回的示例文本。",
            "confidence": 0.99,
            "items": [],
            "source": "SCFS_MOCK_OCR",
        }
    )


if __name__ == "__main__":
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    app.run(host="0.0.0.0", port=9002)
