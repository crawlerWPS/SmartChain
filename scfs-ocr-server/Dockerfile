FROM python:3.12-slim

ENV PYTHONUNBUFFERED=1 PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK=True
WORKDIR /app
ARG DEBIAN_MIRROR=https://mirrors.aliyun.com/debian
RUN sed -i "s|http://deb.debian.org/debian|${DEBIAN_MIRROR}|g; s|http://deb.debian.org/debian-security|${DEBIAN_MIRROR}-security|g; s|https://deb.debian.org/debian|${DEBIAN_MIRROR}|g; s|https://deb.debian.org/debian-security|${DEBIAN_MIRROR}-security|g" /etc/apt/sources.list.d/debian.sources \
    && apt-get -o Acquire::Retries=3 -o Acquire::http::Timeout=30 -o Acquire::https::Timeout=30 update \
    && apt-get install -y --no-install-recommends libgomp1 libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir --timeout 120 --retries 5 \
    -i https://mirrors.aliyun.com/pypi/simple/ -r requirements.txt
COPY app.py .
EXPOSE 9003
CMD ["python", "app.py"]
