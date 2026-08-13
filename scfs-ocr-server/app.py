import json
import os
import tempfile
from pathlib import Path

import pypdfium2 as pdfium
from flask import Flask, jsonify, request
from paddleocr import PaddleOCR

app = Flask(__name__)
ocr = PaddleOCR(
    lang=os.getenv("PADDLEOCR_LANG", "ch"),
    use_doc_orientation_classify=True,
    use_doc_unwarping=True,
    use_textline_orientation=True,
)


def normalize_result(result):
    data = result.json if hasattr(result, "json") else result
    if isinstance(data, str):
        data = json.loads(data)
    data = data.get("res", data) if isinstance(data, dict) else data
    texts = data.get("rec_texts", []) if isinstance(data, dict) else []
    scores = data.get("rec_scores", []) if isinstance(data, dict) else []
    boxes = data.get("rec_boxes", []) if isinstance(data, dict) else []
    items = []
    for index, text in enumerate(texts):
        score = float(scores[index]) if index < len(scores) else 0.0
        box = boxes[index].tolist() if index < len(boxes) and hasattr(boxes[index], "tolist") else (boxes[index] if index < len(boxes) else None)
        items.append({"text": str(text), "confidence": score, "box": box})
    return items


def recognize_image(path):
    items = []
    for result in ocr.predict(str(path)):
        items.extend(normalize_result(result))
    return items


@app.get("/health")
def health():
    return jsonify({"status": "UP", "service": "paddleocr"})


@app.post("/ocr/recognize")
def recognize():
    uploaded = request.files.get("file")
    if not uploaded:
        return jsonify({"success": False, "message": "file is required"}), 400
    suffix = Path(uploaded.filename or "material").suffix.lower()
    with tempfile.TemporaryDirectory() as folder:
        source = Path(folder) / ("source" + suffix)
        uploaded.save(source)
        items = []
        if suffix == ".pdf":
            document = pdfium.PdfDocument(str(source))
            for page_no, page in enumerate(document):
                image_path = Path(folder) / f"page-{page_no}.png"
                page.render(scale=2).to_pil().save(image_path)
                for item in recognize_image(image_path):
                    item["page"] = page_no + 1
                    items.append(item)
        else:
            items = recognize_image(source)
        confidence = sum(item["confidence"] for item in items) / len(items) if items else 0.0
        return jsonify({
            "success": bool(items),
            "fileName": uploaded.filename,
            "text": "\n".join(item["text"] for item in items),
            "confidence": confidence,
            "items": items,
            "source": "PADDLEOCR",
        })


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=9003)
