import json
import os
import tempfile
import base64
import threading
from pathlib import Path

import pypdfium2 as pdfium
from flask import Flask, jsonify, request
from paddleocr import PaddleOCR

app = Flask(__name__)


def create_ocr():
    # Contracts and electronic invoices are already upright documents. The
    # PaddleX orientation/unwarping classifiers consume substantial memory and
    # have proved unstable in the CPU-only container (RuntimeError: std::exception).
    return PaddleOCR(
        lang=os.getenv("PADDLEOCR_LANG", "ch"),
        use_doc_orientation_classify=False,
        use_doc_unwarping=False,
        use_textline_orientation=False,
    )


ocr = create_ocr()
ocr_lock = threading.Lock()


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
    global ocr
    items = []
    # Paddle predictor is not thread-safe. Concurrent requests can corrupt its
    # internal tensor state and fail with "Tensor holds no memory".
    with ocr_lock:
        try:
            for result in ocr.predict(str(path)):
                items.extend(normalize_result(result))
        except RuntimeError:
            # A native Paddle failure can leave predictor state unusable. Rebuild
            # it once and retry the current page instead of returning a 500.
            app.logger.warning("Paddle predictor failed; rebuilding and retrying once", exc_info=True)
            ocr = create_ocr()
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
    include_pages = request.form.get("includePages", "false").lower() == "true"
    try:
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / ("source" + suffix)
            uploaded.save(source)
            items = []
            pages = []
            if suffix == ".pdf":
                document = pdfium.PdfDocument(str(source))
                for page_no, page in enumerate(document):
                    image_path = Path(folder) / f"page-{page_no}.png"
                    # 1.5x is sufficient for field selection and uses materially less memory.
                    page_image = page.render(scale=1.5).to_pil().convert("RGB")
                    page_image.save(image_path, format="PNG", optimize=True)
                    pages.append({"page": page_no + 1, "width": page_image.width, "height": page_image.height,
                                  "image": "data:image/png;base64," + base64.b64encode(image_path.read_bytes()).decode("ascii")})
                    for item in recognize_image(image_path):
                        item["page"] = page_no + 1
                        item["imageWidth"] = page_image.width
                        item["imageHeight"] = page_image.height
                        items.append(item)
            else:
                items = recognize_image(source)
                from PIL import Image
                with Image.open(source) as image:
                    pages.append({"page": 1, "width": image.width, "height": image.height,
                                  "image": "data:image/png;base64," + base64.b64encode(source.read_bytes()).decode("ascii")})
                    for item in items:
                        item["page"] = 1
                        item["imageWidth"] = image.width
                        item["imageHeight"] = image.height
            confidence = sum(item["confidence"] for item in items) / len(items) if items else 0.0
            return jsonify({"success": bool(items), "fileName": uploaded.filename,
                            "text": "\n".join(item["text"] for item in items), "confidence": confidence,
                            "items": items, "pages": pages if include_pages else [], "source": "PADDLEOCR"})
    except Exception as error:
        app.logger.exception("OCR recognition failed")
        return jsonify({"success": False, "message": f"OCR recognition failed: {error}"}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=9003)
